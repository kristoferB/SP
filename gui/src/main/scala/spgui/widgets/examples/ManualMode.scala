package spgui.widgets.examples {

  import java.util.UUID

  import japgolly.scalajs.react._
  import japgolly.scalajs.react.vdom.prefix_<^._
  import spgui.SPWidget
  import spgui.communication._
  import spgui.widgets.itemexplorer.Style
  import spgui.widgets.itemexplorer.Style.{&, styleF}

  import scala.util.Random


  // Import this to make SPAttributes work including json handling
  import sp.messages.Pickles._
  import sp.messages._


  package API_Manualmode {

    // Copy paste the APIs you want to communicate with here
    sealed trait API_Manualmode
    case object dummy extends API_Manualmode
    case class StartTheTicker(id: java.util.UUID) extends API_Manualmode
    case class StopTheTicker(id: java.util.UUID) extends API_Manualmode
    case class SetTheTicker(id: java.util.UUID, map: Map[String, Int]) extends API_Manualmode
    case class ResetAllTickers() extends API_Manualmode
    case class TickerEvent(map: Map[String, Int], id: java.util.UUID) extends API_Manualmode
    case class TheTickers(ids: List[java.util.UUID]) extends API_Manualmode

    object attributes {
      val service = "exampleService"
      val version = 1.0
      val api = "to be fixed by macros"
    }

  }
  import spgui.widgets.examples.{API_Manualmode => api}



  object Manualmode {


    //   Sometimes this needs to be added to make the json magic work. The compiler will complain if
    //   you need it. Another option is to add your api in another file
    //  implicit val readWriter: ReadWriter[API_Manualmode] =
    //    macroRW[StartTheTicker] merge macroRW[StopTheTicker] merge
    //      macroRW[API_Manualmode.SetTheTicker] merge macroRW[API_Manualmode.TickerEvent] merge
    //      macroRW[API_Manualmode.TheTickers] merge macroRW[API_Manualmode.ResetAllTickers]

    case class Pie(id: UUID, map: Map[String, Int])

    case class State(pie: Option[Pie], otherPies: List[UUID])


    private class Backend($: BackendScope[Unit, State]) {
      val pieID = UUID.randomUUID()

      val messObs = BackendCommunication.getMessageObserver(
        mess => {
          //println(s"The widget example got: $mess" +s"parsing: ${mess.getBodyAs[api.API_Manualmode]}")
          mess.getBodyAs[api.API_Manualmode].map {
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
          <.h1(s"Run in Manual Mode"),
          <.h3("Functions"),
          <.br(),

          <.button(
            ^.className := "btn btn-default",
            ^.width := 300,
            ^.height := 50,
            ^.background := "#fffff",
            ^.onClick --> send(api.StartTheTicker(pieID)),
            "Conveyor1 run"
          ),
          <.br(),
          <.button(
            ^.className := "btn btn-default",
            ^.width := 300,
            ^.height := 50,
            ^.background := "#fffff",
            ^.onClick --> send(api.StartTheTicker(pieID)),
            "Conveyor2 run"
          ),
          <.br(),
          <.button(
            ^.className := "btn btn-default",
            ^.width := 300,
            ^.height := 50,
            ^.background := "#fffff",
            ^.onClick --> send(api.StartTheTicker(pieID)),
            "Conveyor3 run"
          ),
          <.br(),
          <.button(
            ^.className := "btn btn-default",
            ^.width := 300,
            ^.height := 50,
            ^.background := "#fffffff",
            ^.onClick --> send(api.StartTheTicker(pieID)), "Robot1 run"
          ),
          <.br(),
          <.button(
            ^.className := "btn btn-default",
            ^.width := 300,
            ^.height := 50,
            ^.onClick --> send(api.StartTheTicker(pieID)), "Robot2 run"
          )
        )
      }

      def onUnmount() = {
        println("Unmounting")
        messObs.kill()
        send(api.StopTheTicker(pieID))
        Callback.empty
      }


      def send(mess: api.API_Manualmode): Callback = {
        val h = SPHeader("ExampleServiceWidget", api.attributes.service, "ExampleServiceWidget", java.util.UUID.randomUUID())
        val json = SPMessage.make(h, mess) // *(...) is a shorthand for toSpValue(...)
        BackendCommunication.publish(json, "services")
        Callback.empty
      }


    }

    private val component = ReactComponentB[Unit]("ExampleServiceWidget")
      .initialState(State(None, List()))
      .renderBackend[Backend]
      .componentWillUnmount(_.backend.onUnmount())
      .build

    def apply() = spgui.SPWidget(spwb => component())
  }


  /* object ChartExample {

     private val component = ReactComponentB[Unit]("ChartExample")
       .initialState(getDataval())
       .render(dcb =>
         <.div(
           <.button("mod state", ^.onClick --> dcb.modState(_ => getDataval())),
           PieChartComponent(dcb.state)
         )
       )
       .build

     def getDataval() : Chart.ChartProps = {
       val cp =
         Chart.ChartProps (
           "Test chart",
           Chart.PieChart,
           ChartData (
             Random.alphanumeric.map (_.toUpper.toString).distinct.take (10),
             Seq (ChartDataset (Iterator.continually (Random.nextInt (30) ).take (10).toSeq, "Data1",
               Iterator.continually (backgroundColour () ).take (10).toSeq) )
           )
         )
       return cp
     }

     def backgroundColour() : String = {
       val colour = new StringBuilder
       colour += '#'
       val letters = Seq('0','1','2','3','4','5','6','7','8','9',
         'A','B','C','D','E','F')
       for (x <- 1 to 6) {
         colour += letters(Random.nextInt(16))
       }
       return colour.toString()
     }
     def apply() = component()
   }

   object PieChartComponent{
     def apply(data: Chart.ChartProps) = component(data)

     private val component = ReactComponentB[Chart.ChartProps]("ChartDivComponent")
       .render(_ => <.div())
         .componentDidUpdate(dcb => Callback(newChart(dcb.currentProps))
       .build

       def newChart
   }*/
}


