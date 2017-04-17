package spgui.widgets.itemeditor

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import scalacss.ScalaCssReact._
import scalajs.js
import js.Dynamic.{ literal => l }
import js.JSON

import spgui.SPWidgetBase

import spgui.communication._
import sp.domain._
//import sp.messages._
import sp.messages.Pickles._

sealed trait API_ItemEditorService
object API_ItemEditorService {
  case class Hello() extends API_ItemEditorService
  case class RequestSampleItem() extends API_ItemEditorService
  case class SampleItem(operationSPV: SPValue) extends API_ItemEditorService
  case class Item(item: SPValue) extends API_ItemEditorService
}

object ItemEditor {

  class Backend($: BackendScope[SPWidgetBase, Unit]) {

    var jsonEditor: JSONEditor = null // initalized for real upon mounting

    def handleCommand: API_ItemEditorService => Unit = {
      case API_ItemEditorService.Hello() =>
        println("ItemEditorWidget: Somebody said hi")
      case opSPV: API_ItemEditorService.SampleItem =>
        jsonEditor.set(JSON.parse(opSPV.operationSPV.toJson))
      case x =>
        println(s"THIS WAS NOT EXPECTED IN ItemEditorWidget: $x")
    }

    val messObs = BackendCommunication.getMessageObserver(
      spm => spm.getBodyAs[API_ItemEditorService] foreach handleCommand,
      "itemEditorAnswers"
    )

    // send ItemRequest command to service once websocket is open
    // TODO maybe make this in some less weird way
    import rx._
    implicit val ctx: Ctx.Owner = Ctx.Owner.safe()
    val statusVar = BackendCommunication.getWebSocketStatus("services")
    statusVar.trigger(if(statusVar.now == true) {
        requestSampleItem()
        statusVar.kill()
      }
    )

    def sendCommand(cmd: API_ItemEditorService) = {
      val h = SPHeader(from = "ItemEditorWidget", to = "itemEditorService", reply = *("ItemEditorWidget"))
      val jsonMsg = SPMessage.make(h, cmd)
      BackendCommunication.publishMessage("services", jsonMsg)
      //BackendCommunication.ask(jsonMsg)
    }

    def requestSampleItem() = sendCommand(API_ItemEditorService.RequestSampleItem())

    def sendHello() = sendCommand(API_ItemEditorService.Hello())

    def returnItem() = Callback{
      val spVal = fromJsonToSPValue(JSON.stringify(jsonEditor.get())).get
      sendCommand(API_ItemEditorService.Item(spVal))
    }

    def render(spwb: SPWidgetBase) =
      <.div(<.button("Save", ^.onClick --> returnItem())) // editor added after mount
  }

  private val component = ReactComponentB[SPWidgetBase]("ItemEditor")
    .renderBackend[Backend]
    .componentDidMount(dcb =>
      Callback(dcb.backend.jsonEditor = JSONEditor(dcb.getDOMNode, ItemEditorOptions()))
    )
    .build

  def apply() = (spwb: SPWidgetBase) => component(spwb)
}
