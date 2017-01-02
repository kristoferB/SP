package spgui.injection

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

class WidgetWrapper(Widget: ReactComponentU[Unit,Unit,Unit,org.scalajs.dom.raw.Element]) {
  // yes, this is the type we are using here
  type ComponentType =  ReactComponentU[Unit,Unit,Unit,org.scalajs.dom.raw.Element]

  // i am really not sure about what the naming convention should be here
  val getComponent = ReactComponentB[Unit]("App")
    .initialState(Widget)
    .renderBackend[WrapperBackend]
    .build
  
  class WrapperBackend($: BackendScope[Unit, ComponentType]) {
    def render(state: ComponentType) =
      <.div("This text is static content in the wrapper",
        state
      )
  }
}


