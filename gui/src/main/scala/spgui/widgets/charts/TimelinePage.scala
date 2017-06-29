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
import spgui.googleAPI._
import spgui.googleAPI.timeline.{OptionsTimeline, TimelineRow}

/*
 * TODO: Remove debugging messages
 */

object TimelinePage {
  /*
   * TODO:
   *      1. Look over which State is needed for Timeline
   */
  // State
  case class State(zoom: String)

  // dummy id
  val id = "widget"


  /* TODO:
  *      1. Let the Widget set the Options through PROPS!
  */
  // ---Backend Class---
  // Argument:
  //            BackendScope[ Props , State ]
  private class Backend($: BackendScope[Unit, State]) {
    // ---render method---
    // Argument:
    //          State
    // Creates a div with CSS from TimelineCSS.scala
    // The div should contain the Google Timeline Chart
    def render() =
    <.div(
      <.div(
        ^.className := TimelineCSS.timelineStyle.htmlClass,
        <.div(^.id := id +"testingTml")
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
        println("initiate timeline chart")
        val timelineElement = js.Dynamic.global.document.getElementById(id + "testingTml")
        val timeline = new GoogleVisualization.Timeline(timelineElement)
        println("   ---done---    ")

        println("create Datatable")
        val data = new GoogleVisualization.DataTable()
        println("   ---done---    ")


        println("addColumns")
        data.addColumn("string", "Timeline id", "1")
        data.addColumn("string", "Timeline Name", "2")
        data.addColumn("date", "Start Date", "4")
        data.addColumn("date", "End Date", "5")
        println("   ---done---    ")

        println("create a row")
        val exampleRow = new TimelineRow("g", "9", new js.Date(2014, 2, 22), new js.Date(2014, 5, 20))
        println("   ---done---    ")

        println("add row to data")
        println("-print row")
        exampleRow.toArray(exampleRow) foreach println
        data.addRow(exampleRow.toArray(exampleRow))
        println("   ---done---    ")

        println("create option")
        val exampleOptions = new OptionsTimeline(300, 400)
        println("   ---done---    ")


        // TODO: Fix OptionsTimeline
        println("draw")
        timeline.draw(data)
        println("   ---done---    ")
      }
      Callback.log("Mounting TimelinePage Done!")
    }

  }

  // Create a value component of type:
  //                                  ReactComponent
  // Set the initial state
  // Set the render class for the backend
  // Builds
  private val component = ReactComponentB[Unit]("TimelinePage")
    .initialState(
      State(
        zoom = "default"
      )
    )
    .renderBackend[Backend]
    .componentDidMount(_.backend.handleStart())
    .componentWillUnmount(_.backend.handleStop())
    .build

  // defines apply
  def apply() = spgui.SPWidget(spwb => component())


}


