package spgui

import org.scalatest._
import sp.domain._

import scala.util.Success




case class TestingCC(p: String, i: Int = 0)

package API_ExampleService {

  // Copy paste the APIs you want to communicate with here
  sealed trait API_ExampleService
  case object dummy extends API_ExampleService
  case class StartTheTicker(id: java.util.UUID) extends API_ExampleService
  case class StopTheTicker(id: java.util.UUID) extends API_ExampleService
  case class SetTheTicker(id: java.util.UUID, map: Map[String, Int]) extends API_ExampleService
  case class ResetAllTickers() extends API_ExampleService
  case class TickerEvent(map: Map[String, Int], id: java.util.UUID) extends API_ExampleService
  case class TheTickers(ids: List[java.util.UUID]) extends API_ExampleService

}
import spgui.{API_ExampleService => api}

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
      val a = api.StartTheTicker(java.util.UUID.randomUUID())
      val json = write(a)
      println("case classes")
      println(json)
      val res = fromJson[api.API_ExampleService](json)
      println(res)
      assert(Success(a) == res)
    }
  }
}


