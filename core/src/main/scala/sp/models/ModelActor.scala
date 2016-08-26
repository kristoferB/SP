package sp.models

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import sp.domain._
import sp.system.messages._
import sp.domain.LogicNoImplicit._
import sp.system.messages.JsonFormatsMessage._
import akka.persistence._
import org.json4s.native.Serialization._

/**
 * Created by Kristofer on 2014-06-12.
 */
class ModelActor(val model: ID) extends PersistentActor with ModelActorState  {
  override def persistenceId = model.toString()
  implicit val timeout = Timeout(2 seconds)
  import context.dispatcher

  // temp
  val eventHandler = context.actorOf(sp.system.EventHandler.props)

  def receiveCommand = {
    //case mess @ _ if {println(s"model got: $mess from $sender"); false} => Unit
    case upd @ UpdateIDs(m, ids, info) =>
      val reply = sender
      createDiffUpd(ids, info) match {
        case Right(diff) =>
          store(diff, {
            reply ! diff
          })
        case Left(error) => reply ! error
      }

    case DeleteIDs(m, dels, info) =>
      val reply = sender
      createDiffDel(dels.toSet, info) match {
        case Right(diff) =>
          reply ! "OK"
          store(diff, reply ! SPOK)
        case Left(error) => reply ! error
      }

    case cm: CreateModel =>
      val diff = ModelDiff(model, List(), List(), SPAttributes("info"->"new model attributes"), state.version, state.version + 1, cm.name, cm.attributes.addTimeStamp)
      val reply = sender
      store(diff, reply ! SPOK)
      println(s"The modelHandler creates a new model called ${cm.name} id: ${cm.id}")

    case UpdateModelInfo(_, ModelInfo(m, newName, v, attribute, _)) =>
      val reply = sender
      val diff = ModelDiff(
        model,
        List(),
        List(),
        SPAttributes("info"->"new model attributes"),
        state.version,
        state.version + 1,
        newName,
        attribute.addTimeStamp)

      store(diff, reply ! SPOK)

    case Revert(_, v) =>
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
          SPAttributes("info"->s"reverted back to version $v"),
          state.version,
          state.version + 1,
          info.name,
          info.attributes.addTimeStamp
        )

        println("model revert diff upd: "+ diff.updatedItems.map(_.name))
        println("model revert diff del: "+ diff.deletedItems.map(_.name))

        self ! (diff, reply)
      }

    case (diff: ModelDiff, reply: ActorRef) =>
      store(diff, reply ! getModelInfo)


    /**
     * TODO: This is a temporary solution. When we go more production
     * the Query should be in a separate actor. 140630
     * Query handled in trait below
     */
    case mess: ModelQuery =>
      queryMessage(sender, mess)

    case "printState" => println(s"$model: $state")
    case "snapshot" => saveSnapshot(state)
    case GetModels => sender ! getModelInfo
    case ExportModel(id) => {
      val mi = getModelInfo.copy(history = List())
      val res = ImportModel(model, mi, state.idMap.values.toList, List())
      println(s"export")
      sender() ! res
    }
    case ImportModel(id, mi, ids, h) => {
      val reply = sender()
      val diffUpd = createDiffUpd(ids, SPAttributes("info"->"Model imported"), true)
      val idsKeys = ids.map(_.id).toSet
      val dels = state.idMap.filterKeys(id => !idsKeys.contains(id))
      println(s"import")
      diffUpd.left.map(err => reply ! err)
      diffUpd.right.map{diff =>
        store(diff.copy(deletedItems = dels.values.toList, name = mi.name, modelAttr = mi.attributes), reply ! SPOK)
      }
    }

  }


  def store(diff: ModelDiff, after: => Unit = Unit) = {
    val json = write(diff)
    persist(json){ d =>
      updateState(diff)
      after
      eventHandler ! diff
      eventHandler ! getModelInfo
    }
  }

}

object ModelActor{
  def props(model: ID) = Props(classOf[ModelActor], model)
}

trait ModelActorState  {
  val model: ID
  //def persist[A](event: A)(handler: A ⇒ Unit)

