/**
  * Created by alexa on 15/06/2017.
  */
package spgui.widgets.charts

// imports for scalajs react
import javax.xml.stream.events.{EndDocument, StartDocument}

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scala.reflect.ClassTag
import org.scalajs.dom.document
import org.scalajs.dom.html.Div

import scalajs.js
import spgui.communication._
import aleastchs.googleCharts.GoogleChartsLoaded
import aleastchs.googleCharts.helpers.chartsHelp.TimelineHelper
import aleastchs.googleCharts.helpers.chartsHelp.TimelineRow

/*
 * TODO: Remove debugging messages
 */

object TimelineExampleUse {
  /*
   * TODO:
   *      1. Look over which State is needed for Timeline
   */
  // State
  case class State(zoom: String)

  /*********EXAMPLE USE OF GOOGLE API*************/

  // ensures that the name in div when rendering and
  // the Timeline Chart have the same Name
  val idName: String = "exampleUse"


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
        <.div(^.id := idName)
      )
    )

    // Handles start-events for a cycle
    def handleStart() = {
      println(GoogleChartsLoaded)
      if (GoogleChartsLoaded.asInstanceOf[Boolean]) {
        // create a element that gets the div we create before
        val timelineElement = js.Dynamic.global.document.getElementById(idName)

        // create a new Timeline chart
        // argument the element
        val helper = TimelineHelper(timelineElement, "ExampeUse")

        // creates example data
        val exampleRow = TimelineRow("RowLbl", "BarLbl", new js.Date(2014, 2, 22), new js.Date(2014, 5, 20))

        // add the example object to the data table
        helper.newRow(exampleRow)
        //helper.emptyRow()

        helper.draw()
      }
      // send Callback log
      Callback.log("Mounting ExampleUse Done!")
    }

    // Handles the updates of the Timeline cycles
    def handleUpdate(s: State) = ???

    // Handles stopevents from a cycle
    def handleStop() = {
      Callback.log("Unmounting ExampleUse")
    }

  }

  // Create a value component of type:
  //                                  ReactComponent
  private val component = ScalaComponent.builder[Unit]("TimelineExampleUse")
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


