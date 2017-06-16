package spgui.menu

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom._

import spgui.components.{Icon, SPNavbarElements}
import spgui.circuit.{CloseAllWidgets, SPGUICircuit}

object SPMenu {
  private val component = ScalaComponent.builder[Unit]("SPMenu")
    .render(_ =>
      <.nav(
        ^.className:= SPMenuCSS.topNav.htmlClass,
        ^.className := "navbar navbar-default",

        // navbar header: logo+toggle button
        <.div(
          ^.className := "navbar-header",
          <.a(
            ^.className := SPMenuCSS.navbarToggleButton.htmlClass,
            ^.className := "navbar-toggle collapsed",
            VdomAttr("data-toggle") := "collapse",
            VdomAttr("data-target") := "#navbar-contents",
            <.div(
              ^.className := SPMenuCSS.navbarToggleButtonIcon.htmlClass,
              Icon.bars
            )
          ),
          <.a(
            ^.className:= "navbar-brand",
            ^.className := SPMenuCSS.splogoContainer.htmlClass,
            <.div(
              ^.className := SPMenuCSS.spLogo.htmlClass
            )
          )
        ),

        // navbar contents
        <.div(
          ^.className := SPMenuCSS.navbarContents.htmlClass,
          ^.className := "collapse navbar-collapse",
          ^.id := "navbar-contents",
          <.ul(
            ^.className := "nav navbar-nav",

            WidgetMenu(),
            SPNavbarElements.button("Close All", Callback(SPGUICircuit.dispatch(CloseAllWidgets)))

          )
        )
      )
    ).build

  def apply() = component()
}




