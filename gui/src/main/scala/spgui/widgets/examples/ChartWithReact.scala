package spgui.widgets.examples

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.ReactDOM
import spgui.SPWidget

import scala.util.Random

object ChartWithReact {
  def apply() = SPWidget{spwb =>
    component()
  }
  private val component = ReactComponentB[Unit]("ChartWithReact")
    .initialState(getDataval())
    .render(dcb =>
      <.div(
        <.button("mod state", ^.onClick --> dcb.modState(_ => getDataval())),
        Chart(dcb.state)
      )
    )
    .build

  val data = getDataval()
  def getDataval() : Chart.ChartProps = {
    val cp =
      Chart.ChartProps (
        "Test chart",
        Chart.PieChart,
        ChartData (
          Random.alphanumeric.map (_.toUpper.toString).distinct.take (10),
          Seq (ChartDataset (Iterator.continually (Random.nextInt (30) ).take (10).toSeq, "Data1",
            Iterator.continually (backgroundColour () ).take (10).toSeq) )
        )
      )
    return cp
  }

  def backgroundColour() : String = {
    val colour = new StringBuilder
    colour += '#'
    val letters = Seq('0','1','2','3','4','5','6','7','8','9',
      'A','B','C','D','E','F')
    for (x <- 1 to 6) {
      colour += letters(Random.nextInt(16))
    }
    return colour.toString()
  }
}


/* object PieChartComponent{
   def apply(data: Chart.ChartProps) = component(data)

   private val component = ReactComponentB[Chart.ChartProps]("ChartDivComponent")
     .render(_ => <.div())
       .componentDidUpdate(dcb => Callback(Chart(dcb.))
     .build
 }*/

