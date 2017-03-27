package spgui.widgets.itemexplorer

trait DirectoryItem {
  val name: String
  // TODO: will be a string/UUID
  val id: String
}
case class Directory(name: String, id: String, childrenIds: Seq[String]) extends DirectoryItem

class RootDirectory(private var _items: Seq[DirectoryItem]) {
  private val nestedItemIds = _items.flatMap{
    case Directory(_, id, childrenIds) => childrenIds
    case item: DirectoryItem => Nil
  }.toSet

  private var _rootLevelItemIds = _items.map(_.id).filterNot(nestedItemIds)

  // parentIdMap(str) points to parent of item of id str, undefined if item is on root level
  private var parentIdMap: Map[String, String] = _items.flatMap{
      case Directory(_, id, childrenIds) => childrenIds.map((_, id))
      case _ => Nil
    }.toMap

  // apparently the only scala way of stopping the client from modifiying a var
  def items = _items
  def rootLevelItemIds = _rootLevelItemIds

  // return self, to make $.modstate calls look cleaner
  def addItem(item: DirectoryItem) = {
    _items = item +: _items
    _rootLevelItemIds = item.id +: _rootLevelItemIds
    this
  }

  // move item to parent of target if target is not a directory
  def moveItem(movedItemId: String, newParentId: String) = {
    _items.find(_.id == newParentId).get match {
      case mapp: Directory =>
        moveItemToDir(movedItemId, newParentId)
      case item: DirectoryItem if(parentIdMap.isDefinedAt(newParentId)) =>
        moveItemToDir(movedItemId, parentIdMap(newParentId))
      case item: DirectoryItem =>
        moveItemToRoot(movedItemId)
    }
    this
  }

  private def moveItemToDir(movedItemId: String, newDirId: String) = {
    _items = _items.map{
      case Directory(name, `newDirId`, childrenIds) => Directory(name, newDirId, movedItemId +: childrenIds)
      case Directory(name, id, childrenIds) => Directory(name, id, childrenIds.filter(_ != movedItemId))
      case item: DirectoryItem => item
    }

    if(!parentIdMap.contains(movedItemId)) _rootLevelItemIds = _rootLevelItemIds.filter(_ != movedItemId)

    parentIdMap = parentIdMap.filterKeys(_ != movedItemId) ++ Map(movedItemId -> newDirId)
  }

  private def moveItemToRoot(movedItemId: String) = {
    _items = _items.map{
      case Directory(name, id, childrenIds) => Directory(name, id, childrenIds.filter(_ != movedItemId))
      case item: DirectoryItem => item
    }
    _rootLevelItemIds = movedItemId +: _rootLevelItemIds
    parentIdMap = parentIdMap.filterKeys(_ != movedItemId)
  }
}
