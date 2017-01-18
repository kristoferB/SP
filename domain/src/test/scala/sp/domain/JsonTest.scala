package sp.domain

import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization._
import org.scalatest._

/**
 * Created by kristofer on 15-05-27.
 */
class JsonTest extends FreeSpec with Matchers  {



import sp.domain.Logic._


  // Test to show how the json will look:
  "a json string should be created for " - {
    "A proposition" in {
      val id = ID.newID
      val and =
        AND(List(
          EQ("1", 2),
          OR(List(NEQ(1, 2), NEQ(id, "hej")))
        ))
      val jsonString =
        s"""
          |{
          |  "isa":"AND",
          |  "props":[
          |    {
          |      "isa":"EQ",
          |      "left":{
          |        "isa":"ValueHolder",
          |        "v":"1"
          |      },
          |      "right":{
          |        "isa":"ValueHolder",
          |        "v":2
          |      }
          |    },
          |    {
          |      "isa":"OR",
          |      "props":[
          |        {
          |          "isa":"NEQ",
          |          "left":{
          |            "isa":"ValueHolder",
          |            "v":1
          |          },
          |          "right":{
          |            "isa":"ValueHolder",
          |            "v":2
          |          }
          |        },
          |        {
          |          "isa":"NEQ",
          |          "left":{
          |            "isa":"SVIDEval",
          |            "id":"${id.toString}"
          |          },
          |          "right":{
          |            "isa":"ValueHolder",
          |            "v":"hej"
          |          }
          |        }
          |      ]
          |    }
          |  ]
          |}
        """.stripMargin

      writePretty(parse(jsonString)) shouldEqual writePretty(and)
    }
    "An operation" in {
      val id = ID.newID
      val x = Operation(
        name = "x",
        conditions = List(PropositionCondition(
          guard = AND(List(EQ("1", 2), NEQ(1, 2))),
          action = List(Action(id, ValueHolder("hej"))))
        ))
    }
  }

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


    "when converting conditions" in {
      val id1 = ID.newID
      val id2 = ID.newID
      val guard: Proposition = AND(List(
        OR(List(EQ(id1, "1"), GREQ(20, id2)))
      ))
      val action = List(Action(id1, INCR(2)), Action(id2, ASSIGN(id1)))
      val cond = PropositionCondition(guard, action, SPAttributes(("hej",1), "ja"->2))
      val json = Extraction.decompose(cond)
      val str = write(json)
      //println(writePretty(json))
      val r = read[Condition](str)
      r shouldEqual cond
    }

    "when converting sops" in {
      val sop = Parallel(
        Sequence(Hierarchy(ID.newID), Hierarchy(ID.newID)),
        Alternative(Hierarchy(ID.newID),Hierarchy(ID.newID))
      )
      val json = Extraction.decompose(sop)
      val str = write(json)
      //println(writePretty(json))
      val r = read[SOP](str)
      r shouldEqual sop
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

  "a json string should be converted" - {
    val x = SPAttributes("hej"->"AND", "haj"->false, "list"->List(1,2,3))
    val jsonAttr =
      s"""
         |{
         |  "hej":"AND",
         |  "haj":false,
         |  "list": [1,2,3]
         |  }
        """.stripMargin
    "to an SPAttribute" in {

      println(x.pretty)
      println(x.toJson)


      SPAttributes.fromJson(jsonAttr) shouldEqual Some(x)
    }
    "not to an SPAttribute" in {
      SPAttributes.fromJson("["+jsonAttr+"]") shouldEqual None
    }
    "to an SPValue" in {
      SPValue.fromJson("["+jsonAttr+"]") shouldEqual Some(JArray(List(x)))
    }

  }


}
