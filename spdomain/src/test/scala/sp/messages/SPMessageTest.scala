package sp

import org.scalatest._
import sp.messages.APISP
import sp.messages.Pickles._
import sp.domain._
import sp.domain.Logic._


case class L1(a: String)
case class L2(b: L1)
case class L3(c: L2)
case class L4(d: L3)


class ModelMakerAPITest extends FreeSpec with Matchers {
  "Picklers" - {
    "make SPMessage" in {
      val h = SPHeader("from", "to")
      val b = APISP.SPACK(SPAttributes("Hej"->4))
      val x = SPMessage.make(h, b)

      assert(x.isSuccess)

    }

    "SPMessage to json" in {
      val h = SPHeader("tomte", "kalle")
      val b = APISP.SPACK(SPAttributes("Hej"->4))
      val x = SPMessage.make(h, b).map(_.toJson)

      println(x)
      assert(x.isSuccess && x.get.nonEmpty)

    }

    "SPMessage from json" in {
      val h = SPHeader("from", "to")
      val b = APISP.SPACK(SPAttributes("Hej"->4))
      val x = SPMessage.make(h, b).map(_.toJson).getOrElse("")

      val res = SPMessage.fromJson(x)

      println(res)
      assert(res.isSuccess)

    }

    "testing some header union" in {
      val h = SPHeader("tomte", "kalle")
      val b = APISP.SPACK(SPAttributes("Hej"->4))
      val x = SPMessage.make(h, b).get

      val headerEx = SPHeader(from = "upd", to = "yes")

      val updM = x.extendHeader(headerEx)

      println(updM)

    }

    "extract info" in {
      val x = L4(L3(L2(L1("JA"))))
      val p = toPickle(x)

      val p2 = toPickle(10)

      val a = p / "d" / "c" / "b" / "a"
      val b = p / "d" / "k" / "b" / "a"
      val c = p2 / "a"

      assert(a.contains(toPickle("JA")))
      assert(b.isEmpty)
      assert(c.isEmpty)


    }
  }

}



//object APITEST extends SPCommunicationAPI {
//  sealed trait API
//  case class Test1(p1: String, p2: String) extends API
//  case class Test2(p1: Int, p2: Int) extends API
//  case class Test3(p1: Double, p2: Tom) extends API
//
//  sealed trait SUB
//  case class Tom(str: String) extends SUB
//
//  override type MessageType = API
//  override type SUBType = SUB
//  override lazy val apiClasses: List[Class[_]] =   sp.macros.MacroMagic.values[MessageType]
//  override lazy val apiJson: List[String] = sp.macros.MacroMagic.info[MessageType, SUBType]
//}
//
//// Result json from apiJson, copy paste in http://jsoneditoronline.org to view
///**
//  * [{"isa":"APITEST.Test1","p1":{"key":"p1","ofType":"String"},"p2":{"key":"p2","ofType":"String"}},{"isa":"APITEST.Test2","p1":{"key":"p1","ofType":"Int"},"p2":{"key":"p2","ofType":"Int"}},{"isa":"APITEST.Test3","p1":{"key":"p1","ofType":"Double"},"p2":{"key":"p2","ofType":"APITEST.Tom"}},{"subs":{"APITEST.Tom":{"str":{"key":"str","ofType":"String"}}}}]
//  */
//
//
//object APITEST2 extends SPCommunicationAPI {
//  sealed trait API
//  case class Test4(x1: String, x2: String) extends API
//
//  override type MessageType = API
//  override lazy val apiClasses: List[Class[_]] =   sp.macros.MacroMagic.values[MessageType]
//  override lazy val apiJson: List[String] = sp.macros.MacroMagic.info[MessageType, SUBType]
//
//}
//
//
///**
//  * Testing API handling
//  */
//class ModelMakerAPITest extends FreeSpec with Matchers {
//  "CompTest" - {
//    "is it working" in {
//      println(APITEST.apiClasses)
//      implicit val f = APITEST.formats
//      val t = APITEST.Test1("hej", "då")
//
//      val json = APITEST.write(t)
//      val json2 = APITEST.write(APITEST.Test3(2, APITEST.Tom("hej")))
//      println(json)
//      println(json2)
//
//
//      val res = APITEST.read(json)
//      println(res)
//      val res2 = APITEST.read(json2)
//      println(res2)
//
//      println(APITEST.apiJson)
//
//    }
//  }
//
//
//  "Json serialization" - {
//    val m1 = APITEST.Test1("hej", "då")
//    val m2 = APITEST.Test2(1, 2)
//    val m3 = APITEST.Test3(0.1, APITEST.Tom("tom"))
//
//    val jsonM1 = """{"isa":"APITEST$Test1","p1":"hej","p2":"då"}"""
//    val jsonM2 = """{"isa":"APITEST$Test2","p1":1,"p2":2}"""
//    val jsonM3 = """{"isa":"APITEST$Test3","p1":0.1,"p2":{"str":"tom"}}"""
//
//
//    "convert and add isa to modelmaker messages" in {
//      implicit val formats = APITEST.formats
//
//      val jsonM1 = APITEST.write(m1)
//      val jsonM2 = APITEST.write(m2)
//      val jsonM3 = APITEST.write(m3)
//
//      APITEST.write(m1) shouldEqual jsonM1
//      APITEST.write(m2) shouldEqual jsonM2
//      APITEST.write(m3) shouldEqual jsonM3
//    }
//
//    "read modelmaker messages" in {
//      implicit val formats = APITEST.formats
//
//      APITEST.read(jsonM1) shouldEqual Success(m1)
//      APITEST.read(jsonM2) shouldEqual Success(m2)
//      APITEST.read(jsonM3) shouldEqual Success(m3)
//
//    }
//
//    "read pf modelmaker messages" in {
//      implicit val formats = APITEST.formats
//
//      var test = false
//      APITEST.readPF(jsonM1){
//        case m1New: APITEST.Test1 => test = m1 == m1New
//      }
//      {PartialFunction.empty}
//      {x: String => test = false}
//      assert(test)
//    }
//
//    "read pf modelmaker messages and fail" in {
//      implicit val formats = APITEST.formats
//
//      val bad = """{"isa":"APITEST$Test4","p1":"hej","p2":"då"}"""
//      var test = false
//      APITEST.readPF(bad){
//        case cmNew: APITEST.Test1 => test = false
//      }
//      {PartialFunction.empty}
//      {x: String =>
//        test = true
//      }
//      assert(test)
//    }
//
//    "read pf SPMessage messages" in {
//      implicit val formats = APITEST.formats
//      val req = StatusRequest()
//      val jsonR = APITEST.write(req)
//
//      var test = false
//      APITEST.readPF(jsonR){
//        case cmNew: APITEST.API => test = false
//      }
//      {case StatusRequest(param) => test = true}
//      {x: String => test = false}
//      assert(test)
//    }
//
//    "handle multiple formats" in {
//      implicit val formats = APITEST.formats join APITEST2.formats
//
//      val t2 = APITEST2.Test4("ja", "ja")
//      val t2Json = """{"isa":"APITEST2$Test4","x1":"ja","x2":"ja"}"""
//
//      APITEST.write(m1) shouldEqual jsonM1
//      APITEST.write(t2) shouldEqual t2Json
//
//      APITEST.read(jsonM1) shouldEqual Success(m1)
//      APITEST.read(t2Json) shouldEqual Success(t2)
//
//    }
//
//  }





//}

