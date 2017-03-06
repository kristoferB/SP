package spgui.widgets.itemexplorer

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import scalacss.ScalaCssReact._

import spgui.SPWidget
import spgui.components.Icon
import spgui.components.DragAndDrop.{ DataOnDrag, OnDataDrop }
import spgui.components.Dropdown

// TODO: replace with SP API
case class Spotify(name: String, id: Int, content: String) extends DirectoryItem
case class Youtube(name: String, id: Int, content: String) extends DirectoryItem

object TVButton {
  private def button(name: String, icon: ReactNode) =
    <.div(<.div(Style.icon, icon), name, <.div(Style.chevron, Icon.chevronRight))

  def apply(item: DirectoryItem) = item match {
    case Directory(name, _, _) => button(name, Icon.folder)
    case Spotify(name, _, _) => button(name, Icon.spotify)
    case Youtube(name, _, _) => button(name, Icon.youtube)
  }
}

object ListItems {
  val listItems = List(
    Youtube("Smör", 2, "mjölk"),
    Youtube("Ägg", 3, "kalcium"),
    Spotify("Kladd", 4, "Kladd"),
    Directory("kaka", 5, List(2, 3)),
    Spotify("Äpplen", 6, "paj")
  )
  val rootLevelItemIds = List(4, 5, 6)
  def apply() = new RootDirectory(listItems, rootLevelItemIds)
}

object Tree {
  def emptyMap() = Directory("EmptyMap", util.Random.nextInt(1000000) + 10000, List())
  def newSpotify() = Spotify("NewYT", util.Random.nextInt(1000000) + 10000, "content of NewSpotify")
  def newYT() = Youtube("NewYT", util.Random.nextInt(1000000) + 10000, "content of NewYT")

  class TreeBackend($: BackendScope[Unit, RootDirectory]) {
    def addItem(item: DirectoryItem) = $.modState(_.addItem(item))

    def onDrop(senderId: String, receiverId: String) =
      Callback.log(s"item of id $senderId dropped on item of id $receiverId") >>
        $.modState(_.moveItem(senderId.toInt, receiverId.toInt))

    def render(s: RootDirectory) =
      <.div(
        Style.outerDiv,
        Dropdown(
          <.div(
            "Add Item",
            Icon.chevronDown,
            ^.className := "btn btn-default"
          ),
          <.div("Directory", ^.onClick --> addItem(emptyMap())),
          <.div("Spotify", ^.onClick --> addItem(newSpotify())),
          <.div("Youtube", ^.onClick --> addItem(newYT()))
        ),
        <.div(
          Style.treeDiv,
          TVColumn(s.items, s.rootLevelItemIds, onDrop)
        )
      )
  }

  private val component = ReactComponentB[Unit]("Tree")
    .initialState(ListItems())
    .renderBackend[TreeBackend]
    .build

  def apply() = SPWidget(spwb => component())
}

object TVColumn {
  case class Props(items: List[DirectoryItem], itemIds: List[Int], onDrop: (String, String) => Callback)
  case class State(selectedItemId: Int = -1)

  class TVColumnBackend($: BackendScope[Props, State]) {

    def setSelectedId(id: Int) =
      $.state >>= (s => if(s.selectedItemId == id) selectNone else $.modState(s => s.copy(selectedItemId = id)))

    def selectNone() = $.modState(s => s.copy(selectedItemId = -1))

    def deselectDragged(id: Int) =
      $.state >>= (s => if(id == s.selectedItemId) selectNone else Callback.empty)

    def render(p: Props, s: State) =
      <.div(
        Style.tvColumn,
        <.ul(
          Style.ul,
          p.itemIds.map{id =>
            val item = p.items.find(_.id == id).get
            <.li(
              Style.li(item.id == s.selectedItemId),
              TVButton(item),
              DataOnDrag(item.id.toString, deselectDragged(item.id)),
              OnDataDrop(eventData => p.onDrop(eventData, item.id.toString) >> setSelectedId(item.id)),
              ^.onClick --> (setSelectedId(item.id) >> Callback.log("selected sumthing"))
            )
          }
        ),
        if(s.selectedItemId == -1) ""
        else p.items.find(_.id == s.selectedItemId).get match {
          case Directory(_, _, childrenIds) => TVColumn(p.items, childrenIds, p.onDrop)
          case Spotify(_, _, content) => content
          case Youtube(_, _, content) => content
        }
      )
  }

  val component = ReactComponentB[Props]("TreeDummyList")
    .initialState(State())
    .renderBackend[TVColumnBackend]
    .build

  def apply(items: List[DirectoryItem], itemIds: List[Int], onDrop: (String, String) => Callback): ReactElement =
    component(Props(items, itemIds, onDrop))
}
