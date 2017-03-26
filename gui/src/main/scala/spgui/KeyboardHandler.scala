package spgui

import org.scalajs.dom.window._
import org.scalajs.dom.KeyboardEvent
import diode.Action
import spgui.circuit._



object KeyboardHandler {
  def init = () => None
  var hotkeys: Map[Int, Action] = Map()
  addEvent()
  window.addEventListener(
    "keydown",
    (e: KeyboardEvent) => {
      println(e.keyCode)
      if(e.altKey && hotkeys.keySet.exists(_ == e.keyCode)) {
        SPGUICircuit.dispatch(hotkeys(e.keyCode))
      }
    },
    false
  )

  def addEvent() = {
    hotkeys = hotkeys + (67 -> ToggleCompactMode())
  }
}
