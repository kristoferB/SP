package spgui.menu

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import org.scalajs.dom._

import spgui.components.{SPButton,Icon, SPNavbarElements}
import spgui.circuit.{CloseAllWidgets, SPGUICircuit}

object SPMenu {
  private val component = ReactComponentB[Unit]("SPMenu")
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
            ReactAttr.Generic("data-toggle") := "collapse",
            ReactAttr.Generic("data-target") := "#navbar-contents",
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

            <.li(
              SPNavbarElements.button("Close All", Callback(SPGUICircuit.dispatch(CloseAllWidgets)))
            )
          )
        )
      )
    ).build

  def apply() = component()
}
