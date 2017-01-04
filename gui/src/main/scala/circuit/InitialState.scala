package spgui.circuit

import spgui.widgets

object InitialState {
  val initialState = RootModel(
    availableWidgets = List(
      ("Grid Test", spgui.dashboard.Grid.component()),
      ("Widget Injection", widgets.injection.WidgetInjectionTest()),
      ("Item Editor", widgets.itemeditor.ItemEditor()),
      ("PlcHldrC", PlaceholderComp()),
      ("CommTest", widgets.WidgetCommTest())
    ),
    openWidgets = List(PlaceholderComp())
  )
  def apply() = initialState
}
