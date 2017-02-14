package communication

import java.util.UUID
import upickle._

case class UPickleMessage(header: upickle.Js.Value, body: upickle.Js.Value)
case class SPHeader(from: String, to: String, replyTo: String, reqID: UUID = UUID.randomUUID(), replyFrom: String = "", replyID: Option[UUID] = None, answerDirect: Boolean = false)


sealed trait APIWebSocket
object APIWebSocket {
  case class PublishMessage(mess: UPickleMessage, topic: String = "services") extends APIWebSocket

  // fixa så att vi använder APISP i backendComm
  case class SPACK(message: String) extends APIWebSocket
  case class SPError(message: String, attr: Option[upickle.Js.Value] = None) extends APIWebSocket

  import APIParser._
  implicit val readWriter: ReadWriter[APIWebSocket] =
    macroRW[SPError] merge macroRW[SPACK] merge macroRW[PublishMessage]
}

sealed trait APISP
object APISP {
  case class SPError(message: String, attributes: Option [upickle.Js.Obj] = None) extends APISP
  case class SPACK(attributes: Option [upickle.Js.Obj]= None) extends APISP
  case class SPOK(attributes: Option [upickle.Js.Obj]= None) extends APISP
  case class SPDone(attributes: Option [upickle.Js.Obj]= None) extends APISP

  case class StatusRequest(attributes: Option [upickle.Js.Obj]= None) extends APISP
  case class StatusResponse(attributes: Option [upickle.Js.Obj]= None) extends APISP

  import APIParser._
  implicit val readWriter: ReadWriter[APISP] =
    macroRW[SPError] merge macroRW[SPACK] merge macroRW[SPOK] merge macroRW[SPDone] merge macroRW[StatusRequest] merge macroRW[StatusResponse]
}





import scala.reflect.ClassTag
object APIParser extends upickle.AttributeTagged {
  override val tagName = "isa"

  override def annotate[V: ClassTag](rw: Reader[V], n: String) = Reader[V]{
    case Js.Obj(x@_*) if x.contains((tagName, Js.Str(n.split('.').takeRight(2).mkString(".")))) =>
      rw.read(Js.Obj(x.filter(_._1 != tagName):_*))
  }

  override def annotate[V: ClassTag](rw: Writer[V], n: String) = Writer[V]{ case x: V =>
    val filter = n.split('.').takeRight(2).mkString(".")
    Js.Obj((tagName, Js.Str(filter)) +: rw.write(x).asInstanceOf[Js.Obj].value:_*)
  }

}
