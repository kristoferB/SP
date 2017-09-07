package spgui.widgets

import java.time._ //ARTO: Använder wrappern https://github.com/scala-js/scala-js-java-time

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.ReactDOM

import spgui.SPWidget
import spgui.widgets.css.{WidgetStyles => Styles}

import org.scalajs.dom.raw
import org.scalajs.dom

import scala.concurrent.duration._
import scala.scalajs.js

import scalacss.ScalaCssReact._
import scalacss.DevDefaults._


object ClockWidget {

  case class State(secondsElapsed: Long)

  private class Backend($: BackendScope[Unit, State]) {

    var interval: js.UndefOr[js.timers.SetIntervalHandle] =
      js.undefined

      def tick =
        $.modState(s => State(s.secondsElapsed + 1))

        def start = Callback {
          interval = js.timers.setInterval(1000)(tick.runNow())
        }

        def clear = Callback {
          interval foreach js.timers.clearInterval
          interval = js.undefined
        }

        def addZero(i: Int): String = {
          if (i < 10) {
            f"${i}%02d"
          } else {
            i.toString
          }
        }


        def render(s: State) = {
          var currentTime = LocalTime.now()
          <.div(
            ^.height := "70",
            Styles.clock,
            addZero(currentTime.getHour()),".",addZero(currentTime.getMinute())
          )
        }

      }

      private val component = ScalaComponent.builder[Unit]("clockComponent")
      .initialState(State(0))
      .renderBackend[Backend]
      .componentDidMount(_.backend.start)
      .componentWillUnmount(_.backend.clear)
      .build


      def apply() = spgui.SPWidget(spwb => component())
    }
