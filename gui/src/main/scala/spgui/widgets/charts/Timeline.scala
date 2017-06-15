/**
  * Created by alexa on 15/06/2017.
  */
package spgui.widgets.charts

// imports for scalajs react
import javax.xml.stream.events.{EndDocument, StartDocument}

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import scalajs.js
import spgui.communication._

import scala.scalajs.js.Date

object Timeline {
  // State
  private case class State(
                            name: String
                          )

  // dummy id
  val id = "widget"
  // local variable to hold the options
  var timelineOptions = Options("default", 0, 0)

  @js.native
  trait TimelineRow extends js.Object {
    val id:         String    = js.native
    val name:       String    = js.native
    val startDate:  js.Date   = js.native
    val endDate:    js.Date   = js.native
  }

  //facade Rows
  object TimelineRow {
    def apply(
               id:        String,
               name:      String,
               startDate: js.Date,
               endDate:   js.Date
             ) = js.Dynamic.literal(
      id = id,
      name = name,
      startDate = startDate,
      endDate = endDate
    )
  }

  @js.native
  trait TimelineRows extends js.Object {
    val list:        List[TimelineRow] = js.native
    def add()
  }

  object TimelineRows {
    def apply(
      list: js.List[TimelineRow]
      ) = js.Dynamic.literal(
        list = list
      ) 

      def add(row: TimelineRow) = 
        list.add(row) 
  }

  def setupColumns(d: GoogleVisualization.DataTable): GoogleVisualization.DataTable = {
    def adder() = {
      d.addColumn("string", "Timeline id")
      d.addColumn("string", "Timeline name")
      d.addColumn("date", "Start Date")
      d.addColumn("date", "End Date")
    }
    // add columns
    adder()
    // return datatable
    d
  }

  var data = setupColumns(new GoogleVisualization.DataTable())



  // atm mutable
  // FIX THIS WHEN TIMELINE IS WORKING!!!
  // Want it to be immutable
  def setOptions(title: String, height: Int, width: Int) =
    timelineOptions = Options(title, height, width)

  // ---Backend Class---
  // Argument:
  //            BackendScope[ Props , State ]
  private class MyBackend($: BackendScope[Unit, State]) {



    // ---render method---
    // Argument:
    //          State
    // Creates a div with CSS from TimelineCSS.scala
    // The div should contain the Google Timeline Chart
    def render(s: State) = {
      <.div(
        ^.className := TimelineCSS.timelineStyle.htmlClass,
        ^.id := id,
        <.div(
          ^.id := id + "name",
          <.p(s.name)
        ),
        <.div(
          ^.id := id + "timeline"
        )
      )
    }

    // Handles the updates of the Timeline cycles
    def handleUpdate(s: State) = ???

    // Handles stopevents from a cycle
    def handleStop(s: State) = ???

    // Handles start-events for a cycle
    def handleStart(s: State) = ???


  }

  // Create a value component of type:
  //                                  ReactComponent
  // Set the initial state
  // Set the render class for the backend
  // Builds
  private val component = ReactComponentB[Unit]("Timeline")
    .initialState(
      State(
        name = "default"
      )
    )
    .renderBackend[MyBackend]
    .build

  // defines apply
  def apply() = spgui.SPWidget(spwb => component())
}
