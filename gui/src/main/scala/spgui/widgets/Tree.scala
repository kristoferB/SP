package spgui.widgets

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import scalacss.Defaults._
import scalacss.ScalaCssReact._

import spgui.SPWidget
import spgui.Icon

object Tree {
  val kakasListItems = List(
    ListItem("Smör"),
    ListItem("Ägg")
  )

  val listItems = List(
    ListItem("Kladd"),
    ListItem("Kaka", Right(kakasListItems)),
    ListItem("Äpplen")
  )

  def apply() = SPWidget(spwb => TreeDummyList(listItems))
}

case class ListItem(name: String, content: Either[String, List[ListItem]] = Left("std content"))

object TreeDummyList {
  case class Props(listItems: List[ListItem])
  case class State(content: Either[String, List[ListItem]])

  class RBackend($: BackendScope[Props, State]) {
    def setContent(newContent: Either[String, List[ListItem]]) =
      $.setState(State(newContent))

    def render(p: Props, s: State) =
      <.div(
        <.ul(
          Style.ul,
          p.listItems.map(item =>
            <.li(
              Style.li,
              item.name,
              Icon.chevronRight,
              ^.onClick --> setContent(item.content)
            )
          )
        ),
        s.content.fold(str => str, items => TreeDummyList(items))
      )
  }

  val component = ReactComponentB[Props]("TreeDummyList")
    .initialState(State(Left("")))
    .renderBackend[RBackend]
    .build

  def apply(list: List[ListItem]): ReactElement = component(Props(list))

  object Style extends StyleSheet.Inline {
    import dsl._

    val ul = style(float.left)

    val li = style(
      position.relative,
      display.block,
      padding(v = 10.px, h = 15.px),
      border :=! "1px solid #ecf0f1",
      cursor.pointer,
      fontWeight._500,
      //backgroundColor :=! "#146699",
      backgroundColor.white,
      &.hover(color :=! "#555555", backgroundColor :=! "#ecf0f1")
    )

    this.addToDocument()
  }
}
