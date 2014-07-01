package sp.json

import sp.domain._
import sp.system.messages._
import spray.json._


object SPJson extends DefaultJsonProtocol with
SPJsonDomain with
SPJsonIDAble with
SPJsonMessages with
SPJsonRestAPI

//object SPJson extends DefaultJsonProtocol {
//  import SharedConvert._
//
//
//  implicit object IDJsonFormat extends RootJsonFormat[ID] {
//    def write(id: ID) = id.value.toString.toJson
//    def read(value: JsValue) = value match {
//      case JsString(idStr) =>
//        ID.makeID(idStr) match {
//          case Some(id) => id
//          case None => deserializationError("ID expected, got " + idStr)
//        }
//      case x @ _ => deserializationError(s"ID expected, got $x")
//    }
//  }
//
//  implicit object IDAbleJsonFormat extends RootJsonFormat[IDAble] {
//    def write(x: IDAble) = {
//      x match {
//        case x: Operation => x.toJson
//        case Thing(name, sv, attr) => x.toJson
//        case SPObject(name, attr) => x.toJson
//        case SOPSpec(sop, label, attr) => x.toJson
//        case SPSpec(label, attr) => x.toJson
//        case x@_ => println(s"no match IDAble json write: $x"); JsObject(Map[String, JsValue]())
//      }
//    }
//
//    def read(value: JsValue) = {
//      val newV = filterAndUpdateIDAble(value.asJsObject()).getOrElse(value.asJsObject())
//      val obj = for {
//        t <- value.asJsObject.fields.get("type")
//      } yield {
//        t match {
//          case JsString("Operation") => newV.convertTo[Operation]
//          case JsString("Thing") => newV.convertTo[Thing]
//          case JsString("SPObject") => newV.convertTo[SPObject]
//          case JsString("SOPSpec") => newV.convertTo[SOPSpec]
//          case JsString("SPSpec") => newV.convertTo[SPSpec]
//          case _ => throw new DeserializationException(s"IDAble could not be read: $value")
//        }
//      }
//      obj match {
//        case Some(o) => o
//        case None => throw new DeserializationException(s"IDAble could not be read: $value")
//      }
//    }
//  }
//
//  implicit object OperationJsonFormat extends RootJsonFormat[Operation] {
//    def write(x: Operation) = {
//      val map = List(
//        "type" -> "Operation".toJson,
//        "name" -> x.name.toJson,
//        "conditions" -> List[String]().toJson //o.conditions.toJson
//      ) ++ idPart(x)
//      JsObject(map)
//    }
//    def read(value: JsValue) = {
//      val newV = filterAndUpdateIDAble(value.asJsObject).getOrElse(value)
//      newV.asJsObject.getFields("name", "conditions", "attributes") match {
//        case Seq(JsString(name), c: JsArray, a: JsObject) => {
//          val cond = List[Condition]() //c.elements map(_.asJsObject.convertTo[Condition])
//          val attr = a.convertTo[SPAttributes]
//          Operation(name, cond, attr)
//        }
//      }
//    }
//  }
//
//  implicit object ThingJsonFormat extends RootJsonFormat[Thing] {
//    def write(x: Thing) = {
//      val map = List(
//        "type" -> "Thing".toJson,
//        "name" -> x.name.toJson,
//        "stateVariables" -> List[String]().toJson //o.conditions.toJson
//      ) ++ idPart(x)
//      JsObject(map)
//    }
//    def read(value: JsValue) = {
//      val newV = filterAndUpdateIDAble(value.asJsObject).getOrElse(value)
//      newV.asJsObject.getFields("name", "stateVariables", "attributes") match {
//        case Seq(JsString(name), c: JsArray, a: JsObject) => {
//          val sv = List[StateVariable]() //c.elements map(_.asJsObject.convertTo[StateVariable])
//          val attr = a.convertTo[SPAttributes]
//          Thing(name, sv, attr)
//        }
//      }
//    }
//  }
//
//  implicit object SPObjectJsonFormat extends RootJsonFormat[SPObject] {
//    def write(x: SPObject) = {
//      val map = List(
//        "type" -> "SPObject".toJson,
//        "name" -> x.name.toJson
//      ) ++ idPart(x)
//      JsObject(map)
//    }
//    def read(value: JsValue) = {
//      val newV = filterAndUpdateIDAble(value.asJsObject).getOrElse(value)
//      newV.asJsObject.getFields("name", "attributes") match {
//        case Seq(JsString(name), a: JsObject) => {
//          val attr = a.convertTo[SPAttributes]
//          SPObject(name, attr)
//        }
//      }
//    }
//  }
//
//  implicit object SPSpecJsonFormat extends RootJsonFormat[SPSpec] {
//    def write(x: SPSpec) = {
//      val map = List(
//        "type" -> "SPSpec".toJson,
//        "name" -> x.name.toJson
//      ) ++ idPart(x)
//      JsObject(map)
//    }
//    def read(value: JsValue) = {
//      val newV = filterAndUpdateIDAble(value.asJsObject).getOrElse(value)
//      newV.asJsObject.getFields("label", "attributes") match {
//        case Seq(JsString(name), a: JsObject) => {
//          val attr = a.convertTo[SPAttributes]
//          SPSpec(name, attr)
//        }
//      }
//    }
//  }
//
//  implicit object SOPSpecJsonFormat extends RootJsonFormat[SOPSpec] {
//    def write(x: SOPSpec) = {
//      val map = List(
//        "type" -> "SOPSpec".toJson,
//        "name" -> x.name.toJson,
//        "sop" -> x.sop.toJson //o.conditions.toJson
//      ) ++ idPart(x)
//      JsObject(map)
//    }
//    def read(value: JsValue) = {
//      val newV = filterAndUpdateIDAble(value.asJsObject).getOrElse(value)
//      newV.asJsObject.getFields("label", "sop", "attributes") match {
//        case Seq(JsString(label), s: JsObject, a: JsObject) => {
//          val sop = s.convertTo[SOP]
//          val attr = a.convertTo[SPAttributes]
//          SOPSpec(sop, label, attr)
//        }
//      }
//    }
//  }
//
//
//
//  implicit object SOPJsonFormat extends RootJsonFormat[SOP] {
//    def write(sop: SOP) = {
//      val res = JsObject(
//        "type" -> JsString(sop.getClass().getSimpleName()),
//        "sop" -> JsArray(sop.children.filter(_ != EmptySOP) map (c => write(c)) toList)
//      )
//      if (sop.isInstanceOf[Hierarchy]) {
//        val o = sop.asInstanceOf[Hierarchy].operation
//        JsObject(res.fields + ("operation" -> o.toJson))
//      } else res
//    }
//    def read(value: JsValue) = throw new DeserializationException("Operation JsonParse not implemented")
//
//
//  }
//
//  // USE  WHEN ADDING RUNTIMES, KRISTOFER 140627
////  // name: String, runtimeType: String, modelid: String="", id: ID=ID.empty
////  implicit object CreateRunTimeFormat extends RootJsonFormat[CreateRuntime] {
////    def write(x: CreateRuntime) = {
////      JsObject(
////        "name" -> JsString(x.name),
////        "type" -> JsString(x.runtimeType),
////        "modelID" -> IDJsonFormat.write(x.modelID),
////        "runtimeID" -> IDJsonFormat.write(x.runtimeID)
////      )
////    }
////    def read(value: JsValue) = {
////      value.asJsObject.getFields("name", "type", "modelID") match {
////        case Seq(JsString(name), JsString(rt)) => {
////          CreateRuntime(name, rt)
////        }
////        case Seq(JsString(name), JsString(rt), JsString(mid)) => {
////          val modelid = ID.getID(mid) match {case Some(id)=> id; case None => ID.empty}
////          CreateRuntime(name, rt, modelid)
////        }
////        case _ => throw new DeserializationException("CreateRunTimeFormat is missing parameters: " + value)
////      }
////    }
////
////  }
////  import se.sekvensa.sp.runtime.domain._
////  implicit val StateDiffFormat = jsonFormat1(StateDiff)
//
//
//  implicit object SPAttributesFormat extends RootJsonFormat[SPAttributes] {
//    def write(x: SPAttributes) = {
//      JsObject({
//        x.attrs map {case (k,v)=> k -> {v.toJson}}
//      })
//
//    }
//    def read(value: JsValue) = value match {
//      case a: JsObject => {
//        val map = a.fields map {case (k,v)=> k->v.convertTo[SPAttributeValue]}
//        SPAttributes(map)
//      }
//      case _ => throw new DeserializationException("can not convert to SPAttribute: "+value)
//    }
//  }
//
//
//  implicit object SPAttributeValueFormat extends RootJsonFormat[SPAttributeValue] {
//    def write(x: SPAttributeValue) = {
//      x match {
//          case StringPrimitive(x) => x.toJson
//          case IntPrimitive(x) => x.toJson
//          case LongPrimitive(x) => x.toJson
//          case DoublePrimitive(x) => x.toJson
//          case BoolPrimitive(x) => x.toJson
//          case DatePrimitive(x) => x.toString().toJson
//          case DurationPrimitive(x) => x.getMillis().toJson
//          case IDPrimitive(x) => x.toJson
//          case ListPrimitive(x) => x.toJson
//          case MapPrimitive(x) => x.toJson
//          case OptionAsPrimitive(x) => x match {
//            case Some(d) => d.toJson
//            case None => JsNull
//          }
//      }
//    }
//    def read(value: JsValue) = value match {
//      case JsString(x) => {
//        DatePrimitive.stringToDate(x) match {
//          case Some(d) => d
//          case None => {
//            ID.makeID(x) match {
//              case Some(id) => IDPrimitive(id)
//              case None => StringPrimitive(x)
//            }
//          }
//        }
//      }
//      case JsNumber(x) => {
//        if (x.isValidInt) IntPrimitive(x.intValue)
//        else if (x.isValidDouble) DoublePrimitive(x.doubleValue)
//        else if (x.isValidLong) LongPrimitive(x.longValue)
//        else StringPrimitive(x.toString)
//      }
//      case JsBoolean(x) => BoolPrimitive(x)
//      case JsArray(xs) => ListPrimitive(xs map (_.convertTo[SPAttributeValue]))
//      case JsObject(kvs) => MapPrimitive(kvs map {case (k,v)=> k->v.convertTo[SPAttributeValue]})
//      case JsNull => OptionAsPrimitive(None)
//      case _ => throw new DeserializationException("can not convert to SPAttribute: "+value)
//    }
//  }
//
//  implicit object StateVariableFormat extends RootJsonFormat[StateVariable] {
//    def write(x: StateVariable) = {
//      JsObject("hej"-> 1.toJson )// x match {
////        case x: IntVariable => x.toJson.asJsObject.fields + ("type"-> "IntVariable".toJson)
////        case x: RestrictedIntRangeVariable => x.toJson.asJsObject.fields + ("type"-> "IntVariable".toJson)
////        case x: RestrictedIntVariable => x.toJson.asJsObject.fields + ("type"-> "IntVariable".toJson)
////        case x: StringVariable => x.toJson.asJsObject.fields + ("type"-> "IntVariable".toJson)
////        case x: RestrictedStringVariable => x.toJson.asJsObject.fields + ("type"-> "IntVariable".toJson)
////        case x: BooleanVariable => x.toJson.asJsObject.fields + ("type"-> "IntVariable".toJson)
//      //})
//    }
//    def read(value: JsValue) = {
//      val obj = for {
//        t <- value.asJsObject.fields.get("type")
//      } yield t match {
//        case JsString("IntVariable") => IntVariable("hej")
//        case _ => throw new DeserializationException(s"StateVariable could not be read: $value")
//      }
//      obj match {
//        case Some(o) => o
//        case None => throw new DeserializationException(s"IDAble could not be read: $value")
//      }
//    }
//  }
//
//  implicit object RangeFormat extends RootJsonFormat[Range] {
//    def write(x: Range) = {
//      JsObject(
//        "start" -> x.start.toJson,
//        "end" -> x.end.toJson,
//        "step" -> x.step.toJson
//      )
//    }
//    def read(value: JsValue) = {
//      value.asJsObject.getFields("start", "end", "step") match {
//        case Seq(JsNumber(start), JsNumber(end), JsNumber(step)) =>
//          new Range(start.toInt, end.toInt, step.toInt)
//        case _ => throw new DeserializationException("Range expected")
//      }
//
//    }
//  }
//
//
//
//
///* ---------------------------------------
// * CASE CLASSES
// * ---------------------------------------
//*/
//
////  implicit val svintFormat = jsonFormat2(IntVariable)
////  implicit val svrrintFormat = jsonFormat3(RestrictedIntRangeVariable)
////  implicit val svrintFormat = jsonFormat3(RestrictedIntVariable)
////  implicit val svstrFormat = jsonFormat2(StringVariable)
////  implicit val svrestrFormat = jsonFormat3(RestrictedStringVariable)
////  implicit val svboolFormat = jsonFormat2(BooleanVariable)
//
//  implicit val cmFormat = jsonFormat2(CreateModel)
//  implicit val gidFormat = jsonFormat2(GetIds)
//  implicit val gopsFormat = jsonFormat1(GetOperations)
//  implicit val gtFormat = jsonFormat1(GetThings)
//  implicit val gspFormat = jsonFormat1(GetSpecs)
//  implicit val gqFormat = jsonFormat2(GetQuery)
//  implicit val gDiffFormat = jsonFormat2(GetDiff)
//
//  implicit val uidFormat = jsonFormat3(UpdateID.apply)
//  implicit val uidsFormat = jsonFormat3(UpdateIDs)
//
//  implicit val spidsFormat = jsonFormat3(SPIDs)
//  implicit val mdiffFormat = jsonFormat5(ModelDiff)
//  implicit val modelInfoFormat = jsonFormat3(ModelInfo)
//
//
//  implicit val esFormat = jsonFormat1(SPErrorString)
//  implicit val euFormat = jsonFormat3(UpdateError)
//
//
//  // REST API CLASSES
//  import sp.server._
//  implicit val inclIFormat = jsonFormat2(InclInfo)
//
//
//  //  implicit val modelInfoFormat = jsonFormat3(ModelInfo)
////  implicit val serviceErrorFormat = jsonFormat1(ServiceErrorString)
////  implicit val serviceErrorMissingIdFormat = jsonFormat2(ServiceErrorMissingID)
////  implicit val operationsFormat = jsonFormat1(Operations)
////  implicit val searchResourcesFormat = jsonFormat3(SearchResources)
////  implicit val algorithmMessageFormat = jsonFormat2(AlgorithmMessage)
////  implicit val CreatedFormat = jsonFormat1(Created)
////  implicit val RuntimeInfoFormat = jsonFormat3(RuntimeInfo)
////  implicit val EventFormat = jsonFormat1(Event)
//
//
//
//
//
//    object SharedConvert {
//      def idPart(x: IDAble) = List(
//        "version" -> x.version.toJson,
//        "id" -> x.id.toJson,
//        "attributes" -> x.attributes.toJson
//      )
//
//      def filterAndUpdateIDAble(js: JsObject): Option[JsObject] = {
//        val xs = js.fields
//        for {
//          ver <- xs.get("version")
//          id <- xs.get("id")
//          t <- xs.get("type")
//          attr <- xs.get("attributes")
//        } yield {
//          val update = JsObject(attr.asJsObject.fields + ("basedOn" -> JsObject("id" -> id, "ver" -> ver)))
//          println(s"add based on: ${JsObject(xs + ("attributes" -> update))}" )
//          JsObject(xs + ("attributes" -> update))
//        }
//      }
//    }
//
//
//}