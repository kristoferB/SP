package spgui.menu

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import org.scalajs.dom._

import spgui.components.SPButton
import spgui.circuit.{CloseAllWidgets, SPGUICircuit, UpdateGlobalAttributes}

object SPMenu {
  val storage = SPGUICircuit.connect(x => (x.openWidgets.xs, x.globalState))

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
          NavItem( WidgetMenu()),
          NavItem( SPButton("Close All", Seq(^.onClick -->  Callback(SPGUICircuit.dispatch(CloseAllWidgets)))  ))
        )
      ),
      section   // make this a NavItem!!!
    )
  ).build

  import sp.domain._
  import sp.messages.Pickles._

  def onFilterTextChange(e: ReactEventI) = {
    Callback(SPGUICircuit.dispatch(UpdateGlobalAttributes("team", SPValue(e.target.value))))
  }

  private val section = storage{x =>
      val currentTeam = x()._2.attributes.get("team").map(x => x.str).getOrElse("medicin")

      <.div(
        ^.className := "input-group",
        <.input(
          ^.className := "form-control",
          ^.placeholder := "team",
          ^.aria.describedby := "basic-addon1",
          ^.onChange ==> onFilterTextChange,
          ^.value := currentTeam
        )
      )
    }

  private val spLogo:ReactNode = (
    <.div(
      ^.className := SPMenuCSS.splogoContainer.htmlClass,
      <.div(
        ^.className := SPMenuCSS.spLogo.htmlClass
      )
    ))
  def apply() = component()
}
