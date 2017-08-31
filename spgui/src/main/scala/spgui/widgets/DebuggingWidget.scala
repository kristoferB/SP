package spgui.widgets

import java.time._
import java.time.OffsetDateTime

import spgui.circuit.SPGUICircuit
// import java.time.temporal._
import java.time.format.DateTimeFormatter
import java.text.SimpleDateFormat
import java.util.UUID

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.vdom.all.svg
import japgolly.scalajs.react.ReactDOM

import spgui.SPWidget
import spgui.widgets.css.{WidgetStyles => Styles}
import spgui.communication._

import sp.domain._

import scala.concurrent.duration._
import scala.scalajs.js
import scala.util.{ Try, Success }
import scala.util.Random.nextInt
import scala.collection.mutable.ListBuffer

import org.scalajs.dom
import org.scalajs.dom.raw
import org.scalajs.dom.{svg => *}
import org.singlespaced.d3js.d3
import org.singlespaced.d3js.Ops._

import scalacss.ScalaCssReact._
import scalacss.DevDefaults._

import spgui.widgets.{API_PatientEvent => api}
import spgui.widgets.{API_Patient => apiPatient}

object DebuggingWidget {

  private class Backend($: BackendScope[String, Map[String, apiPatient.Patient]]) {

    var patientObs = Option.empty[rx.Obs]
    def setPatientObs(): Unit = {
      patientObs = Some(spgui.widgets.akuten.PatientModel.getPatientObserver(
        patients => $.setState(patients).runNow()
      ))
    }

    val wsObs = BackendCommunication.getWebSocketStatusObserver(  mess => {
      if (mess) send(api.GetState())
    }, "patient-cards-widget-topic")



    def send(mess: api.Event) {
      val json = ToAndFrom.make(SPHeader(from = "DebuggingWidget", to = "FelsökningsService"), mess)
      BackendCommunication.publish(json, "widget-event")
    }

    /**
    * Checks if the patient belongs to this team.
    */
    def belongsToThisTeam(patient: apiPatient.Patient, filter: String): Boolean = {
      filter.isEmpty || patient.team.team.contains(filter)
    }

    /**
    * Returns the correct hex color for each priority.
    */
    def decodeTriageColor(p: apiPatient.Priority): String = {
      p.color match {
        case "NotTriaged" => "#afafaf"//"#D5D5D5"
        case "Blue" => "#538AF4"
        case "Green" => "#009550" //"prio4"
        case "Yellow" => "#EAC706" //"prio3"
        case "Orange" => "#F08100" //"prio2"
        case "Red" => "#950000" //"prio1"
        case _ =>  {
          println("TriageColor: "+ p.color +" not expected in DebuggingWidget")
          return "#D5D5D5" //"prioNA"
        }
      }
    }
    /*
    Takes an AttendedEvent and returns a tuple containing a bool declaring wether
    icon should be filled or not as we as a string containing text to be shown
    **/
    def decodeAttended(a: apiPatient.Attended): (Boolean, String) = {
      if (a.attended)
      (true, a.doctorId)
      else
      (false, "Ej Påtittad")
    }

    /*
    Converts the Team.team-field of a given Team into a color value and letter to display
    **/
    def decodeTeam(t: apiPatient.Team): (String) = {
      t.team match {
        case "no-match" => ("")
        case "process" => ("P")
        case "stream" => ("S")
        case "kirurgi" => ("K")
        case "medicin gul" => ("M")
        case "medicin blå" => ("M")
        case "medicin" =>  ("M")
        case "NAKM" =>  ("NAKM")
        case "ortopedi" => ("O")
        case "jour" => ("J")
        case _ => ("")
      }
    }


