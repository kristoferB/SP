package sp.models

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import sp.domain._
import sp.system.messages._
import sp.domain.Logic._
import akka.persistence._
import org.json4s.native.Serialization._

/**
 * Created by Kristofer on 2014-06-12.
 */
class ModelActor(val model: ID) extends PersistentActor with ModelActorState  {
  override def persistenceId = model.toString()
  implicit val timeout = Timeout(2 seconds)
  import context.dispatcher

  def receiveCommand = {
    //case mess @ _ if {println(s"model got: $mess from $sender"); false} => Unit
    case upd @ UpdateIDs(m, v, ids) => {
      //println(s"update me: $upd")
      val reply = sender
      createDiffUpd(ids, v) match {
        case Right(diff) => store(diff, reply ! SPIDs(ids))
        case Left(error) => reply ! error
      }
    }
    case DeleteIDs(m, dels) => {
      val reply = sender
      createDiffDel(dels.toSet) match {
        case Right(diff) => store(diff, reply ! SPIDs(diff.deletedItems))
        case Left(error) => reply ! error
      }
    }

    case UpdateModelInfo(_, ModelInfo(m, newName, v, attribute)) => {
      val reply = sender
      val diff = ModelDiff(model, List(), List(), state.version, state.version + 1, newName, attribute.addTimeStamp)
      store(diff, reply ! getModelInfo)
    }

    case Revert(_, v) => {
      val reply = sender
      val view = context.actorOf(sp.models.ModelView.props(model, v, "modelReverter"))
      val infoF = view ? GetModels
      val itemsF = view ? GetIds(model, List())
      for {
        info <- infoF.mapTo[ModelInfo]
        items <- itemsF.mapTo[SPIDs]
      } yield {
        val itemMap = items.items.map(x=> x.id -> x) toMap
        val upd = itemMap.filter{case (id, x) =>
          !state.idMap.contains(id) || state.idMap(id) != x
        }
        val del = state.idMap.filter{case (id, x) =>
          !itemMap.contains(id)
        }
        val diff = ModelDiff(
          model,
          upd.values.toList,
          del.values.toList,
          state.version,
          state.version + 1,
          info.name,
          info.attributes.addTimeStamp
        )
        self ! (diff, reply)
      }
    }

    case (diff: ModelDiff, reply: ActorRef) => {
      store(diff, reply ! getModelInfo)
    }

    /**
     * TODO: This is a temporary solution. When we go more production
     * the Query should be in a separate actor. 140630
     * Query handled in trait below
     */
    case mess: ModelQuery => {
      queryMessage(sender, mess)
    }
    case "printState" => println(s"$model: $state")
    case "snapshot" => saveSnapshot(state)
    case GetModels => sender ! getModelInfo
  }


  def store(diff: ModelDiff, after: => Unit) = {
    val json = write(diff)
    persist(json){ d =>
      updateState(diff)
      after
    }
  }

}

object ModelActor{
  def props(model: ID) = Props(classOf[ModelActor], model)
}

trait ModelActorState  {
  val model: ID
  //def persist[A](event: A)(handler: A ⇒ Unit)

  private val noDiffInMemory = 50;

  // A model state
  case class ModelState(version: Long, idMap: Map[ID, IDAble], diff: Map[Long, ModelDiff], attributes: SPAttributes, name: String){
    lazy val operations = idMap filter (_._2.isInstanceOf[Operation])
    lazy val things = idMap filter (_._2.isInstanceOf[Thing])
    lazy val specifications = idMap filter (_._2.isInstanceOf[Specification])
    lazy val results = idMap filter (_._2.isInstanceOf[Result])
    lazy val items = idMap.values.toSet
  }

  var state = ModelState(0, Map(), Map(), SPAttributes(), "noName")


  def queryMessage(reply: ActorRef, mess: ModelQuery) = {
    mess match {
      case GetIds(_, ids) => {
        if (ids.isEmpty) reply ! SPIDs(state.idMap.values.toList)
        else {
          ids foreach(id=> if (!state.idMap.contains(id)) reply ! MissingID(id, model))
          val res = for {
            i <- ids
            x <- state.idMap.get(i)
          } yield x
          reply ! SPIDs(res)
        }
      }
      case GetOperations(_, f) => {
        val res = state.operations.values filter f
        reply ! SPIDs(res.toList)
      }
      case GetThings(_, f) => {
        val res = state.things.values filter f
        reply ! SPIDs(res.toList)
      }
      case GetSpecs(_, f) => {
        val res = state.specifications.values filter f
        reply ! SPIDs(res.toList)
      }
      case GetResults(_, f) => {
        val res = state.results.values filter f
        reply ! SPIDs(res.toList)
      }
      case GetQuery(_, q, f) => {
        if (!q.isEmpty)
          println("Query STRING NOT IMPLEMENTED ModelActor")

        val res = state.idMap.values filter f
        reply ! SPIDs(res.toList)
      }

      case GetDiffFrom(_,v) => reply ! getDiff(v)
      case GetDiff(_,v) => {
        if (state.diff.contains(v))
          reply ! state.diff(v)
        else
          reply ! SPError(s"The model only stores $noDiffInMemory in memory. Use the view instead")
      }
      case x: GetModelInfo => reply ! getModelInfo
    }
  }


