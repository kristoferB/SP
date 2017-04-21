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

object MedicineYellowStatusWidget {

  sealed trait PatientProperty

  case class Attended(attended: Boolean, doctorId: String, timestamp: String) extends PatientProperty
  case class FinishedStillPresent(finished: Boolean, timestamp: String) extends PatientProperty
  case class Team(team: String, clinic: String, timestamp: String) extends PatientProperty
  case class Finished() extends PatientProperty

  case class Patient(
    var careContactId: String,
    var attended: Attended,
    var finishedStillPresent: FinishedStillPresent,
    var team: Team)

  private class Backend($: BackendScope[Unit, Map[String, Patient]]) {
    spgui.widgets.css.WidgetStyles.addToDocument()

    val messObs = BackendCommunication.getMessageObserver(
      mess => {
        mess.getBodyAs[api.PatientProperty].map {
          case api.Attended(careContactId, timestamp, attended, doctorId) => $.modState{ s => updateState(s, careContactId, Attended(attended, doctorId, timestamp)) }.runNow()
          case api.Team(careContactId, timestamp, team, clinic) => $.modState{ s => updateState(s, careContactId, Team(team, clinic, timestamp)) }.runNow()
          case api.FinishedStillPresent(careContactId, timestamp) => $.modState{ s => updateState(s, careContactId, FinishedStillPresent(true, timestamp)) }.runNow()
          case api.Finished(careContactId, timestamp) => $.modState{ s => updateState(s, careContactId, Finished()) }.runNow()
          case x => println(s"THIS WAS NOT EXPECTED IN MedicineYellowStatusWidget: $x")
        }
      }, "status-diagram-widget-topic"
    )

    /**
    * Updates the current state based on what patient property is received.
    */
    def updateState(s: Map[String, Patient], careContactId: String, prop: PatientProperty): Map[String, Patient] = {
      if (s.keys.exists(_ == careContactId)) {
        if (prop.isInstanceOf[Finished]) {
          return s - careContactId
        }
        return s + (careContactId -> updateExistingPatient(s, careContactId, prop))
      } else {
        return s + (careContactId -> updateNewPatient(careContactId, prop))
      }
    }

    /**
    * Constructs a new patient object.
    */
    def updateNewPatient(ccid: String, prop: PatientProperty): Patient = {
      prop match {
        case Attended(attended, doctorId, timestamp) => Patient(ccid, Attended(attended, doctorId, timestamp), FinishedStillPresent(false, ""), Team("", "", ""))
        case FinishedStillPresent(finished, timestamp) => Patient(ccid, Attended(false, "", ""), FinishedStillPresent(finished, timestamp), Team("", "", ""))
        case Team(team, clinic, timestamp) => Patient(ccid, Attended(false, "", ""), FinishedStillPresent(false, ""), Team(team, clinic, timestamp))
        case _ => Patient(ccid, Attended(false, "", ""), FinishedStillPresent(false, ""), Team("", "", ""))
      }
    }

    /**
    * Constructs an updates patient object.
    */
    def updateExistingPatient(s: Map[String, Patient], ccid: String, prop: PatientProperty): Patient = {
      prop match {
        case Attended(attended, doctorId, timestamp) => Patient(ccid, Attended(attended, doctorId, timestamp), s(ccid).finishedStillPresent, s(ccid).team)
        case FinishedStillPresent(finished, timestamp) => Patient(ccid, s(ccid).attended, FinishedStillPresent(finished, timestamp), s(ccid).team)
        case Team(team, clinic, timestamp) => Patient(ccid, s(ccid).attended, s(ccid).finishedStillPresent, Team(team, clinic, timestamp))
        case _ => Patient(ccid, s(ccid).attended, s(ccid).finishedStillPresent, s(ccid).team)
      }
    }

    def render(p: Map[String, Patient]) = {
      <.div(Styles.helveticaZ)
    }
  }

