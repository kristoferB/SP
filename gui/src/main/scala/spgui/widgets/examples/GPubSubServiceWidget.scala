package spgui.widgets.examples {

  import java.util.UUID

  import japgolly.scalajs.react._
  import japgolly.scalajs.react.vdom.prefix_<^._
  import japgolly.scalajs.react.ReactDOM

  import org.singlespaced.d3js.d3
  import org.singlespaced.d3js.Ops._

  import org.scalajs.dom.raw

  import util.Random.nextInt

  import spgui.communication._

  import scala.util.Try


  // Import this to make SPAttributes work including json handling
  import sp.domain._
  import sp.messages._
  import Pickles._


  package API_GPubSubService {

    // Copy paste the APIs you want to communicate with here
    sealed trait API_GPubSubService
    case object dummy extends API_GPubSubService
    case class StartTheTicker(id: java.util.UUID) extends API_GPubSubService
    case class StopTheTicker(id: java.util.UUID) extends API_GPubSubService
    case class SetTheTicker(id: java.util.UUID, map: Map[String, Int]) extends API_GPubSubService
    case class ResetAllTickers() extends API_GPubSubService
    case class TickerEvent(map: Map[String, Int], id: java.util.UUID) extends API_GPubSubService
    case class TheTickers(ids: List[java.util.UUID]) extends API_GPubSubService

    object attributes {
      val service = "exampleService"
      val version = 1.0
      val api = "to be fixed by macros"
    }

  }
  import spgui.widgets.examples.{API_GPubSubService => api}



  object GPubSubServiceWidget {


    //   Sometimes this needs to be added to make the json magic work. The compiler will complain if
    //   you need it. Another option is to add your api in another file
    //  implicit val readWriter: ReadWriter[API_GPubSubService] =
    //    macroRW[StartTheTicker] merge macroRW[StopTheTicker] merge
    //      macroRW[API_GPubSubService.SetTheTicker] merge macroRW[API_GPubSubService.TickerEvent] merge
    //      macroRW[API_GPubSubService.TheTickers] merge macroRW[API_GPubSubService.ResetAllTickers]

    case class Pie(id: UUID, map: Map[String, Int])

    case class State(pie: Option[Pie], otherPies: List[UUID])


    private class Backend($: BackendScope[Unit, State]) {
      var messageCounter = 0
      var tickCounter = 0
      val pieID = UUID.randomUUID()

      val messObs = BackendCommunication.getMessageObserver(
        mess => {
          //println(s"The widget example got: $mess" +s"parsing: ${mess.getBodyAs[api.API_GPubSubService]}")
          mess.getBodyAs[api.API_GPubSubService].map {
            case api.TickerEvent(m, id) =>
            tickCounter += 1
            messageCounter = 0
            if (id == pieID) {
              val p = Pie(id, m)
              $.modState{ s =>
                s.pie.toList.flatMap { p => p.map.map { case (key, v) =>
                    if (key != "" && key != " ") {
                      messageCounter += 1
                    }
                } }
                s.copy(pie = Some(p)) }.runNow()
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
          <.h1(s"Receive messages from GC PubSub"),
          //s.pie.toList.flatMap { p => p.map.map { case (key, v) => <.div(key) } },
          <.br(),
          <.button(
            ^.className := "btn btn-default",
            ^.onClick --> send(api.StartTheTicker(pieID)), "Listen for messages"
          ),
          <.button(
            ^.className := "btn btn-default",
            ^.onClick --> send(api.ResetAllTickers()), "Reset"
          ),
	        <.div("Left stack: Number of messages received."),
	        <.div("Right stack: Ticks since started listening."),
          <.div(
            d3DivComponent(List(messageCounter, tickCounter))
          )
        )
      }

      def onUnmount() = {
        println("Unmounting")
        messObs.kill()
        send(api.StopTheTicker(pieID))
        Callback.empty
      }


      def send(mess: api.API_GPubSubService): Callback = {
        val h = SPHeader("GPubSubServiceWidget", api.attributes.service, "GPubSubServiceWidget", java.util.UUID.randomUUID())
        val json = SPMessage.make(h, mess) // *(...) is a shorthand for toSpValue(...)
        BackendCommunication.publish(json, "services")
        Callback.empty
      }


    }


    private val component = ReactComponentB[Unit]("GPubSubServiceWidget")
      .initialState(State(None, List()))
      .renderBackend[Backend]
      .componentWillUnmount(_.backend.onUnmount())
      .build

    private val d3DivComponent = ReactComponentB[List[Int]]("d3DivComponent")
      .render(_ => <.div())
      .componentDidUpdate(dcb => Callback(addTheD3(ReactDOM.findDOMNode(dcb.component), dcb.currentProps)))
      .build

      private def addTheD3(element: raw.Element, list: List[Int]): Unit = {
        val graphHeight = 220
        val barWidth = 45
        val barSep = 8
        val frameHeight = 50
        val horizontalBarDistance = barWidth + barSep
        val barHeightMultiplier = graphHeight / frameHeight
        val c = d3.rgb("steelblue")

        val rectXFun = (d: Int, i: Int) => i * horizontalBarDistance
        val rectYFun = (d: Int) => graphHeight - d * barHeightMultiplier
        val rectHeightFun = (d: Int) => d * barHeightMultiplier
        val rectColorFun = (d: Int, i: Int) => c.brighter(i * 0.2).toString

        // clear all before rendering new data
        d3.select(element).selectAll("*").remove()
        val svg = d3.select(element).append("svg").attr("width", "100%").attr("height", "220")
        var bar = svg.selectAll("g")
                  .data(list.toJsArray)
                  .enter().append("g")

        bar.append("rect")
          .attr("x", rectXFun)
          .attr("y", rectYFun)
          .attr("width", barWidth)
          .attr("height", rectHeightFun)
          .style("fill", rectColorFun)

        bar.append("text")
          .data(list.toJsArray)
          .attr("x", rectXFun)
          .attr("y", rectYFun)
          .attr("dx", barWidth/2)
          .attr("class", "text")
          .text({(d:Int) => d.toString})

      }

    def apply() = spgui.SPWidget(spwb => component())
  }

}
