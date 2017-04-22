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

object CoordinatorDiagramServiceWidget {

  sealed trait PatientProperty

  case class Priority(color: String, timestamp: String) extends PatientProperty
  case class Attended(attended: Boolean, doctorId: String, timestamp: String) extends PatientProperty
  case class FinishedStillPresent(finished: Boolean, timestamp: String) extends PatientProperty
  case class Location(roomNr: String, timestamp: String) extends PatientProperty
  case class Team(team: String, clinic: String, timestamp: String) extends PatientProperty
  case class Finished() extends PatientProperty

  case class Patient(
    var careContactId: String,
    var priority: Priority,
    var attended: Attended,
    var location: Location,
    var team: Team,
    var finishedStillPresent: FinishedStillPresent)

  private class Backend($: BackendScope[Unit, Map[String, Patient]]) {
    spgui.widgets.css.WidgetStyles.addToDocument()

    val messObs = BackendCommunication.getMessageObserver(
      mess => {
        mess.getBodyAs[api.PatientProperty] map {
          case api.NotTriaged(careContactId, timestamp) => $.modState{ s => updateState(s, careContactId, Priority("NotTriaged", timestamp)) }.runNow()
          case api.Blue(careContactId, timestamp) => $.modState{ s => updateState(s, careContactId, Priority("Blue", timestamp)) }.runNow()
          case api.Green(careContactId, timestamp) => $.modState{ s => updateState(s, careContactId, Priority("Green", timestamp)) }.runNow()
          case api.Yellow(careContactId, timestamp) => $.modState{ s => updateState(s, careContactId, Priority("Yellow", timestamp)) }.runNow()
          case api.Orange(careContactId, timestamp) => $.modState{ s => updateState(s, careContactId, Priority("Orange", timestamp)) }.runNow()
          case api.Red(careContactId, timestamp) => $.modState{ s => updateState(s, careContactId, Priority("Red", timestamp)) }.runNow()
          case api.Attended(careContactId, timestamp, attended, doctorId) => $.modState{ s => updateState(s, careContactId, Attended(attended, doctorId, timestamp)) }.runNow()
          case api.RoomNr(careContactId, timestamp, roomNr) => $.modState{ s => updateState(s, careContactId, Location(roomNr, timestamp)) }.runNow()
          case api.Team(careContactId, timestamp, team, clinic) => $.modState{ s => updateState(s, careContactId, Team(team, clinic, timestamp)) }.runNow()
          case api.FinishedStillPresent(careContactId, timestamp) => $.modState{ s => updateState(s, careContactId, FinishedStillPresent(true, timestamp)) }.runNow()
          case api.Finished(careContactId, timestamp) => $.modState{ s => updateState(s, careContactId, Finished()) }.runNow()
          case x => println(s"THIS WAS NOT EXPECTED IN CoordinatorDiagramServiceWidget: $x")
        }
      }, "coordinator-diagram-widget-topic"
    )

    /**
    * Updates the current state based on what patient property is received.
    */
    def updateState(s: Map[String, Patient], careContactId: String, prop: PatientProperty): Map[String, Patient] = {
      println(s)
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
        case Priority(color, timestamp) => Patient(ccid, Priority(color, timestamp), Attended(false, "", ""), Location("", ""), Team("", "", ""), FinishedStillPresent(false, ""))
        case Attended(attended, doctorId, timestamp) => Patient(ccid, Priority("", ""), Attended(attended, doctorId, timestamp), Location("", ""), Team("", "", ""), FinishedStillPresent(false, ""))
        case FinishedStillPresent(finished, timestamp) => Patient(ccid, Priority("", ""), Attended(false, "", ""), Location("", ""), Team("", "", ""), FinishedStillPresent(finished, timestamp))
        case Location(roomNr, timestamp) => Patient(ccid, Priority("", ""), Attended(false, "", ""), Location(roomNr, timestamp), Team("", "", ""), FinishedStillPresent(false, ""))
        case Team(team, clinic, timestamp) => Patient(ccid, Priority("", ""), Attended(false, "", ""), Location("", ""), Team(team, clinic, timestamp), FinishedStillPresent(false, ""))
        case _ => Patient(ccid, Priority("", ""), Attended(false, "", ""), Location("", ""), Team("", "", ""), FinishedStillPresent(false, ""))
      }
    }

