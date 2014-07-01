package sp.json

  import sp.domain._
  import spray.json._
  import DefaultJsonProtocol._

trait SPJsonIDAble extends SPJsonDomain {


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
      val newV = filterAndUpdateIDAble(value.asJsObject).getOrElse(value)
      newV.asJsObject.getFields("name", "conditions", "attributes") match {
        case Seq(JsString(name), c: JsArray, a: JsObject) => {
          val cond = List[Condition]() //c.elements map(_.asJsObject.convertTo[Condition])
          val attr = a.convertTo[SPAttributes]
          Operation(name, cond, attr)
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
      val newV = filterAndUpdateIDAble(value.asJsObject).getOrElse(value)
      newV.asJsObject.getFields("name", "stateVariables", "attributes") match {
        case Seq(JsString(name), c: JsArray, a: JsObject) => {
          val sv = List[StateVariable]() //c.elements map(_.asJsObject.convertTo[StateVariable])
          val attr = a.convertTo[SPAttributes]
          Thing(name, sv, attr)
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
      val newV = filterAndUpdateIDAble(value.asJsObject).getOrElse(value)
      newV.asJsObject.getFields("name", "attributes") match {
        case Seq(JsString(name), a: JsObject) => {
          val attr = a.convertTo[SPAttributes]
          SPObject(name, attr)
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
      val newV = filterAndUpdateIDAble(value.asJsObject).getOrElse(value)
      newV.asJsObject.getFields("name", "attributes") match {
        case Seq(JsString(name), a: JsObject) => {
          val attr = a.convertTo[SPAttributes]
          SPSpec(name, attr)
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
      val newV = filterAndUpdateIDAble(value.asJsObject).getOrElse(value)
      newV.asJsObject.getFields("name", "sop", "attributes") match {
        case Seq(JsString(label), s: JsObject, a: JsObject) => {
          val sop = s.convertTo[SOP]
          val attr = a.convertTo[SPAttributes]
          SOPSpec(sop, label, attr)
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
      val newV = filterAndUpdateIDAble(value.asJsObject()).getOrElse(value.asJsObject())
      val obj = for {
        t <- value.asJsObject.fields.get("isa")
      } yield {
        t match {
          case JsString("Operation") => newV.convertTo[Operation]
          case JsString("Thing") => newV.convertTo[Thing]
          case JsString("SPObject") => newV.convertTo[SPObject]
          case JsString("SOPSpec") => newV.convertTo[SOPSpec]
          case JsString("SPSpec") => newV.convertTo[SPSpec]

          case _ => throw new DeserializationException(s"IDAble could not be read: $value")
        }
      }
      obj match {
        case Some(o) => o
        case None => throw new DeserializationException(s"IDAble could not be read: $value")
      }
    }
  }



    def idPart(x: IDAble) = List(
        "version" -> x.version.toJson,
        "id" -> x.id.toJson,
        "attributes" -> x.attributes.toJson
      )

      def filterAndUpdateIDAble(js: JsObject): Option[JsObject] = {
        val xs = js.fields
        for {
          ver <- xs.get("version")
          id <- xs.get("id")
          t <- xs.get("isa")
          attr <- xs.get("attributes")
        } yield {
          val update = JsObject(attr.asJsObject.fields + ("basedOn" -> JsObject("id" -> id, "ver" -> ver)))
          println(s"add based on: ${JsObject(xs + ("attributes" -> update))}" )
          JsObject(xs + ("attributes" -> update))
        }
      }


}

