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

object StreamPlaceWidget {

  sealed trait PatientProperty

  case class Location(roomNr: String, timestamp: String) extends PatientProperty
  case class Team(team: String, clinic: String, timestamp: String) extends PatientProperty
  case class Examination(isOnExam: Boolean, timestamp: String) extends PatientProperty
  case class Finished() extends PatientProperty

  case class Patient(
    var careContactId: String,
    var location: Location,
    var team: Team,
    var examination: Examination)

  private class Backend($: BackendScope[Unit, Map[String, Patient]]) {
    spgui.widgets.css.WidgetStyles.addToDocument()

    val messObs = BackendCommunication.getMessageObserver(
      mess => {
        mess.getBodyAs[api.PatientProperty].map {
          case api.RoomNr(careContactId, timestamp, roomNr) => $.modState{ s => updateState(s, careContactId, Location(roomNr, timestamp)) }.runNow()
          case api.Team(careContactId, timestamp, team, clinic) => $.modState{ s => updateState(s, careContactId, Team(team, clinic, timestamp)) }.runNow()
          case api.Examination(careContactId, timestamp, isOnExam) => $.modState{ s => updateState(s, careContactId, Examination(isOnExam, timestamp)) }.runNow()
          case api.Finished(careContactId, timestamp) => $.modState{ s => updateState(s, careContactId, Finished()) }.runNow()
          case x => println(s"THIS WAS NOT EXPECTED IN StreamPlaceWidget: $x")
        }
      }, "place-diagram-widget-topic"
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
        case Location(roomNr, timestamp) => Patient(ccid, Location(roomNr, timestamp), Team("", "", ""), Examination(false, ""))
        case Team(team, clinic, timestamp) => Patient(ccid, Location("", ""), Team(team, clinic, timestamp), Examination(false, ""))
        case Examination(isOnExam, timestamp) => Patient(ccid, Location("", ""), Team("", "", ""), Examination(isOnExam, timestamp))
        case _ => Patient(ccid, Location("", ""), Team("", "", ""), Examination(false, ""))
      }
    }

    /**
    * Constructs an updates patient object.
    */
    def updateExistingPatient(s: Map[String, Patient], ccid: String, prop: PatientProperty): Patient = {
      prop match {
        case Location(roomNr, timestamp) => Patient(ccid, Location(roomNr, timestamp), s(ccid).team, s(ccid).examination)
        case Team(team, clinic, timestamp) => Patient(ccid, s(ccid).location, Team(team, clinic, timestamp), s(ccid).examination)
        case Examination(isOnExam, timestamp) => Patient(ccid, s(ccid).location, s(ccid).team, Examination(isOnExam, timestamp))
        case _ => Patient(ccid, Location("", ""), Team("", "", ""), Examination(false, ""))
      }
    }

