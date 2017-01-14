package spgui.circuit

object InitialState {
  val initialState = RootModel(
    availableWidgets = List(
      "Grid Test",
      "Widget Injection",
      "Item Editor",
      "DragDrop Example",
      "PlcHldrC"
    ),
    openWidgets = List("PlcHldrC")
  )
  def apply() = initialState
}
