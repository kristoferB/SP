package sp.models

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration._
import sp.domain._
import sp.domain.Logic._
import akka.persistence._

import scala.util.{Failure, Success, Try}
import sp.models.{APIModel => api}


object ModelActor {
  def props(cm: APIModelMaker.CreateModel) = Props(classOf[ModelActor], cm)
}

class ModelActor(val modelSetup: APIModelMaker.CreateModel) extends PersistentActor with ModelLogic with ActorLogging  {
  val id: ID = modelSetup.id
  override def persistenceId = id.toString

  import akka.cluster.pubsub._
  import DistributedPubSubMediator.{ Publish, Subscribe }
  val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe(APISP.services, self)

  sendEvent(SPHeader(from = id.toString), getModelInfo)


  def receiveCommand = {
    case x: String if sender() != self =>
      val mess = SPMessage.fromJson(x)

      for {
        m <- mess
        h <- m.getHeaderAs[SPHeader] if h.to == modelSetup.id.toString || h.to == api.service
        b <-   m.getBodyAs[api.Request]
      } yield {
        val updH = h.copy(from = id.toString, to = h.from)
        sendAnswer(updH, APISP.SPACK())

        b match {
          case k: api.PutItems =>
            val res = putItems(k.items, k.info)
            handleModelDiff(res, updH)
          case k: api.DeleteItems =>
            val res = deleteItems(k.items, k.info)
            handleModelDiff(res, updH)
          case k: api.UpdateModelAttributes =>
            val res = updateAttributes(k.name, k.attributes)
            handleModelDiff(res, updH)
          case k: api.RevertModel =>
          case api.GetModel =>
            sendAnswer(updH, getTheModel)
          case api.GetModelInfo =>
            sendAnswer(updH, getModelInfo)
          case api.GetModelHistory =>
            val res = getModelHistory
            sendAnswer(updH, getModelHistory)
          case api.GetItems =>
            sendAnswer(updH, api.SPItems(state.items))
          case api.GetItem(itemID) =>
            state.idMap.get(itemID) match {
              case Some(r) => sendAnswer(updH, api.SPItem(r))
              case None => sendAnswer(updH, APISP.SPError(s"item $itemID does not exist"))
            }
          case api.GetItemsInList(xs) =>
            val res = xs.flatMap(state.idMap.get)
            sendAnswer(updH, api.SPItems(res))
          case api.GetStructures   =>
            val res = state.items.filter(_.isInstanceOf[Struct])
            sendAnswer(updH, api.SPItems(res))
          case k =>
        }

          sendAnswer(updH, APISP.SPDone())

      }


      ModelsComm.extractAPISP(mess).collect{
        case (h, APISP.StatusRequest) =>
          val updH = h.copy(from = api.service, to = h.from)
          val resp = ModelInfo.attributes.copy(
            service = id.toString,
            instanceID = Some(id),
            attributes = SPAttributes("modelInfo" -> getModelInfo)
          )

          mediator ! Publish(APISP.spevents, SPMessage.makeJson(updH, resp))

      }

  }

  override def receiveRecover: Receive = {
    case x: String =>
      val diff = SPAttributes.fromJsonGetAs[ModelDiff](x)
      diff.foreach(updateState)
  }

  def handleModelDiff(d: Option[ModelDiff], h: SPHeader) = {
    d.foreach{diff =>
      persist(SPValue(diff).toJson){json =>
        val res = makeModelUpdate(diff)
        sendEvent(h, res)
      }
    }
  }

  def sendAnswer(h: SPHeader, b: APISP) = mediator ! Publish(APISP.answers, SPMessage.makeJson(h, b))
  def sendAnswer(h: SPHeader, b: api.Response) = mediator ! Publish(APISP.answers, SPMessage.makeJson(h, b))
  def sendEvent(h: SPHeader, b: api.Response) = mediator ! Publish(APISP.spevents, SPMessage.makeJson(h.copy(to = ""), b))
}


trait ModelLogic {
  val id: ID

