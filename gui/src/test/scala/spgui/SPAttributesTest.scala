package spgui

import org.scalatest._
import sp.domain._

import scala.util.Success




case class TestingCC(p: String, i: Int = 0)

/**
  * Created by kristofer on 2017-02-15.
  */
class SPAttributesTest extends FreeSpec {
  "Writing case classes" - {
    "should write case class" in {
      val t = TestingCC("hej")
      val k = SPValue(t)
      val k2 = SPAttributes(t)
      println(k2)
      assert(true)
    }
    "should write map" in {
      val t = TestingCC("hej")
      val t2 = TestingCC("hej", 3)
      val k = SPValue(t)
      val k2 = SPValue(t2)
      val k3 = SPAttributes(Map("hej"-> *(t), "kalle"-> *(2)))
      println(k2)
      println(k3)
      assert(true)
    }
    "should write to json" in {
      val t = TestingCC("hej")
      val k = SPValue(t)
      val k3 = SPAttributes(Map("hej"-> *(t), "kalle"-> *(2)))
      println(k3)

      val json = k3.toJson
      val json2 = toJson(t)

      println(json)
      println(json2)

      assert(true)

    }
    "should read from json" in {
      val t = TestingCC("hej")
      val k = SPValue(t)
      val k3 = SPAttributes(Map("hej"-> *(t), "kalle"-> *(2)))
      println(k3)

      val json = k3.toJson
      val json2 = toJson(t)

      val res1 = SPAttributes.fromJson(json)
      println(s"from json: $res1")

      val res2 = SPAttributes.fromJson(json2)
      println(s"from json: $res2")

      assert(true)

    }
    "should read from json to case class" in {
      val t = TestingCC("hej")
      val k = SPValue(t)
      val k3 = SPAttributes(Map("hej"-> *(t), "kalle"-> *(2)))
      println(k3)

      val json = k3.toJson
      val json2 = toJson(t)

      val res1 = SPAttributes.fromJson(json)
      println(s"from json to map: ${res1.getAs[Map[String, SPValue]]()}")
      println(s"from json to case class: ${res1.getAs[TestingCC]("hej")}")

      val res2 = SPAttributes.fromJson(json2)
      println(s"from json: ${res2.getAs[TestingCC]()}")

      assert(true)

    }
    "Testing API case classes" in {
      val a = APISP.SPACK(**(Map("hej"->"hej")))
      val json = write(a)
      println(json)
      val res = fromJson[APISP.SPACK](json)
      println(res)
      assert(Success(a) == res)
    }
  }
}


