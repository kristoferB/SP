package spgui

import japgolly.scalajs.react._

import scalajs.js.JSON
import scalajs.js.Dynamic

import spgui.circuit.SPGUICircuit
import spgui.circuit.SetWidgetData

// TODO method to add new widget
// TODO methods to publish and subscribe to bus
// TODO js.Dynamic as prop?
// TODO method to close itself?
// etc
abstract class SPWidget {
  class Backend($: BackendScope[Unit, Unit]) {
    def render = renderWidget
  }

  def renderWidget: ReactElement

  def saveData(json: Dynamic): Callback =
    Callback(SPGUICircuit.dispatch(SetWidgetData(id, JSON.stringify(json))))

  private val component = ReactComponentB[Unit]("WidgetWithJSON")
    .renderBackend[Backend]
    .build

  // wanted this to be a val but couldn't figure out how
  private var id = -1

  def apply(id: Int): ReactElement = {
    this.id = id
    component()
  }
}
