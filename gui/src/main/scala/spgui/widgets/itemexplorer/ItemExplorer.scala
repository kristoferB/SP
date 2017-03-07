package spgui.widgets.itemexplorer

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import scalacss.ScalaCssReact._

import spgui.SPWidget
import spgui.components.Icon


// TODO: replace with SP API
case class Spotify(name: String, id: String, content: String) extends DirectoryItem
case class Youtube(name: String, id: String, content: String) extends DirectoryItem

object IconFunc {
  def apply(item: DirectoryItem): ReactNode = item match {
    case Directory(_, _, _) => Icon.folder
    case Spotify(_, _, _) => Icon.spotify
    case Youtube(_, _, _) => Icon.youtube
  }
}

object RenderContent {
  def apply(item: DirectoryItem): ReactNode = item match {
    case s: Spotify => s.content
    case yt: Youtube => yt.content
  }
}

object ListItems {
  val listItems = List(
    Youtube("Smör", "2", "mjölk"),
    Youtube("Ägg", "3", "kalcium"),
    Spotify("Kladd", "4", "Kladd"),
    Directory("kaka", "5", List("2", "3")),
    Spotify("Äpplen", "6", "paj")
  )
  val rootLevelItemIds = List("4", "5", "6")
  def apply() = new RootDirectory(listItems, rootLevelItemIds)
}

object ItemExplorer {

  def emptyMap() = Directory("EmptyMap", (util.Random.nextInt(1000000) + 10000).toString, List())
  def newSpotify() = Spotify("NewYT", (util.Random.nextInt(1000000) + 10000).toString, "content of NewSpotify")
  def newYT() = Youtube("NewYT", (util.Random.nextInt(1000000) + 10000).toString, "content of NewYT")

  def apply() = SPWidget(spwb => TreeView(
                           ListItems(),
                           ("Directory", () => emptyMap()) ::
                             ("Spotify", () => newSpotify()) ::
                             ("Youtube", () => newYT()) ::
                             Nil,
                           item => IconFunc(item),
                           item => RenderContent(item)
                         ))
}
