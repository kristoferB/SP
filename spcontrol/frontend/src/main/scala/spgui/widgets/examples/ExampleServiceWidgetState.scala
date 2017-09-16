package spgui.widgets.examples

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import spgui.SPWidgetBase
import spgui.communication._
import sp.domain._
import Logic._

  import sp.example.{APIExampleService => api}


  object ExampleServiceWidgetState {
    case class Pie(id: ID, map: Map[String, Int])
    case class State(pie: Option[Pie], otherPies: List[ID], brodcast: Int = 0)
    implicit val fExamplePie: JSFormat[Pie] = deriveFormatSimple[Pie]
    implicit val fExampleState: JSFormat[State] = deriveFormatSimple[State]


    private class Backend($: BackendScope[SPWidgetBase, State]) {

      val messObs = BackendCommunication.getMessageObserver( mess => {

        for {
          b <- mess.getBodyAs[api.Response]
        } yield {
          val callback = b match {
            case api.TickerEvent(m, id) =>
              $.modState{s =>
                val pieID = s.pie.map(_.id)

                if (pieID.contains(id)) {
                  val p = Pie(id, m)
                  val res = s.copy(pie = Some(p))
                  saveData(res)
                  res
                } else {
                  s
                }
              }

            case api.TheTickers(ids) =>
              $.modState{s =>
                val pieID = s.pie.map(_.id).getOrElse(ID.newID)
                val p = if (!ids.contains(pieID)) None else s.pie
                val res = State(pie = p, otherPies = ids)
                saveData(res)
                res
              }
          }
         callback.runNow()
        }


      },
      api.topicResponse
      )

      val widgetComm = WidgetPubSub.sub(mess => {
        val p = $.props.runNow().id.toString
        for {
          h <- mess.getHeaderAs[SPHeader] if h.to == "ExampleServiceWidgetState" && h.from != p
          b <- mess.getBodyAs[SPAttributes]
          i <- b.getAs[Int]("change")
        } yield {
          $.modState(s => s.copy(brodcast = i)).runNow()
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
        //messObs.kill()
        Callback.empty
      }

      def saveData(s: State) = {
        $.props.runNow.updateWidgetData(SPValue(s))
      }
      def createANewPie(s: State) = {
        val p = Pie(ID.newID, Map())
        s.pie.map(x => send(api.StopTheTicker(x.id)))
        send(api.StartTheTicker(p.id))
        $.modState{prev =>
          val res = prev.copy(pie = Some(p))
          saveData(res)
          res
        }
      }


      def send(mess: api.Request): Callback = {
        val h = SPHeader(from = "ExampleServiceWidgetState", to = api.service, reply = SPValue("ExampleServiceWidget"))

        val json = SPMessage.make(h, mess) // *(...) is a shorthand for toSpValue(...)
        BackendCommunication.publish(json, api.topicRequest)
        Callback.empty
      }

      def pubToFriendWidgets(myID: ID, current: Int) = {
        val h = SPHeader(from = myID.toString, to = "ExampleServiceWidgetState")
        val b = current + scala.util.Random.nextInt(5)

        WidgetPubSub.pub(SPMessage.make(h, SPAttributes("change" ->b)))
        Callback.empty

      }


    }

    def initState(spwb: SPWidgetBase): State = {
      spwb.getWidgetData.to[State].getOrElse(State(None, List()))
    }

    private val component = ScalaComponent.builder[SPWidgetBase]("ExampleServiceWidgetState")
      .initialStateFromProps(initState)
      .renderBackend[Backend]
      .componentWillUnmount(_.backend.onUnmount())
      .build

    def apply() = spgui.SPWidget(spwb => component(spwb))
  }


