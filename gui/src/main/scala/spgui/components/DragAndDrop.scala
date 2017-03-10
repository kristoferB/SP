package spgui.components

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

object DragAndDrop {
  object OnDragMod {
    def apply(handleDrag: ReactDragEventFromInput => Callback): TagMod =
      Seq(^.draggable := true, ^.onDragStart ==> handleDrag).toTagMod
  }

  object OnDropMod {
    // this needs to be called by onDragOver for onDrop to work as expected
    def preventDefaultHandling(e: ReactEventFromInput) = Callback(e.preventDefault())

    def apply(handleDrop: ReactDragEvent => Callback): TagMod =
      Seq(^.onDragOver ==> preventDefaultHandling, ^.onDrop ==> handleDrop).toTagMod
  }

  object DataOnDrag {
    def apply(data: String, cb: Callback = Callback.empty) =
      OnDragMod(ev => cb >> Callback(ev.dataTransfer.setData("text/plain", data)))
  }

  object OnDataDrop {
    def apply(handleData: String => Callback) =
      OnDropMod(ev => handleData(ev.dataTransfer.getData("text/plain")))
  }
}
