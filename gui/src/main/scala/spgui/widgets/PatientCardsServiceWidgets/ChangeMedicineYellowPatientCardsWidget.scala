package spgui.widgets

import java.time._
import java.time.OffsetDateTime

import spgui.circuit.SPGUICircuit
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
import Pickles._

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
import spgui.widgets.{API_Patient => apiPatient}

object ChangeMedicineYellowPatientCardsWidget {

  private class Backend($: BackendScope[String, Map[String, apiPatient.Patient]]) {
    spgui.widgets.css.WidgetStyles.addToDocument()

    val messObs = BackendCommunication.getMessageObserver(
      mess => {
        ToAndFrom.eventBody(mess).map {
          case api.State(patients) => $.modState{s => patients}.runNow()
          case _ => println("THIS WAS NOT EXPECTED IN MedicineYellowPatientCardsWidget.")
        }
      }, "patient-cards-widget-topic"
    )

    val wsObs = BackendCommunication.getWebSocketStatusObserver(  mess => {
      if (mess) send(api.GetState())
    }, "patient-cards-widget-topic")



    def send(mess: api.StateEvent) {
      val json = ToAndFrom.make(SPHeader(from = "MedicineYellowPatientCardsWidget", to = "PatientCardsService"), mess)
      BackendCommunication.publish(json, "patient-cards-service-topic")
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
        case "NotTriaged" => "#D5D5D5"
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
    def decodeAttended(a: apiPatient.Attended): (Boolean, String) = {
      if (a.attended)
      (true, a.doctorId)
      else
      (false, "Ej Påtittad")
    }

    /*
    Converts the Team.team-field of a given Team into a color value and letter to display
    **/
    def decodeTeam(t: apiPatient.Team): (String, String) = {
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


    /**
    Decodes the initial background and icon colors of the progress bar. Tuple values as follows:
    (initial background, initial symbol)
    */
    def progressBarInitialColoring(p: apiPatient.Priority): (String, String) = {
      p.color match {
        case "NotTriaged" => ("#E0E0E0", "#AFAFAF")
        case "Blue" => ("#DDE8FF", "#538AF4")
        case "Green" => ("#DCFAEC", "#009550")
        case "Yellow" => ("#FFED8D", "#EAC706")
        case "Orange" => ("#FCC381", "#F08100")
        case "Red" => ("#D99898", "#950000")
        case _ =>  {
          println("TriageColor: "+ p.color +" not expected in MedicineYellowPatientCardsWidget")
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
    import scala.collection.mutable.ListBuffer
    def progressBarColoring(p: apiPatient.Patient): List[(String, String, Boolean)] = {
      val coloring = ListBuffer[(String, String, Boolean)]()
      val initColoring = progressBarInitialColoring(p.priority)

      if (decodeAttended(p.attended)._1) coloring += Tuple3("#8D47AA", "#FFFFFF", true)
      else coloring += Tuple3(initColoring._1, initColoring._2, false)

      if (false) coloring += Tuple3("#E9B7FF", "#FFFFFF", true) // To be implemented: Plan exists
      else coloring += Tuple3(initColoring._1, initColoring._2, false)

      if (p.finishedStillPresent.finishedStillPresent) coloring += Tuple3("#FFEDFF", "#47AA62", true)
      else coloring += Tuple3(initColoring._1, initColoring._2, false)

      coloring.toList
    }

    /**
    * Converts milliseconds to hours and minutes, visualized in string.
    */
    def getTimeDiffReadable(milliseconds: Long): String = {
      val minutes = ((milliseconds / (1000*60)) % 60)
      val hours = ((milliseconds / (1000*60*60)) % 24)
      (hours, minutes) match {
        case (0,_) => {if (minutes == 0) "" else (minutes + " min").toString}
        case _ => (hours + " h " + minutes + " m").toString
      }

      // if (hours == 0) (minutes + " min").toString
      // else (hours + " h " + minutes + " m").toString
      // if (minutes == 0) " "
      // else " nej"
    }

    /*
    Specifies a patientCard in SVG for scalajs-react based on a Patient.
    **/
    def patientCard(p: apiPatient.Patient) = {
      val cardScaler = 1.2

      val fontSizeSmall = 7.7
      val fontSizeMedium = 15.2
      val fontSizeLarge = 35
      //println(triageColor(p.patientData("Priority")))
      //println(p.patientData("Priority"))
      <.svg.svg( //ARTO: Skapar en <svg></svg>-tagg att fylla med objekt
        ^.key := p.careContactId,
        ^.`class` := "patient-card-canvas",
        ^.svg.width := (cardScaler * 176 * 1.04).toString,
        ^.svg.height := (cardScaler * 100 * 1.04).toString,
        ^.svg.viewBox := "0 0 180 104",
        // ^.svg.transform := "scale(" + cardScaler + ")",
        ^.svg.id := p.careContactId,
        <.svg.g(
          ^.`class` := "patient-card-graphics",
          //^.svg.transform := "translate(0,0)",
          <.svg.rect(
            ^.`class` := "shadow",
            ^.svg.y := "2",
            ^.svg.x := "2",
            ^.svg.height := "100",
            ^.svg.width := "176",
            ^.svg.fill := "lightgrey"
          ),
          <.svg.rect(
            ^.`class` := "bg-field",
            ^.svg.y := 0,
            ^.svg.x := 0,
            ^.svg.height := 100,
            ^.svg.width := 176,
            ^.svg.fill := "#ffffff"
          ),
          <.svg.path(
            ^.`class` := "triage-field",
            ^.svg.d := "m 0.13909574,0.13190582 62.53049726,0 0,99.99740418 -62.53049726,0 z",
            ^.svg.fill := decodeTriageColor(p.priority)
          ),
          <.svg.path(
            ^.`class` := "delimiter",
            ^.svg.d := "m 67.626393,80.531612 0,1.07813 103.136807,0 0,-1.07813 -103.136807,0 z",
            ^.svg.fill := "#95989a"
          ),
          if (p.latestEvent.latestEvent != "") { // Only draw this if latestEvent exists.
            <.svg.path(
              ^.`class` := "timer-symbol",
              ^.svg.d := "m 80.372993,39.791592 -5.9892,0 0,1.99638 5.9892,0 0,-1.99638 z m -3.9929,12.97649 1.9964,0 0,-5.98915 -1.9964,0 0,5.98915 z m 8.0157,-6.59805 1.4173,-1.41743 c -0.4292,-0.50908 -0.8984,-0.98821 -1.4074,-1.40745 l -1.4175,1.41743 c -1.5472,-1.23775 -3.4937,-1.97642 -5.6099,-1.97642 -4.961,0 -8.9836,4.02272 -8.9836,8.98373 0,4.96101 4.0127,8.98372 8.9836,8.98372 4.9711,0 8.9838,-4.02271 8.9838,-8.98372 0,-2.11617 -0.7386,-4.06264 -1.9663,-5.59986 z m -7.0175,12.5872 c -3.863,0 -6.9873,-3.12434 -6.9873,-6.98734 0,-3.863 3.1243,-6.98734 6.9873,-6.98734 3.863,0 6.9874,3.12434 6.9874,6.98734 0,3.863 -3.1244,6.98734 -6.9874,6.98734 z",
              ^.svg.fill := "#000000"
            )
          } else {
            <.svg.path(
              ^.`class` := "no-timer-symbol"
            )
          },
          <.svg.path(
            ^.`class` := "clock-symbol",
            //  ^.svg.transform := "translate(0,1)",
            ^.svg.d := "m 72.223193,87.546362 -0.6325,0 0,2.53288 2.2147,1.32892 0.3181,-0.52057 -1.8987,-1.12633 z m -0.2155,-2.10927 a 4.218563,4.218563 0 1 0 4.2229,4.21856 4.2164074,4.2164074 0 0 0 -4.2229,-4.21856 z m 0,7.59373 a 3.3746349,3.3746349 0 1 1 3.3746,-3.37464 3.374096,3.374096 0 0 1 -3.3746,3.37464 z",
            ^.svg.fill := "#000000"
          ),
          <.svg.path(
            ^.`class` := "doctor-symbol",
            //^.svg.transform := "translate(0,2)",
            ^.svg.d := "m 115.9389,90.593352 c -1.1749,0 -3.519,0.58956 -3.519,1.75954 l 0,0.88002 7.0385,0 0,-0.88002 c 0,-1.16998 -2.3446,-1.75954 -3.5195,-1.75954 z m 0,-0.88004 a 1.759531,1.759531 0 1 0 -1.7596,-1.75951 1.7589921,1.7589921 0 0 0 1.7596,1.75951 z",
            ^.svg.fill := "#000000"
          ),
          <.svg.path(
            ^.`class` := "timeline-attended-bg",
            ^.svg.d := "m 17.109896,81.261292 6.5568,9.76783 -6.5535,9.096778 0,-15.203718 z m -16.97080026,0.007 16.97560026,0 0,18.861728 -16.97560026,0 z",
            ^.svg.fill := progressBarColoring(p).head._1
          ),
          <.svg.path(
            ^.`class` := "timeline-attended-symbol",
            ^.svg.d := "m 10.541796,86.403152 c -2.6944999,0 -4.9955003,1.67602 -5.9280003,4.0418 0.9325,2.36581 3.2335004,4.04181 5.9280003,4.04181 2.6946,0 4.9958,-1.676 5.9281,-4.04181 -0.9323,-2.36578 -3.2335,-4.0418 -5.9281,-4.0418 l 0,0 z m 0,6.73634 c -1.4872999,0 -2.6944999,-1.20716 -2.6944999,-2.69454 0,-1.48737 1.2072,-2.69452 2.6944999,-2.69452 1.4876,0 2.6946,1.20715 2.6946,2.69452 0,1.48738 -1.207,2.69454 -2.6946,2.69454 l 0,0 z m 0,-4.31126 c -0.8944999,0 -1.6166999,0.72212 -1.6166999,1.61672 0,0.89458 0.7222,1.61672 1.6166999,1.61672 0.8946,0 1.6169,-0.72214 1.6169,-1.61672 0,-0.8946 -0.7223,-1.61672 -1.6169,-1.61672 l 0,0 z",
            ^.svg.fill := progressBarColoring(p).head._2
          ),
          <.svg.path(
            ^.`class` := "timeline-plan-bg",
            ^.svg.d := "m 40.222496,100.12931 -22.57,0 0,-0.077 6.4593,-8.969548 -6.4593,-9.65344 0,-0.16168 22.57,0 6.5137,9.77577 -6.5137,9.045548 0,0 z",
            ^.svg.fill := progressBarColoring(p).tail.head._1
          ),
          if (!progressBarColoring(p).tail.head._3) { // Choose correct version of symbol
            <.svg.path(
              ^.`class` := "timeline-no-plan-symbol",
              ^.svg.d := "m 33.116996,86.655372 c 0.2401,0 0.4366,0.19645 0.4366,0.43654 0,0.2401 -0.1965,0.43653 -0.4366,0.43653 -0.24,0 -0.4365,-0.19643 -0.4365,-0.43653 0,-0.24009 0.1965,-0.43654 0.4365,-0.43654 l 0,0 z m 3.056,0 -1.8249,0 c -0.1835,-0.50637 -0.6635,-0.87306 -1.2311,-0.87306 -0.5674,0 -1.0477,0.36669 -1.2311,0.87306 l -1.8246,0 c -0.4803,0 -0.8731,0.39289 -0.8731,0.87307 l 0,6.11154 c 0,0.4802 0.3928,0.87309 0.8731,0.87309 l 6.1117,0 c 0.4801,0 0.8731,-0.39289 0.8731,-0.87309 l 0,-6.11154 c 0,-0.48018 -0.393,-0.87307 -0.8731,-0.87307 l 0,0 z",
              ^.svg.fill := progressBarColoring(p).tail.head._2
            )
          } else {
            <.svg.path(
              ^.`class` := "timeline-plan-symbol",
              ^.svg.d := "m 36.172996,86.655372 -1.8249,0 c -0.1835,-0.50637 -0.6635,-0.87306 -1.2311,-0.87306 -0.5674,0 -1.0477,0.36669 -1.2311,0.87306 l -1.8246,0 c -0.4803,0 -0.8731,0.39289 -0.8731,0.87307 l 0,6.11154 c 0,0.4802 0.3928,0.87309 0.8731,0.87309 l 6.1117,0 c 0.4801,0 0.8731,-0.39289 0.8731,-0.87309 l 0,-6.11154 c 0,-0.48018 -0.393,-0.87307 -0.8731,-0.87307 l 0,0 z m -3.056,0 c 0.2401,0 0.4366,0.19645 0.4366,0.43654 0,0.2401 -0.1965,0.43653 -0.4366,0.43653 -0.24,0 -0.4365,-0.19643 -0.4365,-0.43653 0,-0.24009 0.1965,-0.43654 0.4365,-0.43654 l 0,0 z m -0.8731,6.11153 -1.7461,-1.74615 0.6156,-0.6155 1.1305,1.12625 2.877,-2.87679 0.6155,0.61989 -3.4925,3.4923 0,0 z",
              ^.svg.fill := progressBarColoring(p).tail.head._2
            )
          },
          <.svg.path(
            ^.`class` := "timeline-finished-bg",
            ^.svg.d := "m 62.669093,100.12931 -21.873197,0 0,-0.0889 6.4792,-8.997048 -6.4787,-9.68038 0,-0.0954 21.872697,0 z",
            ^.svg.fill := progressBarColoring(p).tail.tail.head._1
          ),
          <.svg.path(
            ^.`class` := "timeline-finished-symbol",
            ^.svg.d := "m 52.551893,92.530302 -2.4802,-2.54684 -0.8266,0.84894 3.3068,3.39576 7.0862,-7.27664 -0.8268,-0.84894 -6.2594,6.42772 z",
            ^.svg.fill := progressBarColoring(p).tail.tail.head._2
          )
        ),

        <.svg.g(
          ^.`class` := "patient-card-text",

          <.svg.text(
            ^.`class` := "room-nr",
            ^.svg.y := "32.670914",
            ^.svg.x := "31.5",
            ^.svg.textAnchor := "middle",
            ^.svg.fontSize :=  fontSizeLarge + "px",
            ^.svg.fill := "#ffffff",
            p.location.roomNr
          ),
          <.svg.text(
            ^.`class` := "team-letter",
            ^.svg.y := "10.549294",
            ^.svg.x := "165.1389",
            ^.svg.textAnchor := "start",
            ^.svg.fontSize :=  fontSizeSmall + "px",
            ^.svg.fill := "#000000",
            decodeTeam(p.team)._2
          ),
          <.svg.text(
            ^.`class` := "header-latest-event",
            ^.svg.y := "10.852942",
            ^.svg.x := "65.937515",
            ^.svg.textAnchor := "start",
            ^.svg.fontSize :=  fontSizeSmall + "px",
            ^.svg.fill := "#000000",
            if (p.latestEvent.latestEvent != "") "Senaste händelse"
            else "Ingen senaste händelse"
          ),
          <.svg.text(
            ^.`class` := "latest-event",
            ^.svg.y := "32.553322",
            ^.svg.x := "65.937515",
            ^.svg.textAnchor := "start",
            ^.svg.fontSize := fontSizeMedium  + "px",
            ^.fontWeight.bold,
            ^.svg.fill := "#000000",
            p.latestEvent.latestEvent.toUpperCase
          ),
          <.svg.text(
            ^.`class` := "time-since-latest-event",
            ^.svg.y := "56.387848",
            ^.svg.x := "92.745262",
            ^.svg.textAnchor := "start",
            ^.svg.fontSize :=  (fontSizeMedium * 0.86) + "px",
            ^.svg.fill := "#000000",
            getTimeDiffReadable(p.latestEvent.timeDiff)
          ),
          <.svg.text(
            ^.`class` := "arrival-time",
            ^.svg.y := "93.13282",
            ^.svg.x := "79.148216",
            ^.svg.textAnchor := "start",
            ^.svg.fontSize :=  fontSizeSmall + "px",
            ^.svg.fill := "#000000",
            p.arrivalTime.timeDiff // WAS:timestampToODT(p.arrivalTime.timestamp).format(DateTimeFormatter.ofPattern("H'.'m'"))
          ),
          <.svg.text(
            ^.`class` := "attendant-id",
            ^.svg.y := "93.13282",
            ^.svg.x := "122.84879",
            ^.svg.textAnchor := "start",
            ^.svg.fontSize :=  fontSizeSmall + "px",
            ^.svg.fill := "#000000",
            decodeAttended(p.attended)._2
          )
        )
      )

    }


    def render(filter: String, pmap: Map[String, apiPatient.Patient]) = {
      spgui.widgets.css.WidgetStyles.addToDocument()

        val pats = (pmap - "-1").filter(p => belongsToThisTeam(p._2, filter))

        <.div(^.`class` := "card-holder-root", Styles.helveticaZ)( // This div is really not necessary
          pats map { case (id, p) =>
            patientCard(p)
          }
        )
    }

    def onUnmount() = {
      println("Unmounting")
      messObs.kill()
      wsObs.kill()
      Callback.empty
    }
  }

  def extractTeam(attributes: Map[String, SPValue]) = {
    attributes.get("team").map(x => x.str).getOrElse("medicin")
  }

  private val cardHolderComponent = ReactComponentB[String]("cardHolderComponent")
  .initialState(Map("-1" ->
    apiPatient.Patient(
      "4502085",
      apiPatient.Priority("NotTriaged", "2017-02-01T15:49:19Z"),
      apiPatient.Attended(true, "sarli29", "2017-02-01T15:58:33Z"),
      apiPatient.Location("52", "2017-02-01T15:58:33Z"),
      apiPatient.Team("GUL", "NAKME", "2017-02-01T15:58:33Z"),
      apiPatient.Examination(false, "2017-02-01T15:58:33Z"),
      apiPatient.LatestEvent("OmsKoord", -1, "2017-02-01T15:58:33Z"),
      apiPatient.ArrivalTime("", "2017-02-01T10:01:38Z"),
      apiPatient.FinishedStillPresent(false, "2017-02-01T10:01:38Z")
    )))
    .renderBackend[Backend]
    .componentWillUnmount(_.backend.onUnmount())
    .build

    def apply() = spgui.SPWidget(spwb => {
      val currentTeam = extractTeam(spwb.frontEndState.attributes)
      cardHolderComponent(currentTeam)
    })
  }
