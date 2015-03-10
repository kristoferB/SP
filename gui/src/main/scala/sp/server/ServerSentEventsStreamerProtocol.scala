package sp.server

object ServerSentEventsStreamerProtocol {
  sealed trait SSEMessage
  case class Event(id: Option[Long], event: Option[String], data: String) extends SSEMessage
  case object EndOfEvents extends SSEMessage
}