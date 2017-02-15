package spgui.widgets.examples

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.ReactDOM

import spgui.SPWidget

import org.singlespaced.d3js.d3
import org.singlespaced.d3js.Ops._
import scala.scalajs.js.{Array => JSArray}
import org.scalajs.dom.raw

object D3Test {
  def apply() = SPWidget(spwb => component())

  private val component = ReactComponentB[Unit]("D3Test")
    .render(_ => <.div())
    .componentDidMount(rawElement => Callback(addTheD3(ReactDOM.findDOMNode(rawElement))))
    .build

  private def addTheD3(element: raw.Element): Unit = {
    val graphHeight = 450
    val barWidth = 80
    val barSep = 10
    val frameHeight = 50
    val horizontalBarDistance = barWidth + barSep
    val barHeightMultiplier = graphHeight / frameHeight
    val c = d3.rgb("PaleVioletRed")

    val rectXFun = (d: Int, i: Int) => i * horizontalBarDistance
    val rectYFun = (d: Int) => graphHeight - d * barHeightMultiplier
    val rectHeightFun = (d: Int) => d * barHeightMultiplier
    val rectColorFun = (d: Int, i: Int) => c.brighter(i * 0.2).toString

    val svg = d3.select(element).append("svg").attr("width", "100%").attr("height", "450px")
    val sel = svg.selectAll("rect").data(JSArray(49, 4, 12, 31, 36, 48, 17, 25))

    sel.enter()
      .append("rect")
      .attr("x", rectXFun)
      .attr("y", rectYFun)
      .attr("width", barWidth)
      .attr("height", rectHeightFun)
      .style("fill", rectColorFun)
  }
}
