package spgui.widgets

import java.time._ //ARTO: Använder wrappern https://github.com/scala-js/scala-js-java-time
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

object PatientReminderServiceWidget {

  case class Patient(careContactId: String, patientData: Map[String, String], events: List[Map[String, String]])

  var activePatientCards: Map[String, Patient] = Map()
  var patientReminders: Map[String, String] = Map()

  private class Backend($: BackendScope[Map[String, String], Unit]) {
    spgui.widgets.css.WidgetStyles.addToDocument()

    val messObs = BackendCommunication.getMessageObserver(
      mess => {
        mess.getBodyAs[api.PatientEvent] map {
          case api.NewPatient(careContactId, patientData, events) => {
            activePatientCards += careContactId -> Patient(careContactId, patientData, events)
            var eventTitle = ""
            events.foreach{ e =>
              val title = e("Title")
              if (title != "" && title != " ") {
                eventTitle = title
              }
            }
            patientReminders += careContactId -> eventTitle
          }
          case api.DiffPatient(careContactId, patientDataDiff, newEvents, removedEvents) => {
            if (activePatientCards.contains(careContactId)) {
              var modPatientData = activePatientCards(careContactId).patientData
              patientDataDiff.foreach{ d =>
                if (d._2 != "" && d._2 != " ") {
                  modPatientData += d._1 -> d._2
                }
              }
              activePatientCards += careContactId -> Patient(careContactId, modPatientData, activePatientCards(careContactId).events)
            }
          }
          case api.RemovedPatient(careContactId) => {
            activePatientCards -= careContactId
          }
          case x => println(s"THIS WAS NOT EXPECTED IN PatientReminderServiceWidget: $x")
        }
      }, "patient-reminder-widget-topic"
    )

    def getLatestEvent(patient: Patient) {
    }

    // What is this function used for?
    def render(p: Map[String, String]) = {
      <.div(^.`class` := "card-holder-root")( // really not necessary
      )
    }
  }

  private val patientReminderHolderComponent = ReactComponentB[Map[String, String]]("patientReminderHolderComponent")
  .renderBackend[Backend]
  .componentDidUpdate(dcb => Callback(addPatientReminders(ReactDOM.findDOMNode(dcb.component), dcb.currentProps)))
  .build

  private def addPatientReminders(element: raw.Element, apc: Map[String, String]): Unit = {
    println("Lägger till patient-påminnelser")

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
      .text(p)

      svg
      .append("text")
      .attr("font-size", 25)
      .attr("x", 20)
      .attr("y", 90)
      .attr("fill", "white")
      .text("hej2")

      svg
      .append("text")
      .attr("font-size", 25)
      .attr("x", 20)
      .attr("y", 130)
      .attr("fill", "white")
      .text("hej3")

      svg
      .append("text")
      .attr("font-size", 25)
      .attr("x", 20)
      .attr("y", 170)
      .attr("fill", "white")
      .text("hej4")

    })
  }

  def apply() = spgui.SPWidget(spwb => {
    patientReminderHolderComponent(patientReminders)
  })
}
