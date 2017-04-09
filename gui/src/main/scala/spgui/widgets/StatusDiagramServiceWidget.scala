package spgui.widgets

import java.time._ //ARTO: Använder wrappern https://github.com/scala-js/scala-js-java-time
import java.text.SimpleDateFormat
import java.util.UUID

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.ReactDOM

import spgui.SPWidget
import spgui.widgets.css.{WidgetStyles => Styles}
import spgui.communication._

import sp.domain._
import sp.messages._
import sp.messages.Pickles._
import upickle._

import org.singlespaced.d3js.d3
import org.singlespaced.d3js.Ops._

import scala.concurrent.duration._
import scala.scalajs.js
import scala.util.{ Try, Success }
import scala.util.Random.nextInt

import org.scalajs.dom
import org.scalajs.dom.raw
import org.scalajs.dom.{svg => *}
import org.singlespaced.d3js.d3
import org.singlespaced.d3js.Ops._

import scalacss.ScalaCssReact._
import scalacss.Defaults._

import scala.collection.mutable.ListBuffer

import spgui.widgets.{API_PatientEvent => api}

object StatusDiagramServiceWidget {

  private class Backend($: BackendScope[Unit, Map[String, Double]]) {
    spgui.widgets.css.WidgetStyles.addToDocument()

    val messObs = BackendCommunication.getMessageObserver(
      mess => {
        mess.getBodyAs[api.StatusEvent] map {
          case api.Unattended(toAdd) => {
            if (toAdd) $.modState(s => s + ("Unattended" -> (s("Unattended") + 1))).runNow()
            else $.modState(s => s + ("Unattended" -> (s("Unattended") - 1))).runNow()
          }
          case api.Attended(toAdd) => {
            if (toAdd) $.modState(s => s + ("Attended" -> (s("Attended") + 1))).runNow()
            else $.modState(s => s + ("Attended" -> (s("Attended") - 1))).runNow()
          }
          case api.Finished(toAdd) => {
            if (toAdd) $.modState(s => s + ("Finished" -> (s("Finished") + 1))).runNow()
            else $.modState(s => s + ("Finished" -> (s("Finished") - 1))).runNow()
          }
          case x => println(s"THIS WAS NOT EXPECTED IN StatusDiagramServiceWidget: $x")
        }
      }, "status-diagram-widget-topic"
    )

    // What is this function used for?
    def render(p: Map[String, Double]) = {
      <.div(^.`class` := "card-holder-root")( // really not necessary
      )
    }
  }

  private val component = ReactComponentB[Unit]("TeamStatusWidget")
  .initialState(Map("Unattended" -> 0.toDouble, "Attended" -> 0.toDouble, "Finished" -> 0.toDouble))
  .renderBackend[Backend]
  .componentDidUpdate(dcb => Callback(addTheD3(ReactDOM.findDOMNode(dcb.component), dcb.currentState)))
  .build

  def distance(d: Double): Double = { // Placerar siffran på rätt avstånd från sidan av grafen.
    if(d > 99) {50}
    else if(d > 9) {35}
    else {20}
  }

  def removeZero(d: Double): String ={ // Ifall det är noll patienter i en kategori så skriver den inte ut nollan.
    if(d.equals(0)) {""}
    else {d.toString()}
  }

  private def addTheD3(element: raw.Element, statusMap: Map[String, Double]): Unit = {

    d3.select(element).selectAll("*").remove()

    val width = 419          // Kroppens bredd
    val height = 86          // Kroppens höjd
    val barHeight = 35.4     // Grafernas höjd
    val smallRec = 15        // Höjd samt bredd, små rektanglar
    val sizeNumbers = "23px" // Storlek på siffror inuti graferna
    val sizeText = "13px"    // Storlek på text intill små rektanglar

    // --- Färger --
    val colorBarOne = "#5c5a5a"
    val colorBarTwo = "#888888"
    val colorBarThree = "#bebebe"
    val colorNumbers = "#FFFFFF"
    // --------------

    var length: Map[String, Double] = Map()

    statusMap.foreach{ s =>
      length += s._1 -> (s._2/(statusMap("Unattended") + statusMap("Attended") + statusMap("Finished")))*width
    }

    val svg = d3.select(element).append("svg")
      .attr("width", width)
      .attr("height", height)

    val g = svg.append("g")
    // ----------- Graf ett -------------
    g.append("rect")
      .attr("x", 0)
      .attr("y", 50)
      .attr("width", length("Unattended"))
      .attr("height", barHeight)
      .attr("fill", colorBarOne)

    svg.append("text")
      .attr("x", length("Unattended") - distance(statusMap("Unattended")))
      .attr("y", 75.2)
      .attr("font-size", sizeNumbers)
      .text(s"${removeZero(statusMap("Unattended"))}")
      .attr("fill", colorNumbers)
    // ----------- Graf två -------------
    g.append("rect")
      .attr("x", length("Unattended"))
      .attr("y", 50)
      .attr("width", length("Attended"))
      .attr("height", barHeight)
      .attr("fill", colorBarTwo)

    svg.append("text")
      .attr("x", length("Unattended") + length("Attended") - distance(statusMap("Attended")))
      .attr("y", 75.2)
      .attr("font-size", sizeNumbers)
      .text(s"${removeZero(statusMap("Attended"))}")
      .attr("fill", colorNumbers)
    // ----------- Graf tre -------------
    g.append("rect")
      .attr("x", length("Attended") + length("Unattended"))
      .attr("y", 50)
      .attr("width", length("Finished"))
      .attr("height", barHeight)
      .attr("fill", colorBarThree)

    svg.append("text")
      .attr("x", length("Unattended") + length("Attended") + length("Finished") - distance(statusMap("Finished")))
      .attr("y", 75.2)
      .attr("font-size", sizeNumbers)
      .text(s"${removeZero(statusMap("Finished"))}")
      .attr("fill", colorNumbers)
    // ------------------------------------

    // -------- Små Rektanglar ------------

    // --------- Rektangel ett -----------
    g.append("rect")
      .attr("x", 0)
      .attr("y", 30)
      .attr("width", smallRec)
      .attr("height", smallRec)
      .attr("fill", colorBarOne)

    svg.append("text")
      .attr("x", 20)
      .attr("y", 42.5)
      .attr("font-size", sizeText)
      .text("Opåtittade")
    // --------- Rektangel två -----------
    g.append("rect")
      .attr("x", 0 + 120)
      .attr("y", 30)
      .attr("width", smallRec)
      .attr("height", smallRec)
      .attr("fill", colorBarTwo)

    svg.append("text")
      .attr("x", 20 + 120)
      .attr("y", 42.5)
      .attr("font-size", sizeText)
      .text("Påtittade")
    // --------- Rektangel tre -----------
    g.append("rect")
      .attr("x", 0 + 240)
      .attr("y", 30)
      .attr("width", smallRec)
      .attr("height", smallRec)
      .attr("fill", colorBarThree)

    svg.append("text")
      .attr("x", 20 + 240)
      .attr("y", 42.5)
      .attr("font-size", sizeText)
      .text("Klara")

    // ------------------------------------

    // ----------- STATUS --------------
    svg.append("text")
      .attr("x", 0)
      .attr("y", 20)
      .attr("font-size", "15px")
      .text("STATUS")
      .attr("font-family", "Helvetica")
      .attr("fill", "#a8a8a8")
    // ---------------------------------
  }

  def apply() = spgui.SPWidget(spwb => {
    component()
  })
}
