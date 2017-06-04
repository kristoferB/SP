package spgui.menu

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import org.scalajs.dom._

object NavBar {
  case class Props (
    navItems: Option[Seq[VdomNode]] = None
  )

  private val component = ScalaComponent.builder[Props]("WidgetItem")
  .render_P(p =>
    <.div(
      <.div(
        ^.className := "navbar-toggle collapsed",
        "Navbar has collapsed, i am a placeholder as width < 768px"
      ),
      ^.className := "container-fluid",
      ^.className := SPMenuCSS.container.htmlClass,
      <.ul(
        ^.id := "navbar-collapse-id",
        ^.className := "collapse navbar-collapse",
        ^.className := "nav navbar-nav",
        p.navItems.get.toTagMod
      )
    )
  ).build

  def apply(navItems:Seq[VdomNode]) = component(Props(Some(navItems)))

}
