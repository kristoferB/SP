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

object PatientCardsServiceWidget {

  case class Patient(careContactId: String, patientData: Map[String, String], events: List[Map[String, String]])

  // Keeps track of active patients and are used for constructing graphical components
  var activePatientCards: Map[String, Patient] = Map()

  private class Backend($: BackendScope[Map[String, Patient], Unit]) {
    spgui.widgets.css.WidgetStyles.addToDocument()

    val messObs = BackendCommunication.getMessageObserver(
      mess => {
        mess.getBodyAs[api.PatientEvent] map {
          case api.NewPatient(careContactId, patientData, events) => {
            // Add new patient and map to a specific careContactId
            activePatientCards += careContactId -> Patient(careContactId, patientData, events)
            //$.modState(s => s.runNow())
          }
          case api.DiffPatient(careContactId, patientDataDiff, newEvents, removedEvents) => {
            var modPatientData = activePatientCards(careContactId).patientData
            // Add and/or modify fields to the patient data
            patientDataDiff.foreach{ d =>
              if (d._2 != "" && d._2 != " ") {
                modPatientData += d._1 -> d._2
              }
            }
            activePatientCards += careContactId -> Patient(careContactId, modPatientData, activePatientCards(careContactId).events)
            //$.modState(s => s.runNow())
          }
          case api.RemovedPatient(careContactId) => {
            // Remove patient mapped to some specific careContactId
            activePatientCards -= careContactId
            //$.modState(s => s.runNow())
          }
          case x => println(s"THIS WAS NOT EXPECTED IN PatientCardsServiceWidget: $x")
        }
      }, "patient-cards-widget-topic"
    )

    def render(p: Map[String, Patient]) = {
      <.div(^.`class` := "card-holder-root")( // really not necessary
        //Styles.patientCardStyle
      )
    }
  }

  private val cardHolderComponent = ReactComponentB[Map[String, Patient]]("cardHolderComponent")
  .renderBackend[Backend]
  .componentDidUpdate(dcb => Callback(addPatientCard(ReactDOM.findDOMNode(dcb.component), dcb.currentProps)))
  .build

  private def addPatientCard(element: raw.Element, apc: Map[String, Patient]): Unit = {
    println("Lägger till patientkort")

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
      .attr("font-size", 52)
      .attr("x", 20)
      .attr("y", 50)
      .attr("fill", "white")
      .text(p.patientData("Location"))

      svg
      .append("text")
      .attr("font-size", 47)
      .attr("x", 100)
      .attr("y", 40)
      .attr("fill", "black")
      .text(p.careContactId)

      svg
      .append("text")
      .attr("font-size", 25)
      .attr("x", 100)
      .attr("y", 100)
      .attr("fill", "black")
      .text(p.patientData("CareContactRegistrationTime"))

    })
  }

  def apply() = spgui.SPWidget(spwb => {
    cardHolderComponent(activePatientCards)
  })
}