    /**
    Decodes the initial background and icon colors of the progress bar. Tuple values as follows:
    (initial background, initial symbol)
    */
    def progressBarInitialColoring(p: apiPatient.Priority): (String, String) = {
      p.color match {
        case "NotTriaged" => ("#E0E0E0", "#AFAFAF")
        case "Blue" => ("#DDE8FF", "#538AF4")
        case "Green" => ("#F5FAF8", "#DCE2DF")
        case "Yellow" => ("#FFED8D", "#EAC706")
        case "Orange" => ("#FCC381", "#F08100")
        case "Red" => ("#D99898", "#950000")
        case _ =>  {
          println("TriageColor: "+ p.color +" not expected in DebuggingWidget")
          return ("#E0E0E0", "#AFAFAF") //"NotTriaged"
        }
      }
    }
    /**
    Decodes the background and icon colors of the progress bar when stages are completed.
    Returns List as follows:
    List((attended background color, attended symbol color, patient is attended),
    (plan background color, plan symbol color, plan does exist),
    (finished background color, finished symbol color, patient is finished))
    */
    def progressBarColoring(p: apiPatient.Patient): List[(String, String, Boolean)] = {
      val coloring = ListBuffer[(String, String, Boolean)]()
      val initColoring = progressBarInitialColoring(p.priority)

      if (decodeAttended(p.attended)._1) coloring += Tuple3("#8D47AA", "#FFFFFF", true)
      else coloring += Tuple3(initColoring._1, initColoring._2, false)

      if (p.plan.hasPlan) coloring += Tuple3("#E9B7FF", "#FFFFFF", true) // To be implemented: Plan exists
      else coloring += Tuple3(initColoring._1, initColoring._2, false)

      if (p.finished.finishedStillPresent) coloring += Tuple3("#47AA62", "#FFEDFF", true)
      else coloring += Tuple3(initColoring._1, initColoring._2, false)

      coloring.toList
    }

    /**
    * Converts milliseconds to hours and minutes, visualized in string.
    */
    def getTimeDiffReadable(milliseconds: Long): (String, String) = {
      val minutes = ((milliseconds / (1000*60)) % 60)
      val hours = ((milliseconds / (1000*60*60)) )//% 24)

      val timeString = (hours, minutes) match {
        case (0,_) => {if (minutes == 0) "" else (minutes + " min").toString}
        case (_,0) => (hours + " h").toString
        case _ => (hours + " h " + minutes + " m").toString
      }
      val days = (milliseconds / (1000*60*60*24))
      val dayString = days match {
        case 0 => ""
        case (n: Long) => "(> "+ n + " dygn)"
      }
      (timeString, dayString)
    }

    /*
    * Returns true if a patient has waited longer than is recommended according
    * to triage priority.
    * Prio red: immediately, Prio orange: 20 min, Prio yellow or green: 60 min.
    **/
    def hasWaitedTooLong(p: apiPatient.Patient) = {
      p.priority.color match {
        case "Green" | "Yellow" => if (p.latestEvent.timeDiff > 3600000) true else false
        case "Orange" => if (p.latestEvent.timeDiff > 1200000) true else false
        case "Red" => true
        case _ => false
      }
    }






    /**
    From ElvisDataHandler.scala
    Code used for data to CoordinatorDiagramWidget
    */
    def decodeTeam(reasonForVisit: String, location: String, clinic: String): String = {
      reasonForVisit match {
        case "AKP" => "stream"
        case "ALL" | "TRAU" => "process"
        case "B" | "MEP" => {
          clinic match {
            case "NAKME" => {
              if (location != "") {
                location.charAt(0) match {
                  case 'B' => "medicin blå"
                  case 'G' => "medicin gul"
                  case 'P' => "process"
                  case _ => "medicin övriga"
                }
              } else {
                "medicin övriga"
              }
            }
            case "NAKKI" => "kirurgi"
            case "NAKOR" => "ortopedi"
            case "NAKBA" | "NAKGY" | "NAKÖN" => "jour"
            case "NAKM" => {
              if (location != "") {
                location.charAt(0) match {
                  case 'B' => "medicin blå"
                  case 'G' => "medicin gul"
                  case 'P' => "process"
                  case _ => "NAKM"
                }
              } else {
                "NAKM"
              }
            }
            case _ => "no-match"
          }
        }
        case _ => "no-match"
      }
    }









