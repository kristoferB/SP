package sp.domain.logic

import org.scalatest._
import sp.domain._
import sp.domain.Logic._



class AttributesLogicTest extends FreeSpec with TestClasses {

  "SPValue testing" - {
    "extracting things" - {
      val t1 = SPValue(anInt)
      val t2 = SPValue(anO)
      val t3 = SPValue(anIn2)

      assert(t1.to[Int].toOption.contains(anInt))
      assert(t2.to[Operation].toOption.contains(anO))
      assert(t3.to[API.Comm].toOption.contains(anIn2))

      assert(t1.as[Int] == anInt)
      assert(t2.as[Operation] == anO)
      assert(t3.as[API.Comm] == anIn2)

    }

    "to json" in {
      val t1 = SPValue(anInt).toJson
      val t2 = SPValue(anO.copy(id = ID.makeID("89269124-1c46-481d-9a54-8e16f2436796").get)).toJson
      val t3 = SPValue(anIn2).toJson

      assert(t1 == "1")
      assert(t2 == """{"isa":"Operation","name":"o","conditions":[],"attributes":{},"id":"89269124-1c46-481d-9a54-8e16f2436796"}""")
      assert(t3 == """{"isa":"In2","a1":"foo","a2":{"isa":"In1","a1":"foo"}}""")
    }

    "from json" in {
      val o = anO.copy(id = ID.makeID("89269124-1c46-481d-9a54-8e16f2436796").get)
      val t1 = SPValue(anInt)
      val t2 = SPValue(o)
      val t3 = SPValue(anIn2)

      assert(SPValue.fromJson(t1.toJson).toOption.contains(t1))
      assert(SPValue.fromJson(t2.toJson).toOption.contains(t2))
      assert(SPValue.fromJson(t3.toJson).toOption.contains(t3))
    }

    "handle equals when ints are strings" in {
      val x = SPValue(1)
      val y = SPValue("1")

      val a = SPValue("false")
      val b = SPValue(false)


      assert(x === y)
      assert(a === b)
    }
  }

  "SPAttributes testing" - {
    val attr = SPAttributes(
      "t1"->anInt,
      "t2"->anO,
      "t3"->anIn2,
      "list"->List(anO, anO2),
      "deep" -> Map("hej"->Map("då"->anO))
    )
    "extracting things" in {
      assert(attr.getAs[Int]("t1").contains(anInt))
      assert(attr.getAs[IDAble]("t2").contains(anO))
      assert(attr.getAs[API.Comm]("t3").contains(anIn2))
      assert(attr.getAs[List[IDAble]]("list").contains(List(anO, anO2)))
    }
    "finding things" in {
      assert(attr.findAs[Operation]("t2") == List(anO))
      assert(attr.findAs[Operation]("då") == List(anO))
      assert(attr.findAs[Operation]("no") == List())
    }
    "finding types" in {
      assert(attr.findType[Operation] == List(anO, anO, anO2, anO))
    }

    "using some play json digging" in {
      val res = attr \ "deep" \ "hej" \ "då"
      assert(res.toOption.contains(SPValue(anO)))
    }

    "to and from json" in {
      assert(SPAttributes.fromJson(attr.toJson).toOption.contains(attr))
      println(attr.pretty)
    }
  }



}


trait TestClasses {
  case class Test1(p1: String, p2: Boolean = true)
  object Test1 { implicit val actionFormat: JSFormat[Test1] = deriveFormatSimple }

  object API {
    sealed trait Comm
    case class In1(a1: String) extends Comm
    case class In2(a1: String, a2: Comm) extends Comm
    object Comm {
      implicit lazy val commF: JSFormat[Comm] = deriveFormatISA[Comm]
    }
  }

  val anInt = 1
  val aBool = false
  val aString = "foo"
  val anO = Operation("o")
  val anO2 = Operation("o2")

  val anIn1 = API.In1("foo")
  val anIn2 = API.In2("foo", API.In1("foo"))

}