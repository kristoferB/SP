package spgui

import org.scalajs.dom.document

import scala.scalajs.js.JSApp
import scala.scalajs.js.annotation.JSExport

object Main extends JSApp {
  @JSExport
  override def main(): Unit = {
    LoadingWidgets.loadWidgets
    Layout().renderIntoDOM(document.getElementById("spgui-root"))
  }
}
