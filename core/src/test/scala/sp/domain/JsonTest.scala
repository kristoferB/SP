package sp.domain

import org.scalatest._
import org.json4s._
import org.json4s.native.JsonMethods._
import sp.domain.Logic._
import org.json4s.native.Serialization._

/**
 * Created by kristofer on 15-05-27.
 */
class JsonTest extends FreeSpec with Matchers  {

  implicit val f = sp.domain.logic.JsonLogic.jsonFormats

  val o = Operation("hej")
  val opJson = SPAttributes(
    ("isa", "Operation"),
    ("name","hej"),
    ("conditions", JArray(List())),
    ("attributes", JObject()),
    ("id", o.id)
  )

  val t = Thing("hej")
  val thingJson = SPAttributes(
    ("isa", "Thing"),
    ("name","hej"),
    ("attributes", JObject()),
    ("id", t.id)
  )

  "A json should be produced" - {
    "when converting an ID" in {
      val id = ID.newID
      val json = Extraction.decompose(id)
      json shouldEqual JString(id.toString())
    }
    "when converting an empty Operation" in {
      val json = Extraction.decompose(o)
      json shouldEqual opJson
    }
    "when converting an empty Thing" in {
      val json = Extraction.decompose(t)
      json shouldEqual thingJson
    }
    "when converting a List of IDAbles " in {
      val list = List(t,o)
      val json = Extraction.decompose(list)
      json shouldEqual JArray(List(thingJson, opJson))
    }
    "when converting an attribute " in {
      val x = SPAttributes(("hej", "nej"), "ja"->10, "nej"->o.id)
      val json = Extraction.decompose(x) // does nothing
      val res = JObject(List(("hej",JString("nej")), ("ja",JInt(10)), ("nej",JString(o.id.toString()))))
      json shouldEqual res
    }


    val id1 = ID.newID
    val id2 = ID.newID
    val guard: Proposition = AND(List(
      OR(List(EQ(id1, "1"), GREQ(20, id2)))
    ))
    val action = List(Action(id1, INCR(2)), Action(id2, ASSIGN(id1)))
    val cond = PropositionCondition(guard, action, SPAttributes(("hej",1), "ja"->2))

    "when converting conditions" in {
      val json = Extraction.decompose(cond)
      val str = write(json)
      println(writePretty(json))
      val r = read[Condition](str)
      println("")
      println(r)

    }


  }

  "A object should be parsed" - {
    "when converting an empty Operation" in {
      val json = write(o)
      val res = read[Operation](json)
      res shouldEqual o
    }
    "when converting an empty Thing" in {
      val json = write(t)
      val res = read[Thing](json)
      res shouldEqual t
    }
    "when converting a List of IDAbles " in {
      val list = List(t,o)
      val json = write(list)
      val res = read[List[IDAble]](json)
      res shouldEqual list
    }
    "when converting an Attribute " in {
      val x = SPAttributes(("hej", "nej"), "ja"->10, "nej"->o.id)
      val json = write(x)
      val res = read[SPAttributes](json)
      res shouldEqual x
    }

  }

}
