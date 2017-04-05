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

object TriageDiagramServiceWidget {

  var triageMap: Map[String, Double] = Map(
    "Undefined" -> 0,
    "Green" -> 0,
    "Yellow" -> 0,
    "Orange" -> 0,
    "Red" -> 0
  )

  def handleTriageEvent(toAdd: Boolean, triage: String) {
    if (toAdd) {
      triageMap += triage -> (triageMap(triage) + 1)
    } else {
      triageMap += triage -> (triageMap(triage) - 1)
    }
  }

  private class Backend($: BackendScope[Map[String, Double], Unit]) {
    spgui.widgets.css.WidgetStyles.addToDocument()

    val messObs = BackendCommunication.getMessageObserver(
      mess => {
        mess.getBodyAs[api.TriageEvent] map {
          case api.Undefined(toAdd) => {
            handleTriageEvent(toAdd, "Undefined")
          }
          case api.Green(toAdd) => {
            handleTriageEvent(toAdd, "Green")
          }
          case api.Yellow(toAdd) => {
            handleTriageEvent(toAdd, "Yellow")
          }
          case api.Orange(toAdd) => {
            handleTriageEvent(toAdd, "Orange")
          }
          case api.Red(toAdd) => {
            handleTriageEvent(toAdd, "Red")
          }
          case x => println(s"THIS WAS NOT EXPECTED IN TriageDiagramServiceWidget: $x")
        }
      }, "triage-diagram-widget-topic"
    )

    // What is this function used for?
    def render(p: Map[String, Double]) = {
      <.div(^.`class` := "card-holder-root")( // really not necessary
      )
    }
  }

  private val component = ReactComponentB[Map[String, Double]]("teamVBelastning")
  .renderBackend[Backend]
  .componentDidUpdate(dcb => Callback(addTheD3(ReactDOM.findDOMNode(dcb.component), dcb.currentProps)))
  .build

  def dist(d: Double): Double = { // Bestämmer avstånd för antal patienter i widget.
    if( d > 99){1}
    else if( d > 9 ){10}
    else {17.5}
  }

  def removeZero(d: Double): String = {
    if(d.equals(0)){""}
    else{d.toString()}
  }

  private def addTheD3(element: raw.Element, triageMap: Map[String, Double]): Unit = {
     d3.select(element).selectAll("*").remove()

     val distance = 50
     val width = 500
     val height = 500
     val barHeight = 350
     val barWidth = 35
     val currentTeam = "STREAMTEAM"

     var length: Map[String, Double] = Map()

     triageMap.foreach{ t =>
       length += t._1 -> (t._2/(triageMap("Undefined") + triageMap("Green") + triageMap("Yellow") + triageMap("Orange") + triageMap("Red")))*barHeight
     }

     var scaleBars = barHeight / length.valuesIterator.max

     val svg = d3.select(element).append("svg")
       .attr("width", width)
       .attr("height", height)

     val g = svg.append("g")
     // ------------------------ Graf ett BLÅ
     g.append("rect")
       .attr("x", distance)
       .attr("y", 500 - length("Undefined") * scaleBars)
       .attr("width", barWidth)
       .attr("height", length("Undefined") * scaleBars)
       .attr("fill", "#000000")

     svg.append("text")
       .attr("x", (dist(triageMap("Undefined")) + distance))
       .attr("y", 495)
       .attr("font-size", "20px")
       .text(s"${removeZero(triageMap("Undefined"))}")
       .attr("fill", "#FFFFFF")

     // ------------------------ Graf två GRÖN
     g.append("rect")
       .attr("x", 2*distance + barWidth)
       .attr("y", 500 - length("Green") * scaleBars)
       .attr("width", barWidth)
       .attr("height", length("Green") * scaleBars)
       .attr("fill", "#289500")

     svg.append("text")
       .attr("x", (dist(triageMap("Green")) + 2 * distance + barWidth))
       .attr("y", 495)
       .attr("font-size", "20px")
       .text(s"${removeZero(triageMap("Green"))}")
       .attr("fill", "#FFFFFF")

     // ------------------------ Graf tre GUL
     g.append("rect")
       .attr("x", 3*distance + 2*barWidth)
       .attr("y", 500 - length("Yellow") * scaleBars)
       .attr("width", barWidth)
       .attr("height", length("Yellow") * scaleBars)
       .attr("fill", "#EAC706")

     svg.append("text")
       .attr("x", (dist(triageMap("Yellow")) + 3 * distance + 2 * barWidth))
       .attr("y", 495)
       .attr("font-size", "20px")
       .text(s"${removeZero(triageMap("Yellow"))}")
       .attr("fill", "#FFFFFF")

     // ------------------------ Graf fyra ORANGE
     g.append("rect")
       .attr("x", 4 * distance + 3 * barWidth)
       .attr("y", 500 - length("Orange") * scaleBars)
       .attr("width", barWidth)
       .attr("height", length("Orange") * scaleBars)
       .attr("fill", "#F08100")

     svg.append("text")
       .attr("x", (dist(triageMap("Orange")) + 4 * distance + 3 * barWidth))
       .attr("y", 495)
       .attr("font-size", "20px")
       .text(s"${removeZero(triageMap("Orange"))}")
       .attr("fill", "#FFFFFF")

     // ------------------------ Graf fem RÖD
     g.append("rect")
       .attr("x",  5 * distance + 4 * barWidth)
       .attr("y", 500 - length("Red") * scaleBars)
       .attr("width", barWidth)
       .attr("height", length("Red") * scaleBars)
       .attr("fill", "#950000")

     svg.append("text")
       .attr("x", (dist(triageMap("Red")) +  5 * distance + 4 * barWidth))
       .attr("y", 495)
       .attr("font-size", "20px")
       .text(s"${removeZero(triageMap("Red"))}")
       .attr("fill", "#FFFFFF")


     // ------- TEAM ------------

     svg.append("text")
       .attr("x", 0)
       .attr("y", 20)
       .attr("font-size", "25px")
       .text(currentTeam)
       .style("font-weight", "bold")  //Helvetica som font
       .attr("fill", "#000000")

     // ------- ANTAL PATIENTER ------------

     svg.append("text")
       .attr("x", 0)
       .attr("y", 50)
       .attr("font-size", "20px")
       .text(s"${triageMap("Undefined") + triageMap("Green") + triageMap("Yellow") + triageMap("Orange") + triageMap("Red")}" + " " + "PATIENTER TOTALT")
       .attr("fill", "#000000")

     // ------- STATUS ------------

     svg.append("text")
       .attr("x", 0)
       .attr("y", 100)
       .attr("font-size", "15px")
       .text("TRIAGE OCH BELASTNING")
       .attr("fill", "#a8a8a8")

  }

  def apply() = spgui.SPWidget(spwb => {
    component(triageMap)
  })
}
