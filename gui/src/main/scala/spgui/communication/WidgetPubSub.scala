package spgui.communication

import java.util.UUID

import sp.domain._
import sp.domain.Logic._

import scala.collection.immutable.Queue


/**
  * Created by kristofer on 2017-02-24.
  */
object WidgetPubSub {
  import rx._
  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()

  var channels = Map[String, Channel]()
  val defaultChannel = "widgets"

  def pub(message: SPMessage, channel: String = defaultChannel) = {
    val c = getOrMakeC(channel)
    c.pub(message)
  }

  def sub(f: SPMessage => Unit, channel: String = defaultChannel): rx.Obs = {
    val c = getOrMakeC(channel)
    c.out.foreach(f)
  }

  def subVar(channel: String = defaultChannel) = {
    val c = getOrMakeC(channel)
    c.out
  }


  private def getOrMakeC(channel: String) = {
    channels.getOrElse(channel, {
      val t = new Channel(channel)
      channels += (channel -> t)
      t
    })
  }

}



class Channel(name: String, logSize: Int = 20) {
  import rx._
  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()

  val input: Var[SPMessage] = Var(SPMessage(SPAttributes(), SPAttributes()))
  val log: Var[Queue[SPMessage]] = Var(Queue())
  val out: Var[SPMessage] = Var(SPMessage(SPAttributes(), SPAttributes()))


  val filterInput = Rx{
    out() = input()
  }

  val addToLog = Rx{
    val l = log()
    val q = if (l.size > logSize) l.dequeue._2 else l
    log() = q.enqueue(out())
  }

  def pub(m: SPMessage) = {
    Rx{
      input() = m
    }
  }


}
