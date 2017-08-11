package spgui.widgets.examples

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import spgui.SPWidget
import spgui.components.DragAndDrop.{ OnDragMod, OnDropMod }

object DragAndDrop {
  private case class State(
    name: String,
    mailbox: String
                          )

  private class MyBackend($: BackendScope[Unit, State]) {
    def handleDrag(s: State)(e: ReactDragEventFromInput): Callback = {
      Callback({
        e.dataTransfer.setData("json", s.name)
      })
    }

    def handleDrop(s: State)(e: ReactDragEvent): Callback = {
      $.setState(
        s.copy(
          mailbox = e.dataTransfer.getData("json")
        )
      )
    }
    
    def handleNameChange(s: State)(e: ReactEventFromInput) =
      $.setState(s.copy(
        name = e.target.value
      ))

    def render(s: State) =
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
          OnDragMod(handleDrag(s)),
          "drag me!"
        ),
        <.div(
          ^.className := DragAndDropCSS.dropZone.htmlClass,
          OnDropMod(handleDrop(s)),
          "drag to me!"
        )
      )
  }

  private val component = ScalaComponent.builder[Unit]("DragAndDrop")
    .initialState(
    State(
      name = "default",
      mailbox = "no mail yet"
    )
  )
    .renderBackend[MyBackend]
    .build

  def apply() = SPWidget(swpb => component())
}
