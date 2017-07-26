package sp.domain.logic

import org.threeten.bp._
import sp.domain._
import play.api.libs.json._
import julienrf.json.derived


object JsonLogic extends JsonImplicit

trait JsonImplicit extends JsonDerived {

  type JSFormat[T] = OFormat[T]
  type JSReads[T] = Reads[T]
  type JSWrites[T] = OWrites[T]

  implicit lazy val stateEvFormat: OFormat[StateEvaluator] = derived.flat.oformat[StateEvaluator]((__ \ "isa").format[String])
  implicit lazy val stateUpdFormat: OFormat[StateUpdater] = derived.flat.oformat[StateUpdater]((__ \ "isa").format[String])
  implicit lazy val propFormat: OFormat[Proposition] = derived.flat.oformat[Proposition]((__ \ "isa").format[String])
  implicit lazy val actionFormat = Json.format[Action]
  implicit lazy val conditionFormat = Json.format[Condition]
  implicit lazy val sopFormat: OFormat[SOP] = derived.flat.oformat[SOP]((__ \ "isa").format[String])
  implicit lazy val structNode = Json.format[StructNode]

  implicit lazy val stateF = new Format[Map[ID, SPValue]] {
    override def reads(json: JsValue): JsResult[Map[ID, SPValue]] = {
      json.validate[Map[String, SPValue]].map(xs => xs.collect{case (k, v) if ID.isID(k) => ID.makeID(k).get -> v})
    }

    override def writes(xs: Map[ID, SPValue]): JsValue = {
      val toFixedMap = xs.map{case (k, v) => k.toString -> v}
      SPValue(toFixedMap)
    }

  }

  implicit lazy val operationF: Reads[Operation] = derived.flat.reads[Operation]((__ \ "isa").read[String])
  implicit lazy val thingF: Reads[Thing] = derived.flat.reads[Thing]((__ \ "isa").read[String])
  implicit lazy val sopSpecF: Reads[SOPSpec] = derived.flat.reads[SOPSpec]((__ \ "isa").read[String])
  implicit lazy val spSpecF: Reads[SPSpec] = derived.flat.reads[SPSpec]((__ \ "isa").read[String])
  implicit lazy val spRes: Reads[SPResult] = derived.flat.reads[SPResult]((__ \ "isa").read[String])
  implicit lazy val spState: Reads[SPState] = derived.flat.reads[SPState]((__ \ "isa").read[String])
  implicit lazy val structF: Reads[Struct] = derived.flat.reads[Struct]((__ \ "isa").read[String])
  implicit lazy val idableFormat: OFormat[IDAble] = derived.flat.oformat[IDAble]((__ \ "isa").format[String])


  val dateF = format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
  implicit lazy val javatimeF = new Format[ZonedDateTime] {
    override def reads(json: JsValue): JsResult[ZonedDateTime] = {
      json.validate[String].map(ZonedDateTime.parse(_, dateF))
    }

    override def writes(o: ZonedDateTime): JsValue = {
      Json.toJson(o.format(dateF))
    }

  }

}




trait JsonDerived{
  import language.experimental.macros
  //def spFormat[A]: OFormat[A] =  derived.oformat[A]()
  val jsonISA = (__ \ "isa").format[String]


  import play.api.libs.json.{OFormat, OWrites, Reads}
  import shapeless.Lazy
  import julienrf.json.derived._

  def deriveFormatSimple[A](implicit derivedReads: Lazy[DerivedReads[A]], derivedOWrites: Lazy[DerivedOWrites[A]]): OFormat[A] =
    OFormat(derivedReads.value.reads(TypeTagReads.nested, NameAdapter.identity), derivedOWrites.value.owrites(TypeTagOWrites.nested, NameAdapter.identity))


  def deriveFormatISA[A](implicit derivedReads: Lazy[DerivedReads[A]], derivedOWrites: Lazy[DerivedOWrites[A]]): OFormat[A] =
    OFormat(derivedReads.value.reads(TypeTagReads.flat(jsonISA), NameAdapter.identity), derivedOWrites.value.owrites(TypeTagOWrites.flat(jsonISA), NameAdapter.identity))

  object derived {

    def reads[A](adapter: NameAdapter = NameAdapter.identity)(implicit derivedReads: Lazy[DerivedReads[A]]): Reads[A] = derivedReads.value.reads(TypeTagReads.nested, adapter)

    def owrites[A](adapter: NameAdapter = NameAdapter.identity)(implicit derivedOWrites: Lazy[DerivedOWrites[A]]): OWrites[A] = derivedOWrites.value.owrites(TypeTagOWrites.nested, adapter)

    def oformat[A](adapter: NameAdapter = NameAdapter.identity)(implicit derivedReads: Lazy[DerivedReads[A]], derivedOWrites: Lazy[DerivedOWrites[A]]): OFormat[A] =
      OFormat(derivedReads.value.reads(TypeTagReads.nested, adapter), derivedOWrites.value.owrites(TypeTagOWrites.nested, adapter))

    object flat {

      def reads[A](typeName: Reads[String], adapter: NameAdapter = NameAdapter.identity)(implicit derivedReads: Lazy[DerivedReads[A]]): Reads[A] =
        derivedReads.value.reads(TypeTagReads.flat(typeName), adapter)

      def owrites[A](typeName: OWrites[String], adapter: NameAdapter = NameAdapter.identity)(implicit derivedOWrites: Lazy[DerivedOWrites[A]]): OWrites[A] =
        derivedOWrites.value.owrites(TypeTagOWrites.flat(typeName), adapter)

      def oformat[A](typeName: OFormat[String], adapter: NameAdapter = NameAdapter.identity)(implicit derivedReads: Lazy[DerivedReads[A]], derivedOWrites: Lazy[DerivedOWrites[A]]): OFormat[A] =
        OFormat(derivedReads.value.reads(TypeTagReads.flat(typeName), adapter), derivedOWrites.value.owrites(TypeTagOWrites.flat(typeName), adapter))

    }

  }


}


