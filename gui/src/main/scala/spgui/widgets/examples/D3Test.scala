package spgui.widgets.examples

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.ReactDOM

import spgui.SPWidget

import org.singlespaced.d3js.d3
import org.singlespaced.d3js.Ops._
import org.scalajs.dom.raw
import util.Random.nextInt
import scalajs.js.JSConverters._
import spgui.components.SPWidgetElements

object D3Example {
  def apply() = SPWidget{spwb =>
    component()
  }

  private val component = ScalaComponent.builder[Unit]("D3Example")
    .initialState(List.fill(8)(nextInt(50)))
    .render{dcb =>
      <.div(
        SPWidgetElements.button("mod state", dcb.modState(_.map(_ => nextInt()))),
        D3BarsComponent(dcb.state)
      )
    }
    .build

}

object D3BarsComponent {
  def apply(data: List[Int]) = component(data)

  private val component = ScalaComponent.builder[List[Int]]("d3DivComponent")
    .render(_ => <.div())
    .componentDidUpdate(ctx => Callback(addTheD3(ctx.getDOMNode, ctx.currentProps)))
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
    // plain-js d3-examples online don't have this line, adding it is a way
    // to let react take care of the rerendering, rather than d3 itself
    d3.select(element).selectAll("*").remove()
    val svg = d3.select(element).append("svg").attr("width", "100%").attr("height", "220")
    val sel = svg.selectAll("rect").data(list.toJSArray)

    sel.enter()
      .append("rect")
      .attr("x", rectXFun)
      .attr("y", rectYFun)
      .attr("width", barWidth)
      .attr("height", rectHeightFun)
      .style("fill", rectColorFun)
  }
}