  /**
   * Checks if the model can be updated. If the version defined in UpdateID
   * is the same as in the model for all items, the model is updated. Else
   * the method returns a UpdateError
   * @param ids The new items to be updated or added
   * @return Either Right[ModelDiff] -> The model can be updated. Left[UpdateError]
   */
  def createDiffUpd(ids: List[IDAble], modelVersion: Long): Either[SPError, ModelDiff] = {

    val conflicts = List() //if (modelVersion < state.version || modelVersion != -1) {
//      val diff = getDiff(modelVersion)
//      val changedIds = diff.updatedItems map(_.id)
//      ids.map(_.id).filter(changedIds.contains)
//    } else List()

    if (conflicts.isEmpty) {
      val upd = ids filter(!state.items.contains(_))
      if (upd.isEmpty) Left(SPError("No changes identified"))
      else {
        Right(ModelDiff(model,
          upd,
          List(),
          state.version,
          state.version + 1,
          state.name,
          state.attributes.addTimeStamp))
      }
    } else {
      Left(UpdateError(state.version, conflicts))
    }
  }

  /**
   * Checks if the model can be updated by deleting.
   * @param delete The ids to be deleted
   * @return Either Right[ModelDiff] -> The model can be updated. Left[UpdateError]
   */
  def createDiffDel(delete: Set[ID]): Either[SPError, ModelDiff] = {
    val upd = updateItemsDueToDelete(delete)
    val modelAttr = sp.domain.logic.IDAbleLogic.removeIDFromAttribute(delete, state.attributes)
    val del = (state.idMap filter( kv =>  delete.contains(kv._1))).values
    if (delete.nonEmpty && del.isEmpty) Left(UpdateError(state.version, delete.toList))
    else {
      Right(ModelDiff(model, upd, del.toList, state.version, state.version + 1, state.name, modelAttr.addTimeStamp))
    }
  }

  def updateState(diff: ModelDiff) = {
    val diffMap = state.diff + (diff.currentVersion -> diff) filter(_._1 > state.version - noDiffInMemory)
    val idm = diff.updatedItems.map(x=> x.id -> x).toMap
    val allItems = (state.idMap ++ idm) filter(kv => !diff.deletedItems.contains(kv._2))
    state = ModelState(state.version+1, allItems, diffMap, diff.attributes, diff.name)
  }

  /**
   * Returns all items that have been change since version fromV. Does not include
   * the changes made in that version
   * @param fromV From what version to return diffs
   * @return The ModelDiff
   */
  def getDiff(fromV: Long) = {
    val allDiffs = state.diff.filter(_._1 > fromV).foldLeft(List[IDAble]())((res,md)=>{
      md._2.updatedItems ++ res
    })
    val allDels = state.diff.filter(_._1 > fromV).foldLeft(List[IDAble]())((res,md)=>{
      md._2.deletedItems ++ res
    })
    ModelDiff(model, allDiffs, allDels, fromV, state.version, state.name, state.attributes)
  }

  def updateItemsDueToDelete(dels: Set[ID]): List[IDAble] = {
    val items = state.idMap.filterKeys(k => !dels.contains(k)).values
    sp.domain.logic.IDAbleLogic.removeID(dels, items.toList)
  }

  // When we need more things here, let us move this to another actor
  def query(mess: ModelQuery) = {

  }

  def getModelInfo = ModelInfo(model, state.name, state.version, state.attributes)




  def receiveRecover: Actor.Receive = {
    case json: String => {
      tryWithOption(read[ModelDiff](json)) match {
        case Some(diff) => updateState(diff)
        case None => println(s"Couldn't convert json to modeldiff: $json")
      }
    }
    case d: ModelDiff  => {
      updateState(d)
    }
    case SnapshotOffer(_, snapshot: ModelState) => state = snapshot
  }

  def tryWithOption[T](t: => T): Option[T] = {
    try {
      Some(t)
    } catch {
      case e: Exception => None
    }
  }


}




class ModelView(val model: ID, version: Long, name: String) extends PersistentView with ModelActorState {
  override def persistenceId: String = model.toString()
  override def viewId: String = ID.newID.toString()

  override def preStart() {
    self ! Recover(toSequenceNr = version)
  }

  override def autoUpdate = false

  def receive = {
    case mess: ModelQuery => {
      queryMessage(sender, mess)
    }
    case GetModels => {
      sender ! getModelInfo
    }
    case m: ModelUpdate => sender ! SPError("You are in view mode and can not change. Switch to a model")
    case x @ _ => receiveRecover(x)

  }

  def fixModelName = {
    if (name != "modelReverter")
      state = state.copy(name = name)
  }
}

object ModelView {
  def props(model: ID, version: Long, name: String) =
    Props(classOf[ModelView], model, version, name)
}