  case class ModelState(version: Int, idMap: Map[ID, IDAble], history: Map[Int, SPAttributes], attributes: SPAttributes, name: String){
    lazy val items = idMap.values.toList
  }

  case class ModelDiff(model: ID,
                       updatedItems: List[IDAble],
                       deletedItems: List[IDAble],
                       diffInfo: SPAttributes,
                       fromVersion: Long,
                       name: String,
                       modelAttr: SPAttributes = SPAttributes().addTimeStamp
                      )

  object ModelDiff {
    implicit lazy val fModelDiff: JSFormat[ModelDiff] = deriveFormatSimple[ModelDiff]
  }

  var state = ModelState(0, Map(), Map(), SPAttributes(), "noName")


  def putItems(items: List[IDAble], info: SPAttributes) = {
    createDiffUpd(items, info)
  }

  def deleteItems(items: List[ID], info: SPAttributes) = {
    createDiffDel(items.toSet, info)
  }

  def updateAttributes(name: Option[String], attr: Option[SPAttributes]) = {
    val uN = name.getOrElse(state.name)
    val uA = attr.getOrElse(state.attributes)
    if (uN != state.name && uA != state.attributes) None
    else Some(ModelDiff(
      model = id,
      updatedItems = List(),
      deletedItems = List(),
      diffInfo = SPAttributes("info"->s"model attributes updated"),
      fromVersion = state.version,
      name = uN,
      modelAttr = uA))
  }

  def getTheModel = api.TheModel(state.name, id, state.version, state.attributes, state.items)
  def getModelInfo = api.ModelInformation(state.name, id, state.version, state.attributes)
  def getModelHistory = api.ModelHistory(id, state.history.toList.sortWith(_._1 > _._1))


  def makeModelUpdate(diff: ModelDiff) = {
    updateState(diff)
    api.ModelUpdate(id, state.version, diff.updatedItems, diff.deletedItems.map(_.id), diff.diffInfo)
  }


    def createDiffUpd(ids: List[IDAble], info: SPAttributes): Option[ModelDiff] = {
      val upd = ids.flatMap{i =>
        val xs = state.idMap.get(i.id)
        if (!xs.contains(i)) Some(i)
        else None
      }
      if (upd.isEmpty ) None
      else {
        val updInfo = if (info.values.isEmpty) SPAttributes("info"->s"updated: ${upd.map(_.name).mkString(",")}") else info
        Some(ModelDiff(id,
          upd,
          List(),
          updInfo,
          state.version,
          state.name,
          state.attributes.addTimeStamp))
      }
    }

    def createDiffDel(delete: Set[ID], info: SPAttributes): Option[ModelDiff] = {
      val upd = updateItemsDueToDelete(delete)
      val modelAttr = sp.domain.logic.IDAbleLogic.removeIDFromAttribute(delete, state.attributes)
      val del = (state.idMap.filter( kv =>  delete.contains(kv._1))).values
      if (delete.nonEmpty && del.isEmpty) None
      else {
        val updInfo = if (info.values.isEmpty) SPAttributes("info"->s"deleted: ${del.map(_.name).mkString(",")}") else info
        Some(ModelDiff(id, upd, del.toList, updInfo, state.version, state.name, modelAttr.addTimeStamp))
      }
    }

    def updateState(diff: ModelDiff) = {
      if (state.version != diff.fromVersion)
        println(s"MODEL DIFF UPDATE is not in phase: diffState: ${diff.fromVersion}, current: ${state.version}")

      val version = state.version + 1
      val diffMap = state.history + (version -> diff.diffInfo)
      val idm = diff.updatedItems.map(x=> x.id -> x).toMap
      val dels = diff.deletedItems.map(_.id).toSet
      val allItems = (state.idMap ++ idm) filterKeys(id => !dels.contains(id))
      state = ModelState(version, allItems, diffMap, diff.modelAttr, diff.name)
    }


    def updateItemsDueToDelete(dels: Set[ID]): List[IDAble] = {
      val items = state.idMap.filterKeys(k => !dels.contains(k)).values
      sp.domain.logic.IDAbleLogic.removeID(dels, items.toList)
    }


}




