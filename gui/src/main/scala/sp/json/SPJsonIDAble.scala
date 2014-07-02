package sp.json

  import sp.domain._
  import spray.json._
  import DefaultJsonProtocol._

trait SPJsonIDAble extends SPJsonDomain {
   import SharedConvert._



  implicit object OperationJsonFormat extends RootJsonFormat[Operation] {
    def write(x: Operation) = {
      val map = List(
        "isa" -> "Operation".toJson,
        "name" -> x.name.toJson,
        "conditions" -> List[String]().toJson //o.conditions.toJson
      ) ++ idPart(x)
      JsObject(map)
    }
    def read(value: JsValue) = {
      value.asJsObject.getFields("name", "conditions", "attributes", "id") match {
        case Seq(JsString(name), c: JsArray, a: JsObject, oid: JsString) => {
          val cond = List[Condition]() //c.elements map(_.asJsObject.convertTo[Condition])
          val attr = a.convertTo[SPAttributes]
          val myid = oid.convertTo[ID]
          new Operation(name, cond, attr){override lazy val id = myid}
        }
      }
    }
  }

  implicit object ThingJsonFormat extends RootJsonFormat[Thing] {
    def write(x: Thing) = {
      val map = List(
        "isa" -> "Thing".toJson,
        "name" -> x.name.toJson,
        "stateVariables" -> List[String]().toJson //o.conditions.toJson
      ) ++ idPart(x)
      JsObject(map)
    }
    def read(value: JsValue) = {
      value.asJsObject.getFields("name", "stateVariables", "attributes", "id") match {
        case Seq(JsString(name), c: JsArray, a: JsObject, oid: JsString) => {
          val sv = List[StateVariable]() //c.elements map(_.asJsObject.convertTo[StateVariable])
          val attr = a.convertTo[SPAttributes]
          val myid = oid.convertTo[ID]
          new Thing(name, sv, attr){override lazy val id = myid}
        }
      }
    }
  }

  implicit object SPObjectJsonFormat extends RootJsonFormat[SPObject] {
    def write(x: SPObject) = {
      val map = List(
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
          new SPObject(name, attr){override lazy val id = myid}
        }
      }
    }
  }

  implicit object SPSpecJsonFormat extends RootJsonFormat[SPSpec] {
    def write(x: SPSpec) = {
      val map = List(
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
          new SPSpec(name, attr){override lazy val id = myid}
        }
      }
    }
  }

  implicit object SOPSpecJsonFormat extends RootJsonFormat[SOPSpec] {
    def write(x: SOPSpec) = {
      val map = List(
        "isa" -> "SOPSpec".toJson,
        "name" -> x.name.toJson,
        "sop" -> x.sop.toJson //o.conditions.toJson
      ) ++ idPart(x)
      JsObject(map)
    }
    def read(value: JsValue) = {
      value.asJsObject.getFields("name", "sop", "attributes", "id") match {
        case Seq(JsString(label), s: JsObject, a: JsObject, oid: JsString) => {
          val sop = s.convertTo[SOP]
          val attr = a.convertTo[SPAttributes]
          val myid = oid.convertTo[ID]
          new SOPSpec(sop, label, attr){override lazy val id = myid}
        }
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
        case x@_ => println(s"no match IDAble json write: $x"); JsObject(Map[String, JsValue]())
      }
    }

    def read(value: JsValue) = {
      val obj = for {
        t <- value.asJsObject.fields.get("isa")
      } yield {
        t match {
          case JsString("Operation") => value.convertTo[Operation]
          case JsString("Thing") => value.convertTo[Thing]
          case JsString("SPObject") => value.convertTo[SPObject]
          case JsString("SOPSpec") => value.convertTo[SOPSpec]
          case JsString("SPSpec") => value.convertTo[SPSpec]
          case _ => throw new DeserializationException(s"IDAble could not be read: $value")
        }
      }
      obj match {
        case Some(o) => o
        case None => throw new DeserializationException(s"IDAble missing isa field: $value")
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

    object SharedConvert {

    }
