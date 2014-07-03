package sp.json

import org.scalatest._

/**
 * Created by Kristofer on 2014-06-10.
 */
class JsonTest extends WordSpec with Matchers  {

  import sp.domain._
  import spray.json._
  import SPJson._

  val fixid = ID(java.util.UUID.fromString("3a0bad18-a4c2-4764-9d5a-b57d6579da24"))

  val o1 = new Operation("o1"){override lazy val id = fixid}
  val o1json = JsObject(
    "name" -> "o1".toJson,
    "id" -> fixid.toJson,
    "isa" -> "Operation".toJson,
    "conditions" -> List[String]().toJson,
    "version" -> (-1).toJson,
    "attributes" -> JsObject()
  )
  val o2 = Operation("o2")
  val t1 = new Thing("t1"){override lazy val id = fixid}
  val t1json = JsObject(
    "name" -> "t1".toJson,
    "id" -> fixid.toJson,
    "isa" -> "Thing".toJson,
    "stateVariables" -> List[String]().toJson,
    "version" -> (-1).toJson,
    "attributes" -> JsObject()
  )
  val sop1 = SOP(Parallel(Sequence(o1, o2), Sequence(o1, o2)))
  val spec = SOPSpec(sop1, "label")

  val attr = SPAttributes(Map(
    "hej" -> "kalle",
    "då" -> 1,
    "tree" -> MapPrimitive(Map(
      "in" -> "tree"
    ))
  ))
  val attrjson = JsObject(
    "hej" -> "kalle".toJson,
    "då" -> 1.toJson,
    "tree" -> JsObject(
      "in" -> "tree".toJson
    )
  )

  val guard =
    AND(
      OR(
        EQ(SVIDEval(fixid), ValueHolder(SPAttributeValue(1))),
        EQ(SVIDEval(fixid), ValueHolder(SPAttributeValue(2)))
      ),
      EQ(SVIDEval(fixid), ValueHolder(SPAttributeValue(1)))
  )
  val action = List(Action(fixid, SPAttributeValue(3)))
  val prop = PropositionCondition(guard, action)



  "An IdAble" when {
    "converter to Json" should {
      "return good json for operation" in {
        println(s"op to json: ${o1.toJson}")
        assert(o1.toJson == o1json)
        assert(o1.asInstanceOf[IDAble].toJson == o1json)
      }

      "return good json for thing" in {
        println(s"thing to json: ${t1.toJson}")
        assert(t1.toJson == t1json)
        assert(t1.asInstanceOf[IDAble].toJson == t1json)
      }
    }

    "converted from json" should {
      "return operation from json" in {
        val newo = o1.toJson.convertTo[Operation]
        println(s"op from json: $newo")
        assert(o1.name == newo.name)
      }

      "return thing from json" in {
        val newo = t1.toJson.convertTo[Thing]
        println(s"thing from json: $newo")
        assert(t1.name == newo.name)
      }
    }

  }

  "An attribute" when {
    "converted to json" should {
      "return correct json" in {
        assert(attr.toJson == attrjson)

      }
    }
    "converted from json" should {
      "return correct attr" in {
        assert(attr.toJson.convertTo[SPAttributes] == attr)

      }
    }
  }

  "An proposition condition" when {
    "converted to json" should {
      "return correct json" in {
        println(s"prop conv: ${prop.toJson}")
        assert(true)

      }
    }
    "converted from json" should {
      "return correct attr" in {
        assert(attr.toJson.convertTo[SPAttributes] == attr)

      }
    }
  }

}

