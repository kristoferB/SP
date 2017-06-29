package spgui.menu

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import org.scalajs.dom._

object NavItem {
  case class Props (
    content: Option[VdomNode] = None
  )

  private val component = ScalaComponent.builder[Props]("WidgetItem")
    .render_P(p =>
      <.li(
        ^.className := "nav-item " + SPMenuCSS.navItem.htmlClass,
        p.content.get // TODO option?
      )
    ).build

  def apply(content:VdomNode) = component(Props(Some(content)))

}
