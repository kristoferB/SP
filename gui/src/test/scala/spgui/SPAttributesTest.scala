package spgui

import org.scalatest._
import spgui.communication.APIParser




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
      val k = SPValue(t)
      val k2 = SPAttributes(Map("hej"->t))
      println(k2)
      assert(true)
    }
  }
}


