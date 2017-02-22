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
  case class State(selectedItemIndex: Int = -1)

  class RBackend($: BackendScope[Props, State]) {

    def setSelectedIndex(i: Int) =
      $.modState(s => s.copy(selectedItemIndex = if(s.selectedItemIndex == i) -1 else i))

    def render(p: Props, s: State) =
      <.div(
        <.ul(
          Style.ul,
          p.listItems.zipWithIndex.map(t =>
            <.li(
              Style.li(t._2 == s.selectedItemIndex),
              t._1.name,
              <.div(Style.icon, Icon.chevronRight),
              ^.onClick --> setSelectedIndex(t._2)
            )
          )
        ),
        if(s.selectedItemIndex == -1) "" else
          p.listItems(s.selectedItemIndex).content.fold(str => str, items => TreeDummyList(items))
      )
  }

  val component = ReactComponentB[Props]("TreeDummyList")
    .initialState(State())
    .renderBackend[RBackend]
    .build

  def apply(list: List[ListItem]): ReactElement = component(Props(list))

  object Style extends StyleSheet.Inline {
    import dsl._

    val ul = style(
      float.left,
      paddingLeft(0 px)
    )

    val li = styleF.bool(selected => styleS(
      position.relative,
      display.block,
      width(160 px),
      padding(v = 10.px, h = 15.px),
      border :=! "1px solid #ecf0f1",
      cursor.pointer,
      fontWeight._500,
      mixinIfElse(selected)(color :=! "#555555", backgroundColor :=! "#A5C2EE")(
        backgroundColor.white,
        &.hover(color :=! "#555555", backgroundColor :=! "#A5C2EE"))
    ))

    val icon = style(float.right)

    this.addToDocument()
  }
}
