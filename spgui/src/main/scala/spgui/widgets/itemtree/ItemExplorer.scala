package spgui.widgets.itemtree

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scalacss.ScalaCssReact._
import spgui.{SPWidget, SPWidgetBase}
import spgui.components.DragAndDrop.DataOnDrag
import spgui.communication.BackendCommunication
import spgui.widgets.itemeditor.{API_ItemServiceDummy => api}
import sp.domain._

import scalajs.js
import js.Dynamic.{literal => l}
import js.JSON
import java.util.UUID


object ItemExplorer {

  case class ItemExplorerState(items: List[IDAble])

  class ItemExplorerBackend($: BackendScope[SPWidgetBase, ItemExplorerState]) {

    val messObs = BackendCommunication.getMessageObserver(
      _.getBodyAs[api].foreach{
        case items: api.SampleItemList => {
          println(items)
          $.setState(ItemExplorerState(items.items)).runNow()
        }
      },
      "itemExplorerAnswers"
    )

    def sendCommand(cmd: api) = Callback{
      val h = SPHeader(from = "ItemExplorerWidget", to = api.attributes.service)
      val jsonMsg = SPMessage.make(h, cmd)
      BackendCommunication.publishMessage("services", jsonMsg)
    }

    def render(p: SPWidgetBase, s: ItemExplorerState) =
      <.div(
        <.button("fetch items from backend", ^.onClick --> sendCommand(api.RequestSampleItems())),
        <.ul(
          s.items.toTagMod(idAble => <.li(idAble.name, DataOnDrag(idAble.id.toString)))
        )
      )
  }

  val ItemExplorerComponent = ScalaComponent.builder[SPWidgetBase]("ItemExplorer")
    .initialState(ItemExplorerState(Nil))
    .renderBackend[ItemExplorerBackend]
    .build

  def apply() = SPWidget(spwb => ItemExplorerComponent(spwb))
}