  private val component = ReactComponentB[Unit]("TeamStatusWidget")
  .initialState(Map("-1" ->
    Patient(
      "-1",
      Attended(false, "", ""),
      FinishedStillPresent(false, ""),
      Team("", "", "")
    )
  ))
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

  /**
  * Checks if the patient belongs to this team. HERE: Medicin gul.
  */
  def belongsToThisTeam(patient: Patient): Boolean = {
    patient.team.team match {
      case "medicin gul" | "medicin" => true
      case _ => false
    }
  }

  private def addTheD3(element: raw.Element, initialStatusMap: Map[String, Patient]): Unit = {

    d3.select(element).selectAll("*").remove()

    val width = element.clientWidth          // Kroppens bredd
    val height = (86/419.0)*width          // Kroppens höjd
    val barHeight = (35.4/419.0)*width     // Grafernas höjd
    val smallRec = (15/419.0)*width        // Höjd samt bredd, små rektanglar
    val sizeNumbers = s"${(23/419.0)*width}px" // Storlek på siffror inuti graferna
    val sizeText = s"${(13/419.0)*width}px"    // Storlek på text intill små rektanglar
    val fontSize = s"${(22/419.0)*width}"

    def distance(d: Double): Double = { // Placerar siffran på rätt avstånd från sidan av grafen.
      if(d > 99) {(50/419.0)*width}
      else if(d > 9) {(35/419.0)*width}
      else {(20/419.0)*width}
    }

    def removeZero(d: Double): String ={ // Ifall det är noll patienter i en kategori så skriver den inte ut nollan.
      if(d.equals(0)) {""}
      else {d.toString()}
    }

    // ------- Färger ---------
    val colorBarOne = "#4a4a4a"
    val colorBarTwo = "#5c5a5a"
    val colorBarThree = "#888888"
    val colorBarFour = "#bebebe"
    val colorNumbers = "#FFFFFF"
    // -------------------------

    // Count the number of patients of each status
    var finishedCount = 0
    var attendedCount = 0
    var attendedWithPlanCount = 0 // not accounted for
    var unattendedCount = 0

    var teamMap: Map[String, Patient] = Map()
    (initialStatusMap - "-1").foreach{ p =>
      if (belongsToThisTeam(p._2)) {
        teamMap += p._1 -> p._2
      }
    }

    teamMap.foreach{ p =>
      if (p._2.finishedStillPresent.finished) {
        finishedCount += 1
      } else {
        if (p._2.attended.attended) {
          attendedCount += 1
        } else {
          unattendedCount += 1
        }
      }
    }

    var statusMap: Map[String, Double] = Map(
      "Finished" -> finishedCount,
      "Attended" -> attendedCount,
      "AttendedWithPlan" -> attendedWithPlanCount,
      "Unattended" -> unattendedCount
    )

    var length: Map[String, Double] = Map()

    statusMap.foreach{ s =>
      length += s._1 -> (s._2/(statusMap("Unattended") + statusMap("Attended") + statusMap("Finished") + statusMap("AttendedWithPlan")))*width
    }

    val svg = d3.select(element).append("svg")
      .attr("width", width)
      .attr("height", height)

    val g = svg.append("g")
    // ----------- Graf ett -------------
  g.append("rect")
    .attr("x", 0)
    .attr("y", (50/419.0)*width)
    .attr("width", length("Unattended"))
    .attr("height", barHeight)
    .attr("fill", colorBarOne)

  svg.append("text")
    .attr("x", length("Unattended") - distance(statusMap("Unattended")))
    .attr("y", (75/419.0)*width)
    .attr("font-size", fontSize)
    .text(s"${removeZero(statusMap("Unattended"))}")
    .attr("fill", colorNumbers)
  // ----------- Graf två -------------
  g.append("rect")
    .attr("x", length("Unattended"))
    .attr("y", (50/419.0)*width)
    .attr("width", length("Attended"))
    .attr("height", barHeight)
    .attr("fill", colorBarTwo)

  svg.append("text")
    .attr("x", length("Unattended") + length("Attended") - distance(statusMap("Attended")))
    .attr("y", (75/419.0)*width)
    .attr("font-size", fontSize)
    .text(s"${removeZero(statusMap("Attended"))}")
    .attr("fill", colorNumbers)
  // ----------- Graf tre -------------
  g.append("rect")
    .attr("x", length("Attended") + length("Unattended"))
    .attr("y", (50/419.0)*width)
    .attr("width", length("AttendedWithPlan"))
    .attr("height", barHeight)
    .attr("fill", colorBarThree)

  svg.append("text")
    .attr("x", length("Unattended") + length("Attended") + length("AttendedWithPlan") - distance(statusMap("AttendedWithPlan")))
    .attr("y", (75/419.0)*width)
    .attr("font-size", fontSize)
    .text(s"${removeZero(statusMap("AttendedWithPlan"))}")
    .attr("fill", colorNumbers)
  // ----------- Graf fyra -------------
  g.append("rect")
    .attr("x", length("AttendedWithPlan") + length("Attended") + length("Unattended"))
    .attr("y", (50/419.0)*width)
    .attr("width", length("Finished"))
    .attr("height", barHeight)
    .attr("fill", colorBarFour)

  svg.append("text")
    .attr("x", length("Unattended") + length("Attended") + length("AttendedWithPlan") + length("Finished") - distance(statusMap("Finished")))
    .attr("y", (75/419.0)*width)
    .attr("font-size", fontSize)
    .text(s"${removeZero(statusMap("Finished"))}")
    .attr("fill", colorNumbers)
  // ------------------------------------

  // -------- Små Rektanglar ------------

  // --------- Rektangel ett -----------
  g.append("rect")
    .attr("x", 0)
    .attr("y", (30/419.0)*width)
    .attr("width", smallRec)
    .attr("height", smallRec)
    .attr("fill", colorBarOne)

  svg.append("text")
    .attr("x", (20/419.0)*width)
    .attr("y", (42.5/419.0)*width)
    .attr("font-size", sizeText)
    .text("Opåtittad")
  // --------- Rektangel två -----------
  g.append("rect")
    .attr("x", ((0 + 120)/419.0)*width)
    .attr("y", (30/419.0)*width)
    .attr("width", smallRec)
    .attr("height", smallRec)
    .attr("fill", colorBarTwo)

  svg.append("text")
    .attr("x", ((20 + 120)/419.0)*width)
    .attr("y", (42.5/419.0)*width)
    .attr("font-size", sizeText)
    .text("Påtittad")
  // --------- Rektangel tre -----------
  g.append("rect")
    .attr("x", ((0 + 240)/419.0)*width)
    .attr("y", (30/419.0)*width)
    .attr("width", smallRec)
    .attr("height", smallRec)
    .attr("fill", colorBarThree)

  svg.append("text")
    .attr("x", ((20 + 240)/419.0)*width)
    .attr("y", (42.5/419.0)*width)
    .attr("font-size", sizeText)
    .text("Påtittad m. plan")
  // --------- Rektangel fyra -----------
  g.append("rect")
    .attr("x", ((0 + 360)/419.0)*width)
    .attr("y", (30/419.0)*width)
    .attr("width", smallRec)
    .attr("height", smallRec)
    .attr("fill", colorBarFour)

  svg.append("text")
    .attr("x", ((20 + 360)/419.0)*width)
    .attr("y", (42.5/419.0)*width)
    .attr("font-size", sizeText)
    .text("Klar")
  // ------------------------------------

  // ----------- PLATS --------------
  svg.append("text")
    .attr("x", 0)
    .attr("y", (20/419.0)*width)
    .attr("font-size", s"${(15/419.0)*width}pt")
    .text("STATUS")
    .attr("fill", "#95989a")
  // ---------------------------------

  }

  def apply() = spgui.SPWidget(spwb => {
    component()
  })
}
