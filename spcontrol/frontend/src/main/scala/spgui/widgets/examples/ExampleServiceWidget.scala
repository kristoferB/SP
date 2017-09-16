package spgui.widgets.examples

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import spgui.communication._

// Import this to make SPAttributes work including json handling
import sp.domain._
import Logic._


import sp.example.{APIExampleService => api}



  object ExampleServiceWidget {

    case class Pie(id: ID, map: Map[String, Int])
    case class State(pie: Option[Pie], otherPies: List[ID])

    private class Backend($: BackendScope[Unit, State]) {
      val pieID = ID.newID

      val messObs = BackendCommunication.getMessageObserver(
        mess => {
          val callback: Option[CallbackTo[Unit]] = mess.getBodyAs[api.Response].map {
            case api.TickerEvent(m, id) =>
              if (id == pieID) {
                $.modState(s => s.copy(pie = Some(Pie(id, m))))
              } else
                $.modState { s =>
                  val updIds = if (s.otherPies.contains(id)) s.otherPies else id :: s.otherPies
                  s.copy(otherPies = updIds)
                }
            case api.TheTickers(ids) =>
              $.modState { s =>
                val p = if (!ids.contains(pieID)) None else s.pie
                State(pie = p, otherPies = ids)
              }
          }
          callback.foreach(_.runNow())

        },
        api.topicResponse
      )

      def render(s: State) = {
        <.div(
          <.h1(s"The Pie ID:"),
          s.pie.toList.map { p => <.div(p.id.toString) }.toTagMod,
          s.pie.toList.flatMap { p => p.map.map { case (key, v) => <.div(key + "--" + v.toString) } }.toTagMod,
          <.br(),
          <.h1("Other pies"),
          s.otherPies.map { id => <.div(id.toString) }.toTagMod,
          <.br(),

          <.button(
            ^.className := "btn btn-default",
            ^.onClick --> send(api.StartTheTicker(pieID)), "new Pie"
          ),

          <.button(
            ^.className := "btn btn-default",
            ^.onClick --> send(api.ResetAllTickers), "Reset all Pies"
          )
        )
      }

      def onUnmount() = {
        println("Unmounting")
        messObs.kill()
        send(api.StopTheTicker(pieID))
        Callback.empty
      }


      def send(mess: api.Request): Callback = {
        val h = SPHeader(from = "ExampleServiceWidget", to = api.service, reply = SPValue("ExampleServiceWidget"))
        val json = SPMessage.make(h, mess) // *(...) is a shorthand for toSpValue(...)
        BackendCommunication.publish(json, api.topicRequest)
        Callback.empty
      }


    }


    private val component = ScalaComponent.builder[Unit]("ExampleServiceWidget")
      .initialState(State(None, List()))
      .renderBackend[Backend]
      .componentWillUnmount(_.backend.onUnmount())
      .build

    def apply() = spgui.SPWidget(spwb => component())
  }
