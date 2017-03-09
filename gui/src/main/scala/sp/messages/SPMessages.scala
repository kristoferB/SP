package sp.messages

import java.util.UUID
import sp.domain._
import scala.util.{Try, Success, Failure}

sealed trait APISP
object APISP {
  case class SPError(message: String, attributes: SPAttributes = SPAttributes()) extends APISP
  case class SPACK(attributes: SPAttributes = SPAttributes()) extends APISP
  case class SPOK(attributes: SPAttributes = SPAttributes()) extends APISP
  case class SPDone(attributes: SPAttributes = SPAttributes()) extends APISP

  case class StatusRequest(attributes: SPAttributes = SPAttributes()) extends APISP
  case class StatusResponse(attributes: SPAttributes = SPAttributes()) extends APISP

  //  implicit val readWriter: ReadWriter[APISP] =
  //    macroRW[SPError] merge macroRW[SPACK] merge macroRW[SPOK] merge macroRW[SPDone] merge macroRW[StatusRequest] merge macroRW[StatusResponse]
}


object Pickles extends SPParser {

  case class SPHeader(from: String, // the name of the sender
                      fromID: Option[UUID] = None, // the id of the sender if applicable
                      to: String = "", // the name of the receiver, empty if to anyone
                      toID: Option[UUID] = None, // the id of the receiver if applicable
                      replyTo: String = "", // the name of who to reply to
                      replyToID: Option[UUID] = None, // the id of the whoto reply to if applicable
                      reqID: UUID = UUID.randomUUID(), // the id to use for replies
                      replyFrom: String = "", // the name of the replier
                      replyFromID: Option[UUID]= None, // the id of the replier if applicable
                      messageID: UUID = UUID.randomUUID(), // a unique id for each message
                      fromTags: List[String] = List(), // a list of tags to define things about the sender. For example where the sender is located
                      toTags: List[String] = List(), // a list of tags to define things about possible receivers
                      history: List[UUID] = List() // to be used in some scenarios, to trace the route of a message by stroing message ids
                     )


  case class SPMessage(header: SPValue, body: SPValue) {
    def getHeaderAs[T: Reader] = fromSPValue[T](header)
    def getBodyAs[T: Reader] = fromSPValue[T](body)

    def toJson = write(this)

    /**
      * Creates an updated SPMessage that keeps the header keyvals that is not defined in h
      * @param h The upd key vals in the header
      * @param b The new body
      * @return an updated SPMessage
      */
    def make[T: Writer, V: Writer](h: T, b: V) = {
        val newh = toSPValue[T](h)
        val newb = toSPValue[V](b)
        val updH = header.union(newh)
        SPMessage(updH, newb)

    }
    def makeJson[T: Writer, V: Writer](header: T, body: V) = {
      this.make[T, V](header, body).toJson
    }
  }

  object SPMessage {
    def make[T: Writer, V: Writer](header: T, body: V) = {
        val h = toSPValue(header)
        val b = toSPValue(body)
        SPMessage(h, b)
    }

    def makeJson[T: Writer, V: Writer](header: T, body: V) = {
      make[T, V](header, body).toJson
    }

    def fromJson(json: String) = Try{
      val x = upickle.json.read(json)
      SPMessage(x.obj("header"), x.obj("body"))
    }
  }





  def toJson[T: Writer](expr: T, indent: Int = 0): String = upickle.json.write(writeJs(expr), indent)
  def toSPValue[T: Writer](expr: T): SPValue = implicitly[Writer[T]].write(expr)
  def toSPAttributes[T: Writer](expr: T): SPAttributes = toSPValue[T](expr).asInstanceOf[SPAttributes]
  def *[T: Writer](expr: T): SPValue = toSPValue[T](expr)
  def **[T: Writer](expr: T): SPAttributes = toSPAttributes[T](expr)

  def fromJson[T: Reader](expr: String): Try[T] = Try{readJs[T](upickle.json.read(expr))}
  def fromSPValue[T: Reader](expr: SPValue): Try[T] = Try{implicitly[Reader[T]].read(expr)}
  def fromJsonToSPValue(expr: String): Try[SPValue] = Try{upickle.json.read(expr)}
  def fromJsonToSPAttributes(expr: String): Try[SPAttributes] = Try{upickle.json.read(expr).asInstanceOf[SPAttributes]}


}

trait SPParser extends upickle.AttributeTagged {
  import upickle._
  import scala.reflect.ClassTag

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