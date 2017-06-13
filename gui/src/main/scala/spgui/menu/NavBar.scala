package spgui.menu

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import org.scalajs.dom._

object NavBar {
  case class Props (
    navItems: Option[Seq[ReactNode]] = None
  )

  private val component = ReactComponentB[Props]("WidgetItem")
  .render_P(p =>
    <.nav(
      <.div(
        //^.tpe := "button",
        ^.className := "navbar-toggle collapsed",
        "Navbar has collapsed, i am a placeholder as width < 768px",
        ReactAttr.Generic("data-toggle") := "collapse",
        ReactAttr.Generic("data-target") := "#bs-example-navbar-collapse-1",
        ReactAttr.Generic("aria-expanded"):= "false" 
      ),
      ^.className := "container-fluid",
      ^.className := SPMenuCSS.container.htmlClass,
      <.ul(
        ^.id := "navbar-collapse-id",
        ^.className := "collapse navbar-collapse",
        ^.className := "nav navbar-nav",
        p.navItems
      )
    )
  ).build

  def apply(navItems:Seq[ReactNode]) = component(Props(Some(navItems)))

}
