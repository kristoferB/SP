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
import spgui.googleCharts._
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

  /*********EXAMPLE USE OF GOOGLE API*************/

  // ensures that the name in div when rendering and
  // the Timeline Chart have the same Name
  val divElement: String = "timelineWidget"


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
      <.div(
        ^.className := TimelineCSS.timelineStyle.htmlClass,
        // create a div with id
        <.div(^.id := divElement)
      )
    )



    // Handles the updates of the Timeline cycles
    def handleUpdate(s: State) = ???

    // Handles stopevents from a cycle
    def handleStop() = {
      println("Unmounting "+ this.toString )
      Callback.log("Unmounting TimelineWidget")
    }

    // Handles start-events for a cycle
    def handleStart() = {
      println(GoogleChartsLoaded)
      if (GoogleChartsLoaded.asInstanceOf[Boolean]) {
        // create a element that gets the div we create before
        val timelineElement = js.Dynamic.global.document.getElementById(divElement)
        // create a new Timeline chart
        // argument the element
        val timeline = new GoogleVisualization.Timeline(timelineElement)

        // create a new DataTable
        val data = new GoogleVisualization.DataTable()

        // creates a example column setup to the DataTable
        data.addColumn("string", "Timeline id", "1")
        data.addColumn("string", "Timeline Name", "2")
        data.addColumn("date", "Start Date", "4")
        data.addColumn("date", "End Date", "5")

        // creates example data
        val exampleRow = new TimelineRow("g", "9", new js.Date(2014, 2, 22), new js.Date(2014, 5, 20))

        // add the data to the DataTable
        data.addRow(exampleRow.toArray())

        // Create a example options (spgui.googleAPI.timeline.{OptionsTimeline, Timeline}
        val exampleOptions = new OptionsTimeline(300,500, new Timeline())

        // draw timeline chart
        // arguments: the DataTable and the options
        timeline.draw(data, exampleOptions.toDynamic())

      }
      // send Callback log
      Callback.log("Mounting TimelinePage Done!")
    }

  }

  // Create a value component of type:
  //                                  ReactComponent
  private val component = ReactComponentB[Unit]("TimelinePage")
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


