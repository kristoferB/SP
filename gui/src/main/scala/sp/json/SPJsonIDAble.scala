package sp.json

import sp.domain._
import spray.json._
import DefaultJsonProtocol._

trait SPJsonIDAble extends SPJsonDomain {

  implicit object OperationJsonFormat extends RootJsonFormat[Operation] {
    def write(x: Operation) = {
      val map = Map(
        "isa" -> "Operation".toJson,
        "name" -> x.name.toJson,
        "conditions" -> x.conditions.toJson
      ) ++ idPart(x)
      JsObject(map)
    }

    def read(value: JsValue) = {
      value.asJsObject.getFields("name", "conditions", "attributes", "id") match {
        case Seq(JsString(name), c: JsArray, a: JsObject, oid: JsString) => {
          val cond = c.elements map (_.convertTo[Condition]) toList
          val attr = a.convertTo[SPAttributes]
          val myid = oid.convertTo[ID]
          new Operation(name, cond, attr) {
            override lazy val id = myid
          }
        }
        case _ => throw new DeserializationException(s"can not convert the Operation from $value")
      }
    }
  }

  implicit object ThingJsonFormat extends RootJsonFormat[Thing] {
    def write(x: Thing) = {
      val map = Map(
        "isa" -> "Thing".toJson,
        "name" -> x.name.toJson,
        "stateVariables" -> x.stateVariables.toJson
      ) ++ idPart(x)
      JsObject(map)
    }

    def read(value: JsValue) = {
      value.asJsObject.getFields("name", "stateVariables", "attributes", "id") match {
        case Seq(JsString(name), c: JsArray, a: JsObject, oid: JsString) => {
          val sv = c.elements map (_.asJsObject.convertTo[StateVariable]) toList
          val attr = a.convertTo[SPAttributes]
          val myid = oid.convertTo[ID]
          new Thing(name, sv, attr) {
            override lazy val id = myid
          }
        }
        case _ => throw new DeserializationException(s"can not convert the Thing from $value")
      }
    }
  }

  implicit object SPObjectJsonFormat extends RootJsonFormat[SPObject] {
    def write(x: SPObject) = {
      val map = Map(
        "isa" -> "SPObject".toJson,
        "name" -> x.name.toJson
      ) ++ idPart(x)
      JsObject(map)
    }

    def read(value: JsValue) = {
      value.asJsObject.getFields("name", "attributes", "id") match {
        case Seq(JsString(name), a: JsObject, oid: JsString) => {
          val attr = a.convertTo[SPAttributes]
          val myid = oid.convertTo[ID]
          new SPObject(name, attr) {
            override lazy val id = myid
          }
        }
        case _ => throw new DeserializationException(s"can not convert the SPObject from $value")
      }
    }
  }

  implicit object SPSpecJsonFormat extends RootJsonFormat[SPSpec] {
    def write(x: SPSpec) = {
      val map = Map(
        "isa" -> "SPSpec".toJson,
        "name" -> x.name.toJson
      ) ++ idPart(x)
      JsObject(map)
    }

    def read(value: JsValue) = {
      value.asJsObject.getFields("name", "attributes", "id") match {
        case Seq(JsString(name), a: JsObject, oid: JsString) => {
          val attr = a.convertTo[SPAttributes]
          val myid = oid.convertTo[ID]
          new SPSpec(name, attr) {
            override lazy val id = myid
          }
        }
        case _ => throw new DeserializationException(s"can not convert the SPSPec from $value")

      }
    }
  }

  implicit object SOPSpecJsonFormat extends RootJsonFormat[SOPSpec] {
    def write(x: SOPSpec) = {
      val map = Map(
        "isa" -> "SOPSpec".toJson,
        "name" -> x.name.toJson,
        "sop" -> x.sop.toJson
      ) ++ idPart(x)
      JsObject(map)
    }

    def read(value: JsValue) = {
      value.asJsObject.getFields("name", "sop", "attributes", "id") match {
        case Seq(JsString(label), s: JsValue, a: JsObject, oid: JsString) => {
          val sop = if(s.isInstanceOf[JsArray]) s.asInstanceOf[JsArray].elements.map(_.convertTo[SOP]).toList else List(s.convertTo[SOP])
          val attr = a.convertTo[SPAttributes]
          val myid = oid.convertTo[ID]
          new SOPSpec(sop, label, attr) {
            override lazy val id = myid
          }
        }
        case _ => throw new DeserializationException(s"can not convert the SOPSpec from $value")

      }
    }
  }

