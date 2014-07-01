package sp.json




trait SPJsonDomain {

  import sp.domain._
  import spray.json._
  import DefaultJsonProtocol._

  implicit object IDJsonFormat extends RootJsonFormat[ID] {
    def write(id: ID) = id.value.toString.toJson

    def read(value: JsValue) = value match {
      case JsString(idStr) =>
        ID.makeID(idStr) match {
          case Some(id) => id
          case None => deserializationError("ID expected, got " + idStr)
        }
      case x@_ => deserializationError(s"ID expected, got $x")
    }
  }

    implicit object SOPJsonFormat extends RootJsonFormat[SOP] {
    def write(sop: SOP) = {
      val res = JsObject(
        "isa" -> JsString(sop.getClass().getSimpleName()),
        "sop" -> JsArray(sop.children.filter(_ != EmptySOP) map (c => write(c)) toList)
      )
      if (sop.isInstanceOf[Hierarchy]) {
        val o = sop.asInstanceOf[Hierarchy].operation
        JsObject(res.fields + ("operation" -> o.toJson))
      } else res
    }
    def read(value: JsValue) = throw new DeserializationException("Operation JsonParse not implemented")

  }



    implicit object SPAttributeValueFormat extends RootJsonFormat[SPAttributeValue] {
    def write(x: SPAttributeValue) = {
      x match {
          case StringPrimitive(x) => x.toJson
          case IntPrimitive(x) => x.toJson
          case LongPrimitive(x) => x.toJson
          case DoublePrimitive(x) => x.toJson
          case BoolPrimitive(x) => x.toJson
          case DatePrimitive(x) => x.toString().toJson
          case DurationPrimitive(x) => x.getMillis().toJson
          case IDPrimitive(x) => x.toJson
          case ListPrimitive(x) => x.toJson
          case MapPrimitive(x) => x.toJson
          case OptionAsPrimitive(x) => x match {
            case Some(d) => d.toJson
            case None => JsNull
          }
      }
    }
    def read(value: JsValue) = value match {
      case JsString(x) => {
        DatePrimitive.stringToDate(x) match {
          case Some(d) => d
          case None => {
            ID.makeID(x) match {
              case Some(id) => IDPrimitive(id)
              case None => StringPrimitive(x)
            }
          }
        }
      }
      case JsNumber(x) => {
        if (x.isValidInt) IntPrimitive(x.intValue)
        else if (x.isValidDouble) DoublePrimitive(x.doubleValue)
        else if (x.isValidLong) LongPrimitive(x.longValue)
        else StringPrimitive(x.toString)
      }
      case JsBoolean(x) => BoolPrimitive(x)
      case JsArray(xs) => ListPrimitive(xs map (_.convertTo[SPAttributeValue]))
      case JsObject(kvs) => MapPrimitive(kvs map {case (k,v)=> k->v.convertTo[SPAttributeValue]})
      case JsNull => OptionAsPrimitive(None)
      case _ => throw new DeserializationException("can not convert to SPAttribute: "+value)
    }
  }

    implicit object SPAttributesFormat extends RootJsonFormat[SPAttributes] {
    def write(x: SPAttributes) = {
      JsObject({
        x.attrs map {case (k,v)=> k -> {v.toJson}}
      })

    }
    def read(value: JsValue) = value match {
      case a: JsObject => {
        val map = a.fields map {case (k,v)=> k->v.convertTo[SPAttributeValue]}
        SPAttributes(map)
      }
      case _ => throw new DeserializationException("can not convert to SPAttribute: "+value)
    }
  }



    implicit object RangeFormat extends RootJsonFormat[Range] {
    def write(x: Range) = {
      JsObject(
        "start" -> x.start.toJson,
        "end" -> x.end.toJson,
        "step" -> x.step.toJson
      )
    }
    def read(value: JsValue) = {
      value.asJsObject.getFields("start", "end", "step") match {
        case Seq(JsNumber(start), JsNumber(end), JsNumber(step)) =>
          new Range(start.toInt, end.toInt, step.toInt)
        case _ => throw new DeserializationException("Range expected")
      }

    }
    }

  implicit val svFormat = jsonFormat3(StateVariable)


//  implicit val svintFormat = jsonFormat2(IntVariable)
//  implicit val svrrintFormat = jsonFormat3(RestrictedIntRangeVariable)
//  implicit val svrintFormat = jsonFormat3(RestrictedIntVariable)
//  implicit val svstrFormat = jsonFormat2(StringVariable)
//  implicit val svrestrFormat = jsonFormat3(RestrictedStringVariable)
//  implicit val svboolFormat = jsonFormat2(BooleanVariable)
//
//  implicit object StateVariableFormat extends RootJsonFormat[StateVariable] {
//    def write(x: StateVariable) = {
//      JsObject()
////      JsObject( x match {
////        case x: IntVariable => x.toJson.asJsObject.fields + ("type"-> "StateVariable".toJson)
////        case x: RestrictedIntRangeVariable => x.toJson.asJsObject.fields + ("type"-> "StateVariable".toJson)
////        case x: RestrictedIntVariable => x.toJson.asJsObject.fields + ("type"-> "StateVariable".toJson)
////        case x: StringVariable => x.toJson.asJsObject.fields + ("type"-> "StateVariable".toJson)
////        case x: RestrictedStringVariable => x.toJson.asJsObject.fields + ("type"-> "StateVariable".toJson)
////        case x: BooleanVariable => x.toJson.asJsObject.fields + ("type"-> "StateVariable".toJson)
////      })
//    }
//    def read(value: JsValue) = {
//      val obj = for {
//        t <- value.asJsObject.fields.get("type")
//      } yield t match {
//          case JsString("StateVariable") => IntVariable("hej")
//          case _ => throw new DeserializationException(s"StateVariable could not be read: $value")
//        }
//      obj match {
//        case Some(o) => o
//        case None => throw new DeserializationException(s"StateVariable could not be read: $value")
//      }
//    }
//  }




}