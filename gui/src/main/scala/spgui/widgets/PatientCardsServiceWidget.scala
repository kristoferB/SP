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


// sealed trait API_PatientCardsService
// object API_PatientCardsService {
//   case class NewPatient() extends API_PatientCardsService
//   case class DiffPatient() extends API_PatientCardsService
//   case class RemovedPatient() extends API_PatientCardsService
//   case class Patient( careContactId: String, patientData: Map[String,Any]) extends API_PatientCardsService
//   case class elvisEvent( eventType: String, patient: Patient) extends API_PatientCardsService
//   case class State(state: State) extends API_PatientCardsService
//
//   val service = "patientCardsService"
// }

package API_PatientEvent {
  sealed trait PatientEvent
  case class NewPatient( careContactId: String, patientData: Map[String,Any]) extends PatientEvent
  case class DiffPatient( careContactId: String, patientData: Map[String,Any]) extends PatientEvent
  case class RemovedPatient( careContactId: String) extends PatientEvent
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

  var activePatientCards = Map.empty[String, api.Patient] //new scala.collection.mutable.HashMap[String, Map[String,api.Patient]]() with scala.collection.mutable.SynchronizedMap[String, Map[String,api.Patient]]

  private class Backend($: BackendScope[Map[String,api.Patient], Unit]) {
    spgui.widgets.css.WidgetStyles.addToDocument()

    val messObs = BackendCommunication.getMessageObserver(
      mess => {
        mess.getBodyAs[api.PatientEvent] map {
          case api.elvisEvent(e, p) => {
            e match {
              case "newLoad" | "new"          => {println("newPatient")}// add patient p.careContactId with all of p.patientData to activePatientCards
              case "diff"                     => {println("diffPatient")}// modify entry p.careContactId in activePatientCards according to p.patientData
              case "removed"                  => {println("removedPatient")}// remove patient p.careContactId prom activePatientCards
              case _ => println("WARNING: elvisEvent of type: "+ e +" does not exist.")
            }
          }
          case x =>
          println(s"THIS WAS NOT EXPECTED IN PatientCardsServiceWidget: $x")
        }
      }, "patientCardsAnswers"
    )

    // def newPatient : Callback = {
    //   val h = SPHeader("PatientCardsServiceWidget", api.service, "PatientCardsServiceWidget")
    //   val json = SPMessage.make(h, api.NewPatient())
    //   BackendCommunication.publishMessage("services", json)
    //   Callback.empty
    // }
    //
    // def diffPatient : Callback = {
    //   val h = SPHeader("PatientCardsServiceWidget", api.service, "PatientCardsServiceWidget")
    //   val json = SPMessage.make(h, api.DiffPatient())
    //   BackendCommunication.publishMessage("services", json)
    //   Callback.empty
    // }
    //
    // def removedPatient : Callback = {
    //   val h = SPHeader("PatientCardsServiceWidget", api.service, "PatientCardsServiceWidget")
    //   val json = SPMessage.make(h, api.RemovedPatient())
    //   BackendCommunication.publishMessage("services", json)
    //   Callback.empty6
    // }


    def render(p: Map[String,api.Patient]) = {
      <.div(^.`class` := "card-holder-root")( // really not necessary
        //Styles.patientCardStyle
      )
    }


  }

  private val cardHolderComponent = ReactComponentB[Map[String,api.Patient]]("cardHolderComponent")
  .renderBackend[Backend]
  .componentDidUpdate(dcb => Callback(addPatientCard(ReactDOM.findDOMNode(dcb.component), dcb.currentProps)))
  .build

  private def addPatientCard(element: raw.Element, apc: Map[String,api.Patient]): Unit = {
    println("lägger till Patientkort")

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
      .attr("class", "roomNr")
      .attr("font-size", 52)
      .attr("x", 20)
      .attr("y", 50)
      .attr("fill", "white")
      .text(p.patientData("roomNr").toString)

      svg
      .append("text")
      .attr("font-size", 47)
      .attr("x", 100)
      .attr("y", 40)
      .attr("fill", "black")
      .text(p.careContactId)//patientData("careContactId").toString)

      svg
      .append("text")
      .attr("font-size", 25)
      .attr("x", 100)
      .attr("y", 100)
      .attr("fill", "black")
      .text(p.patientData("lastEvent").asInstanceOf[Tuple2[String,String]]._1)

    })
  }


  def apply() = spgui.SPWidget(spwb => {
    val llist = List(
      api.Patient("4502085", Map("careContactId" -> "4502085", "roomNr" -> "13", "lastEvent"->("Triage","2017-02-01T15:49:19Z") )),
      api.Patient("4502134", Map("careContactId" -> "4502134", "roomNr" -> "24", "lastEvent"->("Kölapp","2017-02-01T15:58:33Z") )),
      api.Patient("4508659", Map("careContactId" -> "4508659", "roomNr" -> "56", "lastEvent"->("Röntgen","2017-02-01T16:01:37Z") )),
      api.Patient("4538659", Map("careContactId" -> "4538659", "roomNr" -> "93", "lastEvent"->("Fika","2017-02-01T16:01:38Z") ))
    )
    llist map{ p =>
      activePatientCards += (p.careContactId -> p)
    }

    cardHolderComponent(activePatientCards)
  })
}
