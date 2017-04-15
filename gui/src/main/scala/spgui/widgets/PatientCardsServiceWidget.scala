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

object PatientCardsServiceWidget {

  sealed trait PatientProperty

  case class Priority(color: String, timestamp: String) extends PatientProperty
  case class Attended(attended: Boolean, doctorId: String, timestamp: String) extends PatientProperty
  case class Location(roomNr: String, timestamp: String) extends PatientProperty
  case class Team(team: String, clinic: String, timestamp: String) extends PatientProperty
  case class LatestEvent(latestEvent: String, timeDiff: Long, timestamp: String) extends PatientProperty
  case class ArrivalTime(timeDiff: Long, timestamp: String) extends PatientProperty

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
          case api.Tick() => {
              $.modState{ s => updateAllTimeDiffs(s) }.runNow()
          }
          case api.NotTriaged(careContactId, timestamp) => {
              $.modState{ s => updateState(s, careContactId, Priority("NotTriaged", timestamp)) }.runNow()
          }
          case api.Green(careContactId, timestamp) => {
              $.modState{ s => updateState(s, careContactId, Priority("Green", timestamp)) }.runNow()
          }
          case api.Yellow(careContactId, timestamp) => {
              $.modState{ s => updateState(s, careContactId, Priority("Yellow", timestamp)) }.runNow()
          }
          case api.Orange(careContactId, timestamp) => {
              $.modState{ s => updateState(s, careContactId, Priority("Orange", timestamp)) }.runNow()
          }
          case api.Red(careContactId, timestamp) => {
              $.modState{ s => updateState(s, careContactId, Priority("Red", timestamp)) }.runNow()
          }
          case api.Attended(careContactId, timestamp, attended, doctorId) => {
            $.modState{ s => updateState(s, careContactId, Attended(attended, doctorId, timestamp)) }.runNow()
          }
          case api.RoomNr(careContactId, timestamp, roomNr) => {
            $.modState{ s => updateState(s, careContactId, Location(roomNr, timestamp)) }.runNow()
          }
          case api.Team(careContactId, timestamp, team, clinic) => {
            $.modState{ s => updateState(s, careContactId, Team(team, clinic, timestamp)) }.runNow()
          }
          case api.LatestEvent(careContactId, timestamp, latestEvent, timeDiff) => {
            $.modState{ s => updateState(s, careContactId, LatestEvent(latestEvent, timeDiff, timestamp)) }.runNow()
          }
          case api.ArrivalTime(careContactId, timestamp, timeDiff) => {
            $.modState{ s => updateState(s, careContactId, ArrivalTime(timeDiff, timestamp)) }.runNow()
          }
          case api.Finished(careContactId, timestamp) => {
            $.modState{ s => s - careContactId }.runNow()
          }
          case _ => println("THIS WAS NOT EXPECTED IN PatientCardsServiceWidget.")
      }
    }, "patient-cards-widget-topic"
  )

  def updateState(s: Map[String, Patient], careContactId: String, prop: PatientProperty): Map[String, Patient] = {
    if (s.keys.exists(_ == careContactId)) {
      s + (careContactId -> updateExistingPatient(s, careContactId, prop))
    } else {
      s + (careContactId -> updateNewPatient(careContactId, prop))
    }
  }

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

  def updateNewPatient(ccid: String, prop: PatientProperty): Patient = {
    prop match {
      case Priority(color, timestamp) => Patient(ccid, Priority(color, timestamp), Attended(false, "", ""), Location("", ""), Team("", "", ""), LatestEvent("", -1, ""), ArrivalTime(-1, ""))
      case Attended(attended, doctorId, timestamp) => Patient(ccid, Priority("", ""), Attended(attended, doctorId, timestamp), Location("", ""), Team("", "", ""), LatestEvent("", -1, ""), ArrivalTime(-1, ""))
      case Location(roomNr, timestamp) => Patient(ccid, Priority("", ""), Attended(false, "", ""), Location(roomNr, timestamp), Team("", "", ""), LatestEvent("", -1, ""), ArrivalTime(-1, ""))
      case Team(team, clinic, timestamp) => Patient(ccid, Priority("", ""), Attended(false, "", ""), Location("", ""), Team(team, clinic, timestamp), LatestEvent("", -1, ""), ArrivalTime(-1, ""))
      case LatestEvent(latestEvent, timeDiff, timestamp) => Patient(ccid, Priority("", ""), Attended(false, "", ""), Location("", ""), Team("", "", ""), LatestEvent(latestEvent, -1, timestamp), ArrivalTime(-1, ""))
      case ArrivalTime(timeDiff, timestamp) => Patient(ccid, Priority("", ""), Attended(false, "", ""), Location("", ""), Team("", "", ""), LatestEvent("", -1, ""), ArrivalTime(timeDiff, timestamp))
      case _ => Patient(ccid, Priority("", ""), Attended(false, "", ""), Location("", ""), Team("", "", ""), LatestEvent("", -1, ""), ArrivalTime(-1, ""))
    }
  }

  def updateExistingPatient(s: Map[String, Patient], ccid: String, prop: PatientProperty): Patient = {
    prop match {
      case Priority(color, timestamp) => Patient(ccid, Priority(color, timestamp), s(ccid).attended, s(ccid).location, s(ccid).team, s(ccid).latestEvent, s(ccid).arrivalTime)
      case Attended(attended, doctorId, timestamp) => Patient(ccid, s(ccid).priority, Attended(attended, doctorId, timestamp), s(ccid).location, s(ccid).team, s(ccid).latestEvent, s(ccid).arrivalTime)
      case Location(roomNr, timestamp) => Patient(ccid, s(ccid).priority, s(ccid).attended, Location(roomNr, timestamp), s(ccid).team, s(ccid).latestEvent, s(ccid).arrivalTime)
      case Team(team, clinic, timestamp) => Patient(ccid, s(ccid).priority, s(ccid).attended, s(ccid).location, Team(team, clinic, timestamp), s(ccid).latestEvent, s(ccid).arrivalTime)
      case LatestEvent(latestEvent, timeDiff, timestamp) => Patient(ccid, s(ccid).priority, s(ccid).attended, s(ccid).location, s(ccid).team, LatestEvent(latestEvent, timeDiff, timestamp), s(ccid).arrivalTime)
      case ArrivalTime(timeDiff, timestamp) => Patient(ccid, s(ccid).priority, s(ccid).attended, s(ccid).location, s(ccid).team, s(ccid).latestEvent, ArrivalTime(timeDiff, timestamp))
      case _ => Patient(ccid, Priority("", ""), Attended(false, "", ""), Location("", ""), Team("", "", ""), LatestEvent("", -1, ""), ArrivalTime(-1, ""))
    }
  }

  /*
  Takes a Patient modifies it according to passed PatientProperty and returns
  an updated Patient
  **/
  /**
  def updatePatient(p: Patient, pp: api.PatientProperty): Patient ={
    var tempPatient: Patient = p
  }
  */

  /*
  To test if laterTimestamp happened later than timestamp
  **/
  /**
  def happenedAfter(laterTimestamp: String, timestamp: String ): Boolean = {
    timestampToODT(laterTimestamp).isAfter(timestampToODT(timestamp))
  }*/

  /*
  Converts String of type "0000-01-24T00:00:00.000Z" into OffsetDateTime
  Used by happenedAfter.
  **/
  /**
  def timestampToODT(timestamp: String): OffsetDateTime = {
    OffsetDateTime.parse(timestamp, DateTimeFormatter.ISO_INSTANT)
  }*/

  /*

  **/
  def getTimeDiffReadable(milliseconds: Long): String = {
    val minutes = ((milliseconds / (1000*60)) % 60)
    val hours = ((milliseconds / (1000*60*60)) % 24)
    return hours + " h " + minutes + " m "
  }

  def decodeTriageColor(p: Priority): String = {
    p.color match {
      case "NotTriaged" => "#D5D5D5"
      case "Blue" => "#1288FF"
      case "Green" => "#289500" //"prio4"
      case "Yellow" => "#EAC706" //"prio3"
      case "Orange" => "#F08100" //"prio2"
      case "Red" => "#950000" //"prio1"
      case _ =>  {
        println("TriageColor: "+ p.color +" not expected in patientCardsService")
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
  Converts the Team.team-field of a given Team into a color value
  **/
  def decodeTeamColor(t: Team): String = {
    t.team match {
      case "Streamteam" => "#BEABEB"
      case "Process" => "#FF93B8"
      case "Blå" => "#0060C1"
      case "Röd" => "#D15353"
      case "Gul" => ""
      case _ => "#E8E8E8"
    }
  }

  /*
  Converts the Team.klinik-field of a given Team into a letter to present
  **/
  def decodeKlinikLetter(t: Team): String = {
    t.clinic match {
      case "NAKKI" => "K"
      case "NAKME" => "M"
      case "NAKOR" => "O"
      case "NAKBA" | "NAKGY" | "NAKÖN" => "J"
      case _ => ""
    }
  }

  /*
  Takes a PatientProperty and calculates minutes since the event happened
  **/
  /**
  def timeSinceEvent(pp: LatestEvent): String = {
    timestampToODT(pp.timestamp).until(OffsetDateTime.now(ZoneId.of("Z")), temporal.ChronoUnit.valueOf("MINUTES")).toString
  }*/

  /*
  Specifies a patientCard in SVG for scalajs-react based on a Patient.
  **/
  def patientCard(p: Patient) = {
    val cardHeight = 186
    val cardWidth = cardHeight * 1.7
    val triageFieldWidth = (cardWidth / 3)
    val textLeftAlignment = (triageFieldWidth + (triageFieldWidth / 11))
    val fontSize = (cardHeight / 12)
    val roomNrFontSize = (cardHeight / 3)
    //println(triageColor(p.patientData("Priority")))
    //println(p.patientData("Priority"))

    <.svg.svg( //ARTO: Skapar en <svg>-tagg att fylla med obekt
      ^.`class` := "patientcard",
      ^.svg.id := p.careContactId,
      ^.svg.width := cardWidth.toString,
      ^.svg.height := cardHeight.toString,
      <.svg.rect(
        ^.`class` := "bg",
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
      <.svg.path(
        ^.`class` := "klinikfield",
        // ^.svg.d := s"M 0,$cardHeight, "+(cardWidth / 4.6).toString+s",$cardHeight, -"+(cardWidth / 4.6).toString+s",0 Z",
        ^.svg.d := s"M 0,$cardHeight, 0,"+(cardHeight - (cardHeight / 2.6)).toString+s", "+(cardHeight / 2.6).toString+s",$cardHeight Z",
        ^.svg.fill := decodeKlinikLetter(p.team)
      ),
      <.svg.path(
        ^.`class` := "teamfield",
        // ^.svg.d := s"M 0,$cardHeight, "+(cardWidth / 4.6).toString+s",$cardHeight, -"+(cardWidth / 4.6).toString+s",0 Z",
        ^.svg.d := s"M 0,$cardHeight, 0,"+(cardHeight - (cardHeight / 3)).toString+s", "+(cardHeight / 3).toString+s",$cardHeight Z",
        ^.svg.fill := decodeTeamColor(p.team)
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
      // <.svg.text(
      //   ^.`class` := "careContactId",
      //   ^.svg.x := "100",
      //   ^.svg.y := "40",
      //   ^.svg.fontSize := "47",
      //   ^.svg.fill := "black",
      //   p.careContactId
      // ),
      <.svg.text(
        ^.`class` := "textLatestEvent",
        ^.svg.x := textLeftAlignment.toString,
        ^.svg.y := ((cardHeight / 3) / 3).toString,
        ^.svg.textAnchor := "top",
        ^.svg.fontSize := fontSize.toString  + "px",
        "Senaste händelse"
      ),
      <.svg.text(
        ^.`class` := "latestEvent",
        ^.svg.x := textLeftAlignment.toString,
        ^.svg.y := (cardHeight / 2.8).toString,
        ^.svg.textAnchor := "bottom",
        ^.svg.fontSize := (roomNrFontSize * 0.8).toString  + "px",
        p.latestEvent.latestEvent
      ),
      <.svg.svg(
        ^.`class` := "timerSymbol",
        ^.svg.x := textLeftAlignment.toString,
        ^.svg.y := (cardHeight / 2.3).toString,
        //^.svg.viewBox := "0 50 400 400",
        // ^.svg.viewBox := textLeftAlignment.toString+" "+(cardHeight / 2.3).toString+" 40 40",
        <.svg.path(
          ^.svg.d := "m 1.9316099,0.03941873 -0.8388126,0 0,0.27960417 0.8388126,0 0,-0.27960417 z M 1.3724015,1.856846 l 0.2796042,0 0,-0.8388125 -0.2796042,0 0,0.8388125 z M 2.4950124,0.93275411 2.6935313,0.73423513 C 2.6334165,0.66293603 2.5677095,0.59583103 2.4964104,0.53711413 L 2.2978914,0.73563319 C 2.0811982,0.56227853 1.8085841,0.45882498 1.5122036,0.45882498 c -0.69481669,0 -1.25821911,0.56340252 -1.25821911,1.25821892 0,0.6948164 0.56200436,1.2582189 1.25821911,1.2582189 0.6962145,0 1.2582189,-0.5634025 1.2582189,-1.2582189 0,-0.2963805 -0.1034535,-0.5689945 -0.2754101,-0.78428979 z M 1.5122036,2.6956586 c -0.5410343,0 -0.97861487,-0.4375806 -0.97861487,-0.9786147 0,-0.5410341 0.43758057,-0.97861475 0.97861487,-0.97861475 0.5410342,0 0.9786147,0.43758065 0.9786147,0.97861475 0,0.5410341 -0.4375805,0.9786147 -0.9786147,0.9786147 z",
          ^.svg.fill := "black",
          ^.svg.transform := "scale("+(cardHeight / 11).toString+")"

          // <.svg.animateTransform(
          //   ^.svg.`type` := "scale",
          //   ^.svg.attributeType := "XML",
          //   ^.svg.attributeName := "transform",
          //   ^.svg.from := "1 1",
          //   ^.svg.to := "2 2",
          //   ^.svg.dur := "10s"
          // )
        )),
        <.svg.text(
          ^.`class` := "timeSinceLatestEvent",
          ^.svg.x := (textLeftAlignment * 1.48).toString,
          ^.svg.y := (cardHeight / 1.5).toString,
          ^.svg.fontSize := (fontSize * 2).toString  + "px",
          getTimeDiffReadable(p.latestEvent.timeDiff) // WAS: timeSinceEvent(p.latestEvent)
        ),
        // <.svg.svg(
        //   ^.`class` := "latestActor",
        //   ^.svg.x := (cardWidth / 1.8).toString,
        //   ^.svg.y := (cardHeight / 1.7).toString,
        //   <.svg.path(
        //     ^.`class` := "actorSymbol",
        //     ^.svg.d := "M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z",
        //     ^.svg.fill := "black"
        //   ),
        //   <.svg.text(
        //     ^.svg.x := (textLeftAlignment * 0.2).toString,
        //     ^.svg.y := 20.toString,
        //     ^.svg.fontSize := (cardHeight / 9).toString,
        //     Styles.patientCardText,
        //     p.patientData("CareContactRegistrationTime")
        //   )
        // ),
        <.svg.svg(
          ^.`class` := "arrivalTime",
          ^.svg.x := textLeftAlignment.toString,
          ^.svg.y := (cardHeight / 1.2).toString,
          <.svg.path(
            ^.`class` := "clockSymbol",
            //^.svg.width := cardWidth.toString,
            ^.svg.d := "m 12.510169,6.8983051 -1.530508,0 0,6.1220339 5.35678,3.214068 0.765254,-1.255017 -4.591526,-2.724305 z M 11.989797,1.7966102 C 6.3575254,1.7966102 1.7966102,6.3677288 1.7966102,12 c 0,5.632271 4.5609152,10.20339 10.1931868,10.20339 5.642474,0 10.213593,-4.571119 10.213593,-10.20339 0,-5.6322712 -4.571119,-10.2033898 -10.213593,-10.2033898 z M 12,20.162712 C 7.4901017,20.162712 3.8372881,16.509898 3.8372881,12 3.8372881,7.4901017 7.4901017,3.8372881 12,3.8372881 c 4.509898,0 8.162712,3.6528136 8.162712,8.1627119 0,4.509898 -3.652814,8.162712 -8.162712,8.162712 z",
            ^.svg.fill := "black"
          ),
          <.svg.text(
            ^.svg.x := (textLeftAlignment * 0.3).toString,
            ^.svg.y := 20.toString,
            ^.svg.fontSize := fontSize.toString  + "px",
            getTimeDiffReadable(p.arrivalTime.timeDiff) // WAS:timestampToODT(p.arrivalTime.timestamp).format(DateTimeFormatter.ofPattern("H'.'m'"))
          )
        ),
        <.svg.svg(
          ^.`class` := "attendedStatus",
          ^.svg.x := (textLeftAlignment + ((cardWidth - triageFieldWidth) / 2)).toString,
          ^.svg.y := (cardHeight / 1.2).toString,
          <.svg.path(
            ^.`class` := "doctorSymbol",
            //^.svg.width := cardWidth.toString,
            ^.svg.d := "m 4.9752607,1044.5838 c -1.8280207,0 -3.6508627,0.8979 -4.7910985,2.7296 1.8619399,3.5028 7.5608327,3.7247 9.6092216,0 -1.1569646,-1.8081 -2.9901024,-2.7236 -4.8181231,-2.7296 z m 0.011583,0.6602 a 2.059132,2.059132 0 0 1 2.0577403,2.0578 2.059132,2.059132 0 0 1 -2.0577403,2.0615 2.059132,2.059132 0 0 1 -2.05774,-2.0615 2.059132,2.059132 0 0 1 2.05774,-2.0578 z m -0.096517,0.6873 a 1.3727547,1.3727547 0 0 0 -1.274023,1.3705 1.3727547,1.3727547 0 0 0 1.3705399,1.3745 1.3727547,1.3727547 0 0 0 1.3744007,-1.3745 1.3727547,1.3727547 0 0 0 -1.3744007,-1.3705 1.3727547,1.3727547 0 0 0 -0.096517,0 z",
            ^.svg.fill := {
              if (decodeAttended(p.attended)._1) "black"
              else "grey" // "#E8E8E8"
            }

          ),
          <.svg.text(
            ^.svg.x := (textLeftAlignment * 0.3).toString,
            ^.svg.y := 20.toString,
            ^.svg.fontSize := fontSize.toString  + "px",
            decodeAttended(p.attended)._2

          )
        )

      )
    }

    def render(pmap: Map[String, Patient]) = {
      spgui.widgets.css.WidgetStyles.addToDocument()

      <.div(^.`class` := "card-holder-root")( // This div is really not necessary
        (pmap - "-1").values map { p =>
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
      ArrivalTime(-1, "2017-02-01T10:01:38Z")
      )))
  .renderBackend[Backend]
  .build

  def apply() = spgui.SPWidget(spwb => {
    cardHolderComponent()
  })
}