    def render(p: Map[String, Patient]) = {
      <.div(Styles.helveticaZ)
    }
    def onUnmount() = {
      println("Unmounting")
      messObs.kill()
      Callback.empty
    }
  }

  private val component = ReactComponentB[Unit]("TeamStatusWidget")
  .initialState(Map("-1" ->
    Patient(
      "-1",
      Location("", ""),
      Team("", "", ""),
      Examination(false, "")
    )
  ))
  .renderBackend[Backend]
  .componentDidUpdate(dcb => Callback(addTheD3(ReactDOM.findDOMNode(dcb.component), dcb.currentState)))
  .componentWillUnmount(_.backend.onUnmount())
  .build

  /**
  * Checks if the patient belongs to this team. HERE: Stream.
  */
  def belongsToThisTeam(patient: Patient): Boolean = {
    patient.team.team match {
      case "stream" => true
      case _ => false
    }
  }

  def getPlace(patient: Patient): String = {
    if (patient.examination.isOnExam) {
      return "Examination"
    }
    if (patient.location.roomNr == "ivr") {
      return "InnerWaitingRoom"
    }
    if (patient.location.roomNr != "") {
      return "RoomOnSquare"
    }
    return "Other"
  }

  private def addTheD3(element: raw.Element, initialPlaceMap: Map[String, Patient]): Unit = {

  spgui.widgets.css.WidgetStyles.addToDocument()

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
  val colorBarOne = "#006c6c"
  val colorBarTwo = "#008a8a"
  val colorBarThree = "#00a8a8"
  val colorBarFour = "#00c6c6"
  val colorNumbers = "#FFFFFF"
  // -------------------------

  var teamMap: Map[String, Patient] = Map()
  (initialPlaceMap - "-1").foreach{ p =>
    if (belongsToThisTeam(p._2)) {
      teamMap += p._1 -> p._2
    }
  }

  var roomOnSquareCount = 0
  var innerWaitingRoomCount = 0
  var examinationCount = 0
  var otherCount = 0

  teamMap.foreach{ p =>
    getPlace(p._2) match {
      case "RoomOnSquare" => roomOnSquareCount += 1
      case "InnerWaitingRoom" => innerWaitingRoomCount += 1
      case "Examination" => examinationCount += 1
      case "Other" => otherCount += 1
    }
  }

  var placeMap: Map[String, Double] = Map(
    "RoomOnSquare" -> roomOnSquareCount,
    "InnerWaitingRoom" -> innerWaitingRoomCount,
    "Examination" -> examinationCount,
    "Other" -> otherCount
  )


  var length: Map[String, Double] = Map(
    "RoomOnSquare" -> 0,
    "InnerWaitingRoom" -> 0,
    "Examination" -> 0,
    "Other" -> 0
  )

  placeMap.foreach{ p =>
    if (p._2 == 0) 0
    else length += p._1 -> (p._2/(placeMap("RoomOnSquare") + placeMap("InnerWaitingRoom") + placeMap("Examination") + placeMap("Other")))*width
  }

  val svg = d3.select(element).append("svg")
    .attr("width", width)
    .attr("height", height)

  val g = svg.append("g")
  // ----------- Graf ett -------------
  g.append("rect")
    .attr("x", 0)
    .attr("y", (50/419.0)*width)
    .attr("width", length("RoomOnSquare"))
    .attr("height", barHeight)
    .attr("fill", colorBarOne)

  svg.append("text")
    .attr("x", length("RoomOnSquare") - distance(placeMap("RoomOnSquare")))
    .attr("y", (75/419.0)*width)
    .attr("font-size", fontSize)
    .text(s"${removeZero(placeMap("RoomOnSquare"))}")
    .attr("fill", colorNumbers)
  // ----------- Graf två -------------
  g.append("rect")
    .attr("x", length("RoomOnSquare"))
    .attr("y", (50/419.0)*width)
    .attr("width", length("InnerWaitingRoom"))
    .attr("height", barHeight)
    .attr("fill", colorBarTwo)

  svg.append("text")
    .attr("x", length("RoomOnSquare") + length("InnerWaitingRoom") - distance(placeMap("InnerWaitingRoom")))
    .attr("y", (75/419.0)*width)
    .attr("font-size", fontSize)
    .text(s"${removeZero(placeMap("InnerWaitingRoom"))}")
    .attr("fill", colorNumbers)
  // ----------- Graf tre -------------
  g.append("rect")
    .attr("x", length("InnerWaitingRoom") + length("RoomOnSquare"))
    .attr("y", (50/419.0)*width)
    .attr("width", length("Examination"))
    .attr("height", barHeight)
    .attr("fill", colorBarThree)

  svg.append("text")
    .attr("x", length("RoomOnSquare") + length("InnerWaitingRoom") + length("Examination") - distance(placeMap("Examination")))
    .attr("y", (75/419.0)*width)
    .attr("font-size", fontSize)
    .text(s"${removeZero(placeMap("Examination"))}")
    .attr("fill", colorNumbers)
  // ----------- Graf fyra -------------
  g.append("rect")
    .attr("x", length("Examination") + length("InnerWaitingRoom") + length("RoomOnSquare"))
    .attr("y", (50/419.0)*width)
    .attr("width", length("Other"))
    .attr("height", barHeight)
    .attr("fill", colorBarFour)

  svg.append("text")
    .attr("x", length("RoomOnSquare") + length("InnerWaitingRoom") + length("Examination") + length("Other") - distance(placeMap("Other")))
    .attr("y", (75/419.0)*width)
    .attr("font-size", fontSize)
    .text(s"${removeZero(placeMap("Other"))}")
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
    .text("Rum på torg")
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
    .text("Inre väntrum")
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
    .text("Undersökning")
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
    .text("Annat")
  // ------------------------------------

  // ----------- PLATS --------------
  svg.append("text")
    .attr("x", 0)
    .attr("y", (20/419.0)*width)
    .attr("font-size", s"${(15/419.0)*width}pt")
    .text("PLATS")
    .attr("fill", "#95989a")
  // ---------------------------------
}


  def apply() = spgui.SPWidget(spwb => {
    component()
  })
}
