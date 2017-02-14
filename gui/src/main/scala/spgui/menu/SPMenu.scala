package spgui.menu

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

object SPMenu {
  private val component = ReactComponentB[Unit]("SPMenu")
    .render(_ =>
    <.nav(
      ^.className := SPMenuCSS.topNav.htmlClass,
      ^.className := "navbar navbar-static-top navbar-default",
      <.ul(
        ^.className := SPMenuCSS.buttonList.htmlClass,
        ^.className := "nav_navbar-nav",
        ^.className := SPMenuCSS.navbarCell.htmlClass,
        WidgetMenu()
      ),
      spLogo
    )
  )
    .build

  private val spLogo = (
      <.ul(
        ^.className := SPMenuCSS.spLogo.htmlClass,
        ^.className := SPMenuCSS.navbarCell.htmlClass
      )
    )

  def apply() = component()
}
