package spgui.widgets.itemeditor

import sp.domain._
import sp.domain.Logic._

import scala.util.Try
import java.util.UUID

sealed trait API_ItemServiceDummy
object API_ItemServiceDummy {
  case class Hello() extends API_ItemServiceDummy
  case class RequestItem(id: UUID) extends API_ItemServiceDummy
  case class RequestSampleItem() extends API_ItemServiceDummy
  case class RequestSampleItems() extends API_ItemServiceDummy
  case class SampleItem(operation: IDAble) extends API_ItemServiceDummy
  case class SampleItemList(items: List[IDAble]) extends API_ItemServiceDummy
  case class Item(item: IDAble) extends API_ItemServiceDummy

  object attributes {
    val service = "Item"
  }

  implicit val fAPIItemServiceDummy: JSFormat[API_ItemServiceDummy] = deriveFormatISA[API_ItemServiceDummy]

  def extract(mess: Try[SPMessage]): Option[(SPHeader, API_ItemServiceDummy)] = for {
    m <- mess.toOption
    h <- m.getHeaderAs[SPHeader].toOption if h.to == attributes.service
    b <- m.getBodyAs[API_ItemServiceDummy].toOption
  } yield (h, b)

  def makeMess(h: SPHeader, b: API_ItemServiceDummy) = SPMessage.makeJson[SPHeader, API_ItemServiceDummy](h, b)
}
