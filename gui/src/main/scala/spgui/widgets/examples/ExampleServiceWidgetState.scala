package spgui.widgets.examples

import java.util.UUID

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import spgui.SPWidgetBase
import spgui.communication._

import scala.util.{Random, Try}

  // Import this to make SPAttributes work including json handling
  import sp.domain._
  import Logic._


  import sp.example.{APIExampleService => api}


  object ExampleServiceWidgetState {
    case class Pie(id: UUID, map: Map[String, Int])
    case class State(pie: Option[Pie], otherPies: List[UUID], brodcast: Int = 0)
    implicit val fExamplePie: JSFormat[Pie] = deriveFormatSimple[Pie]
    implicit val fExampleState: JSFormat[State] = deriveFormatSimple[State]


    private class Backend($: BackendScope[SPWidgetBase, State]) {

      val messObs = BackendCommunication.getMessageObserver(
        mess => {
          //println(s"The widget example got: $mess" +s"parsing: ${mess.getBodyAs[api.API_ExampleService]}")
          val s = $.state.runNow()
          val pieID = s.pie.map(_.id).getOrElse(UUID.randomUUID())
          val updState = mess.getBodyAs[api.Response].map {
            case api.TickerEvent(m, id) =>
              if (id == pieID) {
                val p = Pie(id, m)
                s.copy(pie = Some(p))
              } else {
                s
              }
            case api.TheTickers(ids) =>
                val p = if (!ids.contains(pieID)) None else s.pie
                State(pie = p, otherPies = ids)
            case x =>
              s
          }

          updState.map{x =>
            if (s != x){
              saveData(x)
              $.setState(x).runNow()
            }
          }

        }
      )

      val widgetComm = WidgetPubSub.sub(mess => {
        val p = $.props.runNow().id.toString
        for {
          h <- mess.getHeaderAs[SPHeader] if h.to == "ExampleServiceWidgetState" && h.from != p
          b <- mess.getBodyAs[Int]
        } yield {
          $.modState(s => s.copy(brodcast = b)).runNow()
        }
      })



      def render(p: SPWidgetBase, s: State) = {
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
            ^.onClick --> createANewPie(s), "new Pie"
          ),

          <.button(
            ^.className := "btn btn-default",
            ^.onClick --> send(api.ResetAllTickers), "Reset all Pies"
          ),
          <.br(),
          <.button(
            ^.className := "btn btn-default",
            ^.onClick --> pubToFriendWidgets(p.id, s.brodcast), "Brodcast"
          ),
        "brodcast: " + s.brodcast
        )
      }

      def onUnmount() = {
        println("Unmounting")
        messObs.kill()
        Callback.empty
      }

      def saveData(s: State) = {
        $.props.runNow.updateWidgetData(SPValue(s))
      }
      def createANewPie(s: State) = {
        val p = Pie(UUID.randomUUID(), Map())
        s.pie.map(x => send(api.StopTheTicker(x.id)))
        send(api.StartTheTicker(p.id))
        $.modState{prev =>
          val res = prev.copy(pie = Some(p))
          saveData(res)
          res
        }
      }


      def send(mess: api.Request): Callback = {
        val h = SPHeader(from = "ExampleServiceWidget", to = api.service, reply = SPValue("ExampleServiceWidget"))

        val json = SPMessage.make(h, mess) // *(...) is a shorthand for toSpValue(...)
        BackendCommunication.publish(json, "services")
        Callback.empty
      }

      def pubToFriendWidgets(myID: UUID, current: Int) = {
        val h = SPHeader(from = myID.toString, to = "ExampleServiceWidgetState")
        val b = current + scala.util.Random.nextInt(5)
        WidgetPubSub.pub(SPMessage.make(h, b))
        Callback.empty

      }


    }

    def initState(spwb: SPWidgetBase): State = {
      spwb.getWidgetData.to[State].getOrElse(State(None, List()))
    }

    private val component = ScalaComponent.builder[SPWidgetBase]("ExampleServiceWidget")
      .initialStateFromProps(initState)
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
