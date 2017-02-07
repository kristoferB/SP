package spgui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import scalajs.js.JSON
import scalajs.js.Dynamic
import scalajs.js.Dynamic.{literal => l}
import scala.util.Try

import spgui.circuit.SPGUICircuit
import spgui.circuit.{SetWidgetData, AddWidget, CloseWidget}
import spgui.SPGUIBus

// TODO methods to publish and subscribe to bus
// TODO convenience function for getting the json? returning an Option perhaps?
// see the messy stuff in SPWidgetBaseTest
// etc
case class SPWidgetBase(id: Int, json: Dynamic) {
  def saveData(json: Dynamic) = SPGUICircuit.dispatch(SetWidgetData(id, JSON.stringify(json)))

  def openWidget(widgetType: String, json: Dynamic = Dynamic.literal()) =
    SPGUICircuit.dispatch(AddWidget(
                                     widgetType = widgetType,
                                     stringifiedWidgetData = JSON.stringify(json)
                                   ))

  def closeSelf() = SPGUICircuit.dispatch(CloseWidget(id))

  def getJson(key: String): Option[String] = Try(json.selectDynamic(key).toString).toOption

  def subscribe: (String => Unit) => Unit = SPGUIBus.subscribe _
  def publish: String => Unit = SPGUIBus.publish _
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
  def apply() = SPWidget{spwb =>
    def saveOnChange(e: ReactEventI): Callback =
      Callback(spwb.saveData(l("spwbtData" -> e.target.value)))

    def copyMe(): Callback =
      Callback(spwb.openWidget(
        "SPWBTest", l("spwbtData" -> spwb.getJson("spwbtData").get)
      ))

    <.div(
      <.h3("This is a sample with id " + spwb.id),
      <.label("My Data"),
      <.input(
        ^.tpe := "text",
        ^.defaultValue := spwb.getJson("spwbtData").get,
        ^.onChange ==> saveOnChange
      ),
      <.button("Copy me", ^.onClick --> copyMe()),
      <.button("Kill me", ^.onClick --> Callback(spwb.closeSelf()))
    )
  }
}
