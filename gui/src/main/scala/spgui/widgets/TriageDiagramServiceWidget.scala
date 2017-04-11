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

object TriageDiagramServiceWidget {

  private class Backend($: BackendScope[Unit, Map[String, String]]) {
    spgui.widgets.css.WidgetStyles.addToDocument()

    val messObs = BackendCommunication.getMessageObserver(
      mess => {
        mess.getBodyAs[api.PatientProperty] map {
          case api.NotTriaged(careContactId, timestamp) => $.modState(s => s + (careContactId -> "NotTriaged")).runNow()
          case api.Green(careContactId, timestamp) => $.modState(s => s + (careContactId -> "Green")).runNow()
          case api.Yellow(careContactId, timestamp) => $.modState(s => s + (careContactId -> "Yellow")).runNow()
          case api.Orange(careContactId, timestamp) => $.modState(s => s + (careContactId -> "Orange")).runNow()
          case api.Red(careContactId, timestamp) => $.modState(s => s + (careContactId -> "Red")).runNow()
          case api.Finished(careContactId, timestamp) => $.modState(s => s - careContactId).runNow()
          case x => println(s"THIS WAS NOT EXPECTED IN TriageDiagramServiceWidget: $x")
        }
      }, "triage-diagram-widget-topic"
    )

    // What is this function used for?
    def render(p: Map[String, String]) = {
      <.div(^.`class` := "card-holder-root")( // really not necessary
      )
    }
  }

  private val component = ReactComponentB[Unit]("teamVBelastning")

  .initialState(Map("0" -> "Initial"))
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

