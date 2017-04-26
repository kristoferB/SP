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

object WaitingRoomServiceWidget {

  private class Backend($: BackendScope[Unit, Map[String, apiPatient.Patient]]) {
    spgui.widgets.css.WidgetStyles.addToDocument()

    val messObs = BackendCommunication.getMessageObserver(
      mess => {
        mess.getBodyAs[api.Event] map {
          case api.State(patients) => $.modState{s => patients}.runNow()
          case x => println(s"THIS WAS NOT EXPECTED IN WaitingRoomServiceWidget: $x")
        }
      }, "waiting-room-widget-topic"
    )

    send(api.GetState())

    def send(mess: api.StateEvent) {
      val h = SPHeader(from = "WaitingRoomWidget", to = "WaitingRoomService")
      val json = SPMessage.make(h, mess)
      BackendCommunication.publish(json, "waiting-room-service-topic")
    }

    // What is this function used for?
    def render(p: Map[String, apiPatient.Patient]) = {
      <.div(Styles.helveticaZ)
    }
  }

  def getWaitingRoomOccupation(m: Map[String, apiPatient.Patient]): List[List[Int]] = {
    var finishedCountMG = 0
    var attendedCountMG = 0
    var attendedWithPlanCountMG = 0
    var unAttendedCountMG = 0

    var finishedCountMB = 0
    var attendedCountMB = 0
    var attendedWithPlanCountMB = 0
    var unAttendedCountMB = 0

    var finishedCountK = 0
    var attendedCountK = 0
    var attendedWithPlanCountK = 0
    var unAttendedCountK = 0

    var finishedCountO = 0
    var attendedCountO = 0
    var attendedWithPlanCountO = 0
    var unAttendedCountO = 0

    var finishedCountS = 0
    var attendedCountS = 0
    var attendedWithPlanCountS = 0
    var unAttendedCountS = 0

    var finishedCountP = 0
    var attendedCountP = 0
    var attendedWithPlanCountP = 0
    var unAttendedCountP = 0

    var finishedCountJ = 0
    var attendedCountJ = 0
    var attendedWithPlanCountJ = 0
    var unAttendedCountJ = 0

    var tmp = 0

    (m - "-1").foreach{ p =>
      if (p._2.location.roomNr == "ivr") {
        if (p._2.finishedStillPresent.finishedStillPresent) {
          p._2.team.team match {
            case "medicin gul" | "medicin" => finishedCountMG += 1
            case "medicin blå" => finishedCountMB += 1
            case "process" => finishedCountP += 1
            case "stream" => finishedCountS += 1
            case "jour" => finishedCountJ += 1
            case "kirurgi" => finishedCountK += 1
            case "ortopedi" => finishedCountO += 1
            case _ => tmp += 1
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
              case _ => tmp += 1
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
              case _ => tmp += 1
            }
          }
        }
      }
    }
    val list: List[List[Int]] = List(
      List(unAttendedCountK, attendedCountK, attendedWithPlanCountK, finishedCountK),
      List(unAttendedCountMG + unAttendedCountMB, attendedCountMB + attendedCountMG, attendedWithPlanCountMB + attendedWithPlanCountMG, finishedCountMB + finishedCountMG),
      List(unAttendedCountO, attendedCountO, attendedWithPlanCountO, finishedCountO),
      List(unAttendedCountJ, attendedCountJ, attendedWithPlanCountJ, finishedCountJ),
      List(unAttendedCountP + unAttendedCountS, attendedCountP + attendedCountS, attendedWithPlanCountP + attendedWithPlanCountS, finishedCountP + finishedCountS)

    )
    return list
  }

  private val component = ReactComponentB[Unit]("KoordMapWidget")
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
  .componentDidUpdate(dcb => Callback(addTheD3(ReactDOM.findDOMNode(dcb.component), dcb.currentState)))
  .build

  private def addTheD3(element: raw.Element, patients: Map[String, apiPatient.Patient]): Unit = {

  d3.select(element).selectAll("*").remove()

  spgui.widgets.css.WidgetStyles.addToDocument()

  val list = getWaitingRoomOccupation(patients)

  val width = element.clientWidth

  case class xyBegin(name: String, x: Double, y: Double)
  case class smallBox(text: String, x: Double, y: Double, c: String, h: Double = (15.4/1094.00)*width, w: Double = (14.6/1094.00)*width)

  val height = (185.00/1094.00)*width

  val boxHeight = (27.3/1094.00)*width
  val boxWidth = (48.9/1094.00)*width

  val svg = d3.select(element).append("svg").attr("width", width).attr("height", height)

  val colors = List("#ffffff", "#4a4a4a", "#5c5a5a", "#888888", "#bebebe", "#000000")
  val fontSizeVantrum = s"${(24.0/1094.00)*width}px"
  val fontSizeSmallBox = s"${(15/1094.00)*width}px"
  val fontSizeBigBox = s"${(19/1094.00)*width}px"

  // Väntrum
  svg.append("text")
    .attr("x", (15.5/1094.00)*width)
    .attr("y", (36.9/1094.00)*width)
    .attr("font-size", fontSizeVantrum)
    .attr("font-weight", "bold")
    .attr("fill", colors.last)
    .text("VÄNTRUM")

  val opatittad = smallBox("Opåtittad", (15.5/1094.00)*width, (57.3/1094.00)*width, colors(1))
  val patittad = smallBox("Påtittad", (130.0/1094.00)*width, (57.3/1094.00)*width, colors(2))
  val patittadMPlan = smallBox("Påtittad m. Plan", (240.0/1094.0)*width, (57.3/1094.00)*width, colors(3))
  val klar = smallBox("Klar", (388.6/1094.00)*width, (57.3/1094.00)*width, colors(4))

  draw2(opatittad)
  draw2(patittad)
  draw2(patittadMPlan)
  draw2(klar)

  def draw2(box: smallBox): Unit = {

    svg.append("rect")
      .attr("x", box.x)
      .attr("y", box.y)
      .attr("width", box.w)
      .attr("height", box.h)
      .style("fill", box.c)

    svg.append("text")
      .attr("x", box.x + box.w + (5.00/1094.00)*width)
      .attr("y", box.y + box.h - (2.00/1094.00)*width)
      .attr("font-size", fontSizeSmallBox)
      .attr("fill", colors.last)
      .text(box.text)
  }

  // Boxar ovansidan
  val kirurgi = xyBegin("Kirurgi", (14.6/1094.00)*width, (98.6/1094.00)*width) //box("Kirurgi",14.6, 98.6, 27.3, 310.5)
  val ortopedi = xyBegin("Ortopedi", (391.7/1094.00)*width, (98.6/1094.00)*width) //box("Ortopedi", 391.7, 98.6, 27.3, 310.5)
  val ovriga = xyBegin("Övriga", (768.7/1094.00)*width, (98.6/1094.00)*width) //box("Övriga", 768.7, 98.6, 27.3, 310.5)

  // Boxar nedsian
  val medicin = xyBegin("Medicin", (14.6/1094.00)*width, (135.6/1094.00)*width) //box("Medicin", 14.6, 135.6, 27.3, 310.5)
  val jour = xyBegin("Jour", (391.7/1094.00)*width, (135.6/1094.00)*width) //box("Jour", 391.7, 135.6, 27.3, 310.5)

  draw(list.head, kirurgi)
  draw(list(1), ortopedi)
  draw(list(2), ovriga)
  draw(list(3), medicin)
  draw(list.last, jour)

  def draw(list: List[Int], xy: xyBegin): Unit = {

    // Rektanglar
    svg.append("rect")
      .attr("x", xy.x)
      .attr("y", xy.y)
      .attr("width", (115.00/1094.00)*width)
      .attr("height", boxHeight)
      .style("fill", colors.head)

    svg.append("rect")
      .attr("x", xy.x + (115.00/1094.00)*width)
      .attr("y", xy.y)
      .attr("width", boxWidth)
      .attr("height", boxHeight)
      .style("fill", colors(1))

    svg.append("rect")
      .attr("x", xy.x + (115.00/1094.00)*width + boxWidth)
      .attr("y", xy.y)
      .attr("width", boxWidth)
      .attr("height", boxHeight)
      .style("fill", colors(2))

    svg.append("rect")
      .attr("x", xy.x + (115.00/1094.00)*width + 2*boxWidth)
      .attr("y", xy.y)
      .attr("width", boxWidth)
      .attr("height", boxHeight)
      .style("fill", colors(3))

    svg.append("rect")
      .attr("x", xy.x + (115.00/1094.00)*width + 3*boxWidth)
      .attr("y", xy.y)
      .attr("width", boxWidth)
      .attr("height", boxHeight)
      .style("fill", colors(4))

    // Text
    svg.append("text")
      .attr("x", xy.x + (10.00/1094.00)*width)
      .attr("y", xy.y + boxHeight - (7.00/1094.00)*width)
      .attr("font-size", fontSizeBigBox)
      .attr("font-weight", "bold")
      .attr("fill", colors.last)
      .text(xy.name)

    svg.append("text")
      .attr("x", xy.x + ((115.00+18.45)/1094.00)*width)
      .attr("y", xy.y + boxHeight - (7.00/1094.00)*width)
      .attr("font-size", fontSizeBigBox)
      .attr("font-weight", "bold")
      .attr("fill", colors.head)
      .text(list.head)

    svg.append("text")
      .attr("x", xy.x + ((115.00+67.35)/1094.00)*width)
      .attr("y", xy.y + boxHeight - (7.00/1094.00)*width)
      .attr("font-size", fontSizeBigBox)
      .attr("font-weight", "bold")
      .attr("fill", colors.head)
      .text(list(1))

    svg.append("text")
      .attr("x", xy.x + ((115.00+116.25)/1094.00)*width)
      .attr("y", xy.y + boxHeight - (7.00/1094.00)*width)
      .attr("font-size", fontSizeBigBox)
      .attr("font-weight", "bold")
      .attr("fill", colors.head)
      .text(list(2))

    svg.append("text")
      .attr("x", xy.x + ((115.00+165.15)/1094.00)*width)
      .attr("y", xy.y + boxHeight - (7.00/1094.00)*width)
      .attr("font-size", fontSizeBigBox)
      .attr("font-weight", "bold")
      .attr("fill", colors.head)
      .text(list.last)
  }
}

  def apply() = SPWidget {spwb => component()}

}
