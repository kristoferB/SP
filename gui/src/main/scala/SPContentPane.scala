package spgui

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import diode.react.ModelProxy

object SPContentPane {
  case class Props(proxy: ModelProxy[ReactElement])
  private val component = ReactComponentB[Props]("SPContentPane")
    .render_P(props => props.proxy())
    .build
  def apply(proxy: ModelProxy[ReactElement]) = component(Props(proxy))
}
