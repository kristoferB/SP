/**
  * Created by alexa on 15/06/2017.
  */
package spgui.widgets.charts

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import scalajs.js

object Timeline {
  private case class State(
   name: String
  )
  private class MyBackend($: BackendScope[Unit, State]) {

    def render(s: State): Unit = {
      <.div
    }

  }
}
