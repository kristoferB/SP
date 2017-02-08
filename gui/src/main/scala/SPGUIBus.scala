package spgui

import japgolly.scalajs.react._

import scala.collection.mutable.ListBuffer

// just a dummy thing for testing so far...
object SPGUIBus {
  val callbacks = ListBuffer[String => Unit]()
  def subscribe(cb: String => Unit) = callbacks.append(cb)
  def publish(msg: String) = callbacks.foreach(f => f(msg))
}
