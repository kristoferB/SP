package spgui.widgets.itemeditor

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import scalacss.ScalaCssReact._
import scalajs.js
import js.Dynamic.{ literal => l }
import js.JSON

import spgui.{ SPWidget, SPWidgetBase }

import spgui.communication._
import sp.domain._
//import sp.messages._
import sp.messages.Pickles._

sealed trait API_ItemServiceDummy
object API_ItemServiceDummy {
  case class Hello() extends API_ItemServiceDummy
  case class RequestSampleItem() extends API_ItemServiceDummy
  case class SampleItem(operation: IDAble) extends API_ItemServiceDummy
  case class Item(item: IDAble) extends API_ItemServiceDummy
}

object ItemEditor {

  class Backend($: BackendScope[SPWidgetBase, Unit]) {

    var jsonEditor: JSONEditor = null // initalized for real upon mounting

    def handleCommand: API_ItemServiceDummy => Unit = {
      case API_ItemServiceDummy.Hello() =>
        println("ItemEditorWidget: Somebody said hi")
      case op: API_ItemServiceDummy.SampleItem =>
        jsonEditor.set(JSON.parse(SPValue(op.operation).toJson))
      case x =>
        println(s"THIS WAS NOT EXPECTED IN ItemEditorWidget: $x")
    }

    val messObs = BackendCommunication.getMessageObserver(
      spm => spm.getBodyAs[API_ItemServiceDummy] foreach handleCommand,
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

    def sendCommand(cmd: API_ItemServiceDummy) = {
      val h = SPHeader(from = "ItemEditorWidget", to = "itemEditorService", reply = *("ItemEditorWidget"))
      val jsonMsg = SPMessage.make(h, cmd)
      BackendCommunication.publishMessage("services", jsonMsg)
      //BackendCommunication.ask(jsonMsg)
    }

    def requestSampleItem() = sendCommand(API_ItemServiceDummy.RequestSampleItem())

    def sendHello() = sendCommand(API_ItemServiceDummy.Hello())

    def returnItem() = Callback{
      // TODO remove shitty gets
      val idAble = fromJsonToSPValue(JSON.stringify(jsonEditor.get())).get.getAs[IDAble]("").get
      sendCommand(API_ItemServiceDummy.Item(idAble))
    }

    def render(spwb: SPWidgetBase) =
      <.div(<.button("Save", ^.onClick --> returnItem())) // editor added after mount
  }

  private val component = ScalaComponent.builder[SPWidgetBase]("ItemEditor")
    .renderBackend[Backend]
    .componentDidMount(dcb =>
      Callback(dcb.backend.jsonEditor = JSONEditor(dcb.getDOMNode, ItemEditorOptions()))
    )
    .build

  def apply() = SPWidget(spwb => component(spwb))
}
