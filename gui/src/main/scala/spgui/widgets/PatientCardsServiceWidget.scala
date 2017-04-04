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

package API_PatientEvent {
  // Messages I can receive
  sealed trait PatientEvent
  case class NewPatient(careContactId: String, patientData: Map[String, String], events: List[Map[String, String]]) extends PatientEvent
  case class DiffPatient(careContactId: String, patientData: Map[String, String], newEvents: List[Map[String, String]], removedEvents: List[Map[String, String]]) extends PatientEvent
  case class RemovedPatient(careContactId: String) extends PatientEvent
  case class Patient( careContactId: String, patientData: Map[String,Any]) extends PatientEvent
  case class elvisEvent( eventType: String, patient: Patient) extends PatientEvent

  sealed trait API_PatientCardsService
  case class State(state: State) extends API_PatientCardsService


  object attributes {
    val service = "patientCardsService"
  }
}

import spgui.widgets.{API_PatientEvent => api}

object PatientCardsServiceWidget {

  case class Patient(careContactId: String, patientData: Map[String, String], events: List[Map[String, String]])

  var activePatientCards: Map[String, Patient] = Map()

  private class Backend($: BackendScope[Map[String, Patient], Unit]) {
    spgui.widgets.css.WidgetStyles.addToDocument()

    val messObs = BackendCommunication.getMessageObserver(
      mess => {
        mess.getBodyAs[api.PatientEvent] map {
          case api.NewPatient(careContactId, patientData, events) => {
            // Add new patient and map to a specific careContactId
            activePatientCards += careContactId -> Patient(careContactId, patientData, events)
          }
          case api.DiffPatient(careContactId, patientDataDiff, newEvents, removedEvents) => {
            var modPatientData = activePatientCards(careContactId).patientData
            // Add and/or modify fields to the patient data
            patientDataDiff.foreach{ d =>
              modPatientData += d._1 -> d._2
            }
            activePatientCards += careContactId -> Patient(careContactId, modPatientData, activePatientCards(careContactId).events)
          }
          case api.RemovedPatient(careContactId) => {
            // Remove patient mapped to some specific careContactId
            activePatientCards -= careContactId
          }
          case x => println(s"THIS WAS NOT EXPECTED IN PatientCardsServiceWidget: $x")
        }
      }, "patient-cards-widget-topic"
    )

    def patientCard(p: Patient) = {
      <.svg.svg( //ARTO: Skapar en <svg>-tagg att fylla med obekt
        ^.`class` := "patientcard",
        ^.svg.id := p.careContactId,
        ^.svg.width := "300",
        ^.svg.height := "200",
        <.svg.rect(
          ^.`class` := "bg",
          ^.svg.x := "0",
          ^.svg.y := "0",
          ^.svg.width := "300",
          ^.svg.height := "200",
          ^.svg.fill := "lightgrey"
        ),
        <.svg.rect(
          ^.`class` := "triagefield",
          ^.svg.x := "0",
          ^.svg.y := "0",
          ^.svg.width := "80",
          ^.svg.height := "200",
          ^.svg.fill := "darkseagreen"
        ),
        <.svg.path(
          ^.`class` := "klinikfield",
          ^.svg.d := "M 0,180, 60,180, -60,0 Z",
          ^.svg.fill := "lightblue"
        ),
        <.svg.text(
          ^.`class` := "roomNr",
          ^.svg.x := "10",
          ^.svg.y := "50",
          ^.svg.fontSize := "52",
          ^.svg.fill := "white",
          p.patientData("Location")
        ),
        <.svg.text(
          ^.`class` := "careContactId",
          ^.svg.x := "100",
          ^.svg.y := "40",
          ^.svg.fontSize := "47",
          ^.svg.fill := "black",
          p.careContactId
        ),
        <.svg.text(
          ^.`class` := "lastEvent",
          ^.svg.x := "100",
          ^.svg.y := "100",
          ^.svg.fontSize := "25",
          ^.svg.fill := "black",
          p.patientData("CareContactRegistrationTime")
        )
      )
    }

    def render(pmap: Map[String, Patient]) = {
      <.div(^.`class` := "card-holder-root")( // This div is really not necessary
        //Styles.patientCardStyle
        pmap.values map ( p =>
          patientCard(p)
        )
      )
    }
  }

  private val cardHolderComponent = ReactComponentB[Map[String, Patient]]("cardHolderComponent")
  .renderBackend[Backend]
  .build

  def apply() = spgui.SPWidget(spwb => {
    cardHolderComponent(activePatientCards)
  })
}