  private def addTheD3(element: raw.Element, initialTriageMap: Map[String, String]): Unit = {
  d3.select(element).selectAll("*").remove()

  val barGap = 22    // Avstånd mellan graferna
  val distance = 68.6 // Första grafens avstånd från vänstra sidan
  val width = 419      // Kroppens bredd
  val height = 380     // Kroppens höjd
  val barHeight = 222  // Grafernas höjd
  val barWidth = 45    // Grafernas bredd
  val sizeNumbers = "23px" // Storlek på siffror inuti graferna
  val sizeTeam = "27px"    // Storlek på text för Team
  val sizePatients = "24px"// Storlek på text för antal patienter
  val sizeTriage = "16px"  // Storlek på text för Triange och belastning
  val currentTeam = "STREAMTEAM"

  // ------- Färger ---------
  val colorBarOne = "#000000"  // Otriagerade
  val colorBarTwo = "#289500" // Grön
  val colorBarThree = "#EAC706" // Gul
  val colorBarFour = "#F08100" // Orange
  val colorBarFive = "#950000" // Röd
  val colorNumbers = "#FFFFFF" // Vit
  val colorText = "#000000"  // Svart
  val colorTriage = "#95989a" // Gråaktig
  val colorLines = "#95989a"
  // -------------------------

  // ----- Belastning ------
  val currentLoad = 0.5 // Denna får läsas in från Service tänker jag, där den anges i procent hur stor belastning det är.
  val paintLoad = height - (barHeight * currentLoad)
  val paintLoad2 = height  - (barHeight * currentLoad) + 10 // IGNORERA DESSA
  val paintLoad3 = height  - (barHeight * currentLoad) - 10 // IGNORERA DESSA

  //-------------------------

  // Count number of patients of each triage color
  var notTriagedCount = 0
  var greenCount = 0
  var yellowCount = 0
  var orangeCount = 0
  var redCount = 0

  initialTriageMap.foreach{ p =>
    p._2 match {
      case "NotTriaged" => notTriagedCount += 1
      case "Green" => greenCount += 1
      case "Yellow" => yellowCount += 1
      case "Orange" => orangeCount += 1
      case "Red" => redCount += 1
      case _ => // do nothing
    }
  }

  var triageMap: Map[String, Double] = Map(
    "NotTriaged" -> notTriagedCount,
    "Green" -> greenCount,
    "Yellow" -> yellowCount,
    "Orange" -> orangeCount,
    "Red" -> redCount
  )

  var length: Map[String, Double] = Map()

  triageMap.foreach{ t =>
    length += t._1 -> (t._2/(triageMap("NotTriaged") + triageMap("Green") + triageMap("Yellow") + triageMap("Orange") + triageMap("Red")))*barHeight
  }

  var scaleBars = barHeight / length.valuesIterator.max // Skalar graferna så den med högst antal patienter blir 100% av höjden

  val svg = d3.select(element).append("svg")
    .attr("width", width)
    .attr("height", height)

  val g = svg.append("g")
  // ----------- Nuvarande Belastning --------------------
   g.append("polygon")
     .attr("points", "41.4,"+paintLoad+" 55,"+paintLoad2+" 55,"+paintLoad3+"")
     .attr("fill", "none")
     .attr("stroke-width", "1")
     .attr("stroke", colorLines)

  g.append("line")
    .attr("x1", 55)
    .attr("y1", paintLoad)
    .attr("x2", width - 38)
    .attr("y2", paintLoad)
    .attr("stroke-width", 1)
    .attr("stroke", colorLines)
  // -----------------------------------------------------

  // ----------- Graf ett, Svart Otriagerade -------------
  g.append("rect")
    .attr("x", distance)
    .attr("y", height - length("NotTriaged") * scaleBars)
    .attr("width", barWidth)
    .attr("height", length("NotTriaged") * scaleBars)
    .attr("fill", colorBarOne)

  svg.append("text")
    .attr("x", (dist(triageMap("NotTriaged")) + distance))
    .attr("y", height - 4)
    .attr("font-size", sizeNumbers)
    .text(s"${removeZero(triageMap("NotTriaged"))}")
    .attr("fill", colorNumbers)

  // ----------- Graf två, Grön  -------------
  g.append("rect")
    .attr("x",  distance + barGap + barWidth)
    .attr("y", height - length("Green") * scaleBars)
    .attr("width", barWidth)
    .attr("height", length("Green") * scaleBars)
    .attr("fill", colorBarTwo)

  svg.append("text")
    .attr("x", (dist(triageMap("Green")) + distance + barGap + barWidth))
    .attr("y", height - 5)
    .attr("font-size", sizeNumbers)
    .text(s"${removeZero(triageMap("Green"))}")
    .attr("fill", colorNumbers)

  // ----------- Graf tre, Gul  -------------
  g.append("rect")
    .attr("x", distance + 2*barGap + 2*barWidth)
    .attr("y", height - length("Yellow") * scaleBars)
    .attr("width", barWidth)
    .attr("height", length("Yellow") * scaleBars)
    .attr("fill", colorBarThree)

  svg.append("text")
    .attr("x", (dist(triageMap("Yellow")) + distance + 2 * barGap + 2 * barWidth))
    .attr("y", height - 5)
    .attr("font-size", sizeNumbers)
    .text(s"${removeZero(triageMap("Yellow"))}")
    .attr("fill", colorNumbers)

  // ----------- Graf fyra, Orange  -------------
  g.append("rect")
    .attr("x", distance + 3 * barGap + 3 * barWidth)
    .attr("y", height - length("Orange") * scaleBars)
    .attr("width", barWidth)
    .attr("height", length("Orange") * scaleBars)
    .attr("fill", colorBarFour)

  svg.append("text")
    .attr("x", (dist(triageMap("Orange")) + distance + 3 * barGap + 3 * barWidth))
    .attr("y", height - 5)
    .attr("font-size", sizeNumbers)
    .text(s"${removeZero(triageMap("Orange"))}")
    .attr("fill", colorNumbers)

  // ----------- Graf fem, Röd  -------------
  g.append("rect")
    .attr("x",  distance + 4 * barGap + 4 * barWidth)
    .attr("y", height - length("Red") * scaleBars)
    .attr("width", barWidth)
    .attr("height", length("Red") * scaleBars)
    .attr("fill", colorBarFive)

  svg.append("text")
    .attr("x", (dist(triageMap("Red")) +  distance + 4 * barGap + 4 * barWidth))
    .attr("y", height - 5)
    .attr("font-size", sizeNumbers)
    .text(s"${removeZero(triageMap("Red"))}")
    .attr("fill", colorNumbers)

  // ---------- TEAM ------------

  svg.append("text")
    .attr("x", 0)
    .attr("y", 20)
    .attr("font-size", sizeTeam)
    .text(currentTeam)
    .style("font-weight", "bold")
    .attr("fill", colorText)

  // ------- ANTAL PATIENTER ------------

  svg.append("text")
    .attr("x", 0)
    .attr("y", 50)
    .attr("font-size", sizePatients)
    .text(s"${triageMap("NotTriaged") + triageMap("Green") + triageMap("Yellow") + triageMap("Orange") + triageMap("Red")}" + " " + "PATIENTER TOTALT")
    .attr("fill", colorText)

  // ------- Triage och Belastning ------------

  svg.append("text")
    .attr("x", 0)
    .attr("y", 110)
    .attr("font-size", sizeTriage)
    .text("TRIAGE OCH BELASTNING")
    .attr("fill", colorTriage)

  // ------- Belasningsdiagrammet --------------

  g.append("line")
    .attr("x1", distance - 27.5)
    .attr("y1", height - barHeight)
    .attr("x2", distance - 27.5)
    .attr("y2", height)
    .attr("stroke-width", 1)
    .attr("stroke", colorLines)

  svg.append("text")
    .attr("x", distance - 53)
    .attr("y", height - barHeight + 12)
    .attr("font-size", "12px")
    .text("Hög")
    .attr("fill", colorLines)

  svg.append("text")
    .attr("x", distance - 64)
    .attr("y", height - (barHeight/2) + 6 )
    .attr("font-size", "12px")
    .text("Medel")
    .attr("fill", colorLines)

  svg.append("text")
    .attr("x", distance - 52)
    .attr("y", height - 5)
    .attr("font-size", "12px")
    .text("Låg")
    .attr("fill", colorLines)

}

  def apply() = spgui.SPWidget(spwb => {
    component()
  })
}
