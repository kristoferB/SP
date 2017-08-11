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

import scalacss.ScalaCssReact._
import scalacss.DevDefaults._

import scala.collection.mutable.ListBuffer

import spgui.widgets.{API_PatientEvent => api}
import spgui.widgets.{API_Patient => apiPatient}

object TriageWidget {

private class Backend($: BackendScope[String, Map[String, apiPatient.Patient]]) {

 var patientObs = Option.empty[rx.Obs]
  def setPatientObs(): Unit = {
    patientObs = Some(spgui.widgets.akuten.PatientModel.getPatientObserver{
          println("are we here?")
      patients => $.setState(patients).runNow()
    })
  }
 //spgui.widgets.css.WidgetStyles.addToDocument()

  val wsObs = BackendCommunication.getWebSocketStatusObserver(  mess => {
    if (mess) send(api.GetState())
  }, "patient-cards-widget-topic")

  def send(mess: api.Event) {
    val h = SPHeader(from = "MedicineYellowTriageWidget", to = "TriageDiagramService")
    val json = ToAndFrom.make(h, mess)
    BackendCommunication.publish(json, "triage-diagram-service-topic")
  }

  def render(p: String, s: Map[String, apiPatient.Patient]) = {
    <.div(Styles.helveticaZ)
  }

  def onUnmount() = {
    println("Unmounting")
    patientObs.foreach(_.kill())
    wsObs.kill()
    Callback.empty
  }
}

private val component = ScalaComponent.builder[String]("teamVBelastning")
.initialState(Map("-1" ->
  EricaLogic.dummyPatient))
.renderBackend[Backend]
.componentDidMount(ctx => Callback(ctx.backend.setPatientObs()))
.componentDidUpdate(ctx => Callback(addTheD3(ctx.getDOMNode, ctx.currentState, ctx.currentProps)))
.componentWillUnmount(_.backend.onUnmount())
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

/**
* Checks if the patient belongs to this team.
*/
def belongsToThisTeam(patient: apiPatient.Patient, filter: String): Boolean = {
  filter.isEmpty || patient.team.team.contains(filter)
}


private def addTheD3(element: raw.Element, patients: Map[String, apiPatient.Patient], filter: String): Unit = {
d3.select(element).selectAll("*").remove()

val width = (element.clientWidth - 20)           // Kroppens bredd
val barGap = (22.0 / 450) * width        // Avstånd mellan graferna
val distance = (68.6 / 450) * width       // Första grafens avstånd från vänstra sidan
val height = (380.0 / 450) * width       // Kroppens höjd
val barHeight = (222.0 / 450) * width    // Grafernas höjd
val barWidth = (45.0 / 450) * width      // Grafernas bredd
val smallRec = (15.0 / 450) * width      // Små rektanglar
val sizeNumbers = s"${(23.0/ 450) * width}px" // Storlek på siffror innuti graferna
val sizeTeam = s"${(27.0/ 450) * width}px"    // Storlek på text för Team
val sizePatients = s"${(24.0/ 450) * width}px"// Storlek på text för antal patienter
val sizeTriage = s"${(16.0/ 450) * width}px"  // Storlek på text för Triange och belastning
val sizeSmallRecText = s"${(14.0/ 450) * width}px"
val moveEverything = (35.0 / 450) * width
val currentTeam = getTeam(filter)

// Count number of patients of each triage color
var notTriagedCount = 0
var blueCount = 0
var greenCount = 0
var yellowCount = 0
var orangeCount = 0
var redCount = 0

  var teamMap: Map[String, apiPatient.Patient] = (patients - "-1").filter(p => belongsToThisTeam(p._2, filter))

teamMap.foreach{ p =>
  p._2.priority.color match {
    case "NotTriaged" => notTriagedCount += 1
    case "Blue" => blueCount += 1
    case "Green" => greenCount += 1
    case "Yellow" => yellowCount += 1
    case "Orange" => orangeCount += 1
    case "Red" => redCount += 1
  }
}

var triageMap: Map[String, Double] = Map(
  "NotTriaged" -> notTriagedCount,
  "Blue" -> blueCount,
  "Green" -> greenCount,
  "Yellow" -> yellowCount,
  "Orange" -> orangeCount,
  "Red" -> redCount
)

var length: Map[String, Double] = Map(
  "NotTriaged" -> 0,
  "Blue" -> 0,
  "Green" -> 0,
  "Yellow" -> 0,
  "Orange" -> 0,
  "Red" -> 0
)

triageMap.foreach{ t =>
  val sum = triageMap("NotTriaged") + triageMap("Blue") + triageMap("Green") + triageMap("Yellow") + triageMap("Orange") + triageMap("Red")
  if (sum == 0) {
    length += t._1 -> 0
  } else {
    length += t._1 -> (t._2/sum)*barHeight
  }
}

// ------- Färger ---------
val colorBarTriage = "#FFFFFF"
val colorBarBlue = "#538af4"
val colorBarGreen = "#009550"
val colorBarYellow = "#eac706"
val colorBarOrange = "#f08100"
val colorBarRed = "#950000"
val colorNumbers = "#FFFFFF"
val colorTriageNumbers = "#000000"
val colorText = "#000000"
val colorTriage = "#95989a"
val colorLines = "#95989a"
// -------------------------

// ----- Belastning ------
val currentLoad = 0.5 // Denna får läsas in från Service tänker jag, där den anges i procent hur stor belastning det är.
val paintLoad = height - moveEverything - (barHeight * currentLoad)
val paintLoad2 = height - moveEverything  - (barHeight * currentLoad) + (10.0/ 450) * width // IGNORERA DESSA
val paintLoad3 = height - moveEverything  - (barHeight * currentLoad) - (10.0/ 450) * width // IGNORERA DESSA
val x1 = (41.4 / 450) * width
val x2 = (55.0/ 450) * width
val x3 = (55.0/ 450) * width
//-------------------------

// Skalar graferna så den med högst antal patienter blir 100% av höjden
var scaleBars: Double = 0.0
if (length.valuesIterator.max != 0) {
  scaleBars = barHeight / length.valuesIterator.max
} else {
  scaleBars = 0
}

def dist(d: Double): Double = { // Placerar siffran på rätt avstånd från sidan av grafen.
  if(d > 99){(2.0 / 450) * width} // 2
  else if( d > 9 ){(10.0/ 450) * width} // 10
  else {(25.0/ 450) * width} //17.5
}

val svg = d3.select(element).append("svg")
  .attr("width", width)
  .attr("height", height)

val g = svg.append("g")
/*  // ----------- Nuvarande Belastning --------------------
g.append("polygon")
.attr("points", ""+x1+","+paintLoad+" "+x2+","+paintLoad2+" "+x3+","+paintLoad3+"")
.attr("fill", "none")
.attr("stroke-width", "1")
.attr("stroke", colorLines)

g.append("line")
.attr("x1", (55.0/ 450) * width)
.attr("y1", paintLoad)
.attr("x2", width - (38.0/ 450) * width)
.attr("y2", paintLoad)
.attr("stroke-width", 1)
.attr("stroke", colorLines)
// ----------------------------------------------------- */

// ----------- Graf ett, Svart Otriagerade -------------
g.append("rect")
.attr("x", distance)
.attr("y", height - length("NotTriaged") * scaleBars - moveEverything)
.attr("width", barWidth)
.attr("height", length("NotTriaged") * scaleBars)
.attr("fill", colorBarTriage)

svg.append("defs").append("pattern")
.append("pattern")
.attr("id", "stuffZ")
.attr("width", "15")
.attr("height", "8")
.attr("patternUnits", "userSpaceOnUse")
.attr("patternTransform", "rotate(45)")
.append("rect")
.attr("width", "2")
.attr("height", "8")
.attr("transform", "translate(0,0)")
.attr("fill", "#afafaf")

svg.append("g").attr("id", "shape")
.append("rect")
.attr("x", distance)
.attr("y", height - length("NotTriaged") * scaleBars  - moveEverything)
.attr("height", length("NotTriaged") * scaleBars)
.attr("width", barWidth)
.attr("fill", "#afafaf")

svg.append("text")
.attr("x", (dist(triageMap("NotTriaged")) + distance))
.attr("y", height - 4  - moveEverything)
.attr("font-size", sizeNumbers)
.text(s"${removeZero(triageMap("NotTriaged"))}")
.attr("fill", colorTriageNumbers)

// ----------- Graf två, Blå  -------------
 g.append("rect")
   .attr("x",  distance + barGap + barWidth)
   .attr("y", height - length("Blue") * scaleBars - moveEverything)
   .attr("width", barWidth)
   .attr("height", length("Blue") * scaleBars)
   .attr("fill", colorBarBlue)

 svg.append("text")
   .attr("x", (dist(triageMap("Blue")) + distance + barGap + barWidth))
   .attr("y", height - (5.0/ 450) * width  - moveEverything)
   .attr("font-size", sizeNumbers)
   .text(s"${removeZero(triageMap("Blue"))}")
   .attr("fill", colorNumbers)

 // ----------- Graf tre, Grön  -------------
 g.append("rect")
   .attr("x", distance + 2*barGap + 2*barWidth)
   .attr("y", height - length("Green") * scaleBars  - moveEverything)
   .attr("width", barWidth)
   .attr("height", length("Green") * scaleBars)
   .attr("fill", colorBarGreen)

 svg.append("text")
   .attr("x", (dist(triageMap("Green")) + distance + 2 * barGap + 2 * barWidth))
   .attr("y", height - (5.0/ 450) * width  - moveEverything)
   .attr("font-size", sizeNumbers)
   .text(s"${removeZero(triageMap("Green"))}")
   .attr("fill", colorNumbers)

 // ----------- Graf fyra, Gul  -------------
 g.append("rect")
   .attr("x", distance + 3 * barGap + 3 * barWidth)
   .attr("y", height - length("Yellow") * scaleBars  - moveEverything)
   .attr("width", barWidth)
   .attr("height", length("Yellow") * scaleBars)
   .attr("fill", colorBarYellow)

 svg.append("text")
   .attr("x", (dist(triageMap("Yellow")) + distance + 3 * barGap + 3 * barWidth))
   .attr("y", height - (5.0/ 450) * width  - moveEverything)
   .attr("font-size", sizeNumbers)
   .text(s"${removeZero(triageMap("Yellow"))}")
   .attr("fill", colorNumbers)

 // ----------- Graf fem, Orange  -------------
 g.append("rect")
   .attr("x",  distance + 4 * barGap + 4 * barWidth)
   .attr("y", height - length("Orange") * scaleBars  - moveEverything)
   .attr("width", barWidth)
   .attr("height", length("Orange") * scaleBars)
   .attr("fill", colorBarOrange)

 svg.append("text")
   .attr("x", (dist(triageMap("Orange")) +  distance + 4 * barGap + 4 * barWidth))
   .attr("y", height - (5.0/ 450) * width  - moveEverything)
   .attr("font-size", sizeNumbers)
   .text(s"${removeZero(triageMap("Orange"))}")
   .attr("fill", colorNumbers)

 // ----------- Graf fem, Röd  -------------
 g.append("rect")
   .attr("x",  distance + 5 * barGap + 5 * barWidth)
   .attr("y", height - length("Red") * scaleBars  - moveEverything)
   .attr("width", barWidth)
   .attr("height", length("Red") * scaleBars )
   .attr("fill", colorBarRed)

 svg.append("text")
   .attr("x", dist(triageMap("Red")) +  distance + 5 * barGap + 5 * barWidth)
   .attr("y", height - (5.0/ 450) * width  - moveEverything)
   .attr("font-size", sizeNumbers)
   .text(s"${removeZero(triageMap("Red"))}")
   .attr("fill", colorNumbers)

 // --------- Små rektanglar -----------
 g.append("rect")
   .attr("x", (10.0/ 450) * width)
   .attr("y", height - (15.0/ 450) * width)
   .attr("width", smallRec)
   .attr("height", smallRec)
   .attr("fill", colorBarTriage)

 svg.append("defs").append("pattern")
   .append("pattern")
   .attr("id", "stuffZ")
   .attr("width", "6")
   .attr("height", "6")
   .attr("patternUnits", "userSpaceOnUse")
   .attr("patternTransform", "rotate(45)")
   .append("rect")
   .attr("width", "2")
   .attr("height", "6")
   .attr("transform", "translate(0,0)")
   .attr("fill", "afafaf")

 svg.append("g").attr("id", "shape")
   .append("rect")
   .attr("x", (10.0/ 450) * width)
   .attr("y", height - (15.0/ 450) * width)
   .attr("height", smallRec)
   .attr("width", smallRec)
   .attr("fill", "#afafaf")

 svg.append("text")
   .attr("x", smallRec + (15.0/ 450) * width)
   .attr("y", height - (3.0/ 450) * width)
   .attr("font-size", sizeSmallRecText)
   .text("Ej triagerad")
   .attr("fill", colorText)

 // ---------- BLÅ -------------
 g.append("rect")
   .attr("x", (120.0/ 450) * width)
   .attr("y", height - (15.0/ 450) * width)
   .attr("width", smallRec)
   .attr("height", smallRec)
   .attr("fill", colorBarBlue)

 svg.append("text")
   .attr("x", (125.0/ 450) * width + smallRec)
   .attr("y", height - (3.0/ 450) * width)
   .attr("font-size", sizeSmallRecText)
   .text("Blå")
   .attr("fill", colorText)
 // ---------- Grön -------------
 g.append("rect")
   .attr("x", (180.0/ 450) * width)
   .attr("y", height - (15.0/ 450) * width)
   .attr("width", smallRec)
   .attr("height", smallRec)
   .attr("fill", colorBarGreen)

 svg.append("text")
   .attr("x", (185.0/ 450) * width + smallRec)
   .attr("y", height - (3.0/ 450) * width)
   .attr("font-size", sizeSmallRecText)
   .text("Grön")
   .attr("fill", colorText)

 // ---------- Gul -------------
 g.append("rect")
   .attr("x", (250.0/ 450) * width)
   .attr("y", height - (15.0/ 450) * width)
   .attr("width", smallRec)
   .attr("height", smallRec)
   .attr("fill", colorBarYellow)

 svg.append("text")
   .attr("x", (255.0/ 450) * width + smallRec)
   .attr("y", height - (3.0/ 450) * width)
   .attr("font-size", sizeSmallRecText)
   .text("Gul")
   .attr("fill", colorText)

 // ---------- Orange -------------
 g.append("rect")
   .attr("x", (315.0/ 450) * width)
   .attr("y", height - (15.0/ 450) * width)
   .attr("width", smallRec)
   .attr("height", smallRec)
   .attr("fill", colorBarOrange)

 svg.append("text")
   .attr("x", (320.0/ 450) * width + smallRec)
   .attr("y", height - (3.0/ 450) * width)
   .attr("font-size", sizeSmallRecText)
   .text("Orange")
   .attr("fill", colorText)
 // ---------- Röd -------------
 g.append("rect")
   .attr("x", (405.0/ 450) * width)
   .attr("y", height - (15.0/ 450) * width)
   .attr("width", smallRec)
   .attr("height", smallRec)
   .attr("fill", colorBarRed)

 svg.append("text")
   .attr("x", (410.0/ 450) * width + smallRec)
   .attr("y", height - (3.0/ 450) * width)
   .attr("font-size", sizeSmallRecText)
   .text("Röd")
   .attr("fill", colorText)


 // ---------- TEAM ------------

 svg.append("text")
   .attr("x", 0)
   .attr("y", (28.0/ 450) * width)
   .attr("font-size", sizeTeam)
   .text(currentTeam)
   .style("font-weight", "bold")
   .attr("fill", colorText)

 // ------- ANTAL PATIENTER ------------

 svg.append("text")
   .attr("x", 0)
   .attr("y", (58.0/ 450) * width)
   .attr("font-size", sizePatients)
   .text(s"${triageMap("NotTriaged") + triageMap("Blue") + triageMap("Green") + triageMap("Yellow") + triageMap("Orange") + triageMap("Red")}" + " " + "PATIENTER TOTALT")
   .attr("fill", colorText)

 // ------- Triage och Belastning ------------

 svg.append("text")
   .attr("x", 0)
   .attr("y", (90.0/ 450) * width)
   .attr("font-size", sizeTriage)
   .text("TRIAGE OCH BELASTNING")
   .attr("fill", colorText)
   .style("font-weight", "bold")

/* // ------- Belasningsdiagrammet --------------

 g.append("line")
   .attr("x1", distance - (27.5/ 450) * width)
   .attr("y1", height - barHeight - moveEverything)
   .attr("x2", distance - (27.5/ 450) * width)
   .attr("y2", height - moveEverything)
   .attr("stroke-width", 1)
   .attr("stroke", colorLines)

 svg.append("text")
   .attr("x", distance - (53.0/ 450) * width)
   .attr("y", height - barHeight + (12.0/ 450) * width - moveEverything)
   .attr("font-size", "12px")
   .text("Hög")
   .attr("fill", colorLines)

 svg.append("text")
   .attr("x", distance - (64.0/ 450) * width)
   .attr("y", height - (barHeight/2) + (6.0/ 450) * width - moveEverything)
   .attr("font-size", "12px")
   .text("Medel")
   .attr("fill", colorLines)

 svg.append("text")
   .attr("x", distance - (52.0/ 450) * width)
   .attr("y", height - (5.0/ 450) * width - moveEverything)
   .attr("font-size", "12px")
   .text("Låg")
   .attr("fill", colorLines) */

}
def getTeam(s: String): String = {
  s match {
    case "medicin gul" | "gul" => "MEDICIN GUL"
    case "medicin blå" | "blå" => "MEDICIN BLÅ"
    case "medicin" => "MEDICINTORG"
    case "streamteam" | "stream" => "STREAMTEAM"
    case "process" => "PROCESS"
    case "kirurgi" | "kirurg" => "KIRURGI"
    case "ortopedi" => "ORTOPEDI"
    case "jour" => "JOUR"
    case "nakm" => "NAKM"
    case _ => ""
  }
}

def extractTeam(attributes: Map[String, SPValue]) = {
  attributes.get("team").flatMap(x => x.asOpt[String]).getOrElse("medicin")
}

def apply() = spgui.SPWidget(spwb => {
  val currentTeam = extractTeam(spwb.frontEndState.attributes)
  component(currentTeam)
})
}
