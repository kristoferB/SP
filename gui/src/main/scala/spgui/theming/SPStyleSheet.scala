package spgui.theming

import scalacss.Defaults._

trait SPStyleSheet extends StyleSheet.Inline {
  import spgui.circuit.SPGUICircuit
  val theme = SPGUICircuit.zoom(_.settings.theme)
}
