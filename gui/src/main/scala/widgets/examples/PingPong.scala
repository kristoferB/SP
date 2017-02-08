package spgui.widgets.examples

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import scalajs.js.Dynamic.{literal => l}
import scala.util.Try

import spgui.SPWidget

object Ping {
  def apply() = SPWidget(spwb =>
    <.div(
      <.h3("Hello from Ping"),
      <.button(
        "Open Pong widget",
        ^.onClick --> Callback(spwb.openWidget("Pong"))
      ),
      <.button(
        "Send message to Pongs",
        ^.onClick --> Callback(spwb.publish("hej där"))
      )
    )
  )
}

object Pong {
  def apply() = SPWidget{spwb =>
    spwb.subscribe{s =>
      //val msgsReceived = Try(spwb.getJson("msgs").get.toInt + 1).getOrElse(0)
      val msgsReceived: Int = Try(spwb.readCaseClassData.theInt).getOrElse(0) + 1
      //spwb.saveData(l("msgs" -> msgsReceived.toString))
      spwb.saveCaseClassData(msgsReceived)
      println("widget of id " + spwb.id + " got a msg saying " + s)
    }
    <.div(
      <.h3("Hello from Pong")//,
      //<.h4(Try(spwb.getJson("msgs").get.toInt).getOrElse(0) + " messages received")
    )
  }
}
