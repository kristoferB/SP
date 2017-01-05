package spgui.widgets.examples.dragndrop

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import org.scalajs.dom.console

object DragAndDrop {
  private case class Props(
    name: String,
    mailbox: String
  )

  private class MyBackend($: BackendScope[Unit, Props]) {

    def handleDrag(s: Props)(e: ReactDragEventI): Callback = {
      Callback({
        e.dataTransfer.setData("json", s.name)
      })
    }

    def handleDrop(s: Props)(e: ReactDragEvent): Callback = {
      $.setState(
        s.copy(
          mailbox = e.dataTransfer.getData("json")
        )
      )
    }

    def preventDefaultHandling(e: ReactEventI): Callback = {
      // this needs to be called by onDragOver for onDrop to work as expected
      Callback(e.preventDefault())
    }

    def handleNameChange(s: Props)(e: ReactEventI) =
      $.setState(s.copy(
        name = e.target.value
      ))

    def render(s: Props) =
      <.div(
        "name:",
        <.input(
          ^.value     := s.name,
          ^.onChange ==> handleNameChange(s)
        ),
        <.h3(
          "last message from " + s.mailbox
        ),
        <.div(
          ^.className := DragAndDropCSS.dragZone.htmlClass,
          ^.draggable := true,
          ^.onDragStart ==> handleDrag(s),
      
          "drag me!"
        ),
        <.div(
          ^.className := DragAndDropCSS.dropZone.htmlClass,
          ^.onDragOver ==> preventDefaultHandling,
          ^.onDrop ==> handleDrop(s),
          "drag to me!"
        )
      )
  }

  private val component = ReactComponentB[Unit]("DragAndDrop")
    .initialState(
    Props(
      name = "default",
      mailbox = "no mail yet"
    )
  )
    .renderBackend[MyBackend]
    .build

  def apply(): ReactElement = component()
}





