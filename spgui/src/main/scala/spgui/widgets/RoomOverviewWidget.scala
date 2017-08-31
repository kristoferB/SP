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

object RoomOverviewWidget {

  private class Backend($: BackendScope[Unit, Map[String, apiPatient.Patient]]) {

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
      val json = ToAndFrom.make(SPHeader(from = "PatientCardsWidget", to = "WidgetService"), mess)
      BackendCommunication.publish(json, "widget-event")
    }

    // What is this function used for?
    def render(p: Map[String, apiPatient.Patient]) = {
      <.div(Styles.helveticaZ)
    }

    def onUnmount() = {
      println("Unmounting")
      patientObs.foreach(_.kill())
      wsObs.kill()
      Callback.empty
    }
  }

  private val component = ScalaComponent.builder[Unit]("KoordMapWidget")
  .initialState(Map("-1" ->
    EricaLogic.dummyPatient))
    .renderBackend[Backend]
    .componentDidMount(ctx => Callback(ctx.backend.setPatientObs()))
    .componentDidUpdate(ctx => Callback(addTheD3(ctx.getDOMNode, ctx.currentState)))
    .componentWillUnmount(_.backend.onUnmount())
    .build

    private def addTheD3(element: raw.Element, patients: Map[String, apiPatient.Patient]): Unit = {

      d3.select(element).selectAll("*").remove()

      var busyRoomsBuffer = new ListBuffer[String]()
      patients.foreach{ p =>
        if (p._1 != "-1") {
          if (p._2.location.roomNr != "" && p._2.location.roomNr != "ivr" && p._2.location != "yvr" && p._2.location != "iv" && p._2.location != "vr") {
            busyRoomsBuffer += p._2.location.roomNr.toLowerCase()
          }
        }
      }
      val busyRooms = busyRoomsBuffer.toList

      val width = element.clientWidth                      // Kroppens bredd
      val height = (1080/1372.0) * width     // Kroppens höjd
      val smallRec = (83.1/1372.0) * width   // Höjd samt bredd, små rektanglar
      val barDistance = (6.8/1372.0) * width

      val sizeNumbers = s"${(30/1372.0) * width}px"
      val sizeText = s"${(32/1372.0) * width}px"
      val barnGynOnh = s"${(22/1372.0) * width}px"

      // ------- Färger ---------
      val colorBarInfektion = "#a3a3a3"
      val colorBarDarkInfektion = "#858585"

      val colorBarYellow = "#fce466"
      val colorBarDarkYellow = "#d8b803"

      val colorBarBlue = "#59abfd"
      val colorBarDarkBlue = "#0060c1"

      val colorBarJour = "#ed9ce1"
      val colorBarDarkJour = "#a26594"

      val colorBarOrto = "#62d870"
      val colorBarDarkOrto = "#26aa68"

      val colorBarKirg = "#e67f7f"
      val colorBarDarkKirg = "#b12323"

      val colorNumbers = "#000000"
      val whiteNumbers = "#FFFFFF"
      val blackNumbers = "#000000"
      // -------------------------

      def checkNumbers(i: String):String = {
        if(busyRooms.contains(i)){whiteNumbers}
        else{blackNumbers}
      }

      def checkInfektion(i: String):String = {
        if(busyRooms.contains(i)){colorBarDarkInfektion}
        else{colorBarInfektion}
      }

      def checkYellow(i: String):String = {
        if(busyRooms.contains(i)){colorBarDarkYellow}
        else{colorBarYellow}
      }

      def checkBlue(i: String):String = {
        if(busyRooms.contains(i)){colorBarDarkBlue}
        else{colorBarBlue}
      }

      def checkJour(i: String):String = {
        if(busyRooms.contains(i)){colorBarDarkJour}
        else{colorBarJour}
      }

      def checkOrto(i: String):String = {
        if(busyRooms.contains(i)){colorBarDarkOrto}
        else{colorBarOrto}
      }

      def checkKirg(i: String):String = {
        if(busyRooms.contains(i)){colorBarDarkKirg}
        else{colorBarKirg}
      }


      val svg = d3.select(element).append("svg")
      .attr("width", width)
      .attr("height", height)
      val g = svg.append("g")
      // -------- TEXTER ----------
      svg.append("text")
      .attr("x", (20.5/1372.0)*width)
      .attr("y", (442.5/1372.0)*width)
      .attr("font-size", sizeText)
      .text("Infektion")
      .attr("fill", blackNumbers)
      .style("font-weight", "bold")

      svg.append("text")
      .attr("x", (75/1372.0)*width  + smallRec)
      .attr("y", (442.5/1372.0)*width + smallRec)
      .attr("font-size", sizeText)
      .text("Triage")
      .attr("fill", blackNumbers)
      .style("font-weight", "bold")

      svg.append("text")
      .attr("x", (410/1372.0)*width)
      .attr("y", (170/1372.0)*width)
      .attr("font-size", sizeText)
      .text("Medicin Gul")
      .attr("fill", blackNumbers)
      .style("font-weight", "bold")

      svg.append("text")
      .attr("x", (410/1372.0)*width)
      .attr("y", (480/1372.0)*width)
      .attr("font-size", sizeText)
      .text("Medicin Blå")
      .attr("fill", blackNumbers)
      .style("font-weight", "bold")

      svg.append("text")
      .attr("x", (860/1372.0)*width)
      .attr("y", (170/1372.0)*width)
      .attr("font-size", sizeText)
      .text("Jour")
      .attr("fill", blackNumbers)
      .style("font-weight", "bold")

      svg.append("text")
      .attr("x", (860/1372.0)*width)
      .attr("y", (200/1372.0)*width)
      .attr("font-size", barnGynOnh)
      .text("Barn")
      .attr("fill", blackNumbers)


      svg.append("text")
      .attr("x", (860/1372.0)*width)
      .attr("y", (230/1372.0)*width)
      .attr("font-size", barnGynOnh)
      .text("Gyn")
      .attr("fill", blackNumbers)


      svg.append("text")
      .attr("x", (860/1372.0)*width)
      .attr("y", (260/1372.0)*width)
      .attr("font-size", barnGynOnh)
      .text("ÖNH")
      .attr("fill", blackNumbers)

      svg.append("text")
      .attr("x", (980/1372.0)*width)
      .attr("y", (460/1372.0)*width)
      .attr("font-size", sizeText)
      .text("Ortopedi")
      .attr("fill", blackNumbers)
      .style("font-weight", "bold")


      svg.append("text")
      .attr("x", (486/1372.0)*width)
      .attr("y", (760/1372.0)*width)
      .attr("font-size", sizeText)
      .text("Akutrum")
      .attr("fill", blackNumbers)
      .style("font-weight", "bold")

      svg.append("text")
      .attr("x", ((939+(smallRec))/1372.0)*width)
      .attr("y", (740/1372.0)*width)
      .attr("font-size", sizeText)
      .text("Kirurgi")
      .attr("fill", blackNumbers)
      .attr("text-anchor", "middle")
      .style("font-weight", "bold")

      svg.append("text")
      .attr("x", ((939+(smallRec))/1372.0)*width)
      .attr("y", (775/1372.0)*width)
      .attr("font-size", sizeText)
      .text("Process")
      .attr("fill", blackNumbers)
      .attr("text-anchor", "middle")
      .style("font-weight", "bold")

      svg.append("text")
      .attr("x", ((939+(smallRec))/1372.0)*width)
      .attr("y", (810/1372.0)*width)
      .attr("font-size", sizeText)
      .text("Stream")
      .attr("fill", blackNumbers)
      .attr("text-anchor", "middle")
      .style("font-weight", "bold")


      // ----------- INFEKTION -------------
      g.append("rect")
      .attr("x", (45/1372.0)*width )
      .attr("y", (34.1/1372.0)*width)
      .attr("width", smallRec)
      .attr("height", smallRec)
      .attr("fill", checkInfektion("i1"))

      svg.append("text")
      .attr("x", (45/1372.0)*width + smallRec/2.5)
      .attr("y", (34.1/1372.0)*width + 2*smallRec/3)
      .attr("font-size", sizeNumbers)
      .text(s"${1}")
      .attr("fill", checkNumbers("i1"))
      .style("font-weight", "bold")

      g.append("rect")
      .attr("x", (45/1372.0)*width)
      .attr("y", (34.1/1372.0)*width + smallRec + barDistance)
      .attr("width", smallRec)
      .attr("height", smallRec)
      .attr("fill", checkInfektion("i2"))

      svg.append("text")
      .attr("x", (45/1372.0)*width + smallRec/2.5)
      .attr("y", (34.1/1372.0)*width + 2*smallRec/3 + smallRec + barDistance)
      .attr("font-size", sizeNumbers)
      .text(s"${2}")
      .attr("fill", checkNumbers("i2"))
      .style("font-weight", "bold")

      g.append("rect")
      .attr("x", (45/1372.0)*width)
      .attr("y", (34.1/1372.0)*width + 2*smallRec + 2*barDistance)
      .attr("width", smallRec)
      .attr("height", smallRec)
      .attr("fill", checkInfektion("i3"))

      svg.append("text")
      .attr("x", (45/1372.0)*width + smallRec/2.5)
      .attr("y", (34.1/1372.0)*width + 2*smallRec/3 + 2*smallRec + 2*barDistance)
      .attr("font-size", sizeNumbers)
      .text(s"${3}")
      .attr("fill", checkNumbers("i3"))
      .style("font-weight", "bold")

      g.append("rect")
      .attr("x", (45/1372.0)*width)
      .attr("y", (34.1/1372.0)*width + 3*smallRec + 3*barDistance)
      .attr("width", smallRec)
      .attr("height", smallRec)
      .attr("fill", checkInfektion("i4"))

      svg.append("text")
      .attr("x", (45/1372.0)*width + smallRec/2.5)
      .attr("y",  (34.1/1372.0)*width + 2*smallRec/3 + 3*smallRec + 3*barDistance)
      .attr("font-size", sizeNumbers)
      .text(s"${4}")
      .attr("fill", checkNumbers("i4"))
      .style("font-weight", "bold")

      // ---------------------------------------

      // ------------ TRIAGE ------------------
      g.append("rect")
      .attr("x", (162.9/1372.0)*width)
      .attr("y", (34.1/1372.0)*width)
      .attr("width", smallRec)
      .attr("height", smallRec)
      .attr("fill", checkInfektion("i5"))

      svg.append("text")
      .attr("x", (162.9/1372.0)*width + smallRec/2.5)
      .attr("y", (34.1/1372.0)*width + 2*smallRec/3)
      .attr("font-size", sizeNumbers)
      .text(s"${5}")
      .attr("fill", checkNumbers("i5"))
      .style("font-weight", "bold")

      g.append("rect")
      .attr("x", (162.9/1372.0)*width)
      .attr("y", (34.1/1372.0)*width + smallRec + barDistance)
      .attr("width", smallRec)
      .attr("height", smallRec)
      .attr("fill", checkInfektion("i6"))

      svg.append("text")
      .attr("x", (162.9/1372.0)*width + smallRec/2.5)
      .attr("y", (34.1/1372.0)*width + 2*smallRec/3 + smallRec + barDistance)
      .attr("font-size", sizeNumbers)
      .text(s"${6}")
      .attr("fill", checkNumbers("i6"))
      .style("font-weight", "bold")

      g.append("rect")
      .attr("x", (162.9/1372.0)*width)
      .attr("y", (34.1/1372.0)*width + 2*smallRec + 2*barDistance)
      .attr("width", smallRec)
      .attr("height", smallRec)
      .attr("fill", checkInfektion("i7"))

      svg.append("text")
      .attr("x", (162.9/1372.0)*width + smallRec/2.5)
      .attr("y", (34.1/1372.0)*width + 2*smallRec/3 + 2*smallRec + 2*barDistance)
      .attr("font-size", sizeNumbers)
      .text(s"${7}")
      .attr("fill", checkNumbers("i7"))
      .style("font-weight", "bold")

      g.append("rect")
      .attr("x", (162.9/1372.0)*width)
      .attr("y", (34.1/1372.0)*width + 3*smallRec + 3*barDistance)
      .attr("width", smallRec)
      .attr("height", smallRec)
      .attr("fill", checkInfektion("i8"))

      svg.append("text")
      .attr("x", (162.9/1372.0)*width + smallRec/2.5)
      .attr("y",  (34.1/1372.0)*width + 2*smallRec/3 + 3*smallRec + 3*barDistance)
      .attr("font-size", sizeNumbers)
      .text(s"${8}")
      .attr("fill", checkNumbers("i8"))
      .style("font-weight", "bold")

      g.append("rect")
      .attr("x", (162.9/1372.0)*width)
      .attr("y", (34.1/1372.0)*width + 4*smallRec + 4*barDistance)
      .attr("width", smallRec)
      .attr("height", smallRec)
      .attr("fill", checkInfektion("i9"))

      svg.append("text")
      .attr("x", (162.9/1372.0)*width + smallRec/2.5)
      .attr("y",  (34.1/1372.0)*width + 2*smallRec/3 + 4*smallRec + 4*barDistance)
      .attr("font-size", sizeNumbers)
      .text(s"${9}")
      .attr("fill", checkNumbers("i9"))
      .style("font-weight", "bold")

      // -------------------------------------------------------------------------

      // --------------------------- MEDICIN GUL ------------------------------------
      g.append("rect")
      .attr("x", (280.9/1372.0)*width)
      .attr("y", (34.1/1372.0)*width)
      .attr("width", smallRec)
      .attr("height", smallRec)
      .attr("fill", checkYellow(12.toString))

      svg.append("text")
      .attr("x", (285.9/1372.0)*width + smallRec/4)
      .attr("y", (34.1/1372.0)*width + 2*smallRec/3)
      .attr("font-size", sizeNumbers)
      .text(s"${12}")
      .attr("fill", checkNumbers(12.toString))
      .style("font-weight", "bold")

      g.append("rect")
      .attr("x", (280.9/1372.0)*width)
      .attr("y", (34.1/1372.0)*width + smallRec + barDistance)
      .attr("width", smallRec)
      .attr("height", smallRec)
      .attr("fill", checkYellow(11.toString))

      svg.append("text")
      .attr("x", (285.9/1372.0)*width + smallRec/4)
      .attr("y", (34.1/1372.0)*width + 2*smallRec/3 + smallRec + barDistance)
      .attr("font-size", sizeNumbers)
      .text(s"${11}")
      .attr("fill", checkNumbers(11.toString))
      .style("font-weight", "bold")

      g.append("rect")
      .attr("x", (280.9/1372.0)*width)
      .attr("y", (34.1/1372.0)*width + 2*smallRec + 2*barDistance)
      .attr("width", smallRec)
      .attr("height", smallRec)
      .attr("fill", checkYellow(10.toString))

      svg.append("text")
      .attr("x", (285.9/1372.0)*width + smallRec/4)
      .attr("y", (34.1/1372.0)*width + 2*smallRec/3 + 2*smallRec + 2*barDistance)
      .attr("font-size", sizeNumbers)
      .text(s"${10}")
      .attr("fill", checkNumbers(10.toString))
      .style("font-weight", "bold")

      g.append("rect")
      .attr("x", (280.9/1372.0)*width + smallRec + barDistance)
      .attr("y", (34.1/1372.0)*width)
      .attr("width", smallRec)
      .attr("height", smallRec)
      .attr("fill", checkYellow(13.toString))

      svg.append("text")
      .attr("x", (285.9/1372.0)*width + smallRec/4 + smallRec + barDistance)
      .attr("y", (34.1/1372.0)*width + 2*smallRec/3)
      .attr("font-size", sizeNumbers)
      .text(s"${13}")
      .attr("fill", checkNumbers(13.toString))
      .style("font-weight", "bold")

      g.append("rect")
      .attr("x", (280.9/1372.0)*width + 2*smallRec + 2*barDistance)
      .attr("y", (34.1/1372.0)*width)
      .attr("width", smallRec)
      .attr("height", smallRec)
      .attr("fill", checkYellow(14.toString))

      svg.append("text")
      .attr("x", (285.9/1372.0)*width + smallRec/4 + 2*smallRec + 2*barDistance)
      .attr("y", (34.1/1372.0)*width + 2*smallRec/3)
      .attr("font-size", sizeNumbers)
      .text(s"${14}")
      .attr("fill", checkNumbers(14.toString))
      .style("font-weight", "bold")

      g.append("rect")
      .attr("x", (280.9/1372.0)*width + 3*smallRec + 3*barDistance)
      .attr("y", (34.1/1372.0)*width)
      .attr("width", smallRec)
      .attr("height", smallRec)
      .attr("fill", checkYellow(15.toString))

      svg.append("text")
      .attr("x", (285.9/1372.0)*width + smallRec/4 + 3*smallRec + 3*barDistance)
      .attr("y", (34.1/1372.0)*width + 2*smallRec/3)
      .attr("font-size", sizeNumbers)
      .text(s"${15}")
      .attr("fill", checkNumbers(15.toString))
      .style("font-weight", "bold")

      g.append("rect")
      .attr("x", (280.9/1372.0)*width + 4*smallRec + 4*barDistance)
      .attr("y", (34.1/1372.0)*width)
      .attr("width", smallRec)
      .attr("height", smallRec)
      .attr("fill", checkYellow(16.toString))

      svg.append("text")
      .attr("x", (285.9/1372.0)*width + smallRec/4 + 4*smallRec + 4*barDistance)
      .attr("y", (34.1/1372.0)*width + 2*smallRec/3)
      .attr("font-size", sizeNumbers)
      .text(s"${16}")
      .attr("fill", checkNumbers(16.toString))
      .style("font-weight", "bold")

      g.append("rect")
      .attr("x", (280.9/1372.0)*width + 4*barDistance + 4*smallRec)
      .attr("y", (34.1/1372.0)*width + smallRec + barDistance)
      .attr("width", smallRec)
      .attr("height", smallRec)
      .attr("fill", checkYellow(17.toString))

      svg.append("text")
      .attr("x", (285.9/1372.0)*width + smallRec/4 + 4* barDistance + 4 * smallRec )
      .attr("y", (34.1/1372.0)*width + 2*smallRec/3 + smallRec + barDistance)
      .attr("font-size", sizeNumbers)
      .text(s"${17}")
      .attr("fill", checkNumbers(17.toString))
      .style("font-weight", "bold")

      g.append("rect")
      .attr("x", (280.9/1372.0)*width + 4*barDistance + 4*smallRec)
      .attr("y", (34.1/1372.0)*width + 2*smallRec + 2*barDistance)
      .attr("width", smallRec)
      .attr("height", smallRec)
      .attr("fill", checkYellow(18.toString))

      svg.append("text")
      .attr("x", (285.9/1372.0)*width + smallRec/4 + 4*barDistance + 4*smallRec)
      .attr("y", (34.1/1372.0)*width + 2*smallRec/3 + 2*smallRec + 2*barDistance)
      .attr("font-size", sizeNumbers)
      .text(s"${18}")
      .attr("fill", checkNumbers(18.toString))
      .style("font-weight", "bold")

      // ---------------------------------------------------------------------------

      //----------------------------  MEDICIN BLÅ ----------------------------------

      g.append("rect")
      .attr("x", (280.9/1372.0)*width)
      .attr("y", (71.2/1372.0)*width + 3*smallRec + 2*barDistance)
      .attr("width", smallRec)
      .attr("height", smallRec)
      .attr("fill", checkBlue(27.toString))

      svg.append("text")
      .attr("x", (285.9/1372.0)*width + smallRec/4)
      .attr("y", (71.2/1372.0)*width + 2*smallRec/3 + 3*smallRec + 2*barDistance)
      .attr("font-size", sizeNumbers)
      .text(s"${27}")
      .attr("fill", checkNumbers(27.toString))
      .style("font-weight", "bold")

      g.append("rect")
      .attr("x", (280.9/1372.0)*width)
      .attr("y", (71.2/1372.0)*width + 4*smallRec + 3*barDistance)
      .attr("width", smallRec)
      .attr("height", smallRec)
      .attr("fill", checkBlue(26.toString))

      svg.append("text")
      .attr("x", (285.9/1372.0)*width + smallRec/4)
      .attr("y", (71.2/1372.0)*width + 2*smallRec/3 + 4*smallRec + 3*barDistance)
      .attr("font-size", sizeNumbers)
      .text(s"${26}")
      .attr("fill", checkNumbers(26.toString))
      .style("font-weight", "bold")

      g.append("rect")
      .attr("x", (280.9/1372.0)*width)
      .attr("y", (71.2/1372.0)*width + 5*smallRec + 4*barDistance)
      .attr("width", smallRec)
      .attr("height", smallRec)
      .attr("fill", checkBlue(25.toString))

      svg.append("text")
      .attr("x", (285.9/1372.0)*width + smallRec/4)
      .attr("y", (71.2/1372.0)*width + 2*smallRec/3 + 5*smallRec + 4*barDistance)
      .attr("font-size", sizeNumbers)
      .text(s"${25}")
      .attr("fill", checkNumbers(25.toString))
      .style("font-weight", "bold")

      g.append("rect")
      .attr("x", (280.9/1372.0)*width + smallRec + barDistance)
      .attr("y", (71.2/1372.0)*width + 5*smallRec + 4*barDistance)
      .attr("width", smallRec)
      .attr("height", smallRec)
      .attr("fill", checkBlue(24.toString))

      svg.append("text")
      .attr("x", (285.9/1372.0)*width + smallRec/4 + smallRec + barDistance)
      .attr("y", (71.2/1372.0)*width + 2*smallRec/3 + 5*smallRec + 4*barDistance)
      .attr("font-size", sizeNumbers)
      .text(s"${24}")
      .attr("fill", checkNumbers(24.toString))
      .style("font-weight", "bold")

      g.append("rect")
      .attr("x", (280.9/1372.0)*width + 2*smallRec + 2*barDistance)
      .attr("y", (71.2/1372.0)*width + 5*smallRec + 4*barDistance)
      .attr("width", smallRec)
      .attr("height", smallRec)
      .attr("fill", checkBlue(23.toString))

      svg.append("text")
      .attr("x", (285.9/1372.0)*width + smallRec/4 + 2*smallRec + 2*barDistance)
      .attr("y", (71.2/1372.0)*width + 2*smallRec/3 + 5*smallRec + 4*barDistance)
      .attr("font-size", sizeNumbers)
      .text(s"${23}")
      .attr("fill", checkNumbers(23.toString))
      .style("font-weight", "bold")

      g.append("rect")
      .attr("x", (280.9/1372.0)*width + 3*smallRec + 3*barDistance)
      .attr("y", (71.2/1372.0)*width + 5*smallRec + 4*barDistance)
      .attr("width", smallRec)
      .attr("height", smallRec)
      .attr("fill", checkBlue(22.toString))

      svg.append("text")
      .attr("x", (285.9/1372.0)*width + smallRec/4 + 3*smallRec + 3*barDistance)
      .attr("y", (71.2/1372.0)*width + 2*smallRec/3 + 5*smallRec + 4*barDistance)
      .attr("font-size", sizeNumbers)
      .text(s"${22}")
      .attr("fill", checkNumbers(22.toString))
      .style("font-weight", "bold")

      g.append("rect")
      .attr("x", (280.9/1372.0)*width + 4*smallRec + 4*barDistance)
      .attr("y", (71.2/1372.0)*width + 5*smallRec + 4*barDistance)
      .attr("width", smallRec)
      .attr("height", smallRec)
      .attr("fill", checkBlue(21.toString))

      svg.append("text")
      .attr("x", (285.9/1372.0)*width + smallRec/4 + 4*smallRec + 4*barDistance)
      .attr("y", (71.2/1372.0)*width + 2*smallRec/3 + 5*smallRec + 4*barDistance)
      .attr("font-size", sizeNumbers)
      .text(s"${21}")
      .attr("fill", checkNumbers(21.toString))
      .style("font-weight", "bold")

      g.append("rect")
      .attr("x", (280.9/1372.0)*width + 4*smallRec + 4*barDistance)
      .attr("y", (71.2/1372.0)*width + 4*smallRec + 3*barDistance)
      .attr("width", smallRec)
      .attr("height", smallRec)
      .attr("fill", checkBlue(20.toString))

      svg.append("text")
      .attr("x", (285.9/1372.0)*width + smallRec/4 + 4*smallRec + 4*barDistance)
      .attr("y", (71.2/1372.0)*width + 2*smallRec/3 + 4*smallRec + 3*barDistance)
      .attr("font-size", sizeNumbers)
      .text(s"${20}")
      .attr("fill", checkNumbers(20.toString))
      .style("font-weight", "bold")

      g.append("rect")
      .attr("x", (280.9/1372.0)*width + 4*smallRec + 4*barDistance)
      .attr("y", (71.2/1372.0)*width + 3*smallRec + 2*barDistance)
      .attr("width", smallRec)
      .attr("height", smallRec)
      .attr("fill", checkBlue(19.toString))

      svg.append("text")
      .attr("x", (285.9/1372.0)*width + smallRec/4 + 4*smallRec + 4*barDistance)
      .attr("y", (71.2/1372.0)*width + 2*smallRec/3 + 3*smallRec + 2*barDistance)
      .attr("font-size", sizeNumbers)
      .text(s"${19}")
      .attr("fill", checkNumbers(19.toString))
      .style("font-weight", "bold")

      // ----------------------------------------------------------------------------

      // -------------------------------- AKUTRUM -----------------------------------

      g.append("rect")
      .attr("x", (280.9/1372.0)*width + smallRec + barDistance)
      .attr("y", (103.7/1372.0)*width + 6*smallRec + 4*barDistance)
      .attr("width", smallRec)
      .attr("height", smallRec)
      .attr("fill", checkInfektion("a4"))

      svg.append("text")
      .attr("x", (285.9/1372.0)*width + smallRec/4 + smallRec + barDistance)
      .attr("y", (103.7/1372.0)*width + 2*smallRec/3 + 6*smallRec + 4*barDistance)
      .attr("font-size", sizeNumbers)
      .text(s"A4")
      .attr("fill", checkNumbers("a4"))
      .style("font-weight", "bold")

      g.append("rect")
      .attr("x", (280.9/1372.0)*width + 2*smallRec + 2*barDistance)
      .attr("y", (103.7/1372.0)*width + 6*smallRec + 4*barDistance)
      .attr("width", smallRec)
      .attr("height", smallRec)
      .attr("fill", checkInfektion("a3"))

      svg.append("text")
      .attr("x", (285.9/1372.0)*width + smallRec/4 + 2*smallRec + 2*barDistance)
      .attr("y", (103.7/1372.0)*width + 2*smallRec/3 + 6*smallRec + 4*barDistance)
      .attr("font-size", sizeNumbers)
      .text(s"A3")
      .attr("fill", checkNumbers("a3"))
      .style("font-weight", "bold")

      g.append("rect")
      .attr("x", (280.9/1372.0)*width + 3*smallRec + 3*barDistance)
      .attr("y", (103.7/1372.0)*width + 6*smallRec + 4*barDistance)
      .attr("width", smallRec)
      .attr("height", smallRec)
      .attr("fill", checkInfektion("a2"))

      svg.append("text")
      .attr("x", (285.9/1372.0)*width + smallRec/4 + 3*smallRec + 3*barDistance)
      .attr("y", (103.7/1372.0)*width + 2*smallRec/3 + 6*smallRec + 4*barDistance)
      .attr("font-size", sizeNumbers)
      .text(s"A2")
      .attr("fill", checkNumbers("a2"))
      .style("font-weight", "bold")

      g.append("rect")
      .attr("x", (280.9/1372.0)*width + 4*smallRec + 4*barDistance)
      .attr("y", (103.7/1372.0)*width + 6*smallRec + 4*barDistance)
      .attr("width", smallRec)
      .attr("height", smallRec)
      .attr("fill", checkInfektion("a1"))

      svg.append("text")
      .attr("x", (285.9/1372.0)*width + smallRec/4 + 4*smallRec + 4*barDistance)
      .attr("y", (103.7/1372.0)*width + 2*smallRec/3 + 6*smallRec + 4*barDistance)
      .attr("font-size", sizeNumbers)
      .text(s"A1")
      .attr("fill", checkNumbers("a1"))
      .style("font-weight", "bold")

      // -----------------------------------------------------------------------------

      // ------------------------------------ JOUR -----------------------------------

      g.append("rect")
      .attr("x", (759.4/1372.0)*width + smallRec + barDistance)
      .attr("y", (34.1/1372.0)*width)
      .attr("width", smallRec)
      .attr("height", smallRec)
      .attr("fill", checkJour(35.toString))

      svg.append("text")
      .attr("x", (764.4/1372.0)*width + smallRec/4 + smallRec + barDistance)
      .attr("y", (34.1/1372.0)*width + 2*smallRec/3)
      .attr("font-size", sizeNumbers)
      .text(s"${35}")
      .attr("fill", checkNumbers(35.toString))
      .style("font-weight", "bold")

      g.append("rect")
      .attr("x", (759.4/1372.0)*width)
      .attr("y", (34.1/1372.0)*width)
      .attr("width", smallRec)
      .attr("height", smallRec)
      .attr("fill", checkJour(34.toString))

      svg.append("text")
      .attr("x", (764.4/1372.0)*width + smallRec/4)
      .attr("y", (34.1/1372.0)*width + 2*smallRec/3)
      .attr("font-size", sizeNumbers)
      .text(s"${34}")
      .attr("fill", checkNumbers(34.toString))
      .style("font-weight", "bold")

      g.append("rect")
      .attr("x", (759.4/1372.0)*width)
      .attr("y", (34.1/1372.0)*width + smallRec + barDistance)
      .attr("width", smallRec)
      .attr("height", smallRec)
      .attr("fill", checkJour(33.toString))

      svg.append("text")
      .attr("x", (764.4/1372.0)*width + smallRec/4)
      .attr("y", (34.1/1372.0)*width + 2*smallRec/3 + smallRec + barDistance)
      .attr("font-size", sizeNumbers)
      .text(s"${33}")
      .attr("fill", checkNumbers(33.toString))
      .style("font-weight", "bold")

      g.append("rect")
      .attr("x", (759.4/1372.0)*width)
      .attr("y", (34.1/1372.0)*width + 2*smallRec + 2*barDistance)
      .attr("width", smallRec)
      .attr("height", smallRec)
      .attr("fill", checkJour(32.toString))

      svg.append("text")
      .attr("x", (764.4/1372.0)*width + smallRec/4)
      .attr("y", (34.1/1372.0)*width + 2*smallRec/3 + 2*smallRec + 2*barDistance)
      .attr("font-size", sizeNumbers)
      .text(s"${32}")
      .attr("fill", checkNumbers(32.toString))
      .style("font-weight", "bold")

      g.append("rect")
      .attr("x", (759.4/1372.0)*width)
      .attr("y", (34.1/1372.0)*width + 3*smallRec + 3*barDistance)
      .attr("width", smallRec)
      .attr("height", smallRec)
      .attr("fill", checkJour(31.toString))

      svg.append("text")
      .attr("x", (764.4/1372.0)*width + smallRec/4)
      .attr("y", (34.1/1372.0)*width + 2*smallRec/3 + 3*smallRec + 3*barDistance)
      .attr("font-size", sizeNumbers)
      .text(s"${31}")
      .attr("fill", checkNumbers(31.toString))
      .style("font-weight", "bold")

      g.append("rect")
      .attr("x", (759.4/1372.0)*width)
      .attr("y", (34.1/1372.0)*width + 4*smallRec + 4*barDistance)
      .attr("width", smallRec)
      .attr("height", smallRec)
      .attr("fill", checkJour(30.toString))

      svg.append("text")
      .attr("x", (764.4/1372.0)*width + smallRec/4)
      .attr("y", (34.1/1372.0)*width + 2*smallRec/3 + 4*smallRec + 4*barDistance)
      .attr("font-size", sizeNumbers)
      .text(s"${30}")
      .attr("fill", checkNumbers(30.toString))
      .style("font-weight", "bold")

      g.append("rect")
      .attr("x", (759.4/1372.0)*width)
      .attr("y", (34.1/1372.0)*width + 5*smallRec + 5*barDistance)
      .attr("width", smallRec)
      .attr("height", smallRec)
      .attr("fill", checkJour(46.toString))

      svg.append("text")
      .attr("x", (764.4/1372.0)*width + smallRec/4)
      .attr("y", (34.1/1372.0)*width + 2*smallRec/3 + 5*smallRec + 5*barDistance)
      .attr("font-size", sizeNumbers)
      .text(s"${46}")
      .attr("fill", checkNumbers(46.toString))
      .style("font-weight", "bold")

      //_---------------------------------------------------------------------------

      // ---------------------------- ORTOPEDI -------------------------------------
      g.append("rect")
      .attr("x", (967.2/1372.0)*width)
      .attr("y", (34.1/1372.0)*width)
      .attr("width", smallRec)
      .attr("height", smallRec)
      .attr("fill", checkOrto(36.toString))

      svg.append("text")
      .attr("x", (972.2/1372.0)*width + smallRec/4)
      .attr("y", (34.1/1372.0)*width + 2*smallRec/3)
      .attr("font-size", sizeNumbers)
      .text(s"${36}")
      .attr("fill", checkNumbers(36.toString))
      .style("font-weight", "bold")

      g.append("rect")
      .attr("x", (967.2/1372.0)*width + smallRec + barDistance)
      .attr("y", (34.1/1372.0)*width)
      .attr("width", smallRec)
      .attr("height", smallRec)
      .attr("fill", checkOrto(37.toString))

      svg.append("text")
      .attr("x", (972.2/1372.0)*width + smallRec/4 + smallRec + barDistance)
      .attr("y", (34.1/1372.0)*width + 2*smallRec/3)
      .attr("font-size", sizeNumbers)
      .text(s"${37}")
      .attr("fill", checkNumbers(37.toString))
      .style("font-weight", "bold")

      g.append("rect")
      .attr("x", (967.2/1372.0)*width + 2*smallRec + 2*barDistance)
      .attr("y", (34.1/1372.0)*width)
      .attr("width", smallRec)
      .attr("height", smallRec)
      .attr("fill", checkOrto(38.toString))

      svg.append("text")
      .attr("x", (972.2/1372.0)*width + smallRec/4 + 2*smallRec + 2*barDistance)
      .attr("y", (34.1/1372.0)*width + 2*smallRec/3)
      .attr("font-size", sizeNumbers)
      .text(s"${38}")
      .attr("fill", checkNumbers(38.toString))
      .style("font-weight", "bold")

      g.append("rect")
      .attr("x", (967.2/1372.0)*width + 2*smallRec + 2*barDistance)
      .attr("y", (34.1/1372.0)*width + smallRec + barDistance)
      .attr("width", smallRec)
      .attr("height", smallRec)
      .attr("fill", checkOrto(39.toString))

      svg.append("text")
      .attr("x", (972.2/1372.0)*width + smallRec/4 + 2*smallRec + 2*barDistance)
      .attr("y", (34.1/1372.0)*width + 2*smallRec/3 + smallRec + barDistance)
      .attr("font-size", sizeNumbers)
      .text(s"${39}")
      .attr("fill", checkNumbers(39.toString))
      .style("font-weight", "bold")

      g.append("rect")
      .attr("x", (967.2/1372.0)*width + 2*smallRec + 2*barDistance)
      .attr("y", (34.1/1372.0)*width + 2*smallRec + 2*barDistance)
      .attr("width", smallRec)
      .attr("height", smallRec)
      .attr("fill", checkOrto(40.toString))

      svg.append("text")
      .attr("x", (972.2/1372.0)*width + smallRec/4 + 2*smallRec + 2*barDistance)
      .attr("y", (34.1/1372.0)*width + 2*smallRec/3 + 2*smallRec + 2*barDistance)
      .attr("font-size", sizeNumbers)
      .text(s"${40}")
      .attr("fill", checkNumbers(40.toString))
      .style("font-weight", "bold")

      g.append("rect")
      .attr("x", (967.2/1372.0)*width + 2*smallRec + 2*barDistance)
      .attr("y", (34.1/1372.0)*width + 3*smallRec + 3*barDistance)
      .attr("width", smallRec)
      .attr("height", smallRec)
      .attr("fill", checkOrto(41.toString))

      svg.append("text")
      .attr("x", (972.2/1372.0)*width + smallRec/4 + 2*smallRec + 2*barDistance)
      .attr("y", (34.1/1372.0)*width + 2*smallRec/3 + 3*smallRec + 3*barDistance)
      .attr("font-size", sizeNumbers)
      .text(s"${41}")
      .attr("fill", checkNumbers(41.toString))
      .style("font-weight", "bold")

      g.append("rect")
      .attr("x", (967.2/1372.0)*width + 2*smallRec + 2*barDistance)
      .attr("y", (34.1/1372.0)*width + 4*smallRec + 4*barDistance)
      .attr("width", smallRec)
      .attr("height", smallRec)
      .attr("fill", checkOrto(42.toString))

      svg.append("text")
      .attr("x", (972.2/1372.0)*width + smallRec/4 + 2*smallRec + 2*barDistance)
      .attr("y", (34.1/1372.0)*width + 2*smallRec/3 + 4*smallRec + 4*barDistance)
      .attr("font-size", sizeNumbers)
      .text(s"${42}")
      .attr("fill", checkNumbers(42.toString))
      .style("font-weight", "bold")

      g.append("rect")
      .attr("x", (967.2/1372.0)*width + 2*smallRec + 2*barDistance)
      .attr("y", (34.1/1372.0)*width + 5*smallRec + 5*barDistance)
      .attr("width", smallRec)
      .attr("height", smallRec)
      .attr("fill", checkOrto(43.toString))

      svg.append("text")
      .attr("x", (972.2/1372.0)*width + smallRec/4 + 2*smallRec + 2*barDistance)
      .attr("y", (34.1/1372.0)*width + 2*smallRec/3 + 5*smallRec + 5*barDistance)
      .attr("font-size", sizeNumbers)
      .text(s"${43}")
      .attr("fill", checkNumbers(43.toString))
      .style("font-weight", "bold")

      g.append("rect")
      .attr("x", (967.2/1372.0)*width + smallRec + barDistance)
      .attr("y", (34.1/1372.0)*width + 5*smallRec + 5*barDistance)
      .attr("width", smallRec)
      .attr("height", smallRec)
      .attr("fill", checkOrto(44.toString))

      svg.append("text")
      .attr("x", (972.2/1372.0)*width + smallRec/4 + smallRec + barDistance)
      .attr("y", (34.1/1372.0)*width + 2*smallRec/3 + 5*smallRec + 5*barDistance)
      .attr("font-size", sizeNumbers)
      .text(s"${44}")
      .attr("fill", checkNumbers(44.toString))
      .style("font-weight", "bold")

      g.append("rect")
      .attr("x", (967.2/1372.0)*width)
      .attr("y", (34.1/1372.0)*width + 5*smallRec + 5*barDistance)
      .attr("width", smallRec)
      .attr("height", smallRec)
      .attr("fill", checkOrto(45.toString))

      svg.append("text")
      .attr("x", (972.2/1372.0)*width + smallRec/4)
      .attr("y", (34.1/1372.0)*width + 2*smallRec/3 + 5*smallRec + 5*barDistance)
      .attr("font-size", sizeNumbers)
      .text(s"${45}")
      .attr("fill", checkNumbers(45.toString))
      .style("font-weight", "bold")

      g.append("rect")
      .attr("x", (1249.1/1372.0)*width)
      .attr("y", (34.1/1372.0)*width)
      .attr("width", smallRec)
      .attr("height", smallRec)
      .attr("fill", checkOrto("47a"))

      svg.append("text")
      .attr("x", (1254.1/1372.0)*width + smallRec/12)
      .attr("y", (34.1/1372.0)*width + 2*smallRec/3)
      .attr("font-size", sizeNumbers)
      .text(s"47A")
      .attr("fill", checkNumbers("47a"))
      .style("font-weight", "bold")

      g.append("rect")
      .attr("x", (1249.1/1372.0)*width)
      .attr("y", (34.1/1372.0)*width + smallRec + barDistance)
      .attr("width", smallRec)
      .attr("height", smallRec)
      .attr("fill", checkOrto("47b"))

      svg.append("text")
      .attr("x", (1254.1/1372.0)*width + smallRec/12)
      .attr("y", (34.1/1372.0)*width + 2*smallRec/3 + smallRec + barDistance)
      .attr("font-size", sizeNumbers)
      .text(s"47B")
      .attr("fill", checkNumbers("47b"))
      .style("font-weight", "bold")

      g.append("rect")
      .attr("x", (1249.1/1372.0)*width)
      .attr("y", (34.1/1372.0)*width + 2*smallRec + 2*barDistance)
      .attr("width", smallRec)
      .attr("height", smallRec)
      .attr("fill", checkOrto("48a"))

      svg.append("text")
      .attr("x", (1254.1/1372.0)*width + smallRec/12)
      .attr("y", (34.1/1372.0)*width + 2*smallRec/3 + 2*smallRec + 2*barDistance)
      .attr("font-size", sizeNumbers)
      .text(s"48A")
      .attr("fill", checkNumbers("48a"))
      .style("font-weight", "bold")

      g.append("rect")
      .attr("x", (1249.1/1372.0)*width)
      .attr("y", (34.1/1372.0)*width + 3*smallRec + 3*barDistance)
      .attr("width", smallRec)
      .attr("height", smallRec)
      .attr("fill", checkOrto("48b"))

      svg.append("text")
      .attr("x", (1254.1/1372.0)*width + smallRec/12)
      .attr("y", (34.1/1372.0)*width + 2*smallRec/3 + 3*smallRec + 3*barDistance)
      .attr("font-size", sizeNumbers)
      .text(s"48B")
      .attr("fill", checkNumbers("48b"))
      .style("font-weight", "bold")

      // -----------------------------------------------------------------------------

      // ---------------------------- KIRURG -----------------------------------------
      g.append("rect")
      .attr("x", (759.4/1372.0)*width + 4*smallRec + 4*barDistance)
      .attr("y", (603.6/1372.0)*width + 4*smallRec + 4*barDistance)
      .attr("width", smallRec)
      .attr("height", smallRec)
      .attr("fill", checkKirg(63.toString))

      svg.append("text")
      .attr("x", (764.4/1372.0)*width + smallRec/4 + 4*smallRec + 4*barDistance)
      .attr("y", (603.6/1372.0)*width + 2*smallRec/3 + 4*smallRec + 4*barDistance)
      .attr("font-size", sizeNumbers)
      .text(s"${63}")
      .attr("fill", checkNumbers(63.toString))
      .style("font-weight", "bold")

      g.append("rect")
      .attr("x", (759.4/1372.0)*width + 4*smallRec + 4*barDistance)
      .attr("y", (603.6/1372.0)*width + 3*smallRec + 3*barDistance)
      .attr("width", smallRec)
      .attr("height", smallRec)
      .attr("fill", checkKirg(62.toString))

      svg.append("text")
      .attr("x", (764.4/1372.0)*width + smallRec/4 + 4*smallRec + 4*barDistance)
      .attr("y", (603.6/1372.0)*width + 2*smallRec/3 + 3*smallRec + 3*barDistance)
      .attr("font-size", sizeNumbers)
      .text(s"${62}")
      .attr("fill", checkNumbers(62.toString))
      .style("font-weight", "bold")

      g.append("rect")
      .attr("x", (759.4/1372.0)*width + 4*smallRec + 4*barDistance)
      .attr("y", (603.6/1372.0)*width + 2*smallRec + 2*barDistance)
      .attr("width", smallRec)
      .attr("height", smallRec)
      .attr("fill", checkKirg(61.toString))

      svg.append("text")
      .attr("x", (764.4/1372.0)*width + smallRec/4 + 4*smallRec + 4*barDistance)
      .attr("y", (603.6/1372.0)*width + 2*smallRec/3 + 2*smallRec + 2*barDistance)
      .attr("font-size", sizeNumbers)
      .text(s"${61}")
      .attr("fill", checkNumbers(61.toString))
      .style("font-weight", "bold")

      g.append("rect")
      .attr("x", (759.4/1372.0)*width + 4*smallRec + 4*barDistance)
      .attr("y", (603.6/1372.0)*width + smallRec + barDistance)
      .attr("width", smallRec)
      .attr("height", smallRec)
      .attr("fill", checkKirg(60.toString))

      svg.append("text")
      .attr("x", (764.4/1372.0)*width + smallRec/4 + 4*smallRec + 4*barDistance)
      .attr("y", (603.6/1372.0)*width + 2*smallRec/3 + smallRec + barDistance)
      .attr("font-size", sizeNumbers)
      .text(s"${60}")
      .attr("fill", checkNumbers(60.toString))
      .style("font-weight", "bold")

      g.append("rect")
      .attr("x", (759.4/1372.0)*width + 5*smallRec + 5*barDistance)
      .attr("y", (603.6/1372.0)*width)
      .attr("width", smallRec)
      .attr("height", smallRec)
      .attr("fill", checkKirg(59.toString))

      svg.append("text")
      .attr("x", (764.4/1372.0)*width + smallRec/4 + 5*smallRec + 5*barDistance)
      .attr("y", (603.6/1372.0)*width + 2*smallRec/3)
      .attr("font-size", sizeNumbers)
      .text(s"${59}")
      .attr("fill", checkNumbers(59.toString))
      .style("font-weight", "bold")

      g.append("rect")
      .attr("x", (759.4/1372.0)*width + 4*smallRec + 4*barDistance)
      .attr("y", (603.6/1372.0)*width)
      .attr("width", smallRec)
      .attr("height", smallRec)
      .attr("fill", checkKirg(58.toString))

      svg.append("text")
      .attr("x", (764.4/1372.0)*width + smallRec/4 + 4*smallRec + 4*barDistance)
      .attr("y", (603.6/1372.0)*width + 2*smallRec/3)
      .attr("font-size", sizeNumbers)
      .text(s"${58}")
      .attr("fill", checkNumbers(58.toString))
      .style("font-weight", "bold")

      g.append("rect")
      .attr("x", (759.4/1372.0)*width + 3*smallRec + 3*barDistance)
      .attr("y", (603.6/1372.0)*width)
      .attr("width", smallRec)
      .attr("height", smallRec)
      .attr("fill", checkKirg(57.toString))

      svg.append("text")
      .attr("x", (764.4/1372.0)*width + smallRec/4 + 3*smallRec + 3*barDistance)
      .attr("y", (603.6/1372.0)*width + 2*smallRec/3)
      .attr("font-size", sizeNumbers)
      .text(s"${57}")
      .attr("fill", checkNumbers(57.toString))
      .style("font-weight", "bold")

      g.append("rect")
      .attr("x", (759.4/1372.0)*width + 2*smallRec + 2*barDistance)
      .attr("y", (603.6/1372.0)*width)
      .attr("width", smallRec)
      .attr("height", smallRec)
      .attr("fill", checkKirg(56.toString))

      svg.append("text")
      .attr("x", (764.4/1372.0)*width + smallRec/4 + 2*smallRec + 2*barDistance)
      .attr("y", (603.6/1372.0)*width + 2*smallRec/3)
      .attr("font-size", sizeNumbers)
      .text(s"${56}")
      .attr("fill", checkNumbers(56.toString))
      .style("font-weight", "bold")

      g.append("rect")
      .attr("x", (759.4/1372.0)*width + smallRec + barDistance)
      .attr("y", (603.6/1372.0)*width)
      .attr("width", smallRec)
      .attr("height", smallRec)
      .attr("fill", checkKirg(55.toString))

      svg.append("text")
      .attr("x", (764.4/1372.0)*width + smallRec/4 + smallRec + barDistance)
      .attr("y", (603.6/1372.0)*width + 2*smallRec/3)
      .attr("font-size", sizeNumbers)
      .text(s"${55}")
      .attr("fill", checkNumbers(55.toString))
      .style("font-weight", "bold")

      g.append("rect")
      .attr("x", (759.4/1372.0)*width)
      .attr("y", (603.6/1372.0)*width)
      .attr("width", smallRec)
      .attr("height", smallRec)
      .attr("fill", checkKirg(54.toString))

      svg.append("text")
      .attr("x", (764.4/1372.0)*width + smallRec/4)
      .attr("y", (603.6/1372.0)*width + 2*smallRec/3)
      .attr("font-size", sizeNumbers)
      .text(s"${54}")
      .attr("fill", checkNumbers(54.toString))
      .style("font-weight", "bold")

      g.append("rect")
      .attr("x", (759.4/1372.0)*width)
      .attr("y", (603.6/1372.0)*width + smallRec + barDistance)
      .attr("width", smallRec)
      .attr("height", smallRec)
      .attr("fill", checkKirg(53.toString))

      svg.append("text")
      .attr("x", (764.4/1372.0)*width + smallRec/4)
      .attr("y", (603.6/1372.0)*width + 2*smallRec/3 + smallRec + barDistance)
      .attr("font-size", sizeNumbers)
      .text(s"${53}")
      .attr("fill", checkNumbers(53.toString))
      .style("font-weight", "bold")

      g.append("rect")
      .attr("x", (759.4/1372.0)*width)
      .attr("y", (603.6/1372.0)*width + 2*smallRec + 2*barDistance)
      .attr("width", smallRec)
      .attr("height", smallRec)
      .attr("fill", checkKirg(52.toString))

      svg.append("text")
      .attr("x", (764.4/1372.0)*width + smallRec/4)
      .attr("y", (603.6/1372.0)*width + 2*smallRec/3 + 2*smallRec + 2*barDistance)
      .attr("font-size", sizeNumbers)
      .text(s"${52}")
      .attr("fill", checkNumbers(52.toString))
      .style("font-weight", "bold")

      g.append("rect")
      .attr("x", (759.4/1372.0)*width)
      .attr("y", (603.6/1372.0)*width + 3*smallRec + 3*barDistance)
      .attr("width", smallRec)
      .attr("height", smallRec)
      .attr("fill", checkKirg(51.toString))

      svg.append("text")
      .attr("x", (764.4/1372.0)*width + smallRec/4)
      .attr("y", (603.6/1372.0)*width + 2*smallRec/3 + 3*smallRec + 3*barDistance)
      .attr("font-size", sizeNumbers)
      .text(s"${51}")
      .attr("fill", checkNumbers(51.toString))
      .style("font-weight", "bold")

      g.append("rect")
      .attr("x", (759.4/1372.0)*width)
      .attr("y", (603.6/1372.0)*width + 4*smallRec + 4*barDistance)
      .attr("width", smallRec)
      .attr("height", smallRec)
      .attr("fill", checkKirg(50.toString))

      svg.append("text")
      .attr("x", (764.4/1372.0)*width + smallRec/4)
      .attr("y", (603.6/1372.0)*width + 2*smallRec/3 + 4*smallRec + 4*barDistance)
      .attr("font-size", sizeNumbers)
      .text(s"${50}")
      .attr("fill", checkNumbers(50.toString))
      .style("font-weight", "bold")

      // ------------------------------------
    }

    //var busyRooms = List[String]("1","3","7","9","14","15","16","20","22","35","36","39","42","43","44","45","55","56","57","63","A2","47A","48A")

    def apply() = SPWidget {spwb => component()}

  }
