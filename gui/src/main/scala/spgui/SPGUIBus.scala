package spgui

import scala.collection.mutable.{Map => MMap}
import scala.collection.mutable.ListBuffer

// just a dummy thing for testing so far...
object SPGUIBus {
  val callbacks = MMap[String, ListBuffer[(Int, String => Unit)]]()

  def subscribe(widgetId: Int, topic: String, cb: String => Unit) =
    if(callbacks.isDefinedAt(topic)) callbacks(topic).append((widgetId, cb))
    else callbacks(topic) = ListBuffer((widgetId, cb))

  def unsubscribeWidget(widgetId: Int): Unit =
    //for((_, listB) <- callbacks) listB.filter{
      //case (id: Int, _) => id != widgetId
    //}
    callbacks.foreach{
      case (topic, listB) => callbacks(topic) = listB.filter(_._1 != widgetId)
    }

  def publish(topic: String, msg: String) =
    if(callbacks.isDefinedAt(topic)) callbacks(topic).foreach(item => item._2(msg))

}
