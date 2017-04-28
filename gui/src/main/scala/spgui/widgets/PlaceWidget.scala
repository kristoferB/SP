package spgui.widgets

import java.time._
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
import scala.util.{Success, Try}
import scala.util.Random.nextInt
import org.scalajs.dom
import org.scalajs.dom.raw
import org.scalajs.dom.{svg => *}
import org.singlespaced.d3js.d3
import org.singlespaced.d3js.Ops._
import spgui.circuit.SPGUICircuit

import scalacss.ScalaCssReact._
import scalacss.Defaults._
import scala.collection.mutable.ListBuffer
import spgui.widgets.{API_PatientEvent => api}
import spgui.widgets.{API_Patient => apiPatient}

object PlaceWidget {


  private class Backend($: BackendScope[String, Map[String, apiPatient.Patient]]) {
    spgui.widgets.css.WidgetStyles.addToDocument()

    val messObs = BackendCommunication.getMessageObserver(
      mess => {
        ToAndFrom.eventBody(mess).map {
          case api.State(xs) => $.modState{s => xs}.runNow()
          case x => println(s"THIS WAS NOT EXPECTED IN PlaceWidget: $x")
        }
      }, "place-diagram-widget-topic"
    )

    //    val globalStateChange = SPGUICircuit.subscribe(SPGUICircuit.zoom(_.globalState)){x =>
    //      val f = extractTeam(x().attributes)
    //      $.modState{s => s.copy(filter = f)}.runNow()
    //    }


    val wsObs = BackendCommunication.getWebSocketStatusObserver(  mess => {
      if (mess) send(api.GetState())
    }, "place-diagram-widget-topic")

    def send(mess: api.StateEvent) {
      val h = SPHeader(from = "PlaceWidget", to = "PlaceDiagramService")
      val json = ToAndFrom.make(h, mess)
      BackendCommunication.publish(json, "place-diagram-service-topic")
    }

    def render(team: String, p: Map[String, apiPatient.Patient]) = {
      <.div(Styles.helveticaZ)
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

  val initState = Map("-1" ->
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
    ))

    private val component = ReactComponentB[String]("TeamStatusWidget")
    .initialState(initState)
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

    def getPlace(patient: apiPatient.Patient): String = {
      if (patient.examination.isOnExam) "Examination"
      else if (patient.location.roomNr == "ivr") "InnerWaitingRoom"
      else if (patient.location.roomNr != "") "RoomOnSquare"
      else "Other"
    }

    private def addTheD3(element: raw.Element, initialPlaceMap: Map[String, apiPatient.Patient], filter: String): Unit = {

      spgui.widgets.css.WidgetStyles.addToDocument()

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
      val colorBarOne = "#4d5256"
      val colorBarTwo = "#1b998b"
      val colorBarThree = "#9df2e9"
      val colorBarFour = "#f5fffe"
      val colorNumbersLight = "#FFFFFF"
      val colorNumbersDark = "#000000"
      // -------------------------



      // TODO: Never use vars if not needed in scala. Use map and foldLeft if you need to aggregate

      var teamMap: Map[String, apiPatient.Patient] = (initialPlaceMap - "-1").filter(p => belongsToThisTeam(p._2, filter))


      var roomOnSquareCount = 0
      var innerWaitingRoomCount = 0
      var examinationCount = 0
      var otherCount = 0

      teamMap.foreach{ p =>
        getPlace(p._2) match {
          case "RoomOnSquare" => roomOnSquareCount += 1
          case "InnerWaitingRoom" => innerWaitingRoomCount += 1
          case "Examination" => examinationCount += 1
          case "Other" => otherCount += 1
        }
      }

      var placeMap: Map[String, Double] = Map(
        "RoomOnSquare" -> roomOnSquareCount,
        "InnerWaitingRoom" -> innerWaitingRoomCount,
        "Examination" -> examinationCount,
        "Other" -> otherCount
      )


      var length: Map[String, Double] = Map(
        "RoomOnSquare" -> 0,
        "InnerWaitingRoom" -> 0,
        "Examination" -> 0,
        "Other" -> 0
      )

      placeMap.foreach{ p =>
        val sum = placeMap("RoomOnSquare") + placeMap("InnerWaitingRoom") + placeMap("Examination") + placeMap("Other")
        if (sum == 0) {
          length += p._1 -> 0
        } else {
          length += p._1 -> (p._2/(sum))*width
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
      .attr("width", length("RoomOnSquare"))
      .attr("height", barHeight)
      .attr("fill", colorBarOne)

      svg.append("text")
      .attr("x", length("RoomOnSquare") - distance(placeMap("RoomOnSquare")))
      .attr("y", (75/419.0)*width)
      .attr("font-size", fontSize)
      .text(s"${removeZero(placeMap("RoomOnSquare"))}")
      .attr("fill", colorNumbersLight)
      // ----------- Graf två -------------
      g.append("rect")
      .attr("x", length("RoomOnSquare"))
      .attr("y", (50/419.0)*width)
      .attr("width", length("InnerWaitingRoom"))
      .attr("height", barHeight)
      .attr("fill", colorBarTwo)

      svg.append("text")
      .attr("x", length("RoomOnSquare") + length("InnerWaitingRoom") - distance(placeMap("InnerWaitingRoom")))
      .attr("y", (75/419.0)*width)
      .attr("font-size", fontSize)
      .text(s"${removeZero(placeMap("InnerWaitingRoom"))}")
      .attr("fill", colorNumbersLight)
      // ----------- Graf tre -------------
      g.append("rect")
      .attr("x", length("InnerWaitingRoom") + length("RoomOnSquare"))
      .attr("y", (50/419.0)*width)
      .attr("width", length("Examination"))
      .attr("height", barHeight)
      .attr("fill", colorBarThree)

      svg.append("text")
      .attr("x", length("RoomOnSquare") + length("InnerWaitingRoom") + length("Examination") - distance(placeMap("Examination")))
      .attr("y", (75/419.0)*width)
      .attr("font-size", fontSize)
      .text(s"${removeZero(placeMap("Examination"))}")
      .attr("fill", colorNumbersDark)
      // ----------- Graf fyra -------------
      g.append("rect")
      .attr("x", length("Examination") + length("InnerWaitingRoom") + length("RoomOnSquare"))
      .attr("y", (50/419.0)*width)
      .attr("width", length("Other"))
      .attr("height", barHeight)
      .attr("fill", colorBarFour)

      svg.append("text")
      .attr("x", length("RoomOnSquare") + length("InnerWaitingRoom") + length("Examination") + length("Other") - distance(placeMap("Other")))
      .attr("y", (75/419.0)*width)
      .attr("font-size", fontSize)
      .text(s"${removeZero(placeMap("Other"))}")
      .attr("fill", colorNumbersDark)
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
      .text("Rum på torg")
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
      .text("Inre väntrum")
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
      .text("Undersökning")
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
      .text("Annat")
      // ------------------------------------

      // ----------- PLATS --------------
      svg.append("text")
      .attr("x", 0)
      .attr("y", (20/419.0)*width)
      .attr("font-size", s"${(15/419.0)*width}pt")
      .text("PLATS")
      .attr("fill", "#95989a")
      // ---------------------------------
    }



    def apply() = spgui.SPWidget(spwb => {
      val currentTeam = extractTeam(spwb.frontEndState.attributes)
      component(currentTeam)
    })
  }
