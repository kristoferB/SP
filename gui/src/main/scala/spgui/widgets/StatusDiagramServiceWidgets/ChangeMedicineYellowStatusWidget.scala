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
import spgui.widgets.{API_Patient => apiPatient}

object ChangeMedicineYellowStatusWidget {

  private class Backend($: BackendScope[String, Map[String, apiPatient.Patient]]) {
    spgui.widgets.css.WidgetStyles.addToDocument()

    val messObs = BackendCommunication.getMessageObserver(
      mess => {
        ToAndFrom.eventBody(mess).map {
          case api.State(patients) => $.modState{s => patients}.runNow()
          case x => println(s"THIS WAS NOT EXPECTED IN MedicineYellowStatusWidget: $x")
        }
      }, "status-diagram-widget-topic"
    )

    send(api.GetState())

    def send(mess: api.StateEvent) {
      val h = SPHeader(from = "MedicineYellowStatusWidget", to = "StatusDiagramService")
      val json = ToAndFrom.make(h, mess)
      BackendCommunication.publish(json, "status-diagram-service-topic")
    }

    def render(p: String, s: Map[String, apiPatient.Patient]) = {
      <.div(Styles.helveticaZ)
    }

    def onUnmount() = {
      println("Unmounting")
      messObs.kill()
      Callback.empty
    }
  }

  private val component = ReactComponentB[String]("TeamStatusWidget")
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
  .componentDidUpdate(dcb => Callback(addTheD3(ReactDOM.findDOMNode(dcb.component), dcb.currentState, dcb.currentProps)))
  .componentWillUnmount(_.backend.onUnmount())
  .build

  /**
  * Checks if the patient belongs to this team.
  */
  def belongsToThisTeam(patient: apiPatient.Patient, filter: String): Boolean = {
    filter.isEmpty || patient.team.team.contains(filter)
  }

  private def addTheD3(element: raw.Element, initialStatusMap: Map[String, apiPatient.Patient], filter: String): Unit = {

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

    var teamMap: Map[String, apiPatient.Patient] = (initialStatusMap - "-1").filter(p => belongsToThisTeam(p._2, filter))


    // TODO: Never use vars if not needed in scala. Use map and foldLeft if you need to aggregate
    teamMap.foreach{ p =>
      if (p._2.finishedStillPresent.finishedStillPresent) {
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
      val sum = statusMap("Unattended") + statusMap("Attended") + statusMap("Finished") + statusMap("AttendedWithPlan")
      if (sum == 0) {
        length += s._1 -> 0
      } else {
        length += s._1 -> (s._2/(sum))*width
      }
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

  def extractTeam(attributes: Map[String, SPValue]) = {
    attributes.get("team").map(x => x.str).getOrElse("medicin")
  }

  def apply() = spgui.SPWidget(spwb => {
    val currentTeam = extractTeam(spwb.frontEndState.attributes)
    component(currentTeam)
  })
}
