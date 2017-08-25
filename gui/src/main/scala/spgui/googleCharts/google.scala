package spgui.googleCharts

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal
import js.annotation.JSName

package google {

  @js.native
  @JSGlobal("charts")
  object charts extends js.Object {
    def load(): Unit = js.native
    def load(loading: String): Unit = js.native
    def load(loading: String, loadObject: js.Object): Unit = js.native

    def setOnLoadCallback(): Unit = js.native
    def setOnLoadCallback(callback: Unit): Unit = js.native
  }
}

