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

sealed trait API_D3ExampleService
object API_D3ExampleService {
  case class Start() extends API_D3ExampleService
  case class Stop() extends API_D3ExampleService
  case class D3Data(barHeights: List[Int]) extends API_D3ExampleService

  val service = "d3ExampleService"
}

object ErrorHandler{

    private class Backend($: BackendScope[Unit, Unit]) {
      def addW(name: String, w: Int, h: Int): Callback =
        Callback(SPGUICircuit.dispatch(AddWidget(name, w, h)))
// Testing comunicatin with services. Now prints in console on recieve
      val eventHandler = BackendCommunication.getMessageObserver(
        mess => {
          mess.getBodyAs[API_D3ExampleService] map {
            case API_D3ExampleService.D3Data(l) =>
              println("I DID IT")
            case x =>
              println(s"THIS WAS NOT EXPECTED IN D3ExampleServiceWidget: $x")
          }
        }, "d3ExampleAnswers"
      )
      val answerHandler = BackendCommunication.getMessageObserver(
        mess => {
          fromSPValue[vdapi.Replies](mess.body).map{
            case vdapi.Resources(r) =>

            case x =>
              println(s"AbilityHandlerWidget - TODO: $x")
          }
          fromSPValue[abapi.Response](mess.body).map{
            case abapi.Abilities(a) =>
            case abapi.AbilityState(id, state) =>
              
            case x =>
              println(s"AbilityHandlerWidget - answers - TODO: $x")
          }
        },
        "answers"
      )
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
