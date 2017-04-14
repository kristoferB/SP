/**
  * Created by Jonas Garsten on 2017-04-14.
  */
package spgui.widgets.Kandidat

  import java.util.UUID

  import japgolly.scalajs.react._
  import japgolly.scalajs.react.vdom.prefix_<^._
  import spgui.circuit.{AddWidget, SPGUICircuit}
  import spgui.{SPWidget, SPWidgetBase}
  import spgui.communication._
  import spgui.widgets.abilityhandler
  import sp.messages._
  import sp.messages.Pickles._
  import spgui.widgets.abilityhandler.{APIVirtualDevice => vdapi}
  import spgui.widgets.abilityhandler.{APIAbilityHandler => abapi}
object ErrorHandler{

    private class Backend($: BackendScope[Unit, Unit]) {
      def addW(name: String, w: Int, h: Int): Callback =
        Callback(SPGUICircuit.dispatch(AddWidget(name, w, h)))

/*      val eventHandler = BackendCommunication.getMessageObserver(
        mess => {
          fromSPValue[abapi.Response](mess.body).map{
            case
        }
      }
      )*/
      def render() = {
        <.div(
          <.button(
            ^.className := "btn btn-default",
            ^.onClick --> addW("Ability Handler", 2, 2), "Add Widget"
          )
        )
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
