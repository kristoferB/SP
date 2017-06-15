package spgui.menu

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import org.scalajs.dom._

object NavItem {
  case class Props (
    content: Option[ReactNode] = None
  )

  private val component = ReactComponentB[Props]("WidgetItem")
  .render_P(p =>
      <.li(
            ^.className := "nav-item " + SPMenuCSS.navItem.htmlClass,
            p.content
        )
  ).build

  def apply(content:ReactNode) = component(Props(Some(content)))

}
