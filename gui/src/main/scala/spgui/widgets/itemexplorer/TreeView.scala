package spgui.widgets.itemexplorer

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import scalacss.ScalaCssReact._

import spgui.components.DragAndDrop.{ DataOnDrag, OnDataDrop }
import spgui.components.{ Icon, Dropdown }


object TreeView {
  case class TreeViewProps(
    rootDirectory: RootDirectory,
    itemCreators: Seq[(String, () => DirectoryItem)],
    getItemIcon: DirectoryItem => ReactNode,
    renderContent: DirectoryItem => ReactNode
  )

  class TreeViewBackend($: BackendScope[TreeViewProps, RootDirectory]) {
    def addItem(item: DirectoryItem) = $.modState(_.addItem(item))

    def onDrop(senderId: String, receiverId: String) =
      Callback.log(s"item of id $senderId dropped on item of id $receiverId") >>
        $.modState(_.moveItem(senderId.toInt, receiverId.toInt))

    def render(p: TreeViewProps, s: RootDirectory) =
      <.div(
        Style.outerDiv,
        Dropdown(
          <.div(
            "Add Item",
            Icon.chevronDown,
            ^.className := "btn btn-default"
          ),
          p.itemCreators.map(ic => <.div(ic._1, ^.onClick --> addItem(ic._2()))): _*
        ),
        <.div(
          Style.treeDiv,
          TVColumn(s.items, s.rootLevelItemIds, p.getItemIcon, p.renderContent, onDrop)
        )
      )
  }

  private val component = ReactComponentB[TreeViewProps]("TreeView")
    .initialState_P(p => p.rootDirectory)
    .renderBackend[TreeViewBackend]
    .build

  def apply(
    rootDirectory: RootDirectory,
    itemCreators: Seq[(String, () => DirectoryItem)],
    getItemIcon: DirectoryItem => ReactNode,
    renderContent: DirectoryItem => ReactNode
  ): ReactElement =
    component(TreeViewProps(rootDirectory, itemCreators, getItemIcon, renderContent))

}

object TVColumn {
  case class TVColumnProps(
    items: Seq[DirectoryItem],
    itemIds: Seq[Int],
    getItemIcon: DirectoryItem => ReactNode,
    renderContent: DirectoryItem => ReactNode,
    onDrop: (String, String) => Callback
  )
  case class TVColumnState(selectedItemId: Int = -1)

  class TVColumnBackend($: BackendScope[TVColumnProps, TVColumnState]) {

    def setSelectedId(id: Int) =
      $.state >>= (s => if(s.selectedItemId == id) selectNone else $.modState(s => s.copy(selectedItemId = id)))

    def selectNone() = $.modState(s => s.copy(selectedItemId = -1))

    def deselectDragged(id: Int) =
      $.state >>= (s => if(id == s.selectedItemId) selectNone else Callback.empty)

    def render(p: TVColumnProps, s: TVColumnState) =
      <.div(
        Style.tvColumn,
        <.ul(
          Style.ul,
          p.itemIds.map{id =>
            val item = p.items.find(_.id == id).get
            <.li(
              Style.li(item.id == s.selectedItemId),
              <.div(
                <.div(Style.icon, p.getItemIcon(item)), item.name, <.div(Style.chevron, Icon.chevronRight)
              ),
              DataOnDrag(item.id.toString, deselectDragged(item.id)),
              OnDataDrop(eventData => p.onDrop(eventData, item.id.toString) >> setSelectedId(item.id)),
              ^.onClick --> (setSelectedId(item.id) >> Callback.log("selected sumthing"))
            )
          }
        ),
        if(s.selectedItemId == -1) ""
        else p.items.find(_.id == s.selectedItemId).get match {
          case Directory(_, _, childrenIds) =>
            TVColumn(p.items, childrenIds, p.getItemIcon, p.renderContent, p.onDrop)
          case item: DirectoryItem => p.renderContent(item)
        }
      )
  }

  val component = ReactComponentB[TVColumnProps]("TVColumn")
    .initialState(TVColumnState())
    .renderBackend[TVColumnBackend]
    .build

  def apply(
    items: Seq[DirectoryItem],
    itemIds: Seq[Int],
    getItemIcon: DirectoryItem => ReactNode,
    renderContent: DirectoryItem => ReactNode,
    onDrop: (String, String) => Callback
  ): ReactElement =
    component(TVColumnProps(items, itemIds, getItemIcon, renderContent, onDrop))
}
