package sp.models

import akka.actor._
import sp.domain._
import sp.system._
import akka.persistence._

/**
 * Created by Kristofer on 2014-06-12.
 */
class ModelActor(name: String) extends EventsourcedProcessor  {

  // A model state
  case class ModelState(version: Long, idMap: Map[ID, IDAble], diff: List[ModelDiff]){
    lazy val operations = idMap filter (_._2.isInstanceOf[Operation])
    lazy val things = idMap filter (_._2.isInstanceOf[Thing])
    lazy val specifications = idMap filter (_._2.isInstanceOf[Specification])
  }

  var state = ModelState(1, Map(),List())


  def receiveCommand = {
    case UpdateIDs(m,v,ids) => {
      val reply = sender
      createDiff(m, v, ids) match {
        case Left(diff) => {
          persist(diff)(d =>{
            updateState(d)
            if (state.version == v+1)
              reply ! diff
            else {
              reply ! getDiff(v)
            }
          })
        }
        case Right(error) => reply ! error
      }
      //persist(Evt(s"${data}-${numEvents}"))(updateState)
    }
    case mess: ModelQuery => {
      val reply = sender
      mess match {
        case GetIds(ids, m) => {

          val res = for {
            i <- ids
            x <- state.idMap.get(i)
          } yield x
          println(s"getIDs: $ids result: $res")
          reply ! SPIDs(name, state.version, res)
        }
        case get: GetOperations => {
          val res = state.operations.values
          reply ! SPIDs(name, state.version, res.toList)
        }
        case get: GetThings => {
          val res = state.things.values
          reply ! SPIDs(name, state.version, res.toList)
        }
        case get: GetSpecs => {
          val res = state.things.values
          reply ! SPIDs(name, state.version, res.toList)
        }
        case GetQuery(attr, _) => {
          println("GETQUERY NOT IMPLEMENTED in ModelActor")
        }
        case GetDiff(_,v) => reply ! getDiff(v)
      }
    }
    case "printState" => println(s"$name: $state")
    case GetModels => sender ! ModelInfo(name, state.version)
  }

  def receiveRecover = {
    case d: ModelDiff  => updateState(d)
    case SnapshotOffer(_, snapshot: ModelState) => state = snapshot
  }

  def createDiff(model: String, modelVersion: Long, ids: List[UpdateID]): Either[ModelDiff, UpdateError] = {
    // Check if any item could not be updated and devide them
    val updateMe = ids partition {case UpdateID(id,v, item) => {
        val current = state.idMap.getOrElse(id, null)
        current == null || current.version <= v
      }
    }
    if (updateMe._2.isEmpty) {
      val upd = updateMe._1 map (uid=> uid.updated.update(uid.id, uid.version))
      Left(ModelDiff(upd, model, modelVersion, state.version+1))
    } else {
      Right(UpdateError(modelVersion, state.version, updateMe._2 map(_.id)))
    }
  }

  def updateState(diff: ModelDiff) = {
    val modelDiff = diff :: state.diff take(30)
    val idm = diff.ids.map(x=> x.id -> x).toMap
    state = ModelState(state.version+1, state.idMap ++ idm, modelDiff)
  }

  def getDiff(fromV: Long) = {
    val allDiffs = state.diff.filter(_.version > fromV).foldLeft(List[IDAble]())((res,md)=>{
       md.ids ++ res
    })
    ModelDiff(allDiffs, name, fromV, state.version)
  }

  // When we need more things here, let us move this to another actor
  def query(mess: ModelQuery) = {

  }

}

object ModelActor{
  def props(name: String) = Props(classOf[ModelActor], name)
}