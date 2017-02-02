package spgui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._


object SPButton {

  case class SPButtonProps(name: String )

  private val component = ReactComponentB[SPButtonProps]("SPButton")
    .render_P( props => {
      <.button(//SPButtonCSS.testStyle
        props.name

      )
    }).build

  def apply(name: String) = component(SPButtonProps(name))

}



