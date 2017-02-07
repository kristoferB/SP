package spgui

import japgolly.scalajs.react._

import scala.collection.mutable.ListBuffer

object SPGUIBus {
  val callbacks = ListBuffer[(String) => Callback]()
  def subscribe(cb: String => Callback) = callbacks.append(cb)
  def publish(msg: String) = Callback(callbacks.foreach(f => f(msg).runNow()))
}
