//package sp
//
//import derive.key
//import org.json4s.ShortTypeHints
//import org.scalatest.{FreeSpec, Matchers}
//import sp.domain._
//import sp.messages._
//import upickle.Js
//import upickle.Js.Value
//
//import scala.reflect.ClassTag
//import scala.util.Success
//
//
//
////object APITEST {
////  sealed trait API
////  case class Test1(p1: String, p2: String) extends API
////  case class Test2(p1: Int, p2: Int) extends API
////  case class Test3(p1: Double, p2: Tom) extends API
////
////  sealed trait SUB
////  case class Tom(str: String) extends SUB
////
////  lazy val apiClasses: List[Class[_]] =   sp.macros.MacroMagic.values[API]
////  lazy val apiJson: List[String] = sp.macros.MacroMagic.info[API, SUB]
////
////}
////
////object FixedType extends upickle.AttributeTagged {
////  override val tagName = "isa"
////
////  override def annotate[V: ClassTag](rw: Reader[V], n: String) = Reader[V]{
////    case Js.Obj(x@_*) if x.contains((tagName, Js.Str(n.split('.').takeRight(2).mkString(".")))) =>
////      rw.read(Js.Obj(x.filter(_._1 != tagName):_*))
////  }
////
////  override def annotate[V: ClassTag](rw: Writer[V], n: String) = Writer[V]{ case x: V =>
////    val filter = n.split('.').takeRight(2).mkString(".")
////    Js.Obj((tagName, Js.Str(filter)) +: rw.write(x).asInstanceOf[Js.Obj].value:_*)
////  }
////
////}
////
////case class Hej(p1: String)
//
///**
//  * Testing API handling
//  */
//class ModelMakerAPITest extends FreeSpec with Matchers {
//  "CompTest" - {
////    "is it working" in {
////      import upickle.default._
////
////      val t = APITEST.Test1("hej", "då")
////      val t2 = APITEST.Test2(10, 2)
////      val t3 = APITEST.Test3(10.0, APITEST.Tom("tom"))
////
////      val j1 = FixedType.write(t)
////      val j2 = FixedType.write(t2)
////      val j3 = FixedType.write(t3)
////
////      println(j1)
////      println(j2)
////      println(j3)
////
////      println(FixedType.read[APITEST.API](j1))
////      println(FixedType.read[APITEST.API](j2))
////      println(FixedType.read[APITEST.API](j3))
////
////      println(APITEST.apiJson)
////
////
////    }
////    "testing more" in {
////      val t = APITEST.Test1("hej", "då")
////      val json = FixedType.write(t)
////
////      val test = FixedType.read[Hej](json)
////      println(test)
////
////      val text = """{"p1":"pp", "p2":"pp2"}"""
////      println(FixedType.read[APITEST.Test1](text))
////
////    }
//
////    "testing even more on SP Messages" in {
////      import sp.domain.Logic._
////      val t1 = sp.messages.UPickleAPI.Test1("hej", "då")
////      val t2 = sp.messages.UPickleAPI.Test4("hej", SPAttributes("hej"->SPAttributes("kalle"->1)))
////      val json = sp.messages.APIParser.write(t2)
////      println(json)
////
////
////      val r = sp.messages.APIParser.read[sp.messages.UPickleAPI.API](json)
////      println(r)
////    }
//
//  }
//
//
//
//
//
//
//
//}
//
