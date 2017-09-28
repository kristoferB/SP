package spgui.widgets.itemeditor

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scalacss.ScalaCssReact._
import scalajs.js
import js.Dynamic.{literal => l}
import js.JSON
import spgui.{SPWidget, SPWidgetBase}
import spgui.components.DragAndDrop.OnDataDrop
import spgui.communication._
import sp.domain._
import sp.domain.Logic._
import java.util.UUID

import scala.util.Try

object ItemEditor {
  import sp.models.{APIModel => mapi}

  def extractMResponse(m: SPMessage) = for {
    h <- m.getHeaderAs[SPHeader]
    b <- m.getBodyAs[mapi.Response]
  } yield (h, b)

  def makeMess(h: SPHeader, b: mapi.Request) = SPMessage.make[SPHeader, mapi.Request](h, b)

  case class ItemEditorState(item: Option[IDAble])

  class Backend($: BackendScope[SPWidgetBase, ItemEditorState]) {

    def handleMess(mess: SPMessage): Unit = {
      println("handlemess: " + mess)
      extractMResponse(mess).map{ case (h, b) =>
        val res = b match {
          case tm@mapi.SPItem(item) => {
            //$.modState(s => s.copy(item = Some(item))) >> Callback.log("got an item")
            jsonEditor = JSONEditor($.getDOMNode.runNow(), ItemEditorOptions())
            $.forceUpdate.runNow() // explicit rerendering cause jsonEditor is not in State
            jsonEditor.set(JSON.parse(SPValue(item).toJson))
            Callback.empty
          }
          case x => Callback.empty
        }
        res.runNow()
      }
    }

    def sendToModel(model: ID, mess: mapi.Request): Callback = {
      val h = SPHeader(from = "ItemEditor", to = model.toString,
        reply = SPValue("ItemEditor"))
      val json = makeMess(h, mess)
      BackendCommunication.publish(json, mapi.topicRequest)
      Callback.empty
    }

    val topic = mapi.topicResponse
    val topicHandler = BackendCommunication.getMessageObserver(handleMess, topic)

    var jsonEditor: JSONEditor = null // initialized for real upon mounting, or receiving Item(item)


    def render(spwb: SPWidgetBase) =
      <.div(
        <.button("Save", ^.onClick --> Callback.log("save button, does nothing")),
        <.div(
          "drop an item from item explorer tree to edit it",
          OnDataDrop(idAsStr => sendToModel(spwb.frontEndState.currentModel.get, mapi.GetItem(UUID.fromString(idAsStr))))
        ).when(jsonEditor == null)
      ) // editor added after mount, or on item dropped
  }

  private val component = ScalaComponent.builder[SPWidgetBase]("ItemEditor")
    .initialState(ItemEditorState(None))
    .renderBackend[Backend]
    /* // this will be used if itemeditor will know what item to edit before its opened
    .componentDidMount(dcb =>
      Callback(dcb.backend.jsonEditor = JSONEditor(dcb.getDOMNode, ItemEditorOptions()))
    )
    */
    .build

  def apply() = SPWidget(spwb => component(spwb))
}
