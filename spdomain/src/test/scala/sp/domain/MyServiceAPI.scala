package sp.domain

import Logic._

object MyServiceAPI {
  sealed trait Request
  sealed trait Response

  case class Connect(url: String) extends Request
  case object Disconnect extends Request
  case object GetNodes extends Request
  case class Subscribe(nodeIDs: List[String]) extends Request
  case class Write(node: String, value: SPValue) extends Request

  // answers
  case class ConnectionStatus(connected: Boolean) extends Response
  case class AvailableNodes(nodes: Map[String, String]) extends Response
  case class StateUpdate(state: Map[String, SPValue], timeStamp: String) extends Response


  object Request {
    implicit lazy val fMyAPIRequest: JSFormat[Request] = deriveFormatISA[Request]
  }
  object Response {
    implicit lazy val fMyAPIResponse: JSFormat[Response] = deriveFormatISA[Response]
  }


}
