package spgui.widgets

import java.time._ //ARTO: Använder wrappern https://github.com/scala-js/scala-js-java-time
import java.time.OffsetDateTime
// import java.time.temporal._
import java.time.format.DateTimeFormatter
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

object ProcessPatientCardsWidget {

  sealed trait PatientProperty

  case class Priority(color: String, timestamp: String) extends PatientProperty
  case class Attended(attended: Boolean, doctorId: String, timestamp: String) extends PatientProperty
  case class Location(roomNr: String, timestamp: String) extends PatientProperty
  case class Team(team: String, clinic: String, timestamp: String) extends PatientProperty
  case class LatestEvent(latestEvent: String, timeDiff: Long, timestamp: String) extends PatientProperty
  case class ArrivalTime(timeDiff: String, timestamp: String) extends PatientProperty
  case class Finished() extends PatientProperty

  case class Patient(
    var careContactId: String,
    var priority: Priority,
    var attended: Attended,
    var location: Location,
    var team: Team,
    var latestEvent: LatestEvent,
    var arrivalTime: ArrivalTime)

  private class Backend($: BackendScope[Unit, Map[String, Patient]]) {
    spgui.widgets.css.WidgetStyles.addToDocument()

    val messObs = BackendCommunication.getMessageObserver(
      mess => {
        mess.getBodyAs[api.PatientProperty].map {
          case api.Tick() => $.modState{ s => updateAllTimeDiffs(s) }.runNow()
          case api.NotTriaged(careContactId, timestamp) => $.modState{ s => updateState(s, careContactId, Priority("NotTriaged", timestamp)) }.runNow()
          case api.Green(careContactId, timestamp) => $.modState{ s => updateState(s, careContactId, Priority("Green", timestamp)) }.runNow()
          case api.Yellow(careContactId, timestamp) => $.modState{ s => updateState(s, careContactId, Priority("Yellow", timestamp)) }.runNow()
          case api.Orange(careContactId, timestamp) => $.modState{ s => updateState(s, careContactId, Priority("Orange", timestamp)) }.runNow()
          case api.Red(careContactId, timestamp) => $.modState{ s => updateState(s, careContactId, Priority("Red", timestamp)) }.runNow()
          case api.Attended(careContactId, timestamp, attended, doctorId) => $.modState{ s => updateState(s, careContactId, Attended(attended, doctorId, timestamp)) }.runNow()
          case api.RoomNr(careContactId, timestamp, roomNr) => $.modState{ s => updateState(s, careContactId, Location(roomNr, timestamp)) }.runNow()
          case api.Team(careContactId, timestamp, team, clinic) => $.modState{ s => updateState(s, careContactId, Team(team, clinic, timestamp)) }.runNow()
          case api.LatestEvent(careContactId, timestamp, latestEvent, timeDiff) => $.modState{ s => updateState(s, careContactId, LatestEvent(latestEvent, timeDiff, timestamp)) }.runNow()
          case api.ArrivalTime(careContactId, timestamp, timeDiff) => $.modState{ s => updateState(s, careContactId, ArrivalTime(timeDiff, timestamp)) }.runNow()
          case api.Finished(careContactId, timestamp) => $.modState{ s => updateState(s, careContactId, Finished()) }.runNow()
          case _ => println("THIS WAS NOT EXPECTED IN ProcessPatientCardsWidget.")
      }
    }, "patient-cards-widget-topic"
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
  * Updates all time differences + 1 min.
  */
  def updateAllTimeDiffs(s: Map[String, Patient]): Map[String, Patient] = {
    var newState: Map[String, Patient] = s
    s.foreach{ p =>
      if (p._2.latestEvent.timeDiff != -1) {
        newState = updateState(newState, p._1, LatestEvent(p._2.latestEvent.latestEvent, p._2.latestEvent.timeDiff + 60000, p._2.latestEvent.timestamp)) // 60 000 ms is one minute
      }
      if (p._2.arrivalTime.timeDiff != -1) {
        newState = updateState(newState, p._1, ArrivalTime(p._2.arrivalTime.timeDiff + 60000, p._2.arrivalTime.timestamp)) // 60 000 ms is one minute
      }
    }
    return newState
  }

  /**
  * Constructs a new patient object.
  */
  def updateNewPatient(ccid: String, prop: PatientProperty): Patient = {
    prop match {
      case Priority(color, timestamp) => Patient(ccid, Priority(color, timestamp), Attended(false, "", ""), Location("", ""), Team("", "", ""), LatestEvent("", -1, ""), ArrivalTime("", ""))
      case Attended(attended, doctorId, timestamp) => Patient(ccid, Priority("", ""), Attended(attended, doctorId, timestamp), Location("", ""), Team("", "", ""), LatestEvent("", -1, ""), ArrivalTime("", ""))
      case Location(roomNr, timestamp) => Patient(ccid, Priority("", ""), Attended(false, "", ""), Location(roomNr, timestamp), Team("", "", ""), LatestEvent("", -1, ""), ArrivalTime("", ""))
      case Team(team, clinic, timestamp) => Patient(ccid, Priority("", ""), Attended(false, "", ""), Location("", ""), Team(team, clinic, timestamp), LatestEvent("", -1, ""), ArrivalTime("", ""))
      case LatestEvent(latestEvent, timeDiff, timestamp) => Patient(ccid, Priority("", ""), Attended(false, "", ""), Location("", ""), Team("", "", ""), LatestEvent(latestEvent, -1, timestamp), ArrivalTime("", ""))
      case ArrivalTime(timeDiff, timestamp) => Patient(ccid, Priority("", ""), Attended(false, "", ""), Location("", ""), Team("", "", ""), LatestEvent("", -1, ""), ArrivalTime(timeDiff, timestamp))
      case _ => Patient(ccid, Priority("", ""), Attended(false, "", ""), Location("", ""), Team("", "", ""), LatestEvent("", -1, ""), ArrivalTime("", ""))
    }
  }

  /**
  * Constructs an updates patient object.
  */
  def updateExistingPatient(s: Map[String, Patient], ccid: String, prop: PatientProperty): Patient = {
    prop match {
      case Priority(color, timestamp) => Patient(ccid, Priority(color, timestamp), s(ccid).attended, s(ccid).location, s(ccid).team, s(ccid).latestEvent, s(ccid).arrivalTime)
      case Attended(attended, doctorId, timestamp) => Patient(ccid, s(ccid).priority, Attended(attended, doctorId, timestamp), s(ccid).location, s(ccid).team, s(ccid).latestEvent, s(ccid).arrivalTime)
      case Location(roomNr, timestamp) => Patient(ccid, s(ccid).priority, s(ccid).attended, Location(roomNr, timestamp), s(ccid).team, s(ccid).latestEvent, s(ccid).arrivalTime)
      case Team(team, clinic, timestamp) => Patient(ccid, s(ccid).priority, s(ccid).attended, s(ccid).location, Team(team, clinic, timestamp), s(ccid).latestEvent, s(ccid).arrivalTime)
      case LatestEvent(latestEvent, timeDiff, timestamp) => Patient(ccid, s(ccid).priority, s(ccid).attended, s(ccid).location, s(ccid).team, LatestEvent(latestEvent, timeDiff, timestamp), s(ccid).arrivalTime)
      case ArrivalTime(timeDiff, timestamp) => Patient(ccid, s(ccid).priority, s(ccid).attended, s(ccid).location, s(ccid).team, s(ccid).latestEvent, ArrivalTime(timeDiff, timestamp))
      case _ => Patient(ccid, Priority("", ""), Attended(false, "", ""), Location("", ""), Team("", "", ""), LatestEvent("", -1, ""), ArrivalTime("", ""))
    }
  }

  /**
  * Checks if the patient belongs to this team. HERE: Process.
  */
  def belongsToThisTeam(patient: Patient): Boolean = {
    patient.team.team match {
      case "process" => true
      case _ => false
    }
  }

  /**
  * Converts milliseconds to hours and minutes, visualized in string.
  */
  def getTimeDiffReadable(milliseconds: Long): String = {
    val minutes = ((milliseconds / (1000*60)) % 60)
    val hours = ((milliseconds / (1000*60*60)) % 24)
    return hours + " h " + minutes + " m "
  }

  /**
  * Returns the correct hex color for each priority.
  */
  def decodeTriageColor(p: Priority): String = {
    p.color match {
      case "NotTriaged" => "#848484"
      case "Blue" => "#538AF4"
      case "Green" => "#009550" //"prio4"
      case "Yellow" => "#EAC706" //"prio3"
      case "Orange" => "#F08100" //"prio2"
      case "Red" => "#950000" //"prio1"
      case _ =>  {
        println("TriageColor: "+ p.color +" not expected in MedicineYellowPatientCardsWidget")
        return "#D5D5D5" //"prioNA"
      }
    }
  }

  /*
  Takes an AttendedEvent and returns a tuple containing a bool declaring wether
  icon should be filled or not as we as a string containing text to be shown
  **/
  def decodeAttended(a: Attended): (Boolean, String) = {
    if (a.attended)
    (true, a.doctorId)
    else
    (false, "Ej Påtittad")
  }

  /*
  Converts the Team.team-field of a given Team into a color value and letter to display
  **/
  def decodeTeam(t: Team): (String, String) = {
    t.team match {
      case "no-match" => ("#E8E8E8","")
      case "process" => ("FF93B8","P")
      case "stream" => ("#F8B150","S")
      case "kirurgi" => ("#D15353","K")
      case "medicin gul" => ("#FFE36F","M")
      case "medicin blå" => ("#0060C1","M")
      case "ortopedi" => ("#7DC761","O")
      case "jour" => ("#BEABEB","J")
      case _ => ("#E8E8E8","")
    }
  }


  /*
  Specifies a patientCard in SVG for scalajs-react based on a Patient.
  **/
  def patientCard(p: Patient) = {
    val cardHeight = 186
    val cardWidth = cardHeight * 1.76
    val triageFieldWidth = (cardHeight / 1.61)
    val textLeftAlignment = (triageFieldWidth + (triageFieldWidth / 11))
    val fontSize = (cardHeight / 13)
    val roomNrFontSize = (cardHeight / 3)
    //println(triageColor(p.patientData("Priority")))
    //println(p.patientData("Priority"))

    <.svg.svg( //ARTO: Skapar en <svg></svg>-tagg att fylla med objekt
      ^.`class` := "patientcardCanvas",
      ^.svg.id := p.careContactId,
      ^.svg.width := (cardWidth * 1.04).toString,
      ^.svg.height := (cardHeight * 1.04).toString,
      <.svg.rect(
        ^.`class` := "shadow",
        ^.svg.x := "3",
        ^.svg.y := "3",
        ^.svg.width := cardWidth.toString,
        ^.svg.height := cardHeight.toString,
        ^.svg.fill := "lightgrey"
      ),
      <.svg.rect(
        ^.`class` := "cardBg",
        ^.svg.x := "0",
        ^.svg.y := "0",
        ^.svg.width := cardWidth.toString,
        ^.svg.height := cardHeight.toString,
        ^.svg.fill := "white"
      ),
      <.svg.rect(
        ^.`class` := "triageField",
        ^.svg.x := "0",
        ^.svg.y := "0",
        ^.svg.width := triageFieldWidth.toString,
        ^.svg.height := cardHeight.toString,
        ^.svg.fill := decodeTriageColor(p.priority)
      ),
      <.svg.rect(
        ^.`class` := "delimiter",
        ^.svg.x := textLeftAlignment,
        ^.svg.y := (cardHeight / 1.24).toString,
        ^.svg.width := (cardWidth - triageFieldWidth - ((textLeftAlignment - triageFieldWidth) * 2)).toString,
        ^.svg.height := "1"
        //^.svg.fill := triageColor(p.patientData("Priority"))
      ),
      <.svg.g(
        ^.`class` := "teamField",
        <.svg.path(
          ^.svg.d := s"M 0,$cardHeight, 0,"+(cardHeight - (cardHeight / 3)).toString+s", "+(cardHeight / 3).toString+s",$cardHeight Z",
          ^.svg.fill := decodeTeam(p.team)._1
        ),
        <.svg.text(
          ^.svg.x := (cardHeight / 11).toString,
          ^.svg.y := (cardHeight - (cardHeight / 12)).toString,
          ^.svg.textAnchor := "middle",
          ^.svg.fontSize := (fontSize * 1.2).toString  + "px",
          ^.svg.fontWeight := "bold",
          ^.svg.fontFamily := "helvetica",
          ^.svg.fill := "white",
          decodeTeam(p.team)._2
        )
      ),
      <.svg.text(
        ^.`class` := "roomNr",
        ^.svg.x := (triageFieldWidth / 2).toString,
        ^.svg.y := (triageFieldWidth - (triageFieldWidth / 2.3)).toString,
        ^.svg.width := triageFieldWidth.toString,
        //^.svg.align := "center",
        ^.svg.textAnchor := "middle",
        ^.svg.fontSize := roomNrFontSize.toString  + "px",
        ^.svg.fill := "white",
        p.location.roomNr
      ),

      <.svg.text(
        ^.`class` := "textLatestEvent",
        ^.svg.x := textLeftAlignment.toString,
        ^.svg.y := (cardHeight / 7.6).toString,
        ^.svg.textAnchor := "top",
        ^.svg.fontSize := fontSize.toString  + "px",
        ^.svg.fontWeight := "bold",
        ^.svg.fontFamily := "Helvetica",
        "Senaste händelse"
      ),
      <.svg.text(
        ^.`class` := "latestEvent",
        ^.svg.x := (triageFieldWidth + ((cardWidth - triageFieldWidth) / 2)).toString, // eller bara leftAlignment
        ^.svg.y := (cardHeight / 2.82).toString,
        ^.svg.textAnchor := "bottom",
        ^.svg.width := triageFieldWidth.toString,
        //^.svg.align := "center",
        ^.svg.textAnchor := "middle",
        ^.svg.fontSize := (roomNrFontSize * 0.5).toString  + "px",
        ^.svg.fontWeight := "900",
        ^.svg.fontStretch := "condensed",
        ^.svg.fontFamily := "Helvetica",
        p.latestEvent.latestEvent.toUpperCase
      ),
      <.svg.svg(
        ^.`class` := "clockSymbol",
        ^.svg.x := textLeftAlignment.toString,
        ^.svg.y := (cardHeight / 2.65).toString,
        <.svg.path(
          ^.svg.d := "m 12.510169,6.8983051 -1.530508,0 0,6.1220339 5.35678,3.214068 0.765254,-1.255017 -4.591526,-2.724305 z M 11.989797,1.7966102 C 6.3575254,1.7966102 1.7966102,6.3677288 1.7966102,12 c 0,5.632271 4.5609152,10.20339 10.1931868,10.20339 5.642474,0 10.213593,-4.571119 10.213593,-10.20339 0,-5.6322712 -4.571119,-10.2033898 -10.213593,-10.2033898 z M 12,20.162712 C 7.4901017,20.162712 3.8372881,16.509898 3.8372881,12 3.8372881,7.4901017 7.4901017,3.8372881 12,3.8372881 c 4.509898,0 8.162712,3.6528136 8.162712,8.1627119 0,4.509898 -3.652814,8.162712 -8.162712,8.162712 z",
          ^.svg.transform := "scale("+(400 / cardHeight).toString+")",
          ^.svg.fill := "black"
        )
      ),
      <.svg.text(
        ^.`class` := "timeSinceLatestEvent",
        ^.svg.x := (textLeftAlignment * 1.42).toString,
        ^.svg.y := (cardHeight / 1.75).toString,
        ^.svg.fontSize := (roomNrFontSize /2.6).toString  + "px",
        getTimeDiffReadable(p.latestEvent.timeDiff) // WAS: timeSinceEvent(p.latestEvent)
      ),
      <.svg.svg(
        ^.`class` := "arrivalTime",
        ^.svg.x := textLeftAlignment.toString,
        ^.svg.y := (cardHeight / 1.19).toString,
        <.svg.path(
          ^.`class` := "timerSymbol",
          ^.svg.d := "m 1.9316099,0.03941873 -0.8388126,0 0,0.27960417 0.8388126,0 0,-0.27960417 z M 1.3724015,1.856846 l 0.2796042,0 0,-0.8388125 -0.2796042,0 0,0.8388125 z M 2.4950124,0.93275411 2.6935313,0.73423513 C 2.6334165,0.66293603 2.5677095,0.59583103 2.4964104,0.53711413 L 2.2978914,0.73563319 C 2.0811982,0.56227853 1.8085841,0.45882498 1.5122036,0.45882498 c -0.69481669,0 -1.25821911,0.56340252 -1.25821911,1.25821892 0,0.6948164 0.56200436,1.2582189 1.25821911,1.2582189 0.6962145,0 1.2582189,-0.5634025 1.2582189,-1.2582189 0,-0.2963805 -0.1034535,-0.5689945 -0.2754101,-0.78428979 z M 1.5122036,2.6956586 c -0.5410343,0 -0.97861487,-0.4375806 -0.97861487,-0.9786147 0,-0.5410341 0.43758057,-0.97861475 0.97861487,-0.97861475 0.5410342,0 0.9786147,0.43758065 0.9786147,0.97861475 0,0.5410341 -0.4375805,0.9786147 -0.9786147,0.9786147 z",
          ^.svg.transform := "scale("+(cardHeight / 26).toString+")",
          ^.svg.fill := "black"
        ),
        <.svg.text(
          ^.svg.x := (textLeftAlignment * 0.20).toString,
          ^.svg.y := (cardHeight / 10.5).toString,
          ^.svg.fontSize := fontSize.toString  + "px",
          p.arrivalTime.timeDiff // WAS:timestampToODT(p.arrivalTime.timestamp).format(DateTimeFormatter.ofPattern("H'.'m'"))
        )

      ),
      <.svg.svg(
        <.svg.svg(
          ^.`class` := "planSymbol",
          ^.svg.x := (triageFieldWidth + ((cardWidth - triageFieldWidth) / 2) - (triageFieldWidth / 10)).toString,
          ^.svg.y := (cardHeight / 1.19).toString,
          <.svg.path(
            ^.svg.d := { // To be implemented
              if (false) "M19,3 L14.82,3 C14.4,1.84 13.3,1 12,1 C10.7,1 9.6,1.84 9.18,3 L5,3 C3.9,3 3,3.9 3,5 L3,19 C3,20.1 3.9,21 5,21 L19,21 C20.1,21 21,20.1 21,19 L21,5 C21,3.9 20.1,3 19,3 L19,3 Z M12,3 C12.55,3 13,3.45 13,4 C13,4.55 12.55,5 12,5 C11.45,5 11,4.55 11,4 C11,3.45 11.45,3 12,3 L12,3 Z M10,17 L6,13 L7.41,11.59 L10,14.17 L16.59,7.58 L18,9 L10,17 L10,17 Z"
              else "m 12,3 c 0.55,0 1,0.45 1,1 0,0.55 -0.45,1 -1,1 -0.55,0 -1,-0.45 -1,-1 0,-0.55 0.45,-1 1,-1 z m 7,0 H 14.82 C 14.4,1.84 13.3,1 12,1 10.7,1 9.6,1.84 9.18,3 H 5 C 3.9,3 3,3.9 3,5 v 14 c 0,1.1 0.9,2 2,2 h 14 c 1.1,0 2,-0.9 2,-2 V 5 C 21,3.9 20.1,3 19,3 Z"
            },
            ^.svg.fill := { // To be implemented
              if (false) "#26AA68"
              else "#E8E8E8"
            }
          ),
          ^.svg.transform := "scale("+(187 / cardHeight).toString+")"
        ),
        // <.svg.rect(
        //   ^.`class` := "slash",
        //   ^.svg.x := (triageFieldWidth + ((cardWidth - triageFieldWidth) / 2) + (triageFieldWidth / 5)).toString,
        //   ^.svg.y := (cardHeight / 4).toString,
        //   ^.svg.width := "1",
        //   ^.svg.height := "30",
        //   ^.svg.transform := "rotate(25)"
        // ),
        <.svg.svg(
          ^.`class` := "attendedSymbol",
          ^.svg.x := (triageFieldWidth + ((cardWidth - triageFieldWidth) / 2) + (triageFieldWidth / 6)).toString,
          ^.svg.y := (cardHeight / 1.19).toString,
          <.svg.path(
            ^.svg.d := "M12,4.5 C7,4.5 2.73,7.61 1,12 C2.73,16.39 7,19.5 12,19.5 C17,19.5 21.27,16.39 23,12 C21.27,7.61 17,4.5 12,4.5 L12,4.5 Z M12,17 C9.24,17 7,14.76 7,12 C7,9.24 9.24,7 12,7 C14.76,7 17,9.24 17,12 C17,14.76 14.76,17 12,17 L12,17 Z M12,9 C10.34,9 9,10.34 9,12 C9,13.66 10.34,15 12,15 C13.66,15 15,13.66 15,12 C15,10.34 13.66,9 12,9 L12,9 Z",
            ^.svg.fill := {
              if (decodeAttended(p.attended)._1) "black"
              else "#E8E8E8"
            }
          ),
          ^.svg.transform := "scale("+(187 / cardHeight).toString+")"
        )
      ),
      <.svg.svg(
        ^.`class` := "attendedStatus",
        ^.svg.x := (textLeftAlignment + ((cardWidth - triageFieldWidth) / 2)).toString,
        ^.svg.y := (cardHeight / 1.2).toString,
        <.svg.text(
          ^.svg.x := (textLeftAlignment * 0.3).toString,
          ^.svg.y := (cardHeight / 10.5).toString,
          ^.svg.fontSize := fontSize.toString  + "px",
          decodeAttended(p.attended)._2

        )
      )

    )
  }

    def render(pmap: Map[String, Patient]) = {
      spgui.widgets.css.WidgetStyles.addToDocument()
      var teamMap: Map[String, Patient] = Map()
      (pmap - "-1").foreach{ p =>
        if (belongsToThisTeam(p._2)) {
          teamMap += p._1 -> p._2
        }
      }

      <.div(^.`class` := "card-holder-root", Styles.helveticaZ)( // This div is really not necessary
        teamMap.values map { p =>
          patientCard(p)
        }
      )
    }
  }

  private val cardHolderComponent = ReactComponentB[Unit]("cardHolderComponent")
  .initialState(Map("-1" ->
    Patient(
      "4502085",
      Priority("NotTriaged", "2017-02-01T15:49:19Z"),
      Attended(true, "sarli29", "2017-02-01T15:58:33Z"),
      Location("52", "2017-02-01T15:58:33Z"),
      Team("GUL", "NAKME", "2017-02-01T15:58:33Z"),
      LatestEvent("OmsKoord", -1, "2017-02-01T15:58:33Z"),
      ArrivalTime("", "2017-02-01T10:01:38Z")
      )))
  .renderBackend[Backend]
  .build

  def apply() = spgui.SPWidget(spwb => {
    cardHolderComponent()
  })
}
