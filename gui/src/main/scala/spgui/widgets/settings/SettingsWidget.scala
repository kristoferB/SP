package spgui.widgets.settings

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import spgui.circuit.{SPGUICircuit, SetTheme}

import spgui.SPWidget

import org.scalajs.dom.window.location
import spgui.theming._
import spgui.components.SPWidgetElements

object SettingsWidget {
  private class MyBackend($: BackendScope[Unit, Unit]) {
    def render(p: Unit) =
      <.div(
        SPWidgetElements.dropdown(
          "Theme picker dropdown placeholder element",
          Themes.themeList.map(theme =>
            <.div(
              theme.name,
              ^.onClick --> Callback({
                SPGUICircuit.dispatch(
                  SetTheme(
                    Themes.themeList.find(e => e.name == theme.name).get
                  )
                )
                location.reload() // reload the page
              })
            )
          )
        )
      )
  }

  private val component = ScalaComponent.builder[Unit]("Settings")
    .renderBackend[MyBackend]
    .build

  def apply() = SPWidget(swpb => component())
}
