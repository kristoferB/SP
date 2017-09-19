package spgui.widgets.gantt

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import scalajs.js
import org.scalajs.dom

import spgui.SPWidgetBase
import spgui.SPWidget

object GanttExample {

  def somRow() = Row(
    name = "sampleRow",
    tasks = js.Array(Task("scalajs task", new js.Date(2017, 5, 20, 9, 0, 0), new js.Date(2017, 5, 20, 10, 0, 0)))
  )

  def patientTimeLine() = js.Array(
    Row("Besök", js.Array(
      Task("Patientens Besök På Sjukhuset", new js.Date(2017, 5, 20, 8, 5, 3, 2), new js.Date(2017, 5, 20, 10, 32, 23, 9), "#f3ed84")
    )),
    Row("Kölapp", js.Array(
      Task("Tar Kölapp", new js.Date(2017, 5, 20, 8, 6, 13, 8), new js.Date(2017, 5, 20, 8, 6, 31, 7), "#ff5a36")
    )),
    Row("Väntetid", js.Array(
      Task("Patient Väntar På inskrivning", new js.Date(2017, 5, 20, 8, 6, 31, 7), new js.Date(2017, 5, 20, 8, 23, 54, 1)),
      Task("Patienten väntar på läkare", new js.Date(2017, 5, 20, 8, 26, 46, 3), new js.Date(2017, 5, 20, 9, 1, 35, 4)),
      Task("Patient väntar på diagnos", new js.Date(2017, 5, 20, 9, 9, 21, 5), new js.Date(2017, 5, 20, 9, 59, 1, 0))
    )),
    Row("Inskrivning", js.Array(
      Task("Patient Skriver in sig", new js.Date(2017, 5, 20, 8, 24, 11, 2), new js.Date(2017, 5, 20, 8, 26, 46, 3), "#b0e89e")
    )),
    Row("Läkarbesök", js.Array(
      Task("Patient träffar läkare", new js.Date(2017, 5, 20, 9, 1, 35, 4), new js.Date(2017, 5, 20, 9, 9, 21, 5), "#8f2525"),
      Task("Patient träffar läkare", new js.Date(2017, 5, 20, 9, 59, 1, 0), new js.Date(2017, 5, 20, 10, 14, 13, 4), "#8f2525")
    )),
    Row("Diagnostiering", js.Array(
      Task("Läkare sätter diagnos", new js.Date(2017, 5, 20, 9, 9, 21, 5), new js.Date(2017, 5, 20, 9, 27, 54, 9), "#8dbad8")
    ))
  )


  class Backend($: BackendScope[SPWidgetBase, Unit]) {
    var spGantt: SPGantt = _

    def render() =
      <.div(
        HtmlTagOf[dom.html.Element]("gantt-component"), // becomes <gantt-component></gantt-component>
        <.button("call addRow from scalajs", ^.onClick --> Callback(spGantt.addRow(somRow()))),
        <.button("sample patientTimeLine", ^.onClick --> Callback(spGantt.setData(patientTimeLine())))
      )
  }

  private val component = ScalaComponent.builder[SPWidgetBase]("GanttExample")
    .renderBackend[Backend]
    .componentDidMount(dcb => Callback{
      dcb.backend.spGantt = SPGantt(dcb.getDOMNode)
      dcb.backend.spGantt.setData(patientTimeLine())
    })
    .build

  def apply() = SPWidget(spwb => component(spwb))
}
