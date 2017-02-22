package spgui.widgets.examples

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import scala.util.Try
import upickle.default.{read, write}

import spgui.SPWidget


// creating a new bus soon
//object Ping {
//  def apply() = SPWidget(spwb =>
//    <.div(
//      <.h3("Hello from Ping"),
//      <.button(
//        "Open Pong widget",
//        ^.onClick --> Callback(spwb.openNewWidget("Pong"))
//      ),
//      <.button(
//        "Send message to Pongs",
//        ^.onClick --> Callback(spwb.publish("PingPongTopic", "hej där"))
//      )
//    )
//  )
//}
//
//object Pong {
//  case class PongData(theInt: Int)
//  def apply() = SPWidget{spwb =>
//    spwb.subscribe("PingPongTopic", s => {
//      val msgsReceived: Int = Try(read[PongData](spwb.data).theInt).getOrElse(0) + 1
//      spwb.saveData(write(PongData(msgsReceived)))
//      println("widget of id " + spwb.id + " got a msg saying " + s)
//    })
//    <.div(
//      <.h3("Hello from Pong"),
//      <.h4(Try(read[PongData](spwb.data).theInt).getOrElse(0) + " messages received")
//    )
//  }
//}
