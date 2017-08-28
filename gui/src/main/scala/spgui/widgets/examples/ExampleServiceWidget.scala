package spgui.widgets.examples {

  import java.util.UUID

  import japgolly.scalajs.react._
  import japgolly.scalajs.react.vdom.html_<^._


  import spgui.communication._

  import scala.util.Try


  // Import this to make SPAttributes work including json handling
  import sp.domain._
  import Logic._


  import sp.example.{APIExampleService => api}



  object ExampleServiceWidget {


    //   Sometimes this needs to be added to make the json magic work. The compiler will complain if
    //   you need it. Another option is to add your api in another file
    //  implicit val readWriter: ReadWriter[API_ExampleService] =
    //    macroRW[StartTheTicker] merge macroRW[StopTheTicker] merge
    //      macroRW[API_ExampleService.SetTheTicker] merge macroRW[API_ExampleService.TickerEvent] merge
    //      macroRW[API_ExampleService.TheTickers] merge macroRW[API_ExampleService.ResetAllTickers]

    case class Pie(id: UUID, map: Map[String, Int])

    case class State(pie: Option[Pie], otherPies: List[UUID])


    private class Backend($: BackendScope[Unit, State]) {
      val pieID = UUID.randomUUID()

      val messObs = BackendCommunication.getMessageObserver(
        mess => {
          //println(s"The widget example got: $mess" +s"parsing: ${mess.getBodyAs[api.API_ExampleService]}")
          mess.getBodyAs[api.Response].foreach {
            case api.TickerEvent(m, id) =>
              if (id == pieID) {
                val p = Pie(id, m)
                $.modState(s => s.copy(pie = Some(p))).runNow()
              } else
                $.modState { s =>
                  val updIds = if (s.otherPies.contains(id)) s.otherPies else id :: s.otherPies
                  s.copy(otherPies = updIds)
                }.runNow()
            case api.TheTickers(ids) =>
              $.modState { s =>
                val p = if (!ids.contains(pieID)) None else s.pie
                State(pie = p, otherPies = ids)
              }.runNow()
            case x =>
              println(s"THIS WAS NOT EXPECTED IN EXAMPLEWIDGET: $x")
          }

        },
        "answers" // the topic you want to listen to. Soon we will also add some kind of backend filter,  but for now you get all answers
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
        BackendCommunication.publish(json, "services")
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

}
