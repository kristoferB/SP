package sp.models

import akka.actor._

import sp.domain._
import sp.domain.Logic._
import sp.service._
import akka.persistence._
import sp.models.APIModel.SPItems


object ModelActor {
  def props(cm: APIModelMaker.CreateModel) = Props(classOf[ModelActor], cm)
}

class ModelActor(val modelSetup: APIModelMaker.CreateModel)
  extends PersistentActor with
    ModelLogic with
    ActorLogging with
    ServiceCommunicationSupport with
    MessageBussSupport
{
  val id: ID = modelSetup.id
  override def persistenceId = id.toString


  subscribe(APIModel.topicRequest)
  triggerServiceRequestComm(serviceResp)
  sendAnswer(SPHeader(from = id.toString), getModelInfo)


  def receiveCommand = {
    case x: String if sender() != self =>
      for {
        m <- SPMessage.fromJson(x)
        h <- m.getHeaderAs[SPHeader] if h.to == modelSetup.id.toString || h.to == APIModel.service
        b <- m.getBodyAs[APIModel.Request]
      } yield {
        val updH = h.copy(from = id.toString, to = h.from)
        sendAnswer(updH, APISP.SPACK())
          b match {
            case k: APIModel.PutItems if h.to == id.toString =>
              val res = putItems(k.items, k.info)
              handleModelDiff(res, updH)
            case k: APIModel.DeleteItems if h.to == id.toString =>
              val res = deleteItems(k.items, k.info)
              handleModelDiff(res, updH)
            case k: APIModel.UpdateModelAttributes if h.to == id.toString =>
              val res = updateAttributes(k.name, k.attributes)
              handleModelDiff(res, updH)
            case k: APIModel.RevertModel if h.to == id.toString =>
            case APIModel.ExportModel =>
              sendAnswer(updH, getTheModelToExport)
            case APIModel.GetModelInfo =>
              sendAnswer(updH, getModelInfo)
            case APIModel.GetModelHistory =>
              val res = getModelHistory
              sendAnswer(updH, getModelHistory)
            case APIModel.GetItems(xs) =>
              val res = xs.flatMap(state.idMap.get)
              sendAnswer(updH, APIModel.SPItems(res.toList))
            case APIModel.GetItemList(from, size, filter) =>
              val res = state.items.slice(from, from + size)
              val appliedFilter = res.filter{item =>
                val nameM = filter.regexName.isEmpty || item.name.toLowerCase.matches(filter.regexName)
                val typeM = filter.regexType.isEmpty || item.getClass.getSimpleName.toLowerCase.matches(filter.regexType)
                nameM && typeM
              }
              sendAnswer(updH, SPItems(appliedFilter))
            case APIModel.GetItem(itemID) =>
              state.idMap.get(itemID) match {
                case Some(r) => sendAnswer(updH, APIModel.SPItem(r))
                case None => sendAnswer(updH, APISP.SPError(s"item $itemID does not exist"))
              }
            case APIModel.GetStructures   =>
              val res = state.items.filter(_.isInstanceOf[Struct])
              sendAnswer(updH, APIModel.SPItems(res))
            case x if h.to == id.toString =>
              println(s"Model $id got something not implemented: ${x}")
            case _ =>
          }

          sendAnswer(updH, APISP.SPDone())

      }

  }

  def serviceResp = ModelInfo.attributes.copy(
    instanceName = id.toString,
    instanceID = Some(id),
    attributes = SPAttributes("modelInfo" -> getModelInfo)
  )

  override def receiveRecover: Receive = {
    case x: String =>
      val diff = SPAttributes.fromJsonGetAs[ModelDiff](x)
      diff.foreach(updateState)
  }

  def handleModelDiff(d: Option[ModelDiff], h: SPHeader) = {
    d.foreach{diff =>
      persist(SPValue(diff).toJson){json =>
        val res = makeModelUpdate(diff)
        sendAnswer(h, res)
      }
    }
  }

  override def postStop() = {
    println("MODEL remove: " + id)
    super.postStop()
  }

  def sendAnswer(h: SPHeader, b: APISP) = publish(APIModel.topicResponse, SPMessage.makeJson(h, b))
  def sendAnswer(h: SPHeader, b: APIModel.Response) = publish(APIModel.topicResponse, SPMessage.makeJson(h, b))
  //def sendEvent(h: SPHeader, b: APIModel.Response) = publish(APISP.spevents, SPMessage.makeJson(h.copy(to = ""), b))
}


trait ModelLogic {
  val id: ID
  val modelSetup: APIModelMaker.CreateModel

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

  val initialHistory = SPAttributes("info"->s"Model created.")
  var state = ModelState(1, Map(), Map(1 -> initialHistory), modelSetup.attributes, modelSetup.name)


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

  def getTheModelToExport = APIModel.ModelToExport(state.name, id, state.version, state.attributes, state.items)
  def getModelInfo = APIModel.ModelInformation(state.name, id, state.version, state.items.size, state.attributes)
  def getModelHistory = APIModel.ModelHistory(id, state.history.toList.sortWith(_._1 > _._1))


  def makeModelUpdate(diff: ModelDiff) = {
    updateState(diff)
    APIModel.ModelUpdate(id, state.version, state.items.size, diff.updatedItems, diff.deletedItems.map(_.id), diff.diffInfo)
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
