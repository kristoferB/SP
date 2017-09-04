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

  class Backend($: BackendScope[SPWidgetBase, Unit]) {

    var jsonEditor: JSONEditor = null // initialized for real upon mounting, or receiving Item(item)

    def handleCommand: API_ItemServiceDummy => Unit = {
      case API_ItemServiceDummy.Hello() =>
        println("ItemEditorWidget: Somebody said hi")
      case API_ItemServiceDummy.Item(item) =>
        jsonEditor = JSONEditor($.getDOMNode.runNow(), ItemEditorOptions())
        $.forceUpdate.runNow() // explicit rerendering cause jsonEditor is not in State
        jsonEditor.set(JSON.parse(SPValue(item).toJson))
      case op: API_ItemServiceDummy.SampleItem =>
        jsonEditor.set(JSON.parse(SPValue(op.operation).toJson))
      case API_ItemServiceDummy.SampleItemList(items) =>
        println("received items: " + items)
      case x =>
        println(s"THIS WAS NOT EXPECTED IN ItemEditorWidget: $x")
    }

    val messObs = BackendCommunication.getMessageObserver(
      spm => spm.getBodyAs[API_ItemServiceDummy] foreach handleCommand,
      "itemEditorAnswers"
    )

    // send ItemRequest command to service once websocket is open
    // TODO maybe make this in some less weird way
    /* something like this will be used if itemeditor will know what item to edit before its opened
    import rx._
    implicit val ctx: Ctx.Owner = Ctx.Owner.safe()
    val statusVar = BackendCommunication.getWebSocketStatus("services")
    statusVar.trigger(if(statusVar.now == true) {
        requestSampleItem().runNow()
        statusVar.kill()
      }
    )
    */

    def sendCommand(cmd: Try[API_ItemServiceDummy]) = Callback{
      cmd.foreach{c =>
        val h = SPHeader(from = "ItemEditorWidget", to = API_ItemServiceDummy.attributes.service, reply = SPValue("ItemEditorWidget"))
        val jsonMsg = SPMessage.make(h, c)
        BackendCommunication.publishMessage("services", jsonMsg)

      }
      //BackendCommunication.ask(jsonMsg)
    }

    def requestSampleItem() = sendCommand(Try(API_ItemServiceDummy.RequestSampleItem()))

    def returnItem(): Callback = {
      // TODO remove shitty gets
      val idAble = fromJsonAs[IDAble](JSON.stringify(jsonEditor.get()))
      val mess = idAble.map(x => API_ItemServiceDummy.Item(x))
      sendCommand(mess)
    }

    def render(spwb: SPWidgetBase) =
      <.div(
        <.button("Save", ^.onClick --> returnItem()),
        <.div(
          "drop an item from item explorer tree to edit it",
          OnDataDrop(idAsStr => sendCommand(Try(API_ItemServiceDummy.RequestItem(UUID.fromString(idAsStr)))))
        ).when(jsonEditor == null)
      ) // editor added after mount, or on item dropped
  }

  private val component = ScalaComponent.builder[SPWidgetBase]("ItemEditor")
    .renderBackend[Backend]
    /* // this will be used if itemeditor will know what item to edit before its opened
    .componentDidMount(dcb =>
      Callback(dcb.backend.jsonEditor = JSONEditor(dcb.getDOMNode, ItemEditorOptions()))
    )
    */
    .build

  def apply() = SPWidget(spwb => component(spwb))
}
