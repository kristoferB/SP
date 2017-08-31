package spgui.communication

import sp.domain._
import Logic._


object APIWebSocket {
  sealed trait API
  case class PublishMessage(mess: SPMessage, topic: String = "services") extends API
  case class FilterHeader(keyValues: Map[String, Set[SPValue]]) extends API
  case class FilterBody(keyValues: Map[String, Set[SPValue]]) extends API
  // removing filters with keys in the set keys. If it is empty, all keys are removed
  case class ClearFilters(keys: Set[String] = Set()) extends API

  object API {
    implicit val apiFormat: JSFormat[API] = deriveFormatISA[API]
  }

}