package spgui.widgets

import java.time._ //ARTO: Använder wrappern https://github.com/scala-js/scala-js-java-time
import java.text.SimpleDateFormat
import java.util.UUID

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.ReactDOM

import spgui.SPWidget
import spgui.widgets.css.{WidgetStyles => Styles}
import spgui.communication._

import sp.domain._
import sp.domain.Logic._

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
import js.Dynamic.{ global => g }

import scalacss.ScalaCssReact._
import scalacss.DevDefaults._


import scala.collection.mutable.ListBuffer

import spgui.widgets.{API_PatientEvent => api}
import spgui.widgets.{API_Patient => apiPatient}

object CoordinatorDiagramWidget {


private class Backend($: BackendScope[Unit, Map[String, apiPatient.Patient]]) {

  val messObs = spgui.widgets.akuten.PatientModel.getPatientObserver(
    patients => {
      $.modState{s =>
        patients
      }.runNow()
    }
  )

  def onUnmount() = {
    messObs.kill()
    Callback.empty
  }

  def render(p: Map[String, apiPatient.Patient]) = {
    <.div(Styles.helveticaZ)
  }

}

case class ys(x: Double, h: Double, y0: Double)

private val component = ScalaComponent.builder[Unit]("teamVStatus")
.initialState(Map("-1" ->
  EricaLogic.dummyPatient))
.renderBackend[Backend]
.componentDidUpdate(ctx => Callback(addTheD3(ctx.getDOMNode, ctx.currentState)))
.componentWillUnmount(_.backend.onUnmount())
.build

def getTriageStatusList(m: Map[String, apiPatient.Patient]): (List[List[Int]], Int) = {
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

  var notTriagedCountNakm = 0
  var blueCountNakm = 0
  var greenCountNakm = 0
  var yellowCountNakm = 0
  var orangeCountNakm = 0
  var redCountNakm = 0
  var finishedCountNakm = 0
  var attendedCountNakm = 0
  var attendedWithPlanCountNakm = 0
  var unAttendedCountNakm = 0

  var notTriagedCountMedicin = 0
  var blueCountMedicin = 0
  var greenCountMedicin = 0
  var yellowCountMedicin = 0
  var orangeCountMedicin = 0
  var redCountMedicin = 0
  var finishedCountMedicin = 0
  var attendedCountMedicin = 0
  var attendedWithPlanCountMedicin = 0
  var unAttendedCountMedicin = 0

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

  // De som ej har något team.
  var erNotTriaged = 0
  var erBlue = 0
  var erGreen = 0
  var erYellow = 0
  var erOrange = 0
  var erRed = 0
  var erFinished = 0
  var erAttPlan = 0
  var erAtt = 0
  var erUnAtt = 0

  (m - "-1").foreach{ p =>
    p._2.priority.color match {
      case "NotTriaged" => {
        p._2.team.team match {
          case "medicin gul" => notTriagedCountMG += 1
          case "medicin blå" => notTriagedCountMB += 1
          case "process" => notTriagedCountP += 1
          case "stream" => notTriagedCountS += 1
          case "jour" => notTriagedCountJ += 1
          case "kirurgi" => notTriagedCountK += 1
          case "ortopedi" => notTriagedCountO += 1
          case "NAKM" => notTriagedCountNakm += 1
          case "medicin" => notTriagedCountMedicin += 1
          case _ => erNotTriaged += 1
            //erNotTriaged += printTmp("NotTriaged", erNotTriaged)
        }
      }
      case "Blue" => {
        p._2.team.team match {
          case "medicin gul" => blueCountMG += 1
          case "medicin blå" => blueCountMB += 1
          case "process" => blueCountP += 1
          case "stream" => blueCountS += 1
          case "jour" => blueCountJ += 1
          case "kirurgi" => blueCountK += 1
          case "ortopedi" => blueCountO += 1
          case "NAKM" => blueCountNakm += 1
          case "medicin" => blueCountMedicin += 1
          case _ => erBlue += 1
        }
      }
      case "Green" => {
        p._2.team.team match {
          case "medicin gul" => greenCountMG += 1
          case "medicin blå" => greenCountMB += 1
          case "process" => greenCountP += 1
          case "stream" => greenCountS += 1
          case "jour" => greenCountJ += 1
          case "kirurgi" => greenCountK += 1
          case "ortopedi" => greenCountO += 1
          case "NAKM" => greenCountNakm += 1
          case "medicin" => greenCountMedicin += 1
          case _ => erGreen += 1
        }
      }
      case "Yellow" => {
        p._2.team.team match {
          case "medicin gul" => yellowCountMG += 1
          case "medicin blå" => yellowCountMB += 1
          case "process" => yellowCountP += 1
          case "stream" => yellowCountS += 1
          case "jour" => yellowCountJ += 1
          case "kirurgi" => yellowCountK += 1
          case "ortopedi" => yellowCountO += 1
          case "NAKM" => yellowCountNakm += 1
          case "medicin" => yellowCountMedicin += 1
          case _ => erYellow += 1
        }
      }
      case "Orange" => {
        p._2.team.team match {
          case "medicin gul" => orangeCountMG += 1
          case "medicin blå" => orangeCountMB += 1
          case "process" => orangeCountP += 1
          case "stream" => orangeCountS += 1
          case "jour" => orangeCountJ += 1
          case "kirurgi" => orangeCountK += 1
          case "ortopedi" => orangeCountO += 1
          case "NAKM" => orangeCountNakm += 1
          case "medicin" => orangeCountMedicin += 1
          case _ => erOrange += 1
        }
      }
      case "Red" => {
        p._2.team.team match {
          case "medicin gul" => redCountMG += 1
          case "medicin blå" => redCountMB += 1
          case "process" => redCountP += 1
          case "stream" => redCountS += 1
          case "jour" => redCountJ += 1
          case "kirurgi" => redCountK += 1
          case "ortopedi" => redCountO += 1
          case "NAKM" => redCountNakm += 1
          case "medicin" => redCountMedicin += 1
          case _ => erRed += 1
        }
      }
    }
    if (p._2.finished.finishedStillPresent) {
      p._2.team.team match {
        case "medicin gul" => finishedCountMG += 1
        case "medicin blå" => finishedCountMB += 1
        case "process" => finishedCountP += 1
        case "stream" => finishedCountS += 1
        case "jour" => finishedCountJ += 1
        case "kirurgi" => finishedCountK += 1
        case "ortopedi" => finishedCountO += 1
        case "NAKM" => finishedCountNakm += 1
        case "medicin" => finishedCountMedicin += 1
        case _ => erFinished += 1
      }
    } else {
      if (p._2.plan.hasPlan) {
        p._2.team.team match {
          case "medicin gul" => attendedWithPlanCountMG += 1
          case "medicin blå" => attendedWithPlanCountMB += 1
          case "process" => attendedWithPlanCountP += 1
          case "stream" => attendedWithPlanCountS += 1
          case "jour" => attendedWithPlanCountJ += 1
          case "kirurgi" => attendedWithPlanCountK += 1
          case "ortopedi" => attendedWithPlanCountO += 1
          case "NAKM" => attendedWithPlanCountNakm += 1
          case "medicin" => attendedWithPlanCountMedicin += 1
          case _ => erAttPlan += 1
        }
      } else if (p._2.attended.attended) {
        p._2.team.team match {
          case "medicin gul" => attendedCountMG += 1
          case "medicin blå" => attendedCountMB += 1
          case "process" => attendedCountP += 1
          case "stream" => attendedCountS += 1
          case "jour" => attendedCountJ += 1
          case "kirurgi" => attendedCountK += 1
          case "ortopedi" => attendedCountO += 1
          case "NAKM" => attendedCountNakm += 1
          case "medicin" => attendedCountMedicin += 1
          case _ => erAtt += 1
        }
      } else {
        p._2.team.team match {
          case "medicin gul" => unAttendedCountMG += 1
          case "medicin blå" => unAttendedCountMB += 1
          case "process" => unAttendedCountP += 1
          case "stream" => unAttendedCountS += 1
          case "jour" => unAttendedCountJ += 1
          case "kirurgi" => unAttendedCountK += 1
          case "ortopedi" => unAttendedCountO += 1
          case "NAKM" => unAttendedCountNakm += 1
          case "medicin" => unAttendedCountMedicin += 1
          case _ => erUnAtt += 1
        }
      }
    }
  }

  val sumNotTriaged: Int = notTriagedCountMG + notTriagedCountMB + notTriagedCountK + notTriagedCountO + notTriagedCountS + notTriagedCountP + notTriagedCountJ + notTriagedCountMedicin + notTriagedCountNakm

  // Om man kör tmp += 1 hela tiden får man dubbletter, då en kanske båda har Orange och Klar -> Ökning med 2 i tmp.
  // Valde att splitta upp dem på Triage och Status.
  val noTeamTriage = erNotTriaged + erBlue + erGreen + erYellow + erOrange + erRed
  val noTeamStatus = erFinished + erAttPlan + erAtt + erUnAtt

  // Bara för att kolla att båda dessa blir samma, vilket de blir.
  //g.console.log("Triage Error " + noTeamTriage)
  //g.console.log("Status Error " + noTeamStatus)

  val listy: List[List[Int]] = List(List(sumNotTriaged),
    List(blueCountMG, greenCountMG, yellowCountMG, orangeCountMG, redCountMG, notTriagedCountMG), List(finishedCountMG, attendedWithPlanCountMG, attendedCountMG, unAttendedCountMG),
    List(blueCountMB, greenCountMB, yellowCountMB, orangeCountMB, redCountMB, notTriagedCountMB), List(finishedCountMB, attendedWithPlanCountMB, attendedCountMB, unAttendedCountMB),
    List(blueCountMedicin, greenCountMedicin, yellowCountMedicin, orangeCountMedicin, redCountMedicin, notTriagedCountMedicin), List(finishedCountMedicin, attendedWithPlanCountMedicin, attendedCountMedicin, unAttendedCountMedicin),
    List(blueCountNakm, greenCountNakm, yellowCountNakm, orangeCountNakm, redCountNakm, notTriagedCountNakm), List(finishedCountNakm, attendedWithPlanCountNakm, attendedCountNakm, unAttendedCountNakm),
    List(blueCountK, greenCountK, yellowCountK, orangeCountK, redCountK, notTriagedCountK), List(finishedCountK, attendedWithPlanCountK, attendedCountK, unAttendedCountK),
    List(blueCountO, greenCountO, yellowCountO, orangeCountO, redCountO, notTriagedCountO), List(finishedCountO, attendedWithPlanCountO, attendedCountO, unAttendedCountO),
    List(blueCountS, greenCountS, yellowCountS, orangeCountS, redCountS, notTriagedCountS), List(finishedCountS, attendedWithPlanCountS, attendedCountS, unAttendedCountS),
    List(blueCountP, greenCountP, yellowCountP, orangeCountP, redCountP, notTriagedCountP), List(finishedCountP, attendedWithPlanCountP, attendedCountP, unAttendedCountP),
    List(blueCountJ, greenCountJ, yellowCountJ, orangeCountJ, redCountJ, notTriagedCountJ), List(finishedCountJ, attendedWithPlanCountJ, attendedCountJ, unAttendedCountJ)
  )
  return (listy, noTeamTriage)
}

private def addTheD3(element: raw.Element, patients: Map[String, apiPatient.Patient]): Unit = {

  d3.select(element).selectAll("*").remove()

  val listyTuple = getTriageStatusList(patients)
  val listy = listyTuple._1
  val noTeamTot = listyTuple._2

  val totWidth = element.clientWidth.toDouble//scaleWidth utgår ifrån detta
  val scaleWidth = 885.0

  def getOffsetBarText(d: Int): Double = {if(d > 9){(28.0/scaleWidth)*totWidth} else{(18.0/scaleWidth)*totWidth}} // Dessa måste fixas manuellt!!!!

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
      if (summera == 0) {
        lista += 0
      } else {
        lista += ( list(i).toDouble / summera ) * h
      }
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

  val graphHeight: Double = (600.0/scaleWidth)*totWidth  // was 390.0/scaleWidth
  val textTopHeight: Double = (154.9/scaleWidth)*totWidth
  val textBotHeight: Double = (190.0/scaleWidth)*totWidth

  val barWidth: Double = (29.3/scaleWidth)*totWidth
  val barSeparation = (29.9/scaleWidth)*totWidth
  val barSecond: Double = barWidth + ((66.1 + 11.2)/scaleWidth)*totWidth
  val horizontalBarDistance = 2*barWidth + barSeparation

  val colorsTriage = List("#538AF4","#009550", "#EAC706", "#F08100", "#950000", "#950000")
  val colorsPatit = List("#FFFDFF", "#E9B7FF", "#8D47AA", "#1C0526")

  val fontSize = s"${(18.0/scaleWidth)*totWidth}pt" // Om man ändrar till px blir texten mindre!!!
  val textWhite = "#FFFFFF"
  val textBlack = "#000000"

  val offset: Double = (40.0/scaleWidth)*totWidth

  //val g1 = d3.select(element).append("svg").attr("width", totWidth).attr("height", totHeight)
  val topText = d3.select(element).append("svg").attr("x", 0).attr("y", 0).attr("width", totWidth).attr("height", textTopHeight)
  val svg = d3.select(element).append("svg").attr("x", 0).attr("y", textTopHeight).attr("width", totWidth).attr("height", graphHeight)
  val botText =  d3.select(element).append("svg").attr("x", 0).attr("y", textTopHeight + graphHeight).attr("width", totWidth).attr("height", textBotHeight)

  // Översikt
  val xBegin: Double = 0
  val yOversikt: Double = (50.0/scaleWidth)*totWidth
  val yPatient: Double = (100.0/scaleWidth)*totWidth
  val yTriagePatient: Double = (145.0/scaleWidth)*totWidth
  val OversiktSize = s"${(37.0/scaleWidth)*totWidth}pt"
  val PatTriSta = s"${(24.0/scaleWidth)*totWidth}pt"

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
    .attr("x", xBegin + (45.0/scaleWidth)*totWidth)
    .attr("y", yPatient)
    .attr("font-size", PatTriSta)
    .attr("fill", textBlack)
    .text("PATIENTER PÅ MOTTAGNINGEN")

  topText.append("text")
    .attr("x", xBegin)
    .attr("y", yTriagePatient)
    .attr("font-size", PatTriSta)
    .style("font-weight", "bold")
    .attr("fill", textBlack)
    .text("TRIAGE/STATUS")

  val sel = svg.selectAll("g").data(js.Array(0,1,2,3,4,5)).enter()
  val sel2 = svg.selectAll("g").data(js.Array(0,1,2,3)).enter()

  val kvot: Double = getKvot(listy)
  val firstRect: Double = if(kvot.equals(0)){0}else{listy.head.sum.toDouble / kvot * graphHeight - offset}

  //listy.head.sum.toDouble / kvot * graphHeight - offset

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
    .attr("x", (66.1/scaleWidth)*totWidth)
    .attr("y", graphHeight - firstRect)
    .attr("height", firstRect)
    .attr("width", barWidth)
    .attr("fill", textWhite)

  svg.append("g").attr("id", "shape")
    .append("rect")
    .attr("x", (66.1/scaleWidth)*totWidth)
    .attr("y", graphHeight - firstRect)
    .attr("height", firstRect)
    .attr("width", barWidth)
    .attr("fill", "#afafaf")

  svg.append("text")
    .attr("x", (66.1/scaleWidth)*totWidth + barWidth - getOffsetBarText(listy.head.sum))
    .attr("y", graphHeight - (5.0/scaleWidth)*totWidth)
    .text(dispFirstText(listy.head.head))
    .attr("font-size", fontSize)
    .attr("fill", textBlack)
  // ----------------------------------

  // Sned text
  val snedText = s"${(12.0/scaleWidth)*totWidth}pt"

  botText.append("text")
    .style("font-weight", "bold")
    .attr("font-size", snedText)
    .attr("fill", textBlack)
    .attr("transform", s"translate(${(10.0/scaleWidth)*totWidth},${(110.0/scaleWidth)*totWidth})rotate(-50)")
    .text("Ej påbörjad triage")

  botText.append("text")
    .style("font-weight", "bold")
    .attr("font-size", snedText)
    .attr("fill", textBlack)
    .style("font-weight", "bold")
    .attr("transform", s"translate(${(95.0/scaleWidth)*totWidth},${(85.0/scaleWidth)*totWidth})rotate(-50)")
    .text("Medicin - Gul")

  botText.append("text")
    .style("font-weight", "bold")
    .attr("font-size", snedText)
    .attr("fill", textBlack)
    .style("font-weight", "bold")
    .attr("transform", s"translate(${(180.0/scaleWidth)*totWidth},${(85.0/scaleWidth)*totWidth})rotate(-50)")
    .text("Medicin - Blå")

  botText.append("text")
    .style("font-weight", "bold")
    .attr("font-size", snedText)
    .attr("fill", textBlack)
    .style("font-weight", "bold")
    .attr("transform", s"translate(${(250.0/scaleWidth)*totWidth},${(105.0/scaleWidth)*totWidth})rotate(-50)")
    .text("Medicin - Övriga")

  botText.append("text")
    .style("font-weight", "bold")
    .attr("font-size", snedText)
    .attr("fill", textBlack)
    .style("font-weight", "bold")
    .attr("transform", s"translate(${(390.0/scaleWidth)*totWidth},${(47.0/scaleWidth)*totWidth})rotate(-50)")
    .text("NAKM")

  botText.append("text")
    .style("font-weight", "bold")
    .attr("font-size", snedText)
    .attr("fill", textBlack)
    .style("font-weight", "bold")
    .attr("transform", s"translate(${(475.0/scaleWidth)*totWidth},${(50.0/scaleWidth)*totWidth})rotate(-50)")
    .text("Kirurgi")

  botText.append("text")
    .style("font-weight", "bold")
    .attr("font-size", snedText)
    .attr("fill", textBlack)
    .style("font-weight", "bold")
    .attr("transform", s"translate(${(555.0/scaleWidth)*totWidth},${(60.0/scaleWidth)*totWidth})rotate(-50)")
    .text("Ortopedi")

  botText.append("text")
    .style("font-weight", "bold")
    .attr("font-size", snedText)
    .attr("fill", textBlack)
    .style("font-weight", "bold")
    .attr("transform", s"translate(${(650.0/scaleWidth)*totWidth},${(47.0/scaleWidth)*totWidth})rotate(-50)")
    .text("Stream")

  botText.append("text")
    .style("font-weight", "bold")
    .attr("font-size", snedText)
    .attr("fill", textBlack)
    .style("font-weight", "bold")
    .attr("transform", s"translate(${(740.0/scaleWidth)*totWidth},${(55.0/scaleWidth)*totWidth})rotate(-50)")
    .text("Process")

  botText.append("text")
    .style("font-weight", "bold")
    .attr("font-size", snedText)
    .attr("fill", textBlack)
    .style("font-weight", "bold")
    .attr("transform", s"translate(${(845.0/scaleWidth)*totWidth},${(35.0/scaleWidth)*totWidth})rotate(-50)")
    .text("Jour")

  // Botten rektanglar -------------------------------
  val lilRectH = (15.4/scaleWidth)*totWidth
  val lilRectW = (15.3/scaleWidth)*totWidth
  val rectBegin = (80.0/scaleWidth)*totWidth
  val rectUpY = (130.0/scaleWidth)*totWidth
  val rectY = (165.0/scaleWidth)*totWidth

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
    .attr("fill", "#afafaf")

  botText.append("rect")
    .attr("x", rectBegin + ((130.0 + lilRectW)/scaleWidth)*totWidth)
    .attr("y", rectUpY)
    .attr("height", lilRectH)
    .attr("width", lilRectW)
    .attr("fill", colorsTriage.head)

  botText.append("rect")
    .attr("x", rectBegin + ((200.0 + lilRectW)/scaleWidth)*totWidth)
    .attr("y", rectUpY)
    .attr("height", lilRectH)
    .attr("width", lilRectW)
    .attr("fill", colorsTriage(1))

  botText.append("rect")
    .attr("x", rectBegin + ((290.0 + lilRectW)/scaleWidth)*totWidth)
    .attr("y", rectUpY)
    .attr("height", lilRectH)
    .attr("width", lilRectW)
    .attr("fill", colorsTriage(2))

  botText.append("rect")
    .attr("x", rectBegin + ((370.0 + lilRectW)/scaleWidth)*totWidth)
    .attr("y", rectUpY)
    .attr("height", lilRectH)
    .attr("width", lilRectW)
    .attr("fill", colorsTriage(3))

  botText.append("rect")
    .attr("x", rectBegin + ((480.0 + lilRectW)/scaleWidth)*totWidth)
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
    .attr("fill", colorsPatit.head)

  botText.append("rect")
    .attr("x", rectBegin + (80.0/scaleWidth)*totWidth)
    .attr("y", rectY)
    .attr("height", lilRectH)
    .attr("width", lilRectW)
    .attr("fill", colorsPatit(1))

  botText.append("rect")
    .attr("x", rectBegin + (270.0/scaleWidth)*totWidth)
    .attr("y", rectY)
    .attr("height", lilRectH)
    .attr("width", lilRectW)
    .attr("fill", colorsPatit(2))

  botText.append("rect")
    .attr("x", rectBegin + (390.0/scaleWidth)*totWidth)
    .attr("y", rectY)
    .attr("height", lilRectH)
    .attr("width", lilRectW)
    .attr("fill", colorsPatit.last)

  // -------------------------------------------------

  // Text botten -------------------------------------
  val textOffX = (20.0/scaleWidth)*totWidth
  val botTextSize = s"${(15.0/scaleWidth)*totWidth}pt"

  // Färg text
  botText.append("text")
    .attr("x", rectBegin + textOffX)
    .attr("y", rectUpY + lilRectH)
    .attr("font-size", botTextSize)
    .text("Ej triagerad")

  botText.append("text")
    .attr("x", rectBegin + ((130.0 + lilRectW)/scaleWidth)*totWidth + textOffX)
    .attr("y", rectUpY + lilRectH)
    .attr("font-size", botTextSize)
    .text("Blå")

  botText.append("text")
    .attr("x", rectBegin + ((200.0 + lilRectW)/scaleWidth)*totWidth + textOffX)
    .attr("y", rectUpY + lilRectH)
    .attr("font-size", botTextSize)
    .text("Grön")

  botText.append("text")
    .attr("x", rectBegin + ((290.0 + lilRectW)/scaleWidth)*totWidth + textOffX)
    .attr("y", rectUpY + lilRectH)
    .attr("font-size", botTextSize)
    .text("Gul")

  botText.append("text")
    .attr("x", rectBegin + ((370.0 + lilRectW)/scaleWidth)*totWidth + textOffX)
    .attr("y", rectUpY + lilRectH)
    .attr("font-size", botTextSize)
    .text("Orange")

  botText.append("text")
    .attr("x", rectBegin + ((480.0 + lilRectW)/scaleWidth)*totWidth + textOffX)
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
    .attr("x", rectBegin +  (80.0/scaleWidth)*totWidth + textOffX)
    .attr("y", rectY + lilRectH)
    .attr("font-size", botTextSize)
    .text("Påtittad m. Plan")

  botText.append("text")
    .attr("x", rectBegin + (270.0/scaleWidth)*totWidth + textOffX)
    .attr("y", rectY + lilRectH)
    .attr("font-size", botTextSize)
    .text("Påtittad")

  botText.append("text")
    .attr("x", rectBegin + (390.0/scaleWidth)*totWidth + textOffX)
    .attr("y", rectY + lilRectH)
    .attr("font-size", botTextSize)
    .text("Opåtittad")


  if (kvot.equals(0)) {
    // Medicin - Gul
    draw(getHeights(getLength(listy(1), listy(1).sum * 0), barSecond, graphHeight), listy(1))
    draw2(getHeights(getLength(listy(2), listy(1).sum * 0), barWidth + barSecond, graphHeight), listy(2))

    // Medicin - Blå
    draw(getHeights(getLength(listy(3), listy(3).sum * 0), horizontalBarDistance + barSecond, graphHeight), listy(3))
    draw2(getHeights(getLength(listy(4), listy(3).sum * 0), horizontalBarDistance + barWidth + barSecond, graphHeight), listy(4))

    // Medicin övriga

    draw(getHeights(getLength(listy(5), listy(5).sum * 0), 2*horizontalBarDistance + barSecond, graphHeight), listy(5))
    draw2(getHeights(getLength(listy(6), listy(5).sum * 0), 2*horizontalBarDistance + barWidth + barSecond, graphHeight), listy(6))

    // Nakm
    draw(getHeights(getLength(listy(7), listy(7).sum * 0), 3*horizontalBarDistance + barSecond, graphHeight), listy(7))
    draw2(getHeights(getLength(listy(8), listy(7).sum * 0), 3*horizontalBarDistance + barWidth + barSecond, graphHeight), listy(8))

    // Kirurgi
    draw(getHeights(getLength(listy(9), listy(9).sum * 0), 4*horizontalBarDistance + barSecond, graphHeight), listy(9))
    draw2(getHeights(getLength(listy(10), listy(9).sum * 0), 4*horizontalBarDistance + barWidth + barSecond, graphHeight), listy(10))

    // Ortopedi
    draw(getHeights(getLength(listy(11), listy(11).sum * 0), 5*horizontalBarDistance + barSecond, graphHeight), listy(11))
    draw2(getHeights(getLength(listy(12), listy(11).sum * 0), 5*horizontalBarDistance + barWidth + barSecond, graphHeight), listy(12))

    // Stream
    draw(getHeights(getLength(listy(13), listy(13).sum * 0), 6*horizontalBarDistance + barSecond, graphHeight), listy(13))
    draw2(getHeights(getLength(listy(14), listy(13).sum * 0), 6*horizontalBarDistance + barWidth + barSecond, graphHeight), listy(14))

    // Process
    draw(getHeights(getLength(listy(15), listy(15).sum * 0), 7*horizontalBarDistance + barSecond, graphHeight), listy(15))
    draw2(getHeights(getLength(listy(16), listy(15).sum * 0), 7*horizontalBarDistance + barWidth + barSecond, graphHeight), listy(16))

    //Jour
    draw(getHeights(getLength(listy(17), listy(17).sum * 0), 8*horizontalBarDistance + barSecond, graphHeight), listy(17))
    draw2(getHeights(getLength(listy(18), listy(17).sum * 0), 8*horizontalBarDistance + barWidth + barSecond, graphHeight), listy(18))
  } else {
    // Medicin - Gul
    draw(getHeights(getLength(listy(1), listy(1).sum * (graphHeight-offset) / kvot), barSecond, graphHeight), listy(1))
    draw2(getHeights(getLength(listy(2), listy(1).sum * (graphHeight-offset) / kvot), barWidth + barSecond, graphHeight), listy(2))

    // Medicin - Blå
    draw(getHeights(getLength(listy(3), listy(3).sum * (graphHeight-offset) / kvot), horizontalBarDistance + barSecond, graphHeight), listy(3))
    draw2(getHeights(getLength(listy(4), listy(3).sum * (graphHeight-offset) / kvot), horizontalBarDistance + barWidth + barSecond, graphHeight), listy(4))

    // Medicin övriga

    draw(getHeights(getLength(listy(5), listy(5).sum * (graphHeight-offset) / kvot), 2*horizontalBarDistance + barSecond, graphHeight), listy(5))
    draw2(getHeights(getLength(listy(6), listy(5).sum * (graphHeight-offset) / kvot), 2*horizontalBarDistance + barWidth + barSecond, graphHeight), listy(6))

    // Nakm
    draw(getHeights(getLength(listy(7), listy(7).sum * (graphHeight-offset) / kvot), 3*horizontalBarDistance + barSecond, graphHeight), listy(7))
    draw2(getHeights(getLength(listy(8), listy(7).sum * (graphHeight-offset) / kvot), 3*horizontalBarDistance + barWidth + barSecond, graphHeight), listy(8))

    // Kirurgi
    draw(getHeights(getLength(listy(9), listy(9).sum * (graphHeight-offset) / kvot), 4*horizontalBarDistance + barSecond, graphHeight), listy(9))
    draw2(getHeights(getLength(listy(10), listy(9).sum * (graphHeight-offset) / kvot), 4*horizontalBarDistance + barWidth + barSecond, graphHeight), listy(10))

    // Ortopedi
    draw(getHeights(getLength(listy(11), listy(11).sum * (graphHeight-offset) / kvot), 5*horizontalBarDistance + barSecond, graphHeight), listy(11))
    draw2(getHeights(getLength(listy(12), listy(11).sum * (graphHeight-offset) / kvot), 5*horizontalBarDistance + barWidth + barSecond, graphHeight), listy(12))

    // Stream
    draw(getHeights(getLength(listy(13), listy(13).sum * (graphHeight-offset) / kvot), 6*horizontalBarDistance + barSecond, graphHeight), listy(13))
    draw2(getHeights(getLength(listy(14), listy(13).sum * (graphHeight-offset) / kvot), 6*horizontalBarDistance + barWidth + barSecond, graphHeight), listy(14))

    // Process
    draw(getHeights(getLength(listy(15), listy(15).sum * (graphHeight-offset) / kvot), 7*horizontalBarDistance + barSecond, graphHeight), listy(15))
    draw2(getHeights(getLength(listy(16), listy(15).sum * (graphHeight-offset) / kvot), 7*horizontalBarDistance + barWidth + barSecond, graphHeight), listy(16))

    //Jour
    draw(getHeights(getLength(listy(17), listy(17).sum * (graphHeight-offset) / kvot), 8*horizontalBarDistance + barSecond, graphHeight), listy(17))
    draw2(getHeights(getLength(listy(18), listy(17).sum * (graphHeight-offset) / kvot), 8*horizontalBarDistance + barWidth + barSecond, graphHeight), listy(18))
  }

  def draw(listzz: ListBuffer[ys], listxx: List[Int]): Unit = {

    val rectXFun = (d: Int, i: Int) => listzz(i).x
    val rectXTextFun = (d: Int, i: Int) => listzz(i).x + barWidth - getOffsetBarText(listxx(i))// Offset 20
    val rectXTextTopFun = if(listxx.sum > 9){listzz.last.x + barWidth - (15.0/scaleWidth)*totWidth} else {listzz.last.x + barWidth - (7.0/scaleWidth)*totWidth}

    val rectYFun = (d: Int) => listzz(d).y0
    val rectYTextFun = (d: Int, i: Int) => listzz(i).y0 + listzz(i).h - (3.0/scaleWidth)*totWidth // Offset från början av baren
    val rectYTopTextFun = graphHeight - listzz.last.h - listzz(4).h - listzz(3).h - listzz(2).h - listzz(1).h - listzz.head.h - (15.0/scaleWidth)*totWidth // Offset från topbaren till texten

    val dispText = (d: Int, i: Int) => if(listxx(i) <= 0){""}else {listxx(i).toString}
    val dispTopText = if(listxx.sum <= 0){""}else {listxx.sum.toString}

    def xDispText2(x: Int): Double = {if(x > 9){(15.0/scaleWidth)*totWidth} else {(7.0/scaleWidth)*totWidth}}
    val dispText2 = if(listxx.last <= 0){""} else {listxx.last.toString}

    val rectHeightFun = (d: Int) => if(d.equals(5) && listxx.last > 0){0} else {listzz(d).h}
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

    svg.append("text")
      .attr("x", rectXTextTopFun)
      .attr("y", rectYTopTextFun)
      .text(dispTopText)
      .attr("font-size", fontSize)
      .attr("fill", textBlack)

    if(listxx.last > 0){
      svg.append("rect")
        .attr("x", listzz.last.x)
        .attr("y", listzz.last.y0)
        .attr("height", listzz.last.h)
        .attr("width", barWidth)
        .attr("fill", textWhite)

      svg.append("g").attr("id", "shape")
        .append("rect")
        .attr("x", listzz.last.x)
        .attr("y", listzz.last.y0)
        .attr("height", listzz.last.h)
        .attr("width", barWidth)
        .attr("fill", "#afafaf")

      svg.append("text")
        .attr("x", listzz.last.x + barWidth - getOffsetBarText(listxx.last))
        .attr("y", listzz.last.y0 + listzz.last.h - (3.0/scaleWidth)*totWidth)
        .text(dispText2)
        .attr("font-size", fontSize)
        .attr("fill", textBlack)
    }
  }

  def draw2(listzz: ListBuffer[ys], listxx: List[Int]): Unit = {

    val rectXFun = (d: Int, i: Int) => listzz(i).x
    val rectXTextFun = (d: Int, i: Int) => listzz(i).x + barWidth - getOffsetBarText(listxx(i))

    val rectYFun = (d: Int) => listzz(d).y0
    val rectYTextFun = (d: Int, i: Int) => listzz(i).y0 + listzz(i).h - (3.0/scaleWidth)*totWidth // Offset från början av baren

    val dispText = (d: Int, i: Int) => if(listxx(i) <= 0){""} else {listxx(i).toString}
    val dispText2 = (d: Int, i: Int) => if(listxx.last <= 0){""} else {listxx.last.toString}

    val rectHeightFun = (d: Int) => listzz(d).h
    val rectColorFun = (d: Int, i: Int) => colorsPatit(d)

    val dispColor = (d: Int) => if(d.equals(0) && listxx.head > 0){textBlack} else{textWhite}

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
      .attr("fill", dispColor)
  }
}


def apply() = spgui.SPWidget(spwb => {
  component()
})
}
