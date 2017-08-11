package spgui.widgets.examples

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import sp.messages._
import Pickles._

import scala.util.Try
import spgui.SPWidget
import spgui.communication.WidgetPubSub


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
//        ^.onClick --> Callback(WidgetPubSub.pub(SPMessage.make(SPHeader("ping", "pong"), "Hej på dig")))
//      )
//    )
//  )
//}
//
//object Pong {
//  import rx._
//  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()
//
//  case class PongData(theInt: Int)
//  def apply() = SPWidget{spwb =>
//    val x = WidgetPubSub.subVar()
//    val res = Var[String]("")
//    Rx {
//      val mess = x()
//      for {
//        h <- mess.getHeaderAs[SPHeader] if h.to == "pong"
//        b <- mess.getBodyAs[String]
//      } yield {
//        res() = b
//      }
//    }
//
//    res.
//
//    <.div(
//      <.h3("Hello from Pong"),
//      <.h4(Try(read[PongData](spwb.data).theInt).getOrElse(0) + " messages received")
//    )
//  }
//}