    /*
    Specifies a patientCard in SVG for scalajs-react based on a Patient.
    **/
    def patientCard(p: apiPatient.Patient) = {
      val cardScaler = 1.2

      val cardHeight = 100 // change only with new graphics
      val cardWidth = 176 // change only with new graphics

      val fontSizeSmall = 7.6
      val fontSizeMedium = 15.2
      val fontSizeLarge = 35

      val cardBackgroundColor = "#ffffff"
      val contentColorDark = "#000000"
      val contentColorLight = "#ffffff"
      val contentColorAttention = "#E60000"
      val delimiterColor = "#95989a"
      val shadowColor = "lightgrey"


      svg.svg( //ARTO: Skapar en <svg>svg>-tagg att fylla med objekt
        ^.key := p.careContactId,
        ^.`class` := "patient-card-canvas",
        svg.width := (cardScaler * cardWidth * 1.04).toString,
        svg.height := (cardScaler * cardHeight * 1.04).toString,
        svg.viewBox := "0 0 "+ (cardWidth + 4).toString +" "+ (cardHeight + 4).toString,
        // svg.transform := "scale(" + cardScaler + ")",
        svg.id := p.careContactId,
        svg.g(
          ^.`class` := "patient-card-graphics",
          //svg.transform := "translate(0,0)",
          svg.rect(
            ^.`class` := "shadow",
            svg.y := "2",
            svg.x := "2",
            svg.height := cardHeight.toString,
            svg.width := cardWidth.toString,
            svg.fill := shadowColor
          ),
          svg.rect(
            ^.`class` := "bg-field",
            svg.y := 0,
            svg.x := 0,
            svg.height := cardHeight,
            svg.width := cardWidth,
            svg.fill := cardBackgroundColor
          ),
          svg.path(
            ^.`class` := "triage-field",
            svg.d := "m 0.13909574,0.13190582 62.53049726,0 0,99.99740418 -62.53049726,0 z",
            svg.fill := decodeTriageColor(p.priority)
          ),
          svg.path(
            ^.`class` := "delimiter",
            svg.d := "m 67.626393,80.531612 0,1.07813 103.136807,0 0,-1.07813 -103.136807,0 z",
            svg.fill := delimiterColor
          ),
          if (p.latestEvent.latestEvent != "") { // Only draw this if latestEvent exists.
            svg.path(
              ^.`class` := "timer-symbol",
              svg.d := "m 72.589477,69.337097 -2.03739,0 0,0.679124 2.03739,0 0,-0.679124 z m -1.358295,4.414306 0.679131,0 0,-2.037372 -0.679131,0 0,2.037372 z m 2.72676,-2.244506 0.482133,-0.482177 C 74.29407,70.851542 74.134459,70.688554 73.961309,70.545937 l -0.482201,0.482178 c -0.526322,-0.421054 -1.188477,-0.672333 -1.90836,-0.672333 -1.687619,0 -3.056016,1.368437 -3.056016,3.05606 0,1.687621 1.36503,3.056057 3.056016,3.056057 1.691055,0 3.056084,-1.368436 3.056084,-3.056057 0,-0.719873 -0.251256,-1.382018 -0.66889,-1.904945 z m -2.387194,4.281878 c -1.314105,0 -2.37692,-1.062829 -2.37692,-2.376933 0,-1.314105 1.062815,-2.376934 2.37692,-2.376934 1.314104,0 2.376954,1.062829 2.376954,2.376934 0,1.314104 -1.06285,2.376933 -2.376954,2.376933 z",
              svg.fill := contentColorDark
            )
          } else {
            svg.path(
              ^.`class` := "no-timer-symbol"
            )
          },
          svg.path(
            ^.`class` := "clock-symbol",
            //  svg.transform := "translate(0,1)",
            svg.d := "m 72.223193,87.546362 -0.6325,0 0,2.53288 2.2147,1.32892 0.3181,-0.52057 -1.8987,-1.12633 z m -0.2155,-2.10927 a 4.218563,4.218563 0 1 0 4.2229,4.21856 4.2164074,4.2164074 0 0 0 -4.2229,-4.21856 z m 0,7.59373 a 3.3746349,3.3746349 0 1 1 3.3746,-3.37464 3.374096,3.374096 0 0 1 -3.3746,3.37464 z",
            svg.fill := contentColorDark
          ),
          svg.path(
            ^.`class` := "doctor-symbol",
            //svg.transform := "translate(0,2)",
            svg.d := "m 127.90317,90.593352 c -1.1749,0 -3.519,0.58956 -3.519,1.75954 l 0,0.88002 7.0385,0 0,-0.88002 c 0,-1.16998 -2.3446,-1.75954 -3.5195,-1.75954 z m 0,-0.88004 a 1.759531,1.759531 0 1 0 -1.7596,-1.75951 1.7589921,1.7589921 0 0 0 1.7596,1.75951 z",
            svg.fill := contentColorDark
          ),
          svg.path(
            ^.`class` := "timeline-attended-bg",
            svg.d := "m 17.109896,81.261292 6.5568,9.76783 -6.5535,9.096778 0,-15.203718 z m -16.97080026,0.007 16.97560026,0 0,18.861728 -16.97560026,0 z",
            svg.fill := progressBarColoring(p).head._1
          ),
          svg.path(
            ^.`class` := "timeline-attended-symbol",
            svg.d := "m 10.541796,86.403152 c -2.6944999,0 -4.9955003,1.67602 -5.9280003,4.0418 0.9325,2.36581 3.2335004,4.04181 5.9280003,4.04181 2.6946,0 4.9958,-1.676 5.9281,-4.04181 -0.9323,-2.36578 -3.2335,-4.0418 -5.9281,-4.0418 l 0,0 z m 0,6.73634 c -1.4872999,0 -2.6944999,-1.20716 -2.6944999,-2.69454 0,-1.48737 1.2072,-2.69452 2.6944999,-2.69452 1.4876,0 2.6946,1.20715 2.6946,2.69452 0,1.48738 -1.207,2.69454 -2.6946,2.69454 l 0,0 z m 0,-4.31126 c -0.8944999,0 -1.6166999,0.72212 -1.6166999,1.61672 0,0.89458 0.7222,1.61672 1.6166999,1.61672 0.8946,0 1.6169,-0.72214 1.6169,-1.61672 0,-0.8946 -0.7223,-1.61672 -1.6169,-1.61672 l 0,0 z",
            svg.fill := progressBarColoring(p).head._2
          ),
          svg.path(
            ^.`class` := "timeline-plan-bg",
            svg.d := "m 40.222496,100.12931 -22.57,0 0,-0.077 6.4593,-8.969548 -6.4593,-9.65344 0,-0.16168 22.57,0 6.5137,9.77577 -6.5137,9.045548 0,0 z",
            svg.fill := progressBarColoring(p).tail.head._1
          ),
          if (!progressBarColoring(p).tail.head._3) { // Choose correct version of symbol
            svg.path(
              ^.`class` := "timeline-no-plan-symbol",
              svg.d := "m 33.116996,86.655372 c 0.2401,0 0.4366,0.19645 0.4366,0.43654 0,0.2401 -0.1965,0.43653 -0.4366,0.43653 -0.24,0 -0.4365,-0.19643 -0.4365,-0.43653 0,-0.24009 0.1965,-0.43654 0.4365,-0.43654 l 0,0 z m 3.056,0 -1.8249,0 c -0.1835,-0.50637 -0.6635,-0.87306 -1.2311,-0.87306 -0.5674,0 -1.0477,0.36669 -1.2311,0.87306 l -1.8246,0 c -0.4803,0 -0.8731,0.39289 -0.8731,0.87307 l 0,6.11154 c 0,0.4802 0.3928,0.87309 0.8731,0.87309 l 6.1117,0 c 0.4801,0 0.8731,-0.39289 0.8731,-0.87309 l 0,-6.11154 c 0,-0.48018 -0.393,-0.87307 -0.8731,-0.87307 l 0,0 z",
              svg.fill := progressBarColoring(p).tail.head._2
            )
          } else {
            svg.path(
              ^.`class` := "timeline-plan-symbol",
              svg.d := "m 36.172996,86.655372 -1.8249,0 c -0.1835,-0.50637 -0.6635,-0.87306 -1.2311,-0.87306 -0.5674,0 -1.0477,0.36669 -1.2311,0.87306 l -1.8246,0 c -0.4803,0 -0.8731,0.39289 -0.8731,0.87307 l 0,6.11154 c 0,0.4802 0.3928,0.87309 0.8731,0.87309 l 6.1117,0 c 0.4801,0 0.8731,-0.39289 0.8731,-0.87309 l 0,-6.11154 c 0,-0.48018 -0.393,-0.87307 -0.8731,-0.87307 l 0,0 z m -3.056,0 c 0.2401,0 0.4366,0.19645 0.4366,0.43654 0,0.2401 -0.1965,0.43653 -0.4366,0.43653 -0.24,0 -0.4365,-0.19643 -0.4365,-0.43653 0,-0.24009 0.1965,-0.43654 0.4365,-0.43654 l 0,0 z m -0.8731,6.11153 -1.7461,-1.74615 0.6156,-0.6155 1.1305,1.12625 2.877,-2.87679 0.6155,0.61989 -3.4925,3.4923 0,0 z",
              svg.fill := progressBarColoring(p).tail.head._2
            )
          },
          svg.path(
            ^.`class` := "timeline-finished-bg",
            svg.d := "m 62.669093,100.12931 -21.873197,0 0,-0.0889 6.4792,-8.997048 -6.4787,-9.68038 0,-0.0954 21.872697,0 z",
            svg.fill := progressBarColoring(p).tail.tail.head._1
          ),
          svg.path(
            ^.`class` := "timeline-finished-symbol",
            svg.d := "m 52.551893,92.530302 -2.4802,-2.54684 -0.8266,0.84894 3.3068,3.39576 7.0862,-7.27664 -0.8268,-0.84894 -6.2594,6.42772 z",
            svg.fill := progressBarColoring(p).tail.tail.head._2
          ),
          svg.rect(
            ^.`class` := "STATUSFÄRG",
            svg.y := "28.914265",
            svg.x := "154.93719",
            svg.height := "11.111678",
            svg.width := "11.111678",
            svg.fill := { if (p.finished.finishedStillPresent) { // code from statuswidget
                "#ffedff"
              } else {
                if (p.plan.hasPlan) {
                  "#e9b7ff"
                } else if (p.attended.attended) {
                  "#8d47aa"
                } else {
                  "#1c0526"
                }
              }
            }

          ),
          svg.rect(
            ^.`class` := "PLATSFÄRG",
            svg.y := "46.844475",
            svg.x := "155.18974",
            svg.height := "11.111678",
            svg.width := "11.111678",
            svg.fill := { // code from placewidget: method getPlace
              if (p.examination.isOnExam) "#9df2e9"
              else if (p.location.roomNr == "ivr") "#1b998b"
              else if (p.location.roomNr != "") "#4d5256"
              else "#f5fffe"
            }
          )

        ),

        svg.g(
          ^.`class` := "patient-card-text",

          svg.text(
            ^.`class` := "room-nr",
            svg.y := "47.546173",
            svg.x := "22",
            svg.textAnchor := "middle",
            svg.fontSize :=  fontSizeLarge + "px",
            svg.fill := {if (p.priority.color == "NotTriaged") contentColorDark else contentColorLight},
            p.debugging.location
          ),
          svg.text(
            ^.`class` := "team-letter",
            svg.y := "10.549294",
            svg.x := "170.73709",
            svg.textAnchor := "end",
            svg.fontSize :=  fontSizeSmall + "px",
            svg.fill := contentColorDark,
            decodeTeam(p.team)
          ),
          svg.text(
            ^.`class` := "header-latest-event",
            svg.y := "52.782063",
            svg.x := "68.144127",
            svg.textAnchor := "start",
            svg.fontSize :=  fontSizeSmall/2 + "px",
            svg.fill := contentColorDark,
            if (p.latestEvent.latestEvent != "") "Senaste händelse"
            else "Ingen senaste händelse"
          ),
          svg.text(
            ^.`class` := "latest-event",
            svg.y := "67.524231",
            svg.x := "66.502892",
            svg.textAnchor := "start",
            svg.fontSize := "12px",
            Styles.freeSansBold,
            svg.fill := contentColorDark,
            p.latestEvent.latestEvent.toUpperCase
          ),
          svg.text(
            ^.`class` := "time-since-latest-event",
            svg.y := "77.683937",
            svg.x := "77.683937",
            svg.textAnchor := "start",
            svg.fontSize :=  "6px",
            svg.fill := { if (hasWaitedTooLong(p)) contentColorAttention else contentColorDark },
            svg.tspan(svg.x := "93")(getTimeDiffReadable(p.latestEvent.timeDiff)._1)
            //svg.tspan(svg.x := "93", svg.dy := "15 px")(getTimeDiffReadable(p.latestEvent.timeDiff)._2)
          ),
          svg.text(
            ^.`class` := "ccid",
            svg.y := "75.8",
            svg.x := "1.9",
            svg.textAnchor := "start",
            svg.fontSize :=  "12px",
            svg.fill := contentColorDark,
            p.careContactId
          ),
          svg.text(
            ^.`class` := "header-team",
            svg.y := "5.7542624",
            svg.x := "7.9752121",
            svg.textAnchor := "start",
            svg.fontSize :=  "4px",
            svg.fill := contentColorDark,
            "Team"
          ),
          svg.text(
            ^.`class` := "header-klinik",
            svg.y := "5.7542624",
            svg.x := "67.975212",
            svg.textAnchor := "start",
            svg.fontSize :=  "4px",
            svg.fill := contentColorDark,
            "Klinik"
          ),
          svg.text(
            ^.`class` := "header-reasonforvisit",
            svg.y := "6.0068011",
            svg.x := "115.65415",
            svg.textAnchor := "start",
            svg.fontSize :=  "4px",
            svg.fill := contentColorDark,
            "ReasonForVisit"
          ),
          svg.text(
            ^.`class` := "header-koordinatordiagramcategory",
            svg.y := "19.364769",
            svg.x := "68.062164",
            svg.textAnchor := "start",
            svg.fontSize :=  "4px",
            svg.fill := contentColorDark,
            "I koordinatordiagrammet"
          ),
          svg.text(
            ^.`class` := "header-triagediagramcategory",
            svg.y := "33.38578",
            svg.x := "67.924202",
            svg.textAnchor := "start",
            svg.fontSize :=  "4px",
            svg.fill := contentColorDark,
            "I triagediagrammet"
          ),
          svg.text(
            ^.`class` := "header-status",
            svg.y := "28.203604",
            svg.x := "154.476092",
            svg.textAnchor := "start",
            svg.fontSize :=  "4px",
            svg.fill := contentColorDark,
            "Status"
          ),
          svg.text(
            ^.`class` := "header-plats",
            svg.y := "45.150215",
            svg.x := "154.476092",
            svg.textAnchor := "start",
            svg.fontSize :=  "4px",
            svg.fill := contentColorDark,
            "Plats"
          ),
          svg.text(
            ^.`class` := "kliniktext",
            svg.y := "14.173706",
            svg.x := "68.062424",
            svg.textAnchor := "start",
            svg.fontSize :=  "12px",
            svg.fill := contentColorDark,
            p.debugging.clinic
          ),
          svg.text(
            ^.`class` := "teamtext",
            svg.y := "14.173706",
            svg.x := "5.0624237",
            svg.textAnchor := "start",
            svg.fontSize :=  "12px",
            svg.fill := contentColorDark,
            p.team.team
          ),
          svg.text(
            ^.`class` := "reasonForVisitext",
            svg.y := "14.629057",
            svg.x := "115.99483",
            svg.textAnchor := "start",
            svg.fontSize :=  "12px",
            svg.fill := contentColorDark,
            p.debugging.reasonForVisit
          ),
          svg.text(
            ^.`class` := "IKOORDINATORDIAGRAMMETTEXT",
            svg.y := "28.01358",
            svg.x := "67.315895",
            svg.textAnchor := "start",
            svg.fontSize :=  "12px",
            svg.fill := contentColorDark,
            decodeTeam(p.debugging.reasonForVisit, p.debugging.location, p.debugging.clinic)
          ),
          svg.text(
            ^.`class` := "ItriageDIAGRAMMETTEXT",
            svg.y := "42.034588",
            svg.x := "67.177933",
            svg.textAnchor := "start",
            svg.fontSize :=  "12px",
            svg.fill := contentColorDark,
            p.priority.color
          ),
          svg.text(
            ^.`class` := "arrival-time",
            svg.y := "93.13282",
            svg.x := "79",
            svg.textAnchor := "start",
            svg.fontSize :=  fontSizeSmall + "px",
            svg.fill := contentColorDark,
            p.arrivalTime.timeDiff // WAS:timestampToODT(p.arrivalTime.timestamp).format(DateTimeFormatter.ofPattern("H'.'m'"))
          ),
          svg.text(
            ^.`class` := "attendant-id",
            svg.y := "93.13282",
            svg.x := "133.8488",
            svg.textAnchor := "start",
            svg.fontSize :=  fontSizeSmall + "px",
            svg.fill := contentColorDark,
            decodeAttended(p.attended)._2
          )
        )
      )
    }

