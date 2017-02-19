package spgui.widgets.examples

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.ReactDOM

import spgui.SPWidget
import spgui.communication._
import sp.messages.Pickles._

import upickle._
import org.singlespaced.d3js.d3
import org.singlespaced.d3js.Ops._
import org.scalajs.dom.raw
import util.Random.nextInt
import util.Try

sealed trait API_D3ExampleService
object API_D3ExampleService {
  case class Start() extends API_D3ExampleService
  case class D3Data(barHeights: List[Int]) extends API_D3ExampleService

  val service = "d3ExampleService"
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
      val h = SPHeader("D3ExampleServiceWidget", API_D3ExampleService.service, "D3ExampleServiceWidget")
      val json = SPMessage.make(h, API_D3ExampleService.Start())
      json foreach (BackendCommunication.publishMessage("services", _))
      Callback.empty
    }

    def render(list: List[Int]) =
      <.div(
        <.button("send", ^.onClick --> start),
        d3DivComponent(list)
      )
  }

  private val component = ReactComponentB[Unit]("D3DataReceiver")
    .initialState(List.fill(8)(nextInt(50)))
    .renderBackend[RBackend]
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
    val c = d3.rgb("PaleVioletRed")

    val rectXFun = (d: Int, i: Int) => i * horizontalBarDistance
    val rectYFun = (d: Int) => graphHeight - d * barHeightMultiplier
    val rectHeightFun = (d: Int) => d * barHeightMultiplier
    val rectColorFun = (d: Int, i: Int) => c.brighter(i * 0.2).toString

    // clear all before rendering new data
    d3.select(element).selectAll("*").remove()
    val svg = d3.select(element).append("svg").attr("width", "100%").attr("height", "220")
    val sel = svg.selectAll("rect").data(list.toJsArray)

    sel.enter()
      .append("rect")
      .attr("x", rectXFun)
      .attr("y", rectYFun)
      .attr("width", barWidth)
      .attr("height", rectHeightFun)
      .style("fill", rectColorFun)
  }
}
