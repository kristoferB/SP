package spgui.widgets.examples

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import spgui.SPWidget
import spgui.components.DragAndDrop.{ OnDragMod, OnDropMod }

object DragAndDrop {
  private case class Props(
    name: String,
    mailbox: String
  )

  private class MyBackend($: BackendScope[Unit, Props]) {

    def handleDrag(s: Props)(e: ReactDragEventFromInput): Callback = {
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
    
    def handleNameChange(s: Props)(e: ReactEventFromInput) =
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
        ) //,
//        <.div(
//          ^.className := DragAndDropCSS.dragZone.htmlClass,
//          OnDragMod(handleDrag(s)),
//          "drag me!"
//        ),
//        <.div(
//          ^.className := DragAndDropCSS.dropZone.htmlClass,
//          OnDropMod(handleDrop(s)),
//          "drag to me!"
//        )
      )
  }

  private val component = ScalaComponent.builder[Unit]("DragAndDrop")
    .initialState(
    Props(
      name = "default",
      mailbox = "no mail yet"
    )
  )
    .renderBackend[MyBackend]
    .build

  def apply() = SPWidget(swpb => component())
}
