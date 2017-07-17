package spgui.widgets.charts {

  import java.util.UUID

  import japgolly.scalajs.react._
  import japgolly.scalajs.react.vdom.prefix_<^._


  import spgui.communication._


  // Import this to make SPAttributes work including json handling
  import sp.domain._
  import sp.messages._
  import Pickles._


  package Timeline_API {
    // Copy paste the APIs you want to communicate with here
    sealed trait Timeline_API
    case class StartTheTicker(id: java.util.UUID) extends Timeline_API
    case class StopTheTicker(id: java.util.UUID) extends Timeline_API
    case class SetTheTicker(id: java.util.UUID, map: Map[String, Int]) extends Timeline_API
    case class ResetAllTickers() extends Timeline_API
    case class TickerEvent(map: Map[String, Int], id: java.util.UUID) extends Timeline_API
    case class TheTickers(ids: List[java.util.UUID]) extends Timeline_API

    object attributes {
      val service = "timelineService"
      val version = 0
      val api = "to be fixed by macros"
    }

  }
  import spgui.widgets.charts.{Timeline_API => api_t}



  object TimelineWebServiceExample {
    case class State(gantt: Option[Gantt], otherGantts: List[UUID], zoom: String)

    case class Gantt(id: UUID, map: Map[String, Int])

    private class Backend($: BackendScope[Unit, State]) {
      val ganttID = UUID.randomUUID()

      val messObs = BackendCommunication.getMessageObserver(
        mess => {
          //println(s"The widget example got: $mess" +s"parsing: ${mess.getBodyAs[api.API_ExampleService]}")
          mess.getBodyAs[api_t.Timeline_API].map {
            case api_t.TickerEvent(m, id) =>
              if (id == ganttID) {
                val p = Gantt(id, m)
                $.modState(s => s.copy(gantt = Some(p))).runNow()
              } else
                $.modState { s =>
                  val updIds = if (s.otherGantts.contains(id)) s.otherGantts else id :: s.otherGantts
                  s.copy(otherGantts = updIds)
                }.runNow()
            case api_t.TheTickers(ids) =>
              $.modState { s =>
                val p = if (!ids.contains(ganttID)) None else s.gantt
                State(gantt = p, otherGantts = ids, zoom = "100%")
              }.runNow()
            case x =>
              println(s"THIS WAS NOT EXPECTED IN TimelineExample: $x")
          }

        },
        "answers" // the topic you want to listen to. Soon we will also add some kind of backend filter,  but for now you get all answers
      )

      def render(s: State) = {
        <.div(
          <.h1("Gantt ID:"),
          s.gantt.toList.map { p => <.div(p.id.toString) },
          s.gantt.toList.flatMap { p => p.map.map { case (key, v) => <.div(key + "--" + v.toString) } },
          <.br(),
          <.h1("Other gantts"),
          s.otherGantts.map { id => <.div(id.toString) },
          <.button(
            ^.className := "btn btn-default",
            ^.onClick --> send(api_t.StartTheTicker(ganttID)), "Create Gantt"
          ),

          <.button(
            ^.className := "btn btn-default",
            ^.onClick --> send(api_t.ResetAllTickers()), "Reset Gantt"
          )
        )
      }

      def onUnmount() = {
        println("Unmounting")
        messObs.kill()
        send(api_t.StopTheTicker(ganttID))
        Callback.empty
      }


      def send(mess: api_t.Timeline_API): Callback = {
        val h = SPHeader(from = "TimelineService", to = api_t.attributes.service, reply = SPValue("TimelineService"))

        val json = SPMessage.make(h, mess) // *(...) is a shorthand for toSpValue(...)
        BackendCommunication.publish(json, "services")
        Callback.empty
      }


    }


    private val component = ReactComponentB[Unit]("TimelineService")
      .initialState(State(None, List(), "100%"))
      .renderBackend[Backend]
      .componentWillUnmount(_.backend.onUnmount())
      .build

    def apply() = spgui.SPWidget(spwb => component())
  }

}
