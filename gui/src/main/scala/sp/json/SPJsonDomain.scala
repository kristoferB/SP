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
    def read(value: JsValue): SOP = value match {
      case sop: JsObject => {
        sop.getFields("isa", "sop") match {
          case Seq(JsString("Parallel"), xs: JsArray) =>
            Parallel((xs.elements map read):_*)
          case Seq(JsString("Alternative"), xs: JsArray) =>
            Alternative((xs.elements map read):_*)
          case Seq(JsString("Arbitrary"), xs: JsArray) =>
            Arbitrary((xs.elements map read):_*)
          case Seq(JsString("Sequence"), xs: JsArray) =>
            Sequence((xs.elements map read):_*)
          case Seq(JsString("SometimeSequence"), xs: JsArray) =>
            SometimeSequence((xs.elements map read):_*)
          case Seq(JsString("Other"), xs: JsArray) =>
            Other((xs.elements map read):_*)
          case Seq(JsString("Hierarchy"),  xs: JsArray) => {
            sop.fields.get("operation") match {
              case Some(id) => Hierarchy(id.convertTo[ID], (xs.elements map read):_*)
              case None => throw new DeserializationException(s"can not find field operation with id on Hierarchy: $value")
            }
          }
          case _ => throw new DeserializationException(s"can not convert the SOP from $value")
        }
      }
      case _ => throw new DeserializationException(s"can not convert the SOP from $value")
    }
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

  implicit object svFormat extends RootJsonFormat[StateVariable] {
    def write(x: StateVariable) = {
      JsObject(
        "name" -> x.name.toJson,
        "attributes" -> x.attributes.toJson,
        "id" -> x.id.toJson
      )
    }
    def read(value: JsValue) = value match {
      case x: JsObject => {
        val id = if (x.fields.contains("id")) x.fields("id").convertTo[ID] else ID.newID
        value.asJsObject.getFields("name", "attributes") match {
          case Seq(JsString(n), attr: JsObject) => StateVariable(n, attr.convertTo[SPAttributes], id)
          case _ => throw new DeserializationException(s"can not convert the Statevariable from $x")
        }
      }
      case _ => throw new DeserializationException("Object expected")
    }
  }




  implicit val vHolderFormat = jsonFormat1(ValueHolder)
  implicit val svidEvalFormat = jsonFormat1(SVIDEval)
  implicit val svnameEvalFormat = jsonFormat1(SVNameEval)


  implicit object StateEvalFormat extends RootJsonFormat[StateEvaluator] {
    def write(x: StateEvaluator) = x match {
      case p: SVIDEval => p.toJson
      case p: ValueHolder => p.toJson
      case p: SVNameEval => p.toJson
      case _ => throw new SerializationException(s"Could not convert that type of condition $x")
    }
    def read(value: JsValue) = {
      value.asJsObject.getFields("v") match {
        case Seq(v: JsValue) => {
          v.convertTo[SPAttributeValue] match {
            case IDPrimitive(id) => SVIDEval(id)
            case a: SPAttributeValue => ValueHolder(a)
          }
        }
        case _ => throw new DeserializationException("StateEvaluator expected")
      }
    }
  }


  implicit val andFormat: JsonFormat[AND] = lazyFormat(jsonFormat(AND, "props"))
  implicit val orFormat: JsonFormat[OR] = lazyFormat(jsonFormat(OR, "props"))
  implicit val notFormat: JsonFormat[NOT] = lazyFormat(jsonFormat(NOT, "p"))
  implicit val eqFormat: JsonFormat[EQ] = lazyFormat(jsonFormat(EQ, "left", "right"))
  implicit val neqFormat: JsonFormat[NEQ] = lazyFormat(jsonFormat(NEQ, "left", "right"))

  implicit object PropositionFormat extends RootJsonFormat[Proposition] {
    def write(p: Proposition) = p match {
      case x: AND => JsObject("isa"->"AND".toJson, "props"-> x.props.toJson)
      case x: OR => JsObject("isa"->"OR".toJson, "props"-> x.props.toJson)
      case x: NOT => JsObject("isa"->"NOT".toJson, "prop"-> x.p.toJson)
      case x: EQ => JsObject("isa"->"EQ".toJson, "left"-> x.left.toJson, "right" -> x.right.toJson)
      case x: NEQ => JsObject("isa"->"NEQ".toJson, "left"-> x.left.toJson, "right" -> x.right.toJson)
      case _ => throw new SerializationException(s"Could not convert that type of proposition $p")
    }
    def read(value: JsValue) = {
       val obj = for {
         t <- value.asJsObject.fields.get("isa")
      } yield {
        t match {
          case JsString("AND") => value.convertTo[AND]
          case JsString("OR") => value.convertTo[OR]
          case JsString("NOT") => value.convertTo[NOT]
          case JsString("EQ") => value.convertTo[EQ]
          case JsString("NEW") => value.convertTo[NEQ]
          case _ => throw new DeserializationException(s"IDAble could not be read: $value")
        }
      }
      obj match {
        case Some(o) => o
        case None => throw new DeserializationException(s"PropositionFormat missing isa field: $value")
      }
    }
  }

  implicit val actionFormat = jsonFormat2(Action)
  implicit val pcFormat = jsonFormat3(PropositionCondition)



  implicit object ConditionFormat extends RootJsonFormat[Condition] {
    def write(x: Condition) = x match {
      case p: PropositionCondition => p.toJson
      case _ => throw new SerializationException(s"Could not convert that type of condition $x")
    }
    def read(value: JsValue) = {
      value.convertTo[PropositionCondition]
    }
  }









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