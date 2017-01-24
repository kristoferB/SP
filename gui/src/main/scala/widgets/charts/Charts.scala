package spgui.widgets.charts

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

object Chart {

  private val component = ReactComponentB[Unit]("Chart")
    .render(_ =>
    <.div(
      ^.className := ChartCSS.charts.htmlClass,
      ChartTest()
    )
  )
    .build

  def apply(): ReactElement = component()
}
