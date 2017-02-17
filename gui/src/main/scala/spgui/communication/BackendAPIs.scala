package spgui.communication

import java.util.UUID

import sp.domain._
import sp.messages._
import Pickles._


sealed trait APIWebSocket
object APIWebSocket {
  case class PublishMessage(mess: SPMessage, topic: String = "services") extends APIWebSocket

  // fixa så att vi använder APISP i backendComm
  case class SPACK(message: String) extends APIWebSocket
  case class SPError(message: String, attr: Option[upickle.Js.Value] = None) extends APIWebSocket

  implicit val readWriter: ReadWriter[APIWebSocket] =
    macroRW[SPError] merge macroRW[SPACK] merge macroRW[PublishMessage]
}


