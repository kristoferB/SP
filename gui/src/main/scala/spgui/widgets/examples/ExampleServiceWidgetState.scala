package spgui.widgets.examples

import java.util.UUID

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import spgui.SPWidgetBase
import spgui.communication._

import scala.util.Try

  // Import this to make SPAttributes work including json handling
  import sp.domain._
  import sp.messages._
  import Pickles._


  import spgui.widgets.examples.{API_ExampleService => api}


  object ExampleServiceWidgetState {
    case class Pie(id: UUID, map: Map[String, Int])
    case class State(pie: Option[Pie], otherPies: List[UUID])

    private class Backend($: BackendScope[SPWidgetBase, State]) {

      val messObs = BackendCommunication.getMessageObserver(
        mess => {
          //println(s"The widget example got: $mess" +s"parsing: ${mess.getBodyAs[api.API_ExampleService]}")

          val s = $.state.runNow()
          val pieID = s.pie.map(_.id).getOrElse(UUID.randomUUID())
          val updState = mess.getBodyAs[api.API_ExampleService].map {
            case api.TickerEvent(m, id) =>
              if (id == pieID) {
                val p = Pie(id, m)
                s.copy(pie = Some(p))
              } else {
                val updIds = if (s.otherPies.contains(id)) s.otherPies else id :: s.otherPies
               s.copy(otherPies = updIds)
              }
            case api.TheTickers(ids) =>
                val p = if (!ids.contains(pieID)) None else s.pie
                State(pie = p, otherPies = ids)
            case x =>
              s
          }

          updState.map{x =>
            saveData(x)
            $.setState(x).runNow()
          }

        }
      )



      def render(s: State) = {
        <.div(
          <.h1(s"The Pie ID:"),
          s.pie.toList.map { p => <.div(p.id.toString) },
          s.pie.toList.flatMap { p => p.map.map { case (key, v) => <.div(key + "--" + v.toString) } },
          <.br(),
          <.h1("Other pies"),
          s.otherPies.map { id => <.div(id.toString) },
          <.br(),

          <.button(
            ^.className := "btn btn-default",
            ^.onClick --> createANewPie(s), "new Pie"
          ),

          <.button(
            ^.className := "btn btn-default",
            ^.onClick --> send(api.ResetAllTickers()), "Reset all Pies"
          )
        )
      }

      def onUnmount() = {
        println("Unmounting")
        messObs.kill()
        Callback.empty
      }

      def saveData(s: State) = {
        $.props.runNow.saveWidgetData(SPValue(s))
      }
      def createANewPie(s: State) = {
        val p = Pie(UUID.randomUUID(), Map())
        s.pie.map(x => send(api.StopTheTicker(x.id)))
        send(api.StartTheTicker(p.id))
        $.modState(prev => prev.copy(pie = Some(p)))
      }


      def send(mess: api.API_ExampleService): Callback = {
        val h = SPHeader("ExampleServiceWidget", api.attributes.service, "ExampleServiceWidget", java.util.UUID.randomUUID())
        val json = SPMessage.make(h, mess) // *(...) is a shorthand for toSpValue(...)
        BackendCommunication.publish(json, "services")
        Callback.empty
      }


    }

    def initState(spwb: SPWidgetBase): State = {
      spwb.data.getAs[State]().getOrElse(State(None, List()))
    }

    private val component = ReactComponentB[SPWidgetBase]("ExampleServiceWidget")
      .initialState_P(initState)
      .renderBackend[Backend]
      .componentWillUnmount(_.backend.onUnmount())
      .build

    def apply() = spgui.SPWidget(spwb => component(spwb))
  }




  object attributes {
      val service = "exampleService"
      val version = 1.0
      val api = "to be fixed by macros"
    }


