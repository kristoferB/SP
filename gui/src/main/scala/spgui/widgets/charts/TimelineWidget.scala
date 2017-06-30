/**
  * Created by alexa on 15/06/2017.
  */
package spgui.widgets.charts

// imports for scalajs react
import javax.xml.stream.events.{EndDocument, StartDocument}

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import scala.reflect.ClassTag
import org.scalajs.dom.document
import org.scalajs.dom.html.Div

import scalajs.js
import spgui.communication._
import spgui.googleCharts.{DescriptionObject, GoogleChartsLoaded, GoogleVisualization}
import spgui.googleCharts.timeline._
/*
 * TODO: Remove debugging messages
 */

object TimelineWidget {
  /*
   * TODO:
   *      1. Look over which State is needed for Timeline
   */
  // State
  case class State(zoom: String)

  /*********EXAMPLE USE OF GOOGLE API WITH Helper-class*************/

  // ensures that the name in div when rendering and
  // the Timeline Chart have the same Name
  val idName: String = "timelineWidget"


  /* TODO:
  *      1. Let the Widget set the Options through PROPS!
  */
  // ---Backend Class---
  // Argument:
  //            BackendScope[ Props , State ]
  private class Backend($: BackendScope[Unit, State]) {
    // ---render method---
    // Creates a div with class CSS from TimelineCSS.scala
    // The div should contain the Google Timeline Chart
    def render() =
    <.div(
      // some random name
      ^.id := idName + "Container",
      <.div(
        // get CSS Cascading Style Sheets
        ^.className := TimelineCSS.timelineStyle.htmlClass,
        // create a div with id of our id name
        <.div(^.id := idName),
        <.div(^.id := idName + "2")
      )
    )

    // Handles start-events for a cycle
    def handleStart() = {
      println(GoogleChartsLoaded)
      if (GoogleChartsLoaded.asInstanceOf[Boolean]) {
        // create a element that gets the div we create before
        val timelineElement = js.Dynamic.global.document.getElementById(idName)

        // create a new Timeline chart - helper
        val helper = TimelineHelper(timelineElement)
        // add new row
        helper.newRow("Besök", "Patientens Besök På Sjukhuset",
          new js.Date(2017, 5, 20, 8, 5, 3, 2), new js.Date(2017, 5, 20, 10, 32, 23, 9))
        helper.newRow("Kölapp", "Tar Kölapp",
          new js.Date(2017, 5, 20, 8, 6, 13, 8), new js.Date(2017, 5, 20, 8, 6, 31, 7))
        helper.newRow("Väntetid", "Patient Väntar På inskrivning",
          new js.Date(2017, 5, 20, 8, 6, 31, 7), new js.Date(2017, 5, 20, 8, 23, 54, 1))
        helper.newRow("Inskrivning", "Patient Skriver in sig",
          new js.Date(2017, 5, 20, 8, 24, 11, 2), new js.Date(2017, 5, 20, 8, 26, 46, 3))
        helper.newRow("Väntetid", "Patienten väntar på läkare",
          new js.Date(2017, 5, 20, 8, 26, 46, 3), new js.Date(2017, 5, 20, 9, 1, 35, 4))
        helper.newRow("Läkarbesök", "Patient träffar läkare",
          new js.Date(2017, 5, 20, 9, 1, 35, 4), new js.Date(2017, 5, 20, 9, 9, 21, 5))
        helper.newRow("Väntetid", "Patient väntar på diagnos",
          new js.Date(2017, 5, 20, 9, 9, 21, 5), new js.Date(2017, 5, 20, 9, 59, 1, 0))
        helper.newRow("Diagnostiering", "Läkare sätter diagnos",
          new js.Date(2017, 5, 20, 9, 9, 21, 5), new js.Date(2017, 5, 20, 9, 27, 54, 9))
        helper.newRow("Läkarbesök", "Patient träffar läkare",
          new js.Date(2017, 5, 20, 9, 59, 1, 0), new js.Date(2017, 5, 20, 10, 14, 13, 4))

        // draw timeline chart
        helper.draw()
        println("Print DataTable in JSON-format")
        println(helper.data.toJSON())

        // try to create JSON-copy
        val timelineElement2 = js.Dynamic.global.document.getElementById(idName +"2")
        // create a new Timeline chart - helper
        val helper2 = TimelineHelper(timelineElement2, helper.data.toJSON())
        helper2.draw()

      }
      // send Callback log
      Callback.log("Mounting TimelineWidget Done!")
    }

    // Handles the updates of the Timeline cycles
    def handleUpdate(s: State) = ???

    // Handles stopevents from a cycle
    def handleStop() = {
      Callback.log("Unmounting TimelineWidget")
    }

  }

  // Create a value component of type:
  //                                  ReactComponent
  private val component = ReactComponentB[Unit]("TimelineWidget")
    .initialState(// Set the initial state
      State(
        zoom = "default"
      )
    )
    .renderBackend[Backend]// Set the render class for the backend
    .componentDidMount(_.backend.handleStart())//when client does mount, get handleStart()
    .componentWillUnmount(_.backend.handleStop())// when unmounting
    .build// Builds

  // defines apply
  def apply() = spgui.SPWidget(spwb => component())


}


