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


object TimelinePage {
  /*
   * TODO:
   *      1. Look over which State is needed for Timeline
   */
  // State
  case class State(zoom: String)

  // dummy id
  val id = "widget"
  /*
   * TODO:
   *      1. Let the Widget set the Options through PROPS!
   */
  // local variable to hold the options

  //val timeline_options = new OptionsTimeline(100, 100)




  // ---setupColumns---
  // Setup the columns for the google timeline charts
  // Argument:
  //          -None-
  // Result:
  //        result value: GoogleVisualization.DataTable
  // Description:
  //              creates a new DataTable and adds the column 
  //              to use in the timeline
  /*
  def setupColumns(): GoogleVisualization.DataTable = {
    // defines inner function
    def adder(d: GoogleVisualization.DataTable): GoogleVisualization.DataTable = {
      d.addColumn("string", "Timeline id")
      d.addColumn("string", "Timeline name")
      d.addColumn("date", "Start Date")
      d.addColumn("date", "End Date")
      d
    }
    adder(new GoogleVisualization.DataTable())
  }

  // atm mutable
  // FIX WHEN POSSIBLE
  val data = setupColumns()


*/


  // ---Backend Class---
  // Argument:
  //            BackendScope[ Props , State ]
  private class MyBackend($: BackendScope[Unit, State]) {



    // ---render method---
    // Argument:
    //          State
    // Creates a div with CSS from TimelineCSS.scala
    // The div should contain the Google Timeline Chart
    def render() =
      <.div(
        <.div(
          ^.className := TimelineCSS.timelineStyle.htmlClass,
          ^.id := id +"testtest"
        )
      )


    /**** SETUP LIST OF THE DIFFERENT CHARTS *****/
    println("initiate timeline chart")

    val divResult = getElement[Div](id +"testtest").fold {
      "Couldn't find div"
    } { div => s"Div display style: ${div.style.display}" }

    println(divResult)


    val timelineElement = js.Dynamic.global.document.getElementById(id +"testingTml")
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
    println("   ---done---    \n")

    println("create a row")
    val exampleRow = new TimelineRow("g", "9", new js.Date(2014, 2, 22), new js.Date(2014, 5, 20))
    println("   ---done---    \n")

    println("add row to data")
    exampleRow.toArray(exampleRow) foreach println
    data.addRow(exampleRow.toArray(exampleRow))
    println("   ---done---    \n")

    println("create option")
    val exampleOptions = new OptionsTimeline(300,400)
    println("   ---done---    \n")


    // TODO: Fix OptionsTimeline
    println("draw")
    timeline.draw(data)
    println("   ---done---    \n")



    // Handles the updates of the Timeline cycles
    def handleUpdate(s: State) = ???

    // Handles stopevents from a cycle
    def handleStop(s: State) = ???

    // Handles start-events for a cycle
    def handleStart(s: State) = ???

    def getElement[T: ClassTag](elementId: String): Option[T] = {
      val queryResult = document.querySelector(s"#$elementId")
      queryResult match {
        case elem: T => Some(elem)
        case other =>
          println(s"Element with ID $elementId is $other")
          None
      }
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
    .renderBackend[MyBackend]
    .build

  // defines apply
  def apply() = spgui.SPWidget(spwb => component())


}


