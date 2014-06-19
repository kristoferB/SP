package sp.json

import sp.domain._
import sp.system.messages._
import spray.json._

object SPJson extends DefaultJsonProtocol {


  
  
  implicit object IDJsonFormat extends RootJsonFormat[ID] {
    def write(id: ID) = id.value.toJson

    def read(value: JsValue) = value match {
      case JsString(idStr) =>
        ID.makeID(idStr) match {
          case Some(id) => id
          case None => deserializationError("ID expected, got " + idStr)
        }
      case x @ _ => deserializationError(s"ID expected, got $x")
    }
  }

  implicit object IDAbleJsonFormat extends RootJsonFormat[IDAble] {
    def write(x: IDAble) = {
      val id = Map(
        "version" -> x.version.toJson,
        "id" -> x.id.toJson,
        "type" -> x.getClass.getSimpleName.toJson
      )
      val extra: Map[String, JsValue] = x match {
        case Operation(name, cond, attr) => Map[String, JsValue](
          "name" -> name.toJson,
          "conditions" -> "to be impl".toJson, //o.conditions.toJson
          "attributes" -> attr.toJson
        )
        case Thing(name, sv, attr) => Map(
          "name" -> name.toJson,
          "stateVariables" -> "to be impl".toJson, //o.conditions.toJson
          "attributes" -> attr.toJson
        )
        case SPObject(name, attr) => Map(
          "name" -> name.toJson,
          "attributes" -> attr.toJson
        )
        case SOPSpec(sop, label, attr) => Map(
          "label" -> label.toJson,
          "attributes" -> attr.toJson,
          "sop" -> sop.toJson
        )
        case SPSpec(label, attr) => Map(
          "label" -> label.toJson,
          "attributes" -> attr.toJson
        )
        case x@_ => println(s"no match IDAble json write: $x"); Map()

      }
      JsObject(id ++ extra)
    }

    def read(value: JsValue) = {
        val xs = value.asJsObject.convertTo[SPAttributes]
        val idPart = for {
          ver <- xs.getAsLong("version")
          id <- xs.getAsID("id")
          t <- xs.getAsString("type")
        } yield {
          val name = xs.getAsString("name")
          val cond = xs.getAsMap("conditions")
          val attr = xs.getAsMap("attributes")
          val label = xs.getAsString("label")
          val sv = xs.getAsMap("stateVariables")
          val sop = xs.getAsMap("sop")

          t match {
            case JsString("Operation") => {

            }
            case JsString("Thing") => {

            }
            case JsString("SPObject") => {

            }
            case JsString("SOPSpec") => {

            }
            case JsString("SPSpec") => {

            }
          }
        }
    }
  }

  implicit object OperationJsonFormat extends RootJsonFormat[Operation] {
    // Add more later!
    def write(o: Operation) = JsObject(List(
      "name" -> o.name.toJson,
      "id" -> o.id.toJson,
      "version" -> o.version.toJson,
      "conditions" -> "to be impl" toJson //o.conditions.toJson
      "attributes" -> o.attributes.toJson,
      "type" -> JsString("Operation")))

    def read(value: JsValue) = value match {
      case o @ JsObject => {

      }
    }
  }

  implicit object SimpleOperationJsonFormat extends RootJsonFormat[SimpleOperationToSend] {
    // Add more later!
    def write(o: SimpleOperationToSend) = JsObject(
      "name" -> JsString(o.o.name),
      "id" -> IDJsonFormat.write(o.o.id))

    def read(value: JsValue) = throw new DeserializationException("Operation JsonParse not implemented")
  }

  import se.sekvensa.sp.algorithms._
  implicit object ParameterJsonFormat extends RootJsonFormat[Parameter] {
    // Add more later!
    def write(p: Parameter) = JsObject(
      "id" -> JsString(p.id),
      {
        p match {
          case ps: StringParameter => "strs" -> JsArray(ps.strs.toList map (s => JsString(s)))
          case pi: IntegerParameter => "ints" -> JsArray(pi.ints.toList map (i => JsNumber(i)))
        }
      })

    def read(value: JsValue) = {
      value.asJsObject.getFields("id", "strs", "ints") match {
        case Seq(JsString(id), JsArray(values)) => {
          if (!values.isEmpty) values.head match {
            case x: JsString =>
              val vs = values map (v => v.asInstanceOf[JsString])
              StringParameter(id, (vs map (_.value)): _*)
            case x: JsNumber =>
              val vs = values map (v => v.asInstanceOf[JsNumber])
              IntegerParameter(id, (vs map (_.value)): _*)
            case _ => deserializationError("Parameter expected, values= " + values)
          }else deserializationError("Parameter expected, values= " +values)
        } 
        case _ => deserializationError("Parameter expected")
      }     
    }

    private def convertArray[T](vs: List[JsValue]): List[T] = {
      vs map (v => v.asInstanceOf[T])
    }
  }

  
  implicit object SOPJsonFormat extends RootJsonFormat[SOP] {
    def write(sop: SOP) = {
      val res = JsObject(
        "type" -> JsString(sop.getClass().getSimpleName()),
        "sop" -> JsArray(sop.children.filter(_ != EmptySOP) map (c => write(c)) toList) 
      )
      if (sop.isInstanceOf[Hierarchy]) {
        val o = sop.asInstanceOf[Hierarchy].operation
        JsObject(res.fields + ("operation" -> OperationJsonFormat.write(o)))
      } else res
    }
    def read(value: JsValue) = throw new DeserializationException("Operation JsonParse not implemented")
    

  }
  
