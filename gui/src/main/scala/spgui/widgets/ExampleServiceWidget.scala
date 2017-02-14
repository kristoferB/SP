package spgui.widgets

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import scala.concurrent.{Future, Promise}
import scala.concurrent.Promise
import scala.util.{Failure, Success, Try}
import spgui.SPWidget
import java.util.UUID

import scala.reflect.ClassTag



// Copy paste the APIs you want to communicate with here
sealed trait API_ExampleService
object API_ExampleService {
  case class StartTheTicker(id: java.util.UUID) extends API_ExampleService
  case class StopTheTicker(id: java.util.UUID) extends API_ExampleService
  case class SetTheTicker(id: java.util.UUID, map: Map[String, Int]) extends API_ExampleService
  case object GetTheTickers extends API_ExampleService
  case class TickerEvent(map: Map[String, Int], id: java.util.UUID) extends API_ExampleService
  case class TheTickers(ids: List[java.util.UUID]) extends API_ExampleService

  val service = "exampleService"
}





import rx._

object ExampleServiceWidget {

  case class Pie(id: UUID, map: Map[String, Int])
  case class State(pies: Map[UUID, Pie])

  val pieID = UUID.randomUUID()

  private class Backend($: BackendScope[Unit, State]) {
    import communication._

    val messObs = BackendCommunication.getMessageObserver(
      mess => {
        val testing = Try {APIParser.readJs[API_ExampleService](mess.body)}.map{
          case API_ExampleService.TickerEvent(m, id) =>
            if (id == pieID){
              val newState = $.
            }
            changeState(p).runNow()
            case API_ExampleService.Hi(p) => changeState(p).runNow()
            case x =>
            println("Va?")
            println(x)
          }

        val sp = Try {APIParser.readJs[APISP](mess.body)}.map{
            case APISP.SPACK(p) => println("SPACK")
            case APISP.SPOK(p) => println("SPOK")
            case APISP.SPDone(p) => println("SPDone")
            case APISP.SPError(m, attr) => println("SPError:"+m)
            case x =>
              println("Va APISP?")
              println(x)
          }



      }


    )


    def changeState(str: String): Callback = $.setState(State(str))

    def render(s: State) =
      <.div(
        <.input(
          ^.value := s.str,
          ^.onChange ==> updateMe
        ),
        <.div(s.str),
        <.br(),
        <.button(
          ^.className := "btn btn-default",
          ^.onClick --> Callback.future(send), "SEND"
        )
      )

    def onUnmount() = {
      println("Unmounting")
      messObs.kill()
      Callback.empty
    }

    def updateMe(e: ReactEventI): Callback = {
      changeState(e.target.value)
    }

    def send(): Future[Callback] = {
      HoHo
    }

//    //implicit val ctx: Ctx.Owner = Ctx.Owner.safe()
//    val a = Var("Init")
//    val b = a.trigger(changeState(a.now).runNow())
//    val c = a.foreach(x => println(x))


    def HoHo = {
      import fr.hmil.roshttp.body._

      val h = SPHeader("widgetCommTest", APITesting.service, "widgetCommTest", java.util.UUID.randomUUID())
      val b = APITesting.ServiceCall("Hej från mig")
      println("hej")

      val mess = UPickleMessage(APIParser.writeJs(h), APIParser.writeJs(b))



      //Comm.initWS


      BackendCommunication.publishMessage("services", mess)



      val b2 = APITesting.RequestCall("hoj från mig")
      val mess2 = UPickleMessage(APIParser.writeJs(h), APIParser.writeJs(b2))

      val f = BackendCommunication.ask(mess2, "requests")
      f.map { v =>
        println("Vi fick via reguest:")
        println(v)
      }

      f.onFailure{case x => println(x)}

      val p = Promise[Callback]()
      //Comm.getWebSocketNotifications(str => p.success(Callback.alert(str)))
      p.future





    }

  }


  private val component = ReactComponentB[Unit]("ExampleServiceWidget")
    .initialState(State("HEJ"))
    .renderBackend[Backend]
      .componentWillUnmount(_.backend.onUnmount())
    .build

  def apply() = SPWidget(spwb => component())
}



