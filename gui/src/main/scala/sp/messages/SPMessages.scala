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
  case class StatusResponse(service: String, instanceID: Option[ID] = None, instanceName: String = "", tags: List[String] = List(), api: SPAttributes = SPAttributes(), version: Int = 1, attributes: SPAttributes = SPAttributes()) extends APISP

  //  implicit val readWriter: ReadWriter[APISP] =
  //    macroRW[SPError] merge macroRW[SPACK] merge macroRW[SPOK] merge macroRW[SPDone] merge macroRW[StatusRequest] merge macroRW[StatusResponse]
}


object Pickles extends SPParser {

  case class SPHeader(from: String = "", // the name of the sender
                      to: String = "", // the name of the receiver, empty if to anyone
                      reqID: UUID = UUID.randomUUID(), // the id to use for replies
                      reply: SPValue = SPValue.empty, // A data structure that should be included in all replies to be used for matching
                      fromTags: List[String] = List(), // a list of tags to define things about the sender. For example where the sender is located
                      toTags: List[String] = List(), // a list of tags to define things about possible receivers
                      attributes: SPAttributes = SPAttributes() // to be used in some scenarios, where more info in the header is needed
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


//  implicit lazy val asdasd = {val asdasd = ();   macroRW[SPMessage]}
//  implicit lazy val sdffsaf = {val sdffsaf = ();   macroRW[SPHeader]}
//  implicit lazy val wefawef = {val wefawef = ();   macroRW[APISP]}
//  implicit lazy val aasdasd = {val aasdasd = ();   macroRW[StateUpdater]}
//  implicit lazy val csdcsdc = {val csdcsdc = ();   macroRW[StateEvaluator]}
//  implicit lazy val oshffef = {val oshffef = ();   macroRW[PropositionEvaluator]}
//  implicit lazy val scvvvds = {val scvvvds = ();   macroRW[Proposition]}
//  implicit lazy val ccscsc = {val ccscsc = ();   macroRW[PropositionCondition]}
//  implicit lazy val vfvfvfv = {val vfvfvfv = ();   macroRW[Condition]}
//  implicit lazy val bfgbfgb = {val bfgbfgb = ();   macroRW[Operation]}

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