package spgui.widgets.examples

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.ReactDOM

import spgui.SPWidget

import org.singlespaced.d3js.d3
import org.singlespaced.d3js.Ops._
import org.scalajs.dom.raw
import util.Random.nextInt

object D3Example {
  def apply() = SPWidget{spwb =>
    component()
  }

  private val component = ReactComponentB[Unit]("D3Example")
    .initialState(List.fill(8)(nextInt(50)))
    .render{dcb =>
      <.div(
        <.button("mod state", ^.onClick --> dcb.modState(_.map(_ => nextInt(50)))),
        d3DivComponent(dcb.state)
      )
    }
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
