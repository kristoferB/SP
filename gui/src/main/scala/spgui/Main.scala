package spgui

import scala.scalajs.js.JSApp
import japgolly.scalajs.react._
import org.scalajs.dom.document
import scala.scalajs.js.annotation.JSExport
import spgui.KeyboardHandler

object Main extends JSApp {
  @JSExport
  override def main(): Unit = {
    ReactDOM.render(Layout(), document.getElementById("spgui-root"))
  }

  KeyboardHandler.init()
}
