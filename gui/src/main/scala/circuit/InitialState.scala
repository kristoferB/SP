package spgui.circuit

import spgui.widgets

object InitialState {
  val initialState = RootModel(
    availableWidgets = List(
      ("Grid Test", spgui.dashboard.Grid.component()),
      ("Widget Injection", widgets.injection.WidgetInjectionTest()),
      ("PlcHldrC", PlaceholderComp())
    ),
    openWidgets = List(PlaceholderComp())
  )
  def apply() = initialState
}
