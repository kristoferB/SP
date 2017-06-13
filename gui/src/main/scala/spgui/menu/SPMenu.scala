package spgui.menu

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import org.scalajs.dom._

import spgui.components.{SPButton, SPButtonElements}
import spgui.circuit.{CloseAllWidgets, SPGUICircuit}

object SPMenu {
  private val component = ReactComponentB[Unit]("SPMenu")
    .render(_ =>
      <.nav(
        ^.className := "navbar navbar-default",
        <.div(
          ^.className := "navbar-header",
          <.button(
            ^.className := "navbar-toggle collapsed",
            ReactAttr.Generic("data-toggle") := "collapse",
            ReactAttr.Generic("data-target") := "#navbar-contents",
            "hello i am the button text"
          ),
          <.a(
            ^.className:= "navbar-brand",
            spLogo
          )
        ),

        // navbar contents
        <.div(
          ^.className := "collapse navbar-collapse",
          ^.id := "navbar-contents",
          <.ul(
            ^.className := "nav navbar-nav",
            <.li(
              WidgetMenuNew()
            ),
            <.li(
              ^.onClick --> (Callback(SPGUICircuit.dispatch(CloseAllWidgets))),
              SPButtonElements.navButton("Close All")
            ) 
          )
        )
      )
    ).build

  private val spLogo: ReactNode = (
    <.div(
      ^.className := SPMenuCSS.splogoContainer.htmlClass,
      <.div(
        ^.className := SPMenuCSS.spLogo.htmlClass
      )
    ))
  def apply() = component()
}
