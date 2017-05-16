/**
  * Created by Jonas Garsten on 2017-04-14.
  */
package spgui.widgets.Kandidat

  import java.util.UUID

  import japgolly.scalajs.react._
  import japgolly.scalajs.react.vdom.prefix_<^._
  import spgui.circuit.{AddWidget, SPGUICircuit}
  import spgui.communication._
  import sp.messages._
  import sp.messages.Pickles._
  import spgui.widgets.abilityhandler.{APIVirtualDevice => vdapi}
  import spgui.widgets.abilityhandler.{APIAbilityHandler => abapi}
  import spgui.widgets.examples.DragAndDropCSS.style

  import scalacss.defaults.Exports.StyleSheet


object ErrorHandler extends StyleSheet.Inline{

    private class Backend($: BackendScope[Unit, Unit]) {

      def render() = {

      }

      def onUnmount() = {
        println("Unmounting")
        Callback.empty
      }
    }
    private val component = ReactComponentB[Unit]("ErrorHandler")
      .renderBackend[Backend]
      .componentWillUnmount(_.backend.onUnmount())
      .build

    def apply() = spgui.SPWidget(spwb => component())
  }
