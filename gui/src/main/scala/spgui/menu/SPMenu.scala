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
      ^.className := SPMenuCSS.topNav.htmlClass,
      <.div(
        ^.className := "navbar-header",
        <.div(
          ^.className := SPMenuCSS.spLogoDiv.htmlClass,
          spLogo
        )
      ),
      NavBar(
        Seq(
          <.div(
            WidgetMenuNew()
          ),
          <.div(
            SPButtonElements.clickable(Callback(SPGUICircuit.dispatch(CloseAllWidgets))),
            SPButtonElements.navButton("Close All")
          )
        )
      )
    )
).build



private val spLogo:ReactNode = (
  <.div(
    ^.className := SPMenuCSS.splogoContainer.htmlClass,
    <.div(
      ^.className := SPMenuCSS.spLogo.htmlClass
    )
  ))
def apply() = component()
}
