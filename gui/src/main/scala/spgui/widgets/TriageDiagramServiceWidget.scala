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

  case class Patient(careContactId: String, patientData: Map[String, String], events: List[Map[String, String]])
  case class Triage(colorMap: Map[String, Int])

  var activePatientCards: Map[String, Patient] = Map()
  var triageCounter: Map[String, Triage] = Map(
    "TriageDiagram" -> Triage(Map(
      "Grön" -> 0,
      "Gul" -> 0,
      "Orange" -> 0,
      "Röd" -> 0
      )
    )
  )

  private class Backend($: BackendScope[List[Double], Unit]) {
    spgui.widgets.css.WidgetStyles.addToDocument()

    val messObs = BackendCommunication.getMessageObserver(
      mess => {
        mess.getBodyAs[api.PatientEvent] map {
          case api.NewPatient(careContactId, patientData, events) => {
            var extractedTriageMap = triageCounter("TriageDiagram").colorMap
            val triage = patientData("Priority")
            if (triage != "" && triage != " ") {
              val oldCounter: Int = extractedTriageMap(triage)
              val count: Int = oldCounter + 1
              extractedTriageMap += triage -> count
              triageCounter += "TriageDiagram" -> Triage(extractedTriageMap)
              activePatientCards += careContactId -> Patient(careContactId, patientData, events)
            }
          }
          case api.DiffPatient(careContactId, patientDataDiff, newEvents, removedEvents) => {
            var extractedTriageMap = triageCounter("TriageDiagram").colorMap
            var modPatientData = activePatientCards(careContactId).patientData
            val oldTriage = modPatientData("Priority")
            patientDataDiff.foreach{ d =>
              modPatientData += d._1 -> d._2
            }
            activePatientCards += careContactId -> Patient(careContactId, modPatientData, activePatientCards(careContactId).events)
            val newTriage = modPatientData("Priority")
            if (newTriage != "" && newTriage != " ") {

              val oldCounterNewTriage = extractedTriageMap(newTriage)
              val oldCounterOldTriage = extractedTriageMap(oldTriage)
              if (oldTriage != "" && oldTriage != " ") {
                if (newTriage != oldTriage) {
                  val newCount: Int = oldCounterNewTriage + 1
                  val oldCount: Int = oldCounterOldTriage - 1
                  extractedTriageMap += newTriage -> newCount
                  extractedTriageMap += oldTriage -> oldCount
                  triageCounter += "TriageDiagram" -> Triage(extractedTriageMap)
                }
              } else {
                val count: Int = oldCounterNewTriage + 1
                extractedTriageMap += newTriage -> count
                triageCounter += "TriageDiagram" -> Triage(extractedTriageMap)
              }
            }
          }
          case api.RemovedPatient(careContactId) => {
            var extractedTriageMap = triageCounter("TriageDiagram").colorMap
            val triage = activePatientCards(careContactId).patientData("Priority")
            if (triage != "" && triage != " ") {
              val oldCounter = extractedTriageMap(triage)
              val count = oldCounter - 1
              extractedTriageMap += triage -> count
              triageCounter += triage -> Triage(extractedTriageMap)
            }
            activePatientCards -= careContactId
          }
          case x => println(s"THIS WAS NOT EXPECTED IN TriageDiagramServiceWidget: $x")
        }
      }, "triage-diagram-widget-topic"
    )

    // What is this function used for?
    def render(p: List[Double]) = {
      <.div(^.`class` := "card-holder-root")( // really not necessary
      )
    }
  }

  private val component = ReactComponentB[List[Double]]("teamVBelastning")
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

  private def addTheD3(element: raw.Element, list: List[Double]): Unit = {
     d3.select(element).selectAll("*").remove()

     val distance = 50
     val width = 500
     val height = 500
     val barHeight = 350
     val barWidth = 35
     val currentTeam = "STREAMTEAM"

     var length = new ListBuffer[Double]()

     for(i <- 0 to 4){
       length += (list(i)/(list(0) + list(1) + list(2) + list(3) + list(4)))*barHeight
     }

     var scaleBars = barHeight / length.max

     val svg = d3.select(element).append("svg")
       .attr("width", width)
       .attr("height", height)

     val g = svg.append("g")
     // ------------------------ Graf ett BLÅ
     g.append("rect")
       .attr("x", distance)
       .attr("y", 500 - length(0) * scaleBars)
       .attr("width", barWidth)
       .attr("height", length(0) * scaleBars)
       .attr("fill", "#0000cd")

     svg.append("text")
       .attr("x", (dist(list(0)) + distance))
       .attr("y", 495)
       .attr("font-size", "20px")
       .text(s"${removeZero(list(0))}")
       .attr("fill", "#FFFFFF")

     // ------------------------ Graf två GRÖN
     g.append("rect")
       .attr("x", 2*distance + barWidth)
       .attr("y", 500 - length(1) * scaleBars)
       .attr("width", barWidth)
       .attr("height", length(1) * scaleBars)
       .attr("fill", "#008000")

     svg.append("text")
       .attr("x", (dist(list(1)) + 2 * distance + barWidth))
       .attr("y", 495)
       .attr("font-size", "20px")
       .text(s"${removeZero(list(1))}")
       .attr("fill", "#FFFFFF")

     // ------------------------ Graf tre GUL
     g.append("rect")
       .attr("x", 3*distance + 2*barWidth)
       .attr("y", 500 - length(2) * scaleBars)
       .attr("width", barWidth)
       .attr("height", length(2) * scaleBars)
       .attr("fill", "#ebeb00")

     svg.append("text")
       .attr("x", (dist(list(2)) + 3 * distance + 2 * barWidth))
       .attr("y", 495)
       .attr("font-size", "20px")
       .text(s"${removeZero(list(2))}")
       .attr("fill", "#FFFFFF")

     // ------------------------ Graf fyra ORANGE
     g.append("rect")
       .attr("x", 4 * distance + 3 * barWidth)
       .attr("y", 500 - length(3) * scaleBars)
       .attr("width", barWidth)
       .attr("height", length(3) * scaleBars)
       .attr("fill", "#FFA500")

     svg.append("text")
       .attr("x", (dist(list(3)) + 4 * distance + 3 * barWidth))
       .attr("y", 495)
       .attr("font-size", "20px")
       .text(s"${removeZero(list(3))}")
       .attr("fill", "#FFFFFF")

     // ------------------------ Graf fem RÖD
     g.append("rect")
       .attr("x",  5 * distance + 4 * barWidth)
       .attr("y", 500 - length(4) * scaleBars)
       .attr("width", barWidth)
       .attr("height", length(4) * scaleBars)
       .attr("fill", "#FF0000")

     svg.append("text")
       .attr("x", (dist(list(4)) +  5 * distance + 4 * barWidth))
       .attr("y", 495)
       .attr("font-size", "20px")
       .text(s"${removeZero(list(4))}")
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
       .text(s"${list(0) + list(1) + list(2) + list(3) + list(4)}" + " " + "PATIENTER TOTALT")
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
    var listToSend = new ListBuffer[Double]()
    listToSend += 0 // noll "svarta"
    triageCounter.foreach{ t =>
      t._2.colorMap.foreach{ c =>
        listToSend += c._2.toDouble
      }
    }
    component(listToSend.toList)
  })
}
