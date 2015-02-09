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
        val h = sop.asInstanceOf[Hierarchy]
        JsObject(res.fields ++ Map("operation" -> h.operation.toJson, "conditions"->h.conditions.toJson))
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
              case Some(id) => {
                val conds = if (sop.fields.contains("conditions"))
                  sop.fields("conditions").convertTo[List[Condition]] else List()
                Hierarchy(id.convertTo[ID], conds, (xs.elements map read):_*)
              }
              case None => throw new DeserializationException(s"can not find field operation with id on Hierarchy: $value")
            }
          }
          case _ => throw new DeserializationException(s"can not convert the SOP from $value")
        }
      }
      case _ => throw new DeserializationException(s"can not convert the SOP from $value")
    }
  }


    implicit val defFormat = jsonFormat2(DefinitionPrimitive)
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
          case d: DefinitionPrimitive => d.toJson
      }
    }
    def read(value: JsValue) = value match {
      case JsString(x) => {
        DatePrimitive.stringToDate(x) match {
          case Some(d) => d
          case None => {
            ID.makeID(x) match {
              case Some(id) => IDPrimitive(id)
              case None => {
                if (x == "true" || x == "false") BoolPrimitive(x.toBoolean)
                else StringPrimitive(x)
              }
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
      case JsArray(xs) => ListPrimitive(xs map (_.convertTo[SPAttributeValue]) toList)
      case JsObject(kvs) => kvs.toList match{
        case ("itemID", JsString(value)) :: Nil =>  ID.makeID(value) match {
            case Some(id) => IDPrimitive(id)
            case None => StringPrimitive(value)
          }
        case ("definition", JsString(valueType)) :: Nil => {
          DefinitionPrimitive(valueType)
        }
        case ("definition", JsString(valueType)) :: ("default", _) :: Nil => {
          JsObject(kvs).convertTo[DefinitionPrimitive]
        }
        case _ => MapPrimitive(kvs map {case (k,v)=> k->v.convertTo[SPAttributeValue]})
      }
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





  implicit val vHolderFormat = jsonFormat1(ValueHolder)
  implicit val svidEvalFormat = jsonFormat1(SVIDEval)
  implicit val svnameEvalFormat = jsonFormat1(SVNameEval)


  implicit object StateEvalFormat extends RootJsonFormat[StateEvaluator] {
    def write(x: StateEvaluator) = x match {
      case p: SVIDEval => p.toJson
      case p: ValueHolder => p.v.toJson
      case p: SVNameEval => p.v.toJson
      case _ => throw new SerializationException(s"Could not convert that type of StateEvaluator $x")
    }
    def read(value: JsValue) = {
      val res = value match {
        case o: JsObject => {
          if (o.fields.contains("v")) o.fields("v")
          else if (o.fields.contains("id")) o.fields("id")
          else value
        }
        case _ => value
      }
     res.convertTo[SPAttributeValue] match {
        case IDPrimitive(id) => SVIDEval(id)
        case a: SPAttributeValue => ValueHolder(a)
     }
    }
  }

  implicit val vAssignFormat = jsonFormat1(ASSIGN)
  implicit object StateUpdateFormat extends RootJsonFormat[StateUpdater] {
    def write(x: StateUpdater) = x match {
      case p: INCR => JsObject(Map("isa"->"INCR".toJson, "n"->p.n.toJson))
      case p: DECR => JsObject(Map("isa"->"DECR".toJson, "n"->p.n.toJson))
      case p: ValueHolder => p.v.toJson
      case p: ASSIGN => p.toJson
      case _ => throw new SerializationException(s"Could not convert that type of StateEvaluator $x")
    }
    def read(value: JsValue) = {
      value match {
        case o: JsObject => {
          o.getFields("isa", "n", "id") match {
            case Seq(JsString("INCR"), JsNumber(n)) => INCR(n.toInt)
            case Seq(JsString("DECR"), JsNumber(n)) => DECR(n.toInt)
            case Seq(id) => id.convertTo[SPAttributeValue] match {
              case IDPrimitive(id) => ASSIGN(id)
              case _ => throw new DeserializationException(s"StateUpdater expexted id, but got: $id in $value")
            }
            case _ => throw new DeserializationException(s"StateUpdater could not be read: $value")
          }
        }
        case _ => value.convertTo[SPAttributeValue] match {
              case IDPrimitive(id) => ASSIGN(id)
              case a: SPAttributeValue => ValueHolder(a)
            }
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
      case AlwaysTrue => JsObject("isa"->"alwaysTrue".toJson)
      case AlwaysFalse => JsObject("isa"->"alwaysFalse".toJson)
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



  implicit val stateFormat = jsonFormat1(State)
  implicit val statesFormat = jsonFormat1(States)
  implicit val enabledStateFormat = jsonFormat2(EnabledStates)
  implicit val enabledStateMapFormat = jsonFormat1(EnabledStatesMap)

  implicit object relationMapFormat extends RootJsonFormat[RelationMap] {
    def write(x: RelationMap) = {
      val remap = x.relations map{
        case (ops, sop) =>
          val opsArray = ops.toJson
          JsObject("operations" -> opsArray, "sop" -> sop.toJson)
      }
      JsObject(
        "relationmap" -> JsArray(remap.toList),
        "enabledMap" -> x.enabledStates.toJson)
    }
    def read(value: JsValue) = {
      throw new DeserializationException(s"Can not deserilize relation maps")
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