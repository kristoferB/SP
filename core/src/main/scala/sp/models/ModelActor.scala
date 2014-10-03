package sp.models

import akka.actor._
import sp.domain._
import sp.system.messages._
import akka.persistence._

/**
 * Created by Kristofer on 2014-06-12.
 */
class ModelActor(name: String, attr: SPAttributes) extends EventsourcedProcessor  {

  // A model state
  case class ModelState(version: Long, idMap: Map[ID, IDAble], diff: Map[Long, ModelDiff], attributes: SPAttributes){
    lazy val operations = idMap filter (_._2.isInstanceOf[Operation])
    lazy val things = idMap filter (_._2.isInstanceOf[Thing])
    lazy val specifications = idMap filter (_._2.isInstanceOf[Specification])
    lazy val results = idMap filter (_._2.isInstanceOf[Result])
    lazy val stateVariables = svs map (sv=> sv.id -> sv) toMap
    lazy val svs =  things flatMap(_._2.asInstanceOf[Thing].stateVariables)
  }

  var state = ModelState(1, Map(), Map(), attr)


  def receiveCommand = {
    case UpdateIDs(m, ids) => {
      val reply = sender
      createDiffUpd(m, ids) match {
        case Right(diff) => {
          persist(diff)(d =>{
            updateState(d)
            reply ! SPIDs(diff.updatedItems)
          })
        }
        case Left(error) => reply ! error
      }
    }
    case DeleteIDs(m, dels) => {
      val reply = sender
      createDiffDel(m, dels.toSet) match {
        case Right(diff) => {
          persist(diff)(d =>{
            updateState(d)
            reply ! SPIDs(diff.deletedItems)
          })
        }
        case Left(error) => reply ! error
      }
    }

    case UpdateModelInfo(m, newName, attribute) => {
      val reply = sender
      val diff = ModelDiff(List(), List(), m, state.version, state.version + 1, (attribute + ("time", DatePrimitive.now)))

      persist(diff)( d => {
          updateState(d)
          reply ! ModelInfo(name, state.version, state.attributes)
      })
    }

    /**
     * TODO: This is a temporary solution. When we go more production
     * the Query should be in a separate actor. 140630
     */
    case mess: ModelQuery => {
      val reply = sender
      mess match {
        case GetIds(ids, m) => {
          if (ids.isEmpty) reply ! SPIDs(state.idMap.values.toList)
          else {
            ids foreach(id=> if (!state.idMap.contains(id)) reply ! MissingID(id, m))
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
        case GetStateVariable(m, id) => {
          if (!state.stateVariables.contains(id)) reply ! MissingID(id, m)
          else {
            val res = state.stateVariables(id)
            reply ! SPSVs(List(res))
          }
        }
        case GetStateVariables(m, f) => {
          val res = state.stateVariables.values.toList filter f
          reply ! SPSVs(res)
        }

        case GetQuery(_, q, f) => {
          if (!q.isEmpty)
            println("Query STRING NOT IMPLEMENTED ModelActor")

          val res = state.idMap.values filter f
          reply ! SPIDs(res.toList)
        }
        case GetDiff(_,v) => reply ! getDiff(v)
        case x: GetModelInfo => reply ! ModelInfo(name, state.version, state.attributes)
      }
    }
    case "printState" => println(s"$name: $state")
    case "snapshot" => saveSnapshot(state)
    case GetModels => sender ! ModelInfo(name, state.version, state.attributes)
  }

  def receiveRecover = {
    case d: ModelDiff  => updateState(d)
    case SnapshotOffer(_, snapshot: ModelState) => state = snapshot
  }

  /**
   * Checks if the model can be updated. If the version defined in UpdateID
   * is the same as in the model for all items, the model is updated. Else
   * the method returns a UpdateError
   * @param model The name of the model
   * @param ids The new items to be updated or added
   * @return Either Right[ModelDiff] -> The model can be updated. Left[UpdateError]
   */
  def createDiffUpd(model: String, ids: List[UpdateID]): Either[UpdateError, ModelDiff] = {

    // Check if any item could not be updated and divide them
    val updateMe = ids partition {case UpdateID(id, v, item) => {
        val current = state.idMap.getOrElse(id, null)
        // TODO: also need to check so that the classes match. Impl when everything is working. 140627
        current == null || current.version <= v
      }
    }

    if (updateMe._2.isEmpty) {
      val upd = updateMe._1 map (uid=> uid.item.update(uid.id, uid.version))
      Right(ModelDiff(upd, List(), model, state.version, state.version + 1, SPAttributes(state.attributes.attrs + ("time" -> DatePrimitive.now))))

    } else {
      Left(UpdateError(state.version, updateMe._2 map(_.id)))
    }
  }

  /**
   * Checks if the model can be updated by deleting.
   * @param model The name of the model
   * @param delete The ids to be deleted
   * @return Either Right[ModelDiff] -> The model can be updated. Left[UpdateError]
   */
  def createDiffDel(model: String, delete: Set[ID]): Either[UpdateError, ModelDiff] = {
    val upds = updateItemsDueToDelete(delete)
    val upd = upds map (uid=> uid.item.update(uid.id, uid.version))
    val del = (state.idMap filter( kv =>  delete.contains(kv._1))).values
    if (delete.nonEmpty && del.isEmpty) Left(UpdateError(state.version, delete.toList))
    else {
      Right(ModelDiff(upd, del.toList, model, state.version, state.version + 1, SPAttributes(state.attributes.attrs + ("time" -> DatePrimitive.now))))

    }
  }

  def updateState(diff: ModelDiff) = {
    val diffMap = state.diff + (diff.currentVersion -> diff) filter(_._1 > state.version - 30)
    val idm = diff.updatedItems.map(x=> x.id -> x).toMap
    val allItems = (state.idMap ++ idm) filter(kv => !diff.deletedItems.contains(kv._2))
    state = ModelState(state.version+1, allItems, diffMap, diff.attributes)
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
    ModelDiff(allDiffs, allDels, name, fromV, state.version)
  }

  def updateItemsDueToDelete(dels: Set[ID]): List[UpdateID] = {
    val items = state.idMap.filterKeys(k => !dels.contains(k)).values
    println("deleting")
    println(s"DELS: $dels")


    val t = sp.domain.logic.IDAbleLogic.removeID(dels, items.toList)
    println(s"need update: $t")
    t
  }

  // When we need more things here, let us move this to another actor
  def query(mess: ModelQuery) = {

  }

}

object ModelActor{
  def props(name: String, attr: SPAttributes) = Props(classOf[ModelActor], name, attr)
}