    /**
    * Constructs an updates patient object.
    */
    def updateExistingPatient(s: Map[String, Patient], ccid: String, prop: PatientProperty): Patient = {
      prop match {
        case Priority(color, timestamp) => Patient(ccid, Priority(color, timestamp), s(ccid).attended, s(ccid).location, s(ccid).team, s(ccid).finishedStillPresent)
        case Attended(attended, doctorId, timestamp) => Patient(ccid, s(ccid).priority, Attended(attended, doctorId, timestamp), s(ccid).location, s(ccid).team, s(ccid).finishedStillPresent)
        case FinishedStillPresent(finished, timestamp) => Patient(ccid, s(ccid).priority, s(ccid).attended, s(ccid).location, s(ccid).team, FinishedStillPresent(finished, timestamp))
        case Location(roomNr, timestamp) => Patient(ccid, s(ccid).priority, s(ccid).attended, Location(roomNr, timestamp), s(ccid).team, s(ccid).finishedStillPresent)
        case Team(team, clinic, timestamp) => Patient(ccid, s(ccid).priority, s(ccid).attended, s(ccid).location, Team(team, clinic, timestamp), s(ccid).finishedStillPresent)
        case _ => Patient(ccid, Priority("", ""), Attended(false, "", ""), Location("", ""), Team("", "", ""), FinishedStillPresent(false, ""))
      }
    }

