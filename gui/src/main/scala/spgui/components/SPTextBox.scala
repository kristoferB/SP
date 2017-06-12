package spgui.components

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.vdom.all.aria

object SPTextBox {
  case class Props( defaultText: String, onChange: String => Callback )
  case class State( text: String )

  class Backend($: BackendScope[Props, State]) {
    def render(p:Props,s: State) =
    <.div(
      ^.className := "input-group",
      <.input(
        ^.className := "form-control",
        ^.placeholder := p.defaultText,
        ^.aria.describedby := "basic-addon1",
        ^.onChange ==> onFilterTextChange(p)
      )    
    )
    def onFilterTextChange(p:Props)(e: ReactEventI): Callback = 
      e.extract(_.target.value)(v => (p.onChange(v)))
  }

  private val component = ReactComponentB[Props]("SPTextBox")
    .initialState(State("test"))
    .renderBackend[Backend]
    .build

  def apply(defaultText: String, onChange: String => Callback) = 
    component(Props(defaultText, onChange))
}

