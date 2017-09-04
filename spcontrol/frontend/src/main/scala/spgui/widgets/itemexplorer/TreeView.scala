
package spgui.widgets.itemexplorer

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.vdom.all.aria
import scalacss.ScalaCssReact._

import spgui.components.DragAndDrop.{ DataOnDrag, OnDataDrop }
import spgui.components.{ Icon, SPWidgetElements }

object TreeView {
  case class TreeViewProps(
    rootDirectory: RootDirectory,
    itemCreators: Seq[(String, () => DirectoryItem)],
    getItemIcon: DirectoryItem => VdomNode,
    renderContent: DirectoryItem => VdomNode,
    onSaveButtonClick: RootDirectory => Callback
  )

  case class TreeViewState(
    rt: RootDirectory,
    visIds: Seq[String]
  )

  class TreeViewBackend($: BackendScope[TreeViewProps, TreeViewState]) {
    def addItem(item: DirectoryItem) = {
      $.modState(s => (  TreeViewState(s.rt.addItem(item), s.visIds :+ item.id)) )
    }

    def onDrop(senderId: String, receiverId: String) =
      $.modState(s => s.copy(s.rt.moveItem(senderId, receiverId)))

    private def filter(rts: RootDirectory)(s:String) = {
      var visMap: Seq[String] = Seq()
      rts.items.map(item =>
        if(item.name.toLowerCase.contains(s.toLowerCase)){
          visMap :+= item.id
          visMap = visMap.union(findParentsTo(item,rts))
        })
      $.modState(_.copy(visIds = visMap))
    }

    private def findParentsTo(childItem:DirectoryItem,rts:RootDirectory): Seq[String] = {
      var visMap:Seq[String] = Seq()
      rts.items.foreach(item => item match{
        case item:Directory =>
          if(item.childrenIds.contains(childItem.id))  visMap :+= item.id
        case _ => null
      })
      visMap
    }


    def render(p: TreeViewProps, s: TreeViewState) =
      <.div(^.className := "nav", Style.outerDiv,
        <.div(
          Style.optionBar,
          <.li(SPWidgetElements.dropdown(
            "Add Item",
            p.itemCreators.map(ic => <.div(ic._1, ^.onClick --> addItem(ic._2())))
          )),
          <.li(SPWidgetElements.button(Icon.floppyO, p.onSaveButtonClick(s.rt))),
          SPWidgetElements.TextBox("Filter...", filter(s.rt))
        ),
        <.div(
          Style.treeDiv,
          TVColumn(s.rt.items, s.rt.rootLevelItemIds, p.getItemIcon, p.renderContent, onDrop, s.visIds)
        )
      )
  }

  private val component = ScalaComponent.builder[TreeViewProps]("TreeView")
    .initialStateFromProps(p => TreeViewState(p.rootDirectory, p.rootDirectory.items.map(item => item.id) ) )
    .renderBackend[TreeViewBackend]
    .build

  def apply(
    rootDirectory: RootDirectory,
    itemCreators: Seq[(String, () => DirectoryItem)],
    getItemIcon: DirectoryItem => VdomNode,
    renderContent: DirectoryItem => VdomNode,
    onSaveButtonClick: RootDirectory => Callback
  ): VdomElement =
    component(TreeViewProps(rootDirectory, itemCreators, getItemIcon, renderContent, onSaveButtonClick))

}

object TVColumn {
  case class TVColumnProps(
    items: Seq[DirectoryItem],
    itemIds: Seq[String],
    getItemIcon: DirectoryItem => VdomNode,
    renderContent: DirectoryItem => VdomNode,
    onDrop: (String, String) => Callback,
    visIds : Seq[String]
  )
  case class TVColumnState(selectedItemId: String = "-1")

  class TVColumnBackend($: BackendScope[TVColumnProps, TVColumnState]) {

    def setSelectedId(id: String) =
      $.state >>= (s => if(s.selectedItemId == id) selectNone else $.modState(s => s.copy(selectedItemId = id)))

    def selectNone() = $.modState(s => s.copy(selectedItemId = "-1"))

    def deselectDragged(id: String) =
      $.state >>= (s => if(id == s.selectedItemId) selectNone else Callback.empty)

    def render(p: TVColumnProps, s: TVColumnState) =
      <.div(
        Style.tvColumn,
        <.ul(
          Style.ul,
          {
            val visItems = p.visIds.intersect(p.itemIds)
            visItems.map{id =>
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
            }.toTagMod
          }),
        if(s.selectedItemId == "-1") ""
        else p.items.find(_.id == s.selectedItemId).get match {
          case Directory(_, _, childrenIds) =>
            TVColumn(p.items, childrenIds, p.getItemIcon, p.renderContent, p.onDrop,p.visIds)
          case item: DirectoryItem => p.renderContent(item)
        }
      )
  }

  val component = ScalaComponent.builder[TVColumnProps]("TVColumn")
    .initialState(TVColumnState())
    .renderBackend[TVColumnBackend]
    .build

  def apply(
    items: Seq[DirectoryItem],
    itemIds: Seq[String],
    getItemIcon: DirectoryItem => VdomNode,
    renderContent: DirectoryItem => VdomNode,
    onDrop: (String, String) => Callback,
    visIds : Seq[String]
  ): VdomElement =
    component(TVColumnProps(items, itemIds, getItemIcon, renderContent, onDrop,visIds))
}
