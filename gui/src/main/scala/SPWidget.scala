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
// TODO get the pickling in here, turned out to be tricky, upickle doesn like generic types...
// TODO unsubscribe, to be called when SPWidget is closed
// TODO move widget "frame-functionality" into here
// TODO kill subscriptions directly on widget close
// etc
case class SPWidgetBase(id: Int, data: String) {

  def saveData(data: String): Unit = {
    SPGUIBus.unsubscribeWidget(id)
    SPGUICircuit.dispatch(SetWidgetData(id, data))
  }

  def openWidget(widgetType: String, data: String = "") =
    SPGUICircuit.dispatch(AddWidget(
                                     widgetType = widgetType,
                                     stringifiedWidgetData = data
                                   ))

  def closeSelf() = SPGUICircuit.dispatch(CloseWidget(id))

  def subscribe(topic: String, cb: String => Unit) = SPGUIBus.subscribe(id, topic, cb)
  def publish(topic: String, msg: String) = SPGUIBus.publish(topic, msg)
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
      Callback(spwb.saveData(e.target.value))

    def copyMe(): Callback =
      Callback(spwb.openWidget(
        "SPWBTest", Try(spwb.data.toString).getOrElse("hej")
      ))

    <.div(
      <.h3("This is a sample with id " + spwb.id),
      <.label("My Data"),
      <.input(
        ^.tpe := "text",
        ^.defaultValue := Try(spwb.data).getOrElse("0"),
        ^.onChange ==> saveOnChange
      ),
      <.button("Copy me", ^.onClick --> copyMe()),
      <.button("Kill me", ^.onClick --> Callback(spwb.closeSelf()))
    )
  }
}
