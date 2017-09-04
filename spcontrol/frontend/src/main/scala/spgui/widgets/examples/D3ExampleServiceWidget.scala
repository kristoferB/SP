package spgui.widgets.examples

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.ReactDOM

import spgui.SPWidget
import spgui.communication._
import sp.domain._
import sp.domain.Logic._

import org.singlespaced.d3js.d3
import org.singlespaced.d3js.Ops._
import org.scalajs.dom.raw
import util.Random.nextInt
import util.Try

sealed trait API_D3ExampleService
object API_D3ExampleService {
  case class Start() extends API_D3ExampleService
  case class Stop() extends API_D3ExampleService
  case class D3Data(barHeights: List[Int]) extends API_D3ExampleService

  val service = "d3ExampleService"

  implicit val fAPI_D3ExampleService: JSFormat[API_D3ExampleService] = deriveFormatISA[API_D3ExampleService]
}

object D3ExampleServiceWidget {
  def apply() = SPWidget{spwb =>
    component()
  }

  private class RBackend($: BackendScope[Unit, List[Int]]) {
    val messObs = BackendCommunication.getMessageObserver(
      mess => {
        mess.getBodyAs[API_D3ExampleService] map {
          case API_D3ExampleService.D3Data(l) =>
            $.modState(_ => l).runNow()
          case x =>
            println(s"THIS WAS NOT EXPECTED IN D3ExampleServiceWidget: $x")
        }
      }, "d3ExampleAnswers"
    )

    def start: Callback = {
      val h = SPHeader(from = "D3ExampleServiceWidget", to = API_D3ExampleService.service, reply = SPValue("D3ExampleServiceWidget"))
      val json = SPMessage.make(h, API_D3ExampleService.Start())
      BackendCommunication.publishMessage("services", json)
      Callback.empty
    }

    def stop: Callback = {
      val h = SPHeader(from = "D3ExampleServiceWidget", to = API_D3ExampleService.service, reply = SPValue("D3ExampleServiceWidget"))
      val json = SPMessage.make(h, API_D3ExampleService.Stop())
      BackendCommunication.publishMessage("services", json)
      Callback.empty
    }

   def render(list: List[Int]) =
      <.div(
        <.button("Start", ^.onClick --> start),
        <.button("Stop", ^.onClick --> stop),
        D3BarsComponent(list)
      )
  }

  private val component = ScalaComponent.builder[Unit]("D3DataReceiver")
    .initialState(List.fill(8)(nextInt(50)))
    .renderBackend[RBackend]
    .componentWillUnmount(_.backend.stop)
    .build
}