    def render(p: Map[String, Patient]) = {
      <.div(Styles.helveticaZ)
    }
  }
  
  case class ys(x: Double, h: Double, y0: Double)

  private val component = ReactComponentB[Unit]("teamVStatus")
  .initialState(Map("-1" ->
    Patient(
      "-1",
      Priority("", ""),
      Attended(false, "", ""),
      Location("", ""),
      Team("", "", ""),
      FinishedStillPresent(false, "")
    )))
  .renderBackend[Backend]
  .componentDidUpdate(dcb => Callback(addTheD3(ReactDOM.findDOMNode(dcb.component), dcb.currentState)))
  .build

  def dist(d: Double): Double = { // Bestämmer avstånd för antal patienter i widget.
    if( d > 99){1}
    else if( d > 9 ){10}
    else {17.5}
  }

  def removeZero(d: Double): String = {
    if(d.equals(0)){""}
    else{d.toString()}
  }

  def getTriageStatusList(m: Map[String, Patient]): List[List[Int]] = {
    // Count number of patients of each triage color and status
    var notTriagedCountMG = 0
    var blueCountMG = 0
    var greenCountMG = 0
    var yellowCountMG = 0
    var orangeCountMG = 0
    var redCountMG = 0
    var finishedCountMG = 0
    var attendedCountMG = 0
    var attendedWithPlanCountMG = 0
    var unAttendedCountMG = 0

    var notTriagedCountMB = 0
    var blueCountMB = 0
    var greenCountMB = 0
    var yellowCountMB = 0
    var orangeCountMB = 0
    var redCountMB = 0
    var finishedCountMB = 0
    var attendedCountMB = 0
    var attendedWithPlanCountMB = 0
    var unAttendedCountMB = 0

    var notTriagedCountK = 0
    var blueCountK = 0
    var greenCountK = 0
    var yellowCountK = 0
    var orangeCountK = 0
    var redCountK = 0
    var finishedCountK = 0
    var attendedCountK = 0
    var attendedWithPlanCountK = 0
    var unAttendedCountK = 0

    var notTriagedCountO = 0
    var blueCountO = 0
    var greenCountO = 0
    var yellowCountO = 0
    var orangeCountO = 0
    var redCountO = 0
    var finishedCountO = 0
    var attendedCountO = 0
    var attendedWithPlanCountO = 0
    var unAttendedCountO = 0

    var notTriagedCountS = 0
    var blueCountS = 0
    var greenCountS = 0
    var yellowCountS = 0
    var orangeCountS = 0
    var redCountS = 0
    var finishedCountS = 0
    var attendedCountS = 0
    var attendedWithPlanCountS = 0
    var unAttendedCountS = 0

    var notTriagedCountP = 0
    var blueCountP = 0
    var greenCountP = 0
    var yellowCountP = 0
    var orangeCountP = 0
    var redCountP = 0
    var finishedCountP = 0
    var attendedCountP = 0
    var attendedWithPlanCountP = 0
    var unAttendedCountP = 0

    var notTriagedCountJ = 0
    var blueCountJ = 0
    var greenCountJ = 0
    var yellowCountJ = 0
    var orangeCountJ = 0
    var redCountJ = 0
    var finishedCountJ = 0
    var attendedCountJ = 0
    var attendedWithPlanCountJ = 0
    var unAttendedCountJ = 0

    (m - "-1").foreach{ p =>
      p._2.priority.color match {
        case "NotTriaged" => {
          p._2.team.team match {
            case "medicin gul" | "medicin" => notTriagedCountMG += 1
            case "medicin blå" => notTriagedCountMB += 1
            case "process" => notTriagedCountP += 1
            case "stream" => notTriagedCountS += 1
            case "jour" => notTriagedCountJ += 1
            case "kirurgi" => notTriagedCountK += 1
            case "ortopedi" => notTriagedCountO += 1
          }
        }
        case "Blue" => {
          p._2.team.team match {
            case "medicin gul" | "medicin" => blueCountMG += 1
            case "medicin blå" => blueCountMB += 1
            case "process" => blueCountP += 1
            case "stream" => blueCountS += 1
            case "jour" => blueCountJ += 1
            case "kirurgi" => blueCountK += 1
            case "ortopedi" => blueCountO += 1
          }
        }
        case "Green" => {
          p._2.team.team match {
            case "medicin gul" | "medicin" => greenCountMG += 1
            case "medicin blå" => greenCountMB += 1
            case "process" => greenCountP += 1
            case "stream" => greenCountS += 1
            case "jour" => greenCountJ += 1
            case "kirurgi" => greenCountK += 1
            case "ortopedi" => greenCountO += 1
          }
        }
        case "Yellow" => {
          p._2.team.team match {
            case "medicin gul" | "medicin" => yellowCountMG += 1
            case "medicin blå" => yellowCountMB += 1
            case "process" => yellowCountP += 1
            case "stream" => yellowCountS += 1
            case "jour" => yellowCountJ += 1
            case "kirurgi" => yellowCountK += 1
            case "ortopedi" => yellowCountO += 1
          }
        }
        case "Orange" => {
          p._2.team.team match {
            case "medicin gul" | "medicin" => orangeCountMG += 1
            case "medicin blå" => orangeCountMB += 1
            case "process" => orangeCountP += 1
            case "stream" => orangeCountS += 1
            case "jour" => orangeCountJ += 1
            case "kirurgi" => orangeCountK += 1
            case "ortopedi" => orangeCountO += 1
          }
        }
        case "Red" => {
          p._2.team.team match {
            case "medicin gul" | "medicin" => redCountMG += 1
            case "medicin blå" => redCountMB += 1
            case "process" => redCountP += 1
            case "stream" => redCountS += 1
            case "jour" => redCountJ += 1
            case "kirurgi" => redCountK += 1
            case "ortopedi" => redCountO += 1
          }
        }
      }
      if (p._2.finishedStillPresent.finished) {
        p._2.team.team match {
          case "medicin gul" | "medicin" => finishedCountMG += 1
          case "medicin blå" => finishedCountMB += 1
          case "process" => finishedCountP += 1
          case "stream" => finishedCountS += 1
          case "jour" => finishedCountJ += 1
          case "kirurgi" => finishedCountK += 1
          case "ortopedi" => finishedCountO += 1
        }
      } else {
        if (p._2.attended.attended) {
          p._2.team.team match {
            case "medicin gul" | "medicin" => attendedCountMG += 1
            case "medicin blå" => attendedCountMB += 1
            case "process" => attendedCountP += 1
            case "stream" => attendedCountS += 1
            case "jour" => attendedCountJ += 1
            case "kirurgi" => attendedCountK += 1
            case "ortopedi" => attendedCountO += 1
          }
        } else {
          p._2.team.team match {
            case "medicin gul" | "medicin" => unAttendedCountMG += 1
            case "medicin blå" => unAttendedCountMB += 1
            case "process" => unAttendedCountP += 1
            case "stream" => unAttendedCountS += 1
            case "jour" => unAttendedCountJ += 1
            case "kirurgi" => unAttendedCountK += 1
            case "ortopedi" => unAttendedCountO += 1
          }
        }
      }
    }

    val sumNotTriaged = notTriagedCountMG + notTriagedCountMB + notTriagedCountK + notTriagedCountO + notTriagedCountS + notTriagedCountP + notTriagedCountJ

    val listy = List[List[Int]](List(sumNotTriaged.toInt),
      List(blueCountMG, greenCountMG, yellowCountMG, orangeCountMG, redCountMG, notTriagedCountMG), List(unAttendedCountMG, attendedCountMG, attendedWithPlanCountMG, finishedCountMG, notTriagedCountMG),
      List(blueCountMB, greenCountMB, yellowCountMB, orangeCountMB, redCountMB, notTriagedCountMB), List(unAttendedCountMB, attendedCountMB, attendedWithPlanCountMB, finishedCountMB, notTriagedCountMB),
      List(blueCountK, greenCountK, yellowCountK, orangeCountK, redCountK, notTriagedCountK), List(unAttendedCountK, attendedCountK, attendedWithPlanCountK, finishedCountK, notTriagedCountK),
      List(blueCountO, greenCountO, yellowCountO, orangeCountO, redCountO, notTriagedCountO), List(unAttendedCountO, attendedCountO, attendedWithPlanCountO, finishedCountO, notTriagedCountO),
      List(blueCountS, greenCountS, yellowCountS, orangeCountS, redCountS, notTriagedCountS), List(unAttendedCountS, attendedCountS, attendedWithPlanCountS, finishedCountS, notTriagedCountS),
      List(blueCountP, greenCountP, yellowCountP, orangeCountP, redCountP, notTriagedCountP), List(unAttendedCountP, attendedCountP, attendedWithPlanCountP, finishedCountP, notTriagedCountP),
      List(blueCountJ, greenCountJ, yellowCountJ, orangeCountJ, redCountJ, notTriagedCountJ), List(unAttendedCountJ, attendedCountJ, attendedWithPlanCountJ, finishedCountJ, notTriagedCountJ)
    )
    return listy
  }

  private def addTheD3(element: raw.Element, patients: Map[String, Patient]): Unit = {

   d3.select(element).selectAll("*").remove()

   spgui.widgets.css.WidgetStyles.addToDocument()

   val listy = getTriageStatusList(patients)
   println(listy)

   val totWidth = element.clientWidth.toDouble//701.0 utgår ifrån detta

   def getOffsetBarText(d: Int): Double = {if(d > 9){(28.0/701.0)*totWidth} else{(18.0/701.0)*totWidth}} // Dessa måste fixas manuellt!!!!

   def getTotPatients(lizt: List[List[Int]]): Int = {
     var totPatients = 0
     var i = 1

     while(i < lizt.length){
       totPatients += lizt(i).sum
       i = i + 2
     }
     return totPatients
   }

   def getLength(list: List[Int], h: Double): ListBuffer[Double] = {
     val lista = new ListBuffer[Double]()
     val summera = list.sum

     for(i <- list.indices){
       lista += ( list(i).toDouble / summera ) * h
     }
     return lista
   }

   def getHeights(length: ListBuffer[Double], x: Double, h: Double): ListBuffer[ys] = {
     val heights = ListBuffer[ys]()
     var heighty: Double = h

     for(i <- length.indices){
       heighty = heighty - length(i)
       heights += ys(x, length(i), heighty)
     }
     return heights
   }

   def getKvot(lists: List[List[Int]]): Int = {
     var retList = lists.head.sum

     for(i <- lists.indices){
       if(lists(i).sum > retList){
         retList = lists(i).sum
       }
     }
     return retList
   }

   val graphHeight: Double = (600.0/701.0)*totWidth  // was 390.0/701.0
   val textTopHeight: Double = (154.9/701.0)*totWidth
   val textBotHeight: Double = (190.0/701.0)*totWidth

   val barWidth: Double = (29.3/701.0)*totWidth
   val barSeparation = (29.9/701.0)*totWidth
   val barSecond: Double = barWidth + ((66.1 + 11.2)/701.0)*totWidth
   val horizontalBarDistance = 2*barWidth + barSeparation

   // Ändra färger här:

   // #1288FF, #289500
   val colorsTriage = List("#538af4","#009550", "#eac706", "#f08100", "#950000", "#950000")
   val colorsPatit = List("#4a4a4a", "#5c5a5a", "#888888", "#bebebe", "#bebebe")

   val fontSize = s"${(16.0/701.0)*totWidth}pt" // Om man ändrar till px blir texten mindre!!!
   val textWhite = "#ffffff"
   val textBlack = "#000000"

   val offset: Double = (40.0/701.0)*totWidth

   //val g1 = d3.select(element).append("svg").attr("width", totWidth).attr("height", totHeight)
   val topText = d3.select(element).append("svg").attr("x", 0).attr("y", 0).attr("width", totWidth).attr("height", textTopHeight)
   val svg = d3.select(element).append("svg").attr("x", 0).attr("y", textTopHeight).attr("width", totWidth).attr("height", graphHeight)
   val botText =  d3.select(element).append("svg").attr("x", 0).attr("y", textTopHeight + graphHeight).attr("width", totWidth).attr("height", textBotHeight)

   // Översikt
   val xBegin: Double = 0
   val yOversikt: Double = (66.0/701.0)*totWidth
   val yPatient: Double = (109.7/701.0)*totWidth
   val yTriagePatient: Double = ((154.9-0.5)/701.0)*totWidth
   val OversiktSize = s"${(37.0/701.0)*totWidth}pt"
   val PatTriSta = s"${(24.0/701.0)*totWidth}pt"

   topText.append("text")
     .attr("x", xBegin)
     .attr("y", yOversikt)
     .attr("font-size", OversiktSize)
     .style("font-weight", "bold")
     .attr("fill", textBlack)
     .text("ÖVERSIKT")

   // Antal patienter
   topText.append("text")
     .attr("x", xBegin)
     .attr("y", yPatient)
     .attr("font-size", PatTriSta)
     .style("font-weight", "bold")
     .attr("fill", textBlack)
     .text(getTotPatients(listy))

   topText.append("text")
     .attr("x", xBegin + (45.0/701.0)*totWidth)
     .attr("y", yPatient)
     .attr("font-size", PatTriSta)
     .attr("fill", textBlack)
     .text("PATIENTER TOTALT")

   topText.append("text")
     .attr("x", xBegin)
     .attr("y", yTriagePatient)
     .attr("font-size", PatTriSta)
     .style("font-weight", "bold")
     .attr("fill", textBlack)
     .text("TRIAGE/STATUS")

   val sel = svg.selectAll("g").data(js.Array(0,1,2,3,4,5)).enter()
   val sel2 = svg.selectAll("g").data(js.Array(0,1,2,3,4)).enter()

   val kvot: Double = getKvot(listy)
   val firstRect: Double = listy.head.sum.toDouble / kvot * graphHeight

   // Ej påbörjade
   val dispFirstText = (d: Int) => if(d <= 0){""} else{d.toString}

   svg.append("defs")
     .append("pattern")
       .attr("id", "stuffZ")
       .attr("width", "14")
       .attr("height", "14")
       .attr("patternUnits", "userSpaceOnUse")
       .attr("patternTransform", "rotate(45)")
     .append("rect")
       .attr("width", "2")
       .attr("height", "14")
       .attr("transform", "translate(0,0)")
       .attr("fill", "#afafaf")

   svg.append("rect")
     .attr("x", (66.1/701.0)*totWidth)
     .attr("y", graphHeight - firstRect)
     .attr("height", firstRect)
     .attr("width", barWidth)
     .attr("fill", textWhite)

   svg.append("g").attr("id", "shape")
     .append("rect")
       .attr("x", (66.1/701.0)*totWidth)
       .attr("y", graphHeight - firstRect)
       .attr("height", firstRect)
       .attr("width", barWidth)
       .attr("fill", "url(#stuffZ)")

   svg.append("text")
     .attr("x", (66.1/701.0)*totWidth + barWidth - getOffsetBarText(listy.head.sum))
     .attr("y", graphHeight - (5.0/701.0)*totWidth)
     .text(dispFirstText(listy.head.head))
     .attr("font-size", fontSize)
     .attr("fill", textBlack)
   // ----------------------------------

   // Sned text
   val snedText = s"${(12.0/701.0)*totWidth}pt"

   botText.append("text")
     .style("font-weight", "bold")
     .attr("font-size", snedText)
     .attr("fill", textBlack)
     .attr("transform", s"translate(${(10.0/701.0)*totWidth},${(115.0/701.0)*totWidth})rotate(-50)")
     .text("Ej påbörjad triage")

   botText.append("text")
     .style("font-weight", "bold")
     .attr("font-size", snedText)
     .attr("fill", textBlack)
     .style("font-weight", "bold")
     .attr("transform", s"translate(${(95.0/701.0)*totWidth},${(85.0/701.0)*totWidth})rotate(-50)")
     .text("Medicin - Gul")

   botText.append("text")
     .style("font-weight", "bold")
     .attr("font-size", snedText)
     .attr("fill", textBlack)
     .style("font-weight", "bold")
     .attr("transform", s"translate(${(180.0/701.0)*totWidth},${(85.0/701.0)*totWidth})rotate(-50)")
     .text("Medicin - Blå")

   botText.append("text")
     .style("font-weight", "bold")
     .attr("font-size", snedText)
     .attr("fill", textBlack)
     .style("font-weight", "bold")
     .attr("transform", s"translate(${(300.0/701.0)*totWidth},${(50.0/701.0)*totWidth})rotate(-50)")
     .text("Kirurgi")

   botText.append("text")
     .style("font-weight", "bold")
     .attr("font-size", snedText)
     .attr("fill", textBlack)
     .style("font-weight", "bold")
     .attr("transform", s"translate(${(380.0/701.0)*totWidth},${(60.0/701.0)*totWidth})rotate(-50)")
     .text("Ortopedi")

   botText.append("text")
     .style("font-weight", "bold")
     .attr("font-size", snedText)
     .attr("fill", textBlack)
     .style("font-weight", "bold")
     .attr("transform", s"translate(${(470.0/701.0)*totWidth},${(47.0/701.0)*totWidth})rotate(-50)")
     .text("Stream")

   botText.append("text")
     .style("font-weight", "bold")
     .attr("font-size", snedText)
     .attr("fill", textBlack)
     .style("font-weight", "bold")
     .attr("transform", s"translate(${(560.0/701.0)*totWidth},${(55.0/701.0)*totWidth})rotate(-50)")
     .text("Process")

   botText.append("text")
     .style("font-weight", "bold")
     .attr("font-size", snedText)
     .attr("fill", textBlack)
     .style("font-weight", "bold")
     .attr("transform", s"translate(${(665.0/701.0)*totWidth},${(35.0/701.0)*totWidth})rotate(-50)")
     .text("Jour")

   // Botten rektanglar -------------------------------
   val lilRectH = (15.4/701.0)*totWidth
   val lilRectW = (15.3/701.0)*totWidth
   val rectBegin = (80.0/701.0)*totWidth
   val rectUpY = (130.0/701.0)*totWidth
   val rectY = (165.0/701.0)*totWidth

   // Näst längst ner rektanglar
   botText.append("defs")
     .append("pattern")
       .attr("id", "stuffZ")
       .attr("width", "8")
       .attr("height", "8")
       .attr("patternUnits", "userSpaceOnUse")
       .attr("patternTransform", "rotate(45)")
     .append("rect")
       .attr("width", "2")
       .attr("height", "8")
       .attr("transform", "translate(0,0)")
       .attr("fill", "#afafaf")

   botText.append("rect")
     .attr("x", rectBegin)
     .attr("y", rectUpY)
     .attr("height", lilRectH)
     .attr("width", lilRectW)
     .attr("fill", textWhite)

   botText.append("g").attr("id", "shape")
     .append("rect")
     .attr("x", rectBegin)
     .attr("y", rectUpY)
     .attr("height", lilRectH)
     .attr("width", lilRectW)
     .attr("fill", "url(#stuffZ)")

   botText.append("rect")
     .attr("x", rectBegin + ((130.0 + lilRectW)/701.0)*totWidth)
     .attr("y", rectUpY)
     .attr("height", lilRectH)
     .attr("width", lilRectW)
     .attr("fill", colorsTriage.head)

   botText.append("rect")
     .attr("x", rectBegin + ((200.0 + lilRectW)/701.0)*totWidth)
     .attr("y", rectUpY)
     .attr("height", lilRectH)
     .attr("width", lilRectW)
     .attr("fill", colorsTriage(1))

   botText.append("rect")
     .attr("x", rectBegin + ((290.0 + lilRectW)/701.0)*totWidth)
     .attr("y", rectUpY)
     .attr("height", lilRectH)
     .attr("width", lilRectW)
     .attr("fill", colorsTriage(2))

   botText.append("rect")
     .attr("x", rectBegin + ((370.0 + lilRectW)/701.0)*totWidth)
     .attr("y", rectUpY)
     .attr("height", lilRectH)
     .attr("width", lilRectW)
     .attr("fill", colorsTriage(3))

   botText.append("rect")
     .attr("x", rectBegin + ((480.0 + lilRectW)/701.0)*totWidth)
     .attr("y", rectUpY)
     .attr("height", lilRectH)
     .attr("width", lilRectW)
     .attr("fill", colorsTriage(4))


   // Längst ner rektanglar

   botText.append("rect")
     .attr("x", rectBegin)
     .attr("y", rectY)
     .attr("height", lilRectH)
     .attr("width", lilRectW)
     .attr("fill", colorsPatit(3))

   botText.append("rect")
     .attr("x", rectBegin + (80.0/701.0)*totWidth)
     .attr("y", rectY)
     .attr("height", lilRectH)
     .attr("width", lilRectW)
     .attr("fill", colorsPatit(2))

   botText.append("rect")
     .attr("x", rectBegin + (270.0/701.0)*totWidth)
     .attr("y", rectY)
     .attr("height", lilRectH)
     .attr("width", lilRectW)
     .attr("fill", colorsPatit(1))

   botText.append("rect")
     .attr("x", rectBegin + (390.0/701.0)*totWidth)
     .attr("y", rectY)
     .attr("height", lilRectH)
     .attr("width", lilRectW)
     .attr("fill", colorsPatit.head)

   // -------------------------------------------------

   // Text botten -------------------------------------
   val textOffX = (20.0/701.0)*totWidth
   val botTextSize = s"${(15.0/701.0)*totWidth}pt"

   // Färg text
   botText.append("text")
     .attr("x", rectBegin + textOffX)
     .attr("y", rectUpY + lilRectH)
     .attr("font-size", botTextSize)
     .text("Ej triagerad")

   botText.append("text")
     .attr("x", rectBegin + ((130.0 + lilRectW)/701.0)*totWidth + textOffX)
     .attr("y", rectUpY + lilRectH)
     .attr("font-size", botTextSize)
     .text("Blå")

   botText.append("text")
     .attr("x", rectBegin + ((200.0 + lilRectW)/701.0)*totWidth + textOffX)
     .attr("y", rectUpY + lilRectH)
     .attr("font-size", botTextSize)
     .text("Grön")

   botText.append("text")
     .attr("x", rectBegin + ((290.0 + lilRectW)/701.0)*totWidth + textOffX)
     .attr("y", rectUpY + lilRectH)
     .attr("font-size", botTextSize)
     .text("Gul")

   botText.append("text")
     .attr("x", rectBegin + ((370.0 + lilRectW)/701.0)*totWidth + textOffX)
     .attr("y", rectUpY + lilRectH)
     .attr("font-size", botTextSize)
     .text("Orange")

   botText.append("text")
     .attr("x", rectBegin + ((480.0 + lilRectW)/701.0)*totWidth + textOffX)
     .attr("y", rectUpY + lilRectH)
     .attr("font-size", botTextSize)
     .text("Röd")


   // Påtittad text
   botText.append("text")
     .attr("x", rectBegin + textOffX)
     .attr("y", rectY + lilRectH)
     .attr("font-size", botTextSize)
     .text("Klar")

   botText.append("text")
     .attr("x", rectBegin +  (80.0/701.0)*totWidth + textOffX)
     .attr("y", rectY + lilRectH)
     .attr("font-size", botTextSize)
     .text("Påtittad m. Plan")

   botText.append("text")
     .attr("x", rectBegin + (270.0/701.0)*totWidth + textOffX)
     .attr("y", rectY + lilRectH)
     .attr("font-size", botTextSize)
     .text("Påtittad")

   botText.append("text")
     .attr("x", rectBegin + (390.0/701.0)*totWidth + textOffX)
     .attr("y", rectY + lilRectH)
     .attr("font-size", botTextSize)
     .text("Opåtittad")


   // Kanske sätta in intervall ???
   // Mellan 0 - 5 har du en viss pixel höjd
   // Mellan 5 - 10 har du en annan pixel höjd
   // Mellan 10 - 15 har du en annan pixel höjd
   // Mellan 15 - 20 har du en annan pixel höjd
   // Mellan 20 - 25 har du en annan pixel höjd
   // Sätta en pixel höjd för varannat steg? Om man har 600 som höjd samt 24 patient max, får man då ett steg pixel höjden: 50


   // Medicin - Gul
   draw(getHeights(getLength(listy(1), listy(1).sum * (graphHeight-offset) / kvot), barSecond, graphHeight), listy(1))
   draw2(getHeights(getLength(listy(2), listy(1).sum * (graphHeight-offset) / kvot), barWidth + barSecond, graphHeight), listy(2))

   // Medicin - Blå
   draw(getHeights(getLength(listy(3), listy(3).sum * (graphHeight-offset) / kvot), horizontalBarDistance + barSecond, graphHeight), listy(3))
   draw2(getHeights(getLength(listy(4), listy(3).sum * (graphHeight-offset) / kvot), horizontalBarDistance + barWidth + barSecond, graphHeight), listy(4))

   // Kirurgi
   draw(getHeights(getLength(listy(5), listy(5).sum * (graphHeight-offset) / kvot), 2*horizontalBarDistance + barSecond, graphHeight), listy(5))
   draw2(getHeights(getLength(listy(6), listy(5).sum * (graphHeight-offset) / kvot), 2*horizontalBarDistance + barWidth + barSecond, graphHeight), listy(6))

   // Ortopedi
   draw(getHeights(getLength(listy(7), listy(7).sum * (graphHeight-offset) / kvot), 3*horizontalBarDistance + barSecond, graphHeight), listy(7))
   draw2(getHeights(getLength(listy(8), listy(7).sum * (graphHeight-offset) / kvot), 3*horizontalBarDistance + barWidth + barSecond, graphHeight), listy(8))

   // Stream
   draw(getHeights(getLength(listy(9), listy(9).sum * (graphHeight-offset) / kvot), 4*horizontalBarDistance + barSecond, graphHeight), listy(9))
   draw2(getHeights(getLength(listy(10), listy(9).sum * (graphHeight-offset) / kvot), 4*horizontalBarDistance + barWidth + barSecond, graphHeight), listy(10))

   // Process
   draw(getHeights(getLength(listy(11), listy(11).sum * (graphHeight-offset) / kvot), 5*horizontalBarDistance + barSecond, graphHeight), listy(11))
   draw2(getHeights(getLength(listy(12), listy(11).sum * (graphHeight-offset) / kvot), 5*horizontalBarDistance + barWidth + barSecond, graphHeight), listy(12))

   // Jour
   draw(getHeights(getLength(listy(13), listy(13).sum * (graphHeight-offset) / kvot), 6*horizontalBarDistance + barSecond, graphHeight), listy(13))
   draw2(getHeights(getLength(listy(14), listy(13).sum * (graphHeight-offset) / kvot), 6*horizontalBarDistance + barWidth + barSecond, graphHeight), listy(14))

   def draw(listzz: ListBuffer[ys], listxx: List[Int]): Unit = {

     val rectXFun = (d: Int, i: Int) => listzz(i).x
     val rectXTextFun = (d: Int, i: Int) => listzz(i).x + barWidth - getOffsetBarText(listxx(i))// Offset 20
     val rectXTextTopFun = (d: Int, i: Int) => if(listxx.sum > 9){listzz.last.x + barWidth - (15.0/701.0)*totWidth} else {listzz.last.x + barWidth - (7.0/701.0)*totWidth}

     val rectYFun = (d: Int) => listzz(d).y0
     val rectYTextFun = (d: Int, i: Int) => listzz(i).y0 + listzz(i).h - (3.0/701.0)*totWidth // Offset från början av baren
     val rectYTopTextFun = (d: Int, i: Int) => graphHeight - listzz.last.h - listzz(4).h - listzz(3).h - listzz(2).h - listzz(1).h - listzz.head.h - (15.0/701.0)*totWidth // Offset från topbaren till texten

     val dispText = (d: Int, i: Int) => if(listxx(i) <= 0){""}else {listxx(i).toString}
     val dispTopText = (d: Int, i: Int) => if(listxx.sum <= 0){""}else {listxx.sum.toString}

     val rectHeightFun = (d: Int) => listzz(d).h
     val rectColorFun = (d: Int, i: Int) => colorsTriage(d)

     sel.append("rect")
       .attr("x", rectXFun)
       .attr("y", rectYFun)
       .attr("width", barWidth)
       .attr("height", rectHeightFun)
       .style("fill", rectColorFun)

     sel.append("text")
       .attr("x", rectXTextFun)
       .attr("y", rectYTextFun)
       .text(dispText)
       .attr("font-size", fontSize)
       .attr("fill", textWhite)

     sel.append("text")
       .attr("x", rectXTextTopFun)
       .attr("y", rectYTopTextFun)
       .text(dispTopText)
       .attr("font-size", fontSize)
       .attr("fill", textBlack)
   }

   def draw2(listzz: ListBuffer[ys], listxx: List[Int]): Unit = {

     val rectXFun = (d: Int, i: Int) => listzz(i).x
     val rectXTextFun = (d: Int, i: Int) => listzz(i).x + barWidth - getOffsetBarText(listxx(i))

     val rectYFun = (d: Int) => listzz(d).y0
     val rectYTextFun = (d: Int, i: Int) => listzz(i).y0 + listzz(i).h - (3.0/701.0)*totWidth // Offset från början av baren

     val dispText = (d: Int, i: Int) => if(listxx(i) <= 0){""} else {listxx(i).toString}
     val dispText2 = (d: Int, i: Int) => if(listxx.last <= 0){""} else {listxx.last.toString}

     def xDispText2(x: Int): Double = {if(x > 9){(15.0/701.0)*totWidth} else {(7.0/701.0)*totWidth}}

     val rectHeightFun = (d: Int) => listzz(d).h
     val rectColorFun = (d: Int, i: Int) => colorsPatit(d)

     sel2.append("rect")
       .attr("x", rectXFun)
       .attr("y", rectYFun)
       .attr("width", barWidth)
       .attr("height", rectHeightFun)
       .style("fill", rectColorFun)

     sel2.append("text")
       .attr("x", rectXTextFun)
       .attr("y", rectYTextFun)
       .text(dispText)
       .attr("font-size", fontSize)
       .attr("fill", textWhite)

     if(listxx.last > 0){

       sel2.append("rect")
         .attr("x", listzz.last.x - barWidth)
         .attr("y", listzz.last.y0)
         .attr("height", listzz.last.h)
         .attr("width", 2*barWidth)
         .attr("fill", textWhite)

       sel2.append("g").attr("id", "shape")
         .append("rect")
         .attr("x", listzz.last.x - barWidth)
         .attr("y",listzz.last.y0)
         .attr("height", listzz.last.h)
         .attr("width", 2*barWidth)
         .attr("fill", "url(#stuffZ)")

       sel2.append("text")
         .attr("x", listzz.last.x - xDispText2(listxx.last))
         .attr("y", listzz.last.y0 + listzz.last.h - (3.0/701.0)*totWidth)
         .text(dispText2)
         .attr("font-size", fontSize)
         .attr("fill", textBlack)
     }
   }
 }


  def apply() = spgui.SPWidget(spwb => {
    component()
  })
}
