package spgui.widgets.examples

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import scala.util.Try
import sp.domain._
import Logic._

import spgui.SPWidget

object WidgetWithData {
  // calling MyData() (with no arguments) will give MyData(someInt = -17)
  case class MyData(someInt: Int = -17)
  implicit val fMyData: JSFormat[MyData] = deriveFormatSimple[MyData]

  def apply() = SPWidget{spwb =>
    // upickle's read tries to turn the string in browser storage into a MyData-instance
    // if there is nothing there or casting fails, creates the standard instance instead
    val myData = spwb.getWidgetData.to[MyData].getOrElse(MyData())
    val theInt = myData.someInt
    // upickle's write turns a new version of myData into a string that is saved in storage

    def increment = Callback {
      val updD = myData.copy(theInt + 1)
      spwb.updateWidgetData(SPValue(updD))
    }

    <.div(
      <.h3("count is " + theInt),
      <.button("increment", ^.onClick --> increment),
      <.p("this piece of data is stored in the browser")
    )
  }
}
