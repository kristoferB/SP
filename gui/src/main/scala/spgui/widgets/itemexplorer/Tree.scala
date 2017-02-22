package spgui.widgets.itemexplorer

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import scalacss.ScalaCssReact._

import spgui.SPWidget
import spgui.Icon

// TODO: replace with SP API
// this is just a dummy to have something to work with
sealed abstract class Item {
  val name: String
}
case class Mapp(name: String, content: List[Item]) extends Item
case class Spotify(name: String, content: String) extends Item
case class Youtube(name: String, content: String) extends Item

object TVButton {
  private def button(name: String, icon: ReactNode) =
    <.div(<.div(Style.icon, icon), name, <.div(Style.chevron, Icon.chevronRight))

  def apply(item: Item) = item match {
    case Mapp(name, _) => button(name, Icon.folder)
    case Spotify(name, _) => button(name, Icon.spotify)
    case Youtube(name, _) => button(name, Icon.youtube)
  }
}

object ItemContent {
  def apply(item: Item): ReactNode = item match {
    case Mapp(_, content) => TreeView(content)
    case Spotify(_, content) => content
    case Youtube(_, content) => content
  }
}

object Tree {
  val kaka = List(
    Youtube("Smör", "mjölk"),
    Youtube("Ägg", "kalcium")
  )

  val listItems =
    List(
      Spotify("Kladd", "Kladd"),
      Mapp("kaka", kaka),
      Spotify("Äpplen", "paj")
    )

  def apply() = SPWidget(spwb => TreeView(listItems))
}

object TreeView {
  case class Props(items: List[Item])
  case class State(selectedItemIndex: Int = -1)

  class RBackend($: BackendScope[Props, State]) {

    def setSelectedIndex(i: Int) =
      $.modState(s => s.copy(selectedItemIndex = if(s.selectedItemIndex == i) -1 else i))

    def render(p: Props, s: State) =
      <.div(
        <.ul(
          Style.ul,
          p.items.zipWithIndex.map(t =>
            <.li(
              Style.li(t._2 == s.selectedItemIndex),
              TVButton(t._1),
              ^.onClick --> setSelectedIndex(t._2)
            )
          )
        ),
        if(s.selectedItemIndex == -1) "" else ItemContent(p.items(s.selectedItemIndex))
      )
  }

  val component = ReactComponentB[Props]("TreeDummyList")
    .initialState(State())
    .renderBackend[RBackend]
    .build

  def apply(items: List[Item]): ReactElement = component(Props(items))
}
