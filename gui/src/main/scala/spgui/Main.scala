package spgui

import scala.scalajs.js.JSApp
import japgolly.scalajs.react._
import org.scalajs.dom.document
import scala.scalajs.js.annotation.JSExport

object Main extends JSApp {
  @JSExport
  override def main(): Unit = {
    Layout().renderIntoDOM(document.getElementById("spgui-root"))
  }
}