/**
  * Created by alexa on 15/06/2017.
  */
package spgui.widgets.charts

// imports for scalajs react
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import scalajs.js
import spgui.communication._

object Timeline {
  // State
  private case class State(
                            name: String
                          )

  // dummy id
  val id = "timeline"

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
