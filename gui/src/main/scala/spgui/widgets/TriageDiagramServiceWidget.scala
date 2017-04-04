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

  private class Backend($: BackendScope[Map[String, Triage], Unit]) {
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
    def render(p: Map[String, Triage]) = {
      <.div(^.`class` := "card-holder-root")( // really not necessary
      )
    }
  }

  private val diagramHolderComponent = ReactComponentB[Map[String, Triage]]("diagramHolderComponent")
  .renderBackend[Backend]
  .componentDidUpdate(dcb => Callback(addTriageDiagram(ReactDOM.findDOMNode(dcb.component), dcb.currentProps)))
  .build

  private def addTriageDiagram(element: raw.Element, apc: Map[String, Triage]): Unit = {
    println("Lägger till triagediagram")

    d3.select(element).selectAll("*").remove() // clear all before rendering new data

    apc.values map ( p => {
      val svg = d3.select(element).append("svg").attr("width", "300").attr("height", "200")

      svg
      .append("rect")
      .attr("width", 300)
      .attr("height", 200)
      .attr("x", 0)
      .attr("y", 0)
      .attr("fill", "lightgrey")

      svg
      .append("rect")
      .attr("width", 80)
      .attr("height", 200)
      .attr("x", 0)
      .attr("y", 0)
      .attr("fill", "darkseagreen")

      svg
      .append("path")
      .attr("class", "klinikfield")
      .attr("d", "m 0,160 40,40 -40,0 z")
      .attr("fill", "lightblue")

      svg
      .append("text")
      .attr("class", "location")
      .attr("font-size", 25)
      .attr("x", 20)
      .attr("y", 50)
      .attr("fill", "white")
      .text("#gröna: " + p.colorMap("Grön"))

      svg
      .append("text")
      .attr("font-size", 25)
      .attr("x", 20)
      .attr("y", 90)
      .attr("fill", "white")
      .text("#gula: " + p.colorMap("Gul"))

      svg
      .append("text")
      .attr("font-size", 25)
      .attr("x", 20)
      .attr("y", 130)
      .attr("fill", "white")
      .text("#orangea: " + p.colorMap("Orange"))

      svg
      .append("text")
      .attr("font-size", 25)
      .attr("x", 20)
      .attr("y", 170)
      .attr("fill", "white")
      .text("#röda: " + p.colorMap("Röd"))

    })
  }

  def apply() = spgui.SPWidget(spwb => {
    diagramHolderComponent(triageCounter)
  })
}