  // name: String, runtimeType: String, modelid: String="", id: ID=ID.empty
  implicit object CreateRunTimeFormat extends RootJsonFormat[CreateRuntime] {
    def write(x: CreateRuntime) = {
      JsObject(
        "name" -> JsString(x.name),
        "type" -> JsString(x.runtimeType),
        "modelID" -> IDJsonFormat.write(x.modelID),
        "runtimeID" -> IDJsonFormat.write(x.runtimeID)
      )
    }
    def read(value: JsValue) = {
      value.asJsObject.getFields("name", "type", "modelID") match {
        case Seq(JsString(name), JsString(rt)) => {
          CreateRuntime(name, rt)
        }
        case Seq(JsString(name), JsString(rt), JsString(mid)) => {
          val modelid = ID.getID(mid) match {case Some(id)=> id; case None => ID.empty}         
          CreateRuntime(name, rt, modelid)
        }
        case _ => throw new DeserializationException("CreateRunTimeFormat is missing parameters: " + value)
      }
    } 

  }
  
  /*
   * case class StringPrimitive(value: String) extends SPAttributeValue
case class IntPrimitive(value: Int) extends SPAttributeValue
case class DoublePrimitive(value: Double) extends SPAttributeValue
case class BoolPrimitive(value: Boolean) extends SPAttributeValue
case class DatePrimitive(value: DateTime) extends SPAttributeValue 
case class DurationPrimitive(value: Duration) extends SPAttributeValue 
case class IDPrimitive(value: ID) extends SPAttributeValue
case class ListValue(value: List[SPAttributeValue]) extends SPAttributeValue
   */
  
  
  import se.sekvensa.sp.runtime.domain._
  implicit val StateDiffFormat = jsonFormat1(StateDiff) 
  
  
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
  
  
  implicit object StateVariableFormat extends RootJsonFormat[StateVariable[_]] {
    def write(x: StateVariable[_]) = { 
      "Var".toJson
    }
    def read(value: JsValue) = throw new DeserializationException("StateVariableFormat JsonParse not implemented")
  }
  
  implicit object EntityFormat extends RootJsonFormat[Entity] {
    def write(x: Entity) = {
      JsObject(
        "name" -> x.name.toJson,
        "id" -> x.id.toJson,
        "attributes" -> x.attributes.toJson
      )
    }
    def read(value: JsValue) = throw new DeserializationException("Entity JsonParse not implemented")
  }
  
  
  implicit object ResourceJsonFormat extends RootJsonFormat[Resource] {
    // Add more later!
    def write(o: Resource) = JsObject(
      "name" -> JsString(o.name),
      "id" -> IDJsonFormat.write(o.id),
      "attributes" -> SPAttributesFormat.write(o.attributes),
      "type" -> JsString("Resource"))

    def read(value: JsValue) = throw new DeserializationException("Resource JsonParse not implemented")
  }
  
  

  
    // case classes
  implicit val modelInfoFormat = jsonFormat3(ModelInfo)
  implicit val serviceErrorFormat = jsonFormat1(ServiceErrorString)
  implicit val serviceErrorMissingIdFormat = jsonFormat2(ServiceErrorMissingID)
  implicit val operationsFormat = jsonFormat1(Operations)
  implicit val searchResourcesFormat = jsonFormat3(SearchResources)
  implicit val algorithmMessageFormat = jsonFormat2(AlgorithmMessage)
  implicit val CreatedFormat = jsonFormat1(Created)
  implicit val RuntimeInfoFormat = jsonFormat3(RuntimeInfo)
  implicit val EventFormat = jsonFormat1(Event)


  


}