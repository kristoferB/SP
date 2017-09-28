package spgui.widgets.itemtree

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scalacss.ScalaCssReact._
import spgui.{SPWidget, SPWidgetBase}
import spgui.components.DragAndDrop.{ DataOnDrag, OnDataDrop }
import spgui.communication.BackendCommunication
import spgui.widgets.itemeditor.{API_ItemServiceDummy => api}
import spgui.circuit.{ SPGUICircuit, UpdateGlobalState, GlobalState }
import sp.domain._

import scalajs.js
import js.Dynamic.{literal => l}
import js.JSON
import js.annotation.JSExportTopLevel
import java.util.UUID


object ItemExplorer {

  // TODO temporary way of setting currentModel, currentModel-field will be moved to global state attributes
  @JSExportTopLevel("setCurrentModel")
  def setCurrentModel(modelIDString: String) = {
    val id = UUID.fromString(modelIDString)
    val action = UpdateGlobalState(GlobalState(currentModel = Some(id)))
    SPGUICircuit.dispatch(action)
  }

  import sp.models.{APIModel => mapi}

  def extractMResponse(m: SPMessage) = for {
    h <- m.getHeaderAs[SPHeader]
    b <- m.getBodyAs[mapi.Response]
  } yield (h, b)

  def makeMess(h: SPHeader, b: mapi.Request) = SPMessage.make[SPHeader, mapi.Request](h, b)

  case class ItemExplorerState(items: List[IDAble], modelIDFieldString: String = "modelID")

  class ItemExplorerBackend($: BackendScope[SPWidgetBase, ItemExplorerState]) {

    def handleMess(mess: SPMessage): Unit = {
      println("handlemess: " + mess)
      extractMResponse(mess).map{ case (h, b) =>
        val res = b match {
          case tm@mapi.SPItems(items) => $.modState(s => s.copy(items = items))
          case x => Callback.empty
        }
        res.runNow()
      }
    }

    def sendToModel(model: ID, mess: mapi.Request): Callback = {
      val h = SPHeader(from = "ItemExplorer", to = model.toString,
        reply = SPValue("ItemExplorer"))
      val json = makeMess(h, mess)
      BackendCommunication.publish(json, mapi.topicRequest)
      Callback.empty
    }

    val topic = mapi.topicResponse
    val wsObs = BackendCommunication.getWebSocketStatusObserver(  mess => {
      if (mess) $.props.map(p => p.frontEndState.currentModel.foreach(m => sendToModel(m, mapi.GetItemList()))).runNow()
    }, topic)
    val topicHandler = BackendCommunication.getMessageObserver(handleMess, topic)

    def render(p: SPWidgetBase, s: ItemExplorerState) =
      <.div(
        if (p.frontEndState.currentModel.isDefined) renderItems(s.items) else renderIfNoModel,
        OnDataDrop(str => Callback.log("dropped " + str + " on item explorer tree"))
      )

    def renderIfNoModel =
      <.div(
        "No model selected. Create a model with som items in ModelsWidget and call setCurrentModel(idString) in console to select one, then open this widget again"
      )

    def renderItems(items: List[IDAble]) =
      <.div(
        <.ul(
          items.toTagMod(idAble => <.li(idAble.name, DataOnDrag(idAble.id.toString, Callback.log("picked sumthing up"))))
          //items.toTagMod(idAble => <.li(idAble.name))
        )
      )
  }

  val itemExplorerComponent = ScalaComponent.builder[SPWidgetBase]("ItemExplorer")
    .initialState(ItemExplorerState(Nil))
    .renderBackend[ItemExplorerBackend]
    .build

  def apply() = SPWidget { spwb =>
    //println(spwb.frontEndState)
    itemExplorerComponent(spwb)
  }
}
