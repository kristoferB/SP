package spgui.communication

import java.util.UUID

import sp.domain._
import sp.messages._
import Pickles._


package APIWebSocket {
  sealed trait  APIWebSocket
  case class PublishMessage(mess: SPMessage, topic: String = "services") extends APIWebSocket
  case class FilterHeader(keyValues: Map[String, Set[Pickle]]) extends APIWebSocket
  case class FilterBody(keyValues: Map[String, Set[Pickle]]) extends APIWebSocket
  // removing filters with keys in the set keys. If it is empty, all keys are removed
  case class ClearFilters(keys: Set[String] = Set()) extends APIWebSocket
}

