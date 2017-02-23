package spgui.widgets.itemexplorer

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import scalacss.ScalaCssReact._

import spgui.SPWidget
import spgui.components.Icon
import spgui.components.DragAndDrop.{ DataOnDrag, OnDataDrop }
import spgui.menu.SPDropdown

// TODO: replace with SP API
// this is just a dummy to have something to work with
sealed abstract class Item {
  val name: String
  // will be an UUID
  val id: Int
}
case class Mapp(name: String, id: Int, childrenIds: List[Int]) extends Item
case class Spotify(name: String, id: Int, content: String) extends Item
case class Youtube(name: String, id: Int, content: String) extends Item

object TVButton {
  private def button(name: String, icon: ReactNode) =
    <.div(<.div(Style.icon, icon), name, <.div(Style.chevron, Icon.chevronRight))

  def apply(item: Item) = item match {
    case Mapp(name, _, _) => button(name, Icon.folder)
    case Spotify(name, _, _) => button(name, Icon.spotify)
    case Youtube(name, _, _) => button(name, Icon.youtube)
  }
}

class TreeState(private var _items: List[Item], private var _rootLevelItemIds: List[Int]) {
  // parentIdMap(i) points to to parent of item of id i, undefined if item is on root level
  private var parentIdMap: Map[Int, Int] = _items.flatMap{
      case Mapp(_, id, childrenIds) => childrenIds.map((_, id))
      case _ => Nil
    }.toMap

  // apparently the only scala way of stopping the client from modifiying a var
  def items = _items
  def rootLevelItemIds = _rootLevelItemIds

  // return self, to make $.modstate calls look cleaner
  def addItem(item: Item) = {
    _items = item :: _items
    _rootLevelItemIds = item.id :: _rootLevelItemIds
    this
  }

  def moveItem(movedItemId: Int, newParentId: Int) = {
    _items = _items.map{
      case Mapp(name, `newParentId`, childrenIds) => Mapp(name, newParentId, movedItemId :: childrenIds)
      case Mapp(name, id, childrenIds) => Mapp(name, id, childrenIds.filter(_ != movedItemId))
      case item: Item => item
    }

    if(!parentIdMap.contains(movedItemId)) _rootLevelItemIds = _rootLevelItemIds.filter(_ != movedItemId)

    parentIdMap = parentIdMap.filterKeys(_ != movedItemId) ++ Map(movedItemId -> newParentId)

    this
  }
}

object ListItems {
  val listItems = List(
    Youtube("Smör", 2, "mjölk"),
    Youtube("Ägg", 3, "kalcium"),
    Spotify("Kladd", 4, "Kladd"),
    Mapp("kaka", 5, List(2, 3)),
    Spotify("Äpplen", 6, "paj")
  )
  val rootLevelItemIds = List(4, 5, 6)
  def apply() = new TreeState(listItems, rootLevelItemIds)
}

object Tree {
  def emptyMap() = Mapp("EmptyMap", util.Random.nextInt(1000000) + 10000, List())
  def newSpotify() = Spotify("NewYT", util.Random.nextInt(1000000) + 10000, "content of NewSpotify")
  def newYT() = Youtube("NewYT", util.Random.nextInt(1000000) + 10000, "content of NewYT")

  class TreeBackend($: BackendScope[Unit, TreeState]) {
    def addItem(item: Item) = $.modState(_.addItem(item))

    def onDrop(senderId: String, receiverId: String) =
      Callback.log(s"item of id $senderId dropped on item of id $receiverId") >>
        $.modState(_.moveItem(senderId.toInt, receiverId.toInt))

    def render(s: TreeState) =
      <.div(
        SPDropdown(
          ("Directory", addItem(emptyMap())) ::
          ("Spotify", addItem(newSpotify())) ::
          ("Youtube", addItem(newYT())) :: Nil,
          Icon.chevronDown,
          "Add Item"
        ),
        TVColumn(s.items, s.rootLevelItemIds, onDrop)
      )
  }

  private val component = ReactComponentB[Unit]("Tree")
    .initialState(ListItems())
    .renderBackend[TreeBackend]
    .build

  def apply() = SPWidget(spwb => component())
}

object TVColumn {
  case class Props(items: List[Item], itemIds: List[Int], onDrop: (String, String) => Callback)
  case class State(selectedItemId: Int = -1)

  class TVColumnBackend($: BackendScope[Props, State]) {

    def setSelectedId(id: Int) =
      $.modState(s => s.copy(selectedItemId = if(s.selectedItemId == id) -1 else id))

    def render(p: Props, s: State) =
      <.div(
        <.ul(
          Style.ul,
          p.itemIds.map{id =>
            val item = p.items.find(_.id == id).get
            <.li(
              Style.li(item.id == s.selectedItemId),
              TVButton(item),
              DataOnDrag(item.id.toString),
              OnDataDrop(eventData => p.onDrop(eventData, item.id.toString)),
              ^.onClick --> setSelectedId(item.id)
            )
          }
        ),
        if(s.selectedItemId == -1) ""
        else p.items.find(_.id == s.selectedItemId).get match {
          case Mapp(_, _, childrenIds) => TVColumn(p.items, childrenIds, p.onDrop)
          case Spotify(_, _, content) => content
          case Youtube(_, _, content) => content
        }
      )
  }

  val component = ReactComponentB[Props]("TreeDummyList")
    .initialState(State())
    .renderBackend[TVColumnBackend]
    .build

  def apply(items: List[Item], itemIds: List[Int], onDrop: (String, String) => Callback): ReactElement =
    component(Props(items, itemIds, onDrop))
}
