package spgui.menu

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom._

import spgui.components.CloseAllButton

object SPMenu {
  private val component = ScalaComponent.build[Unit]("SPMenu")
    .render(_ =>
    <.nav(
      ^.className := "navbar navbar-default",
      ^.className := SPMenuCSS.topNav.htmlClass,
      <.div(
        ^.className := "navbar-header",
        <.div(
          ^.className := SPMenuCSS.spLogoDiv.htmlClass,
          spLogo
        )
      ),
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
          ^.className := SPMenuCSS.buttonList.htmlClass,
          ^.className := "nav navbar-nav",
          WidgetMenu(),
          CloseAllButton()
        )
      )
    )
  )
      .build



  private val spLogo = (
    <.div(
      ^.className := SPMenuCSS.splogoContainer.htmlClass,
      <.div(
        ^.className := SPMenuCSS.spLogo.htmlClass
      )
    ))
  def apply() = component()
}
