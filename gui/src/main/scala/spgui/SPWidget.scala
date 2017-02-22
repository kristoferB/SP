package spgui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import scala.util.Try
import spgui.circuit._
import sp.domain._
import java.util.UUID


case class SPWidgetBase(id: UUID, data: SPValue, frontEndState: FrontEndState) {

  def saveWidgetData(data: SPValue): Unit = {
    SPGUICircuit.dispatch(SetWidgetData(id, data))
  }

  def saveFrontEndData(state: FrontEndState): Unit = {
    SPGUICircuit.dispatch(UpdateFrontEndState(state))
  }

  def openNewWidget(widgetType: String, initialData: SPValue = SPValue.empty) =
    SPGUICircuit.dispatch(AddWidget(widgetType = widgetType, initialData = initialData))

  def closeSelf() = SPGUICircuit.dispatch(CloseWidget(id))

}

object SPWidget {
  case class Props(spwb: SPWidgetBase, renderWidget: SPWidgetBase => ReactElement)
  private val component = ReactComponentB[Props]("SpWidgetComp")
    .render_P(p => p.renderWidget(p.spwb))
    .build

  def apply(renderWidget: SPWidgetBase => ReactElement) =
    (spwb: SPWidgetBase) => component(Props(spwb, renderWidget))
}


object SPWidgetBaseTest {
  import sp.messages.Pickles._
  def apply() = SPWidget{spwb =>
    def saveOnChange(e: ReactEventI): Callback =
      Callback(spwb.saveWidgetData(*(e.target.value)))

    def copyMe(): Callback = {
      val d = spwb.data
      Callback(spwb.openNewWidget(
        "SPWBTest", d)
      )
    }


    <.div(
      <.h3("This is a sample with id " + spwb.id),
      <.label("My Data"),
      <.input(
        ^.tpe := "text",
        ^.defaultValue := spwb.data.toJson,
        ^.onChange ==> saveOnChange
      ),
      <.button("Copy me", ^.onClick --> copyMe()),
      <.button("Kill me", ^.onClick --> Callback(spwb.closeSelf()))
    )
  }
}
