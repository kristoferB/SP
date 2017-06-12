package spgui.widgets.itemexplorer

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import scalacss.ScalaCssReact._

import spgui.SPWidget
import spgui.components.Icon

import java.util.UUID
import scalajs.js
import js.Dynamic.{ literal => l }
import js.JSON

// TODO refactor this and TreeView into diode-like one-source-of-truth thinking
// RootDirectory should not be needed
// upon dragging an item onto another, a validation should be made somewhere and the notification
// should be sent to itemService/modelService which should in turn notify ItemExplorer to rerender itself
// should have more props and less state

case class OperationDirItem(name: String, id: String, content: String) extends DirectoryItem
case class SOPSpecDirItem(name: String, id: String, content: String) extends DirectoryItem

object SPItemsToRootDirectory {
  def apply(spItems: Seq[IDAble]) = {
    val dirItems = spItems.map{
      case HierarchyRoot(name, children, attributes, id) =>
        Directory(name, id.toString, children.map(_.item.toString))
      case Operation(name, conditions, attributes, id) =>
        OperationDirItem(name, id.toString, "OpDirItemContent")
      case SOPSpec(name, sop, attributes, id) =>
        SOPSpecDirItem(name, id.toString, "SOPSpecDirItemContent")
    }
    new RootDirectory(dirItems)
  }
}

object GetItemIcon {
  def apply(item: DirectoryItem): VdomNode = item match {
    case d: Directory => Icon.folder
    case op: OperationDirItem => Icon.arrowCircleRight
    case ss: SOPSpecDirItem => Icon.sitemap
  }
}

object RenderItemContent {
  // pre-tag keeps the indentation and gives a nice frame
  def contentToElement(content: js.Object) = <.pre(
    Style.itemContent,
    JSON.stringify(content, space = 2),
    ^.onClick --> Callback.log(
      "Clicked button with content " + JSON.stringify(content) +
        ". TODO: make this open the json in itemeditor"
    )
  )

  def apply(item: DirectoryItem): VdomNode = item match {
    case item: OperationDirItem => contentToElement(l("stuff" -> item.content))
    case item: SOPSpecDirItem => contentToElement(l("stuff" -> item.content))

  }
}

object OnSaveButtonClick {
  val printText = Callback.log("You clicked the save button, it does nothing for now, this callback has access to the directoryItems, however, see below:")
  val printItems = (rootDir: RootDirectory) => Callback.log(rootDir.items.toString)
  def apply(rootDirectory: RootDirectory) = printText >> printItems(rootDirectory)
}

object ItemExplorer {

  def emptyDir() = Directory("New Directory", UUID.randomUUID().toString, List())

  def apply() = SPWidget(spwb => TreeView(
                           SPItemsToRootDirectory(SampleSPItems()),
                           ("Directory", () => emptyDir()) ::
                             Nil,
                           item => GetItemIcon(item),
                           item => RenderItemContent(item),
                           rootDir => OnSaveButtonClick(rootDir)
                         ))
}
