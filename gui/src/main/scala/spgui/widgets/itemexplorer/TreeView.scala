package spgui.widgets.itemexplorer

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.vdom.all.aria
import scalacss.ScalaCssReact._

import spgui.components.DragAndDrop.{ DataOnDrag, OnDataDrop }
import spgui.components.{ Icon, Dropdown }

object TreeView {
  case class TreeViewProps(
    rootDirectory: RootDirectory,
    itemCreators: Seq[(String, () => DirectoryItem)],
    getItemIcon: DirectoryItem => ReactNode,
    renderContent: DirectoryItem => ReactNode,
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

    def onFilterTextChange(e :ReactEventI): CallbackTo[Unit] =
        e.extract(_.target.value)(searchText => { $.state >>= (p => ( filter(searchText,p.rt)))  })

    private def filter(s:String,rts:RootDirectory) = {
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
      <.div(
        Style.outerDiv,
        <.div(
          Style.optionBar,
          Dropdown(
            <.div(
              "Add Item",
              Icon.chevronDown,
              ^.className := "btn btn-default"
            ),
            p.itemCreators.map(ic => <.div(ic._1, ^.onClick --> addItem(ic._2()))): _*
          ),
          <.div(
            ^.className := "btn btn-default",
            Icon.floppyO,
            ^.onClick --> p.onSaveButtonClick(s.rt)
          ),
          <.div(
            ^.className := "input-group",
            <.input(
              ^.className := "form-control",
              ^.placeholder := "Filter",
              ^.aria.describedby := "basic-addon1",
              ^.onChange ==> onFilterTextChange
            )
          )
        ),
        <.div(
          Style.treeDiv,
          TVColumn(s.rt.items, s.rt.rootLevelItemIds, p.getItemIcon, p.renderContent, onDrop, s.visIds)
        )
      )
  }

  private val component = ReactComponentB[TreeViewProps]("TreeView")
    .initialState_P(p => TreeViewState(p.rootDirectory, p.rootDirectory.items.map(item => item.id) ) )
    .renderBackend[TreeViewBackend]
    .build

  def apply(
    rootDirectory: RootDirectory,
    itemCreators: Seq[(String, () => DirectoryItem)],
    getItemIcon: DirectoryItem => ReactNode,
    renderContent: DirectoryItem => ReactNode,
    onSaveButtonClick: RootDirectory => Callback
  ): ReactElement =
    component(TreeViewProps(rootDirectory, itemCreators, getItemIcon, renderContent, onSaveButtonClick))

}

object TVColumn {
  case class TVColumnProps(
    items: Seq[DirectoryItem],
    itemIds: Seq[String],
    getItemIcon: DirectoryItem => ReactNode,
    renderContent: DirectoryItem => ReactNode,
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
          }
        }),
        if(s.selectedItemId == "-1") ""
        else p.items.find(_.id == s.selectedItemId).get match {
          case Directory(_, _, childrenIds) =>
            TVColumn(p.items, childrenIds, p.getItemIcon, p.renderContent, p.onDrop,p.visIds)
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
    itemIds: Seq[String],
    getItemIcon: DirectoryItem => ReactNode,
    renderContent: DirectoryItem => ReactNode,
    onDrop: (String, String) => Callback,
    visIds : Seq[String]
  ): ReactElement =
    component(TVColumnProps(items, itemIds, getItemIcon, renderContent, onDrop,visIds))
}