  implicit object RelationResultJsonFormat extends RootJsonFormat[RelationResult] {
    def write(x: RelationResult) = {
      val map = Map(
        "isa" -> "RelationResult".toJson,
        "name" -> x.name.toJson,
        "relationmap" -> x.relationMap.toJson,
        "model" -> x.model.toJson,
        "modelVersion" -> x.modelVersion.toJson
      ) ++ idPart(x)
      JsObject(map)
    }

    def read(value: JsValue) = {
      value.asJsObject.getFields("name", "relationmap", "model", "version", "attributes", "id") match {
        case Seq(JsString(name), rel: JsObject, model:JsString, version: JsNumber, a: JsObject, id: JsString) => {
          val relMap = rel.convertTo[RelationMap]
          val attr = a.convertTo[SPAttributes]
          val mid = model.convertTo[ID]
          val modelV = version.convertTo[Long]
          val myid = id.convertTo[ID]
          new RelationResult(name, relMap, mid, modelV, attr) {
            override lazy val id = myid
          }
        }
        case _ => throw new DeserializationException(s"can not convert the RelationResult from $value")

      }
    }
  }




  implicit object ResultJsonFormat extends RootJsonFormat[Result] {
    def write(x: Result) = {
      x match {
        case x: RelationResult => {
          x.toJson
        }
        case x@_ => throw new SerializationException(s"can not convert the Result from $x")
      }
    }

    def read(value: JsValue) = {
      value match {
        case obj: JsObject if obj.fields.contains("isa") => {
          obj.fields("isa") match {
            case JsString("RelationResult") => {
              obj.convertTo[RelationResult]
            }
            case res@_ =>
              throw new DeserializationException(s"isa $res does not match RelationResult")
          }
        }
        case _ => throw new DeserializationException(s"Result missing isa field: $value")
      }
    }
  }


  implicit object IDAbleJsonFormat extends RootJsonFormat[IDAble] {
    def write(x: IDAble) = {
      x match {
        case x: Operation => x.toJson
        case x: Thing => x.toJson
        case x: SPObject => x.toJson
        case x: SOPSpec => x.toJson
        case x: SPSpec => x.toJson
        case x: Result => x.toJson
        case x@_ => throw new SerializationException(s"can not convert the IDAble from $x")
      }
    }

    def read(value: JsValue) = {
      value match {
        case obj: JsObject if obj.fields.contains("isa") => {
          obj.fields("isa") match {
            case JsString("Operation") => value.convertTo[Operation]
            case JsString("Thing") => value.convertTo[Thing]
            case JsString("SPObject") => value.convertTo[SPObject]
            case JsString("SOPSpec") => value.convertTo[SOPSpec]
            case JsString("SPSpec") => value.convertTo[SPSpec]
            case JsString("RelationResult") => value.convertTo[RelationResult]
            case res@_ =>
              throw new DeserializationException(s"IDAble: isa $res does not match any of Operation, Thing, SPObject, SOPSpec or SPSpec")
          }
        }
        case _ => throw new DeserializationException(s"IDAble missing isa field: $value")
      }
    }
  }

  def idPart(x: IDAble) = List(
    "version" -> x.version.toJson,
    "id" -> x.id.toJson,
    "attributes" -> x.attributes.toJson
  )


  //      def filterAndUpdateIDAble(js: JsObject): Option[JsObject] = {
  //        val xs = js.fields
  //        for {
  //          ver <- xs.get("version")
  //          id <- xs.get("id")
  //          t <- xs.get("isa")
  //          attr <- xs.get("attributes")
  //        } yield {
  //          val update = JsObject(attr.asJsObject.fields + ("basedOn" -> JsObject("id" -> id, "ver" -> ver)))
  //          println(s"add based on: ${JsObject(xs + ("attributes" -> update))}" )
  //          JsObject(xs + ("attributes" -> update))
  //        }
  //      }


}



