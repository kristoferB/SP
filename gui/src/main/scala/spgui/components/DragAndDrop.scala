package spgui.components

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

object DragAndDrop {
  object OnDragMod {
    def apply(handleDrag: ReactDragEventI => Callback): TagMod =
      Seq(^.draggable := true, ^.onDragStart ==> handleDrag)
  }

  object OnDropMod {
    // this needs to be called by onDragOver for onDrop to work as expected
    def preventDefaultHandling(e: ReactEventI) = Callback(e.preventDefault())

    def apply(handleDrop: ReactDragEvent => Callback): TagMod =
      Seq(^.onDragOver ==> preventDefaultHandling, ^.onDrop ==> handleDrop)
  }
}