  private val noDiffInMemory = 50

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
      case GetIds(_, ids) =>
        if (ids.isEmpty) reply ! SPIDs(state.idMap.values.toList)
        else {
          ids foreach(id=> if (!state.idMap.contains(id)) reply ! SPError(s"Model ${state.name} does not contain id: $id"))
          val res = for {
            i <- ids
            x <- state.idMap.get(i)
          } yield x
          reply ! SPIDs(res)
        }
      case GetOperations(_, f) =>
        val res = state.operations.values filter f
        reply ! SPIDs(res.toList)
      case GetThings(_, f) =>
        val res = state.things.values filter f
        reply ! SPIDs(res.toList)
      case GetSpecs(_, f) =>
        val res = state.specifications.values filter f
        reply ! SPIDs(res.toList)
      case GetResults(_, f) =>
        val res = state.results.values filter f
        reply ! SPIDs(res.toList)
      case GetQuery(_, q, f) =>
        if (!q.isEmpty)
          println("Query STRING NOT IMPLEMENTED ModelActor")

        val res = state.idMap.values filter f
        reply ! SPIDs(res.toList)

      case GetDiffFrom(_,v) => reply ! getDiff(v)
      case GetDiff(_,v) =>
        if (state.diff.contains(v))
          reply ! state.diff(v)
        else
          reply ! SPError(s"The model only stores $noDiffInMemory in memory. Use the view instead")
      case x: GetModelInfo => reply ! getModelInfo
    }
  }


  def createDiffUpd(ids: List[IDAble], info: SPAttributes, allowNoChanges: Boolean = false): Either[SPError, ModelDiff] = {
    val upd = ids filter(!state.items.contains(_))
    if (upd.isEmpty && !allowNoChanges) Left(SPError("No changes identified"))
    else {
      val updInfo = if (info.obj.isEmpty) SPAttributes("info"->s"updated: ${ids.map(_.name).mkString(",")}") else info
      Right(ModelDiff(model,
        upd,
        List(),
        updInfo,
        state.version,
        state.version + 1,
        state.name,
        state.attributes.addTimeStamp))
    }
  }

  def createDiffDel(delete: Set[ID], info: SPAttributes): Either[SPError, ModelDiff] = {
    val upd = updateItemsDueToDelete(delete)
    val modelAttr = sp.domain.logic.IDAbleLogic.removeIDFromAttribute(delete, state.attributes)
    val del = (state.idMap filter( kv =>  delete.contains(kv._1))).values
    if (delete.nonEmpty && del.isEmpty) Left(UpdateError(state.version, delete.toList))
    else {
      val updInfo = if (info.obj.isEmpty) SPAttributes("info"->s"deleted: ${del.map(_.name).mkString(",")}") else info
      Right(ModelDiff(model, upd, del.toList, updInfo, state.version,state.version + 1, state.name, modelAttr.addTimeStamp))
    }
  }

  def updateState(diff: ModelDiff) = {
    val diffMap = state.diff + (diff.version -> diff) filter(_._1 > state.version - noDiffInMemory)
    val idm = diff.updatedItems.map(x=> x.id -> x).toMap
    val dels = diff.deletedItems.map(_.id).toSet
    val allItems = (state.idMap ++ idm) filterKeys(id => !dels.contains(id))
    state = ModelState(state.version+1, allItems, diffMap, diff.modelAttr, diff.name)
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
    val allDiffInfo = state.diff.filter(_._1 > fromV).foldLeft(SPAttributes())((res,md)=>{
      res + (md._1.toString -> md._2.diffInfo)
    })

    ModelDiff(model, allDiffs, allDels,allDiffInfo, fromV, state.version, state.name, state.attributes)
  }

  def getHistory = {
    val sorted = state.diff.toList.sortWith(_._1 > _._1)
    sorted.map{case (v, d) => SPAttributes("version"->v, "diff"->d)}
  }

  def updateItemsDueToDelete(dels: Set[ID]): List[IDAble] = {
    val items = state.idMap.filterKeys(k => !dels.contains(k)).values
    sp.domain.logic.IDAbleLogic.removeID(dels, items.toList)
  }

  // When we need more things here, let us move this to another actor
  def query(mess: ModelQuery) = {

  }

  def getModelInfo = ModelInfo(model, state.name, state.version, state.attributes, getHistory)




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
    case RecoveryCompleted => println(s"The model called ${getModelInfo.name} id: ${getModelInfo.id}, has recovered")
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

  override def recovery = Recovery(toSequenceNr = version)

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