    /*
    Sorts a Map[String, Patient] by room number and returns a list of sorted ccids.
    Patients missing room number are sorted by careContactId.
    Sorting: (1,2,3,a,b,c, , , )
    **/
    def sortPatientsByRoomNr(pmap: Map[String, apiPatient.Patient]): List[String] = {
      val currentCcids = pmap.map(p => p._1)
      val ccidsSortedByRoomNr = ListBuffer[(String, String)]()
      val ccidsMissingRoomNr = ListBuffer[(String, String)]()
      val ccidsWithSpecialRoomNr = ListBuffer[(String, String)]()

      currentCcids.foreach{ ccid =>
        if (pmap(ccid).location.roomNr == "") ccidsMissingRoomNr += Tuple2(ccid, ccid)
        else if (pmap(ccid).location.roomNr.forall(_.isDigit)) ccidsSortedByRoomNr += Tuple2(ccid, pmap(ccid).location.roomNr)
        else ccidsWithSpecialRoomNr += Tuple2(ccid, pmap(ccid).location.roomNr)
      }
      (ccidsSortedByRoomNr.sortBy(_._2.toInt) ++ ccidsWithSpecialRoomNr.sortBy(_._2) ++ ccidsMissingRoomNr.sortBy(_._2)).map(p => p._1).toList
    }


    def render(filter: String, pmap: Map[String, apiPatient.Patient]) = {
      val pats = (pmap - "-1").filter(p => belongsToThisTeam(p._2, filter))

      <.div(^.`class` := "card-holder-root", Styles.helveticaZ, Styles.hideScrollBar)(
        svg.svg(
          svg.width := "0",
          svg.height := "0",
          svg.defs(
            svg.pattern(
              svg.id := "untriagedPattern",
              svg.width := "35.43",
              svg.height := "35.43",
              svg.patternUnits := "userSpaceOnUse",
              svg.patternTransform := "translate(0,0)",
              svg.path(
                svg.fill := "#000000",
                svg.d := "M 1.96875 0 L 0 1.96875 L 0 2.25 L 2.25 0 L 1.96875 0 z M 10.814453 0 L 0 10.816406 L 0 11.097656 L 11.097656 0 L 10.814453 0 z M 19.65625 0 L 0 19.65625 L 0 19.941406 L 19.939453 0 L 19.65625 0 z M 28.517578 0 L 0 28.517578 L 0 28.800781 L 28.800781 0 L 28.517578 0 z M 35.433594 1.9453125 L 1.9453125 35.433594 L 2.2285156 35.433594 L 35.433594 2.2285156 L 35.433594 1.9453125 z M 35.433594 10.841797 L 10.841797 35.433594 L 11.125 35.433594 L 35.433594 11.125 L 35.433594 10.841797 z M 35.433594 19.738281 L 19.738281 35.433594 L 20.019531 35.433594 L 35.433594 20.021484 L 35.433594 19.738281 z M 35.433594 28.603516 L 28.605469 35.433594 L 28.886719 35.433594 L 35.433594 28.886719 L 35.433594 28.603516 z "
              )
            )
          )
        ),
        sortPatientsByRoomNr(pats).map{ ccid =>
          patientCard(pats(ccid))
        }.toVdomArray
      )
    }

    def onUnmount() = {
      println("Unmounting")
      patientObs.foreach(_.kill())
      wsObs.kill()
      Callback.empty
    }
  }

  def extractTeam(attributes: Map[String, SPValue]) = {
    attributes.get("team").flatMap(x => x.asOpt[String]).getOrElse("medicin")
  }

  private val cardHolderComponent = ScalaComponent.builder[String]("cardHolderComponent")
  .initialState(Map("-1" ->
    EricaLogic.dummyPatient))
    .renderBackend[Backend]
    .componentDidMount(ctx => Callback(ctx.backend.setPatientObs()))
    .componentWillUnmount(_.backend.onUnmount())
    .build

    def apply() = spgui.SPWidget(spwb => {
      val currentTeam = extractTeam(spwb.frontEndState.attributes)
      cardHolderComponent(currentTeam)
    })
  }
