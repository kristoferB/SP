package spgui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import scalajs.js.JSON
import scalajs.js.Dynamic

import spgui.circuit.SPGUICircuit
import spgui.circuit.{SetWidgetData, AddWidget}

// TODO methods to publish and subscribe to bus
// TODO method to close itself?
// etc
case class SPWidgetBase(id: Int, json: Dynamic) {
  def saveData(json: Dynamic): Callback =
    Callback(SPGUICircuit.dispatch(SetWidgetData(id, JSON.stringify(json))))

  def openWidget(widgetType: String, json: Dynamic = Dynamic.literal()): Callback =
    Callback(SPGUICircuit.dispatch(AddWidget(widgetType)))
}

object SPWidgetComp {
  case class Props(spwb: SPWidgetBase, renderWidget: SPWidgetBase => ReactElement)
  private val component = ReactComponentB[Props]("SpWidgetComp")
    .render_P(p => p.renderWidget(p.spwb))
    .build

  def apply(renderWidget: SPWidgetBase => ReactElement) =
    (spwb: SPWidgetBase) => component(Props(spwb, renderWidget))
}

object SPWidgetBaseTest {
  def apply() = SPWidgetComp(spwb => <.h3("This is a sample with id " + spwb.id))
}
