package spgui.widgets.itemexplorer

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import scalacss.ScalaCssReact._

import spgui.SPWidget
import spgui.components.Icon

import java.util.UUID

case class OperationDirItem(name: String, id: String, content: String) extends DirectoryItem
case class SOPSpecDirItem(name: String, id: String, content: String) extends DirectoryItem

object SPItemsToRootDirectory {
  def apply(spItems: Seq[IDAble]) = {
    // TODO: delegate the parentlessIds fiddling to RootDirectory
    var parentlessIds = spItems.map(_.id.toString)
    val dirItems = spItems.map{
      case HierarchyRoot(name, children, attributes, id) =>
        Directory(name, id.toString, children.map{child =>
                    parentlessIds = parentlessIds.filter(_ != child.item.toString)
                    child.item.toString
                  })
      case Operation(name, conditions, attributes, id) =>
        OperationDirItem(name, id.toString, "OpDirItemContent")
      case SOPSpec(name, sop, attributes, id) =>
        SOPSpecDirItem(name, id.toString, "SOPSpecDirItemContent")
    }
    new RootDirectory(dirItems, parentlessIds)
  }
}

object GetItemIcon {
  def apply(item: DirectoryItem): ReactNode = item match {
    case d: Directory => Icon.folder
    case op: OperationDirItem => Icon.arrowCircleRight
    case ss: SOPSpecDirItem => Icon.sitemap
  }
}

object RenderItemContent {
  def apply(item: DirectoryItem): ReactNode = item match {
    case op: OperationDirItem => op.content
    case ss: SOPSpecDirItem => ss.content
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
