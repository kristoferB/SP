//package sp
//
//import derive.key
//import org.scalatest.{FreeSpec, Matchers}
//import sp.domain._
//
//import scala.reflect.ClassTag
//import scala.util.Success
//
//
//
//object APITEST {
//  sealed trait API
//  case class Test1(p1: String, p2: String) extends API
//  case class Test2(p1: Int, p2: Int) extends API
//  case class Test3(p1: Double, p2: Tom) extends API
//  case class Test4(p1: SPAttributes) extends API
//
//  sealed trait SUB
//  case class Tom(str: String) extends SUB
//
//  lazy val apiClasses: List[Class[_]] =   sp.macros.MacroMagic.values[API]
//  lazy val apiJson: List[String] = sp.macros.MacroMagic.info[API, SUB]
//
//}
//
//
//object APIParser extends upickle.AttributeTagged {
//  override val tagName = "isa"
//
//  import sp.domain.Logic._
//
//  override def annotate[V: ClassTag](rw: Reader[V], n: String) = Reader[V]{
//    case x: Js.Obj if n == "org.json4s.JsonAST.JObject" =>
//      val res = x.value.map(kv => kv._1 -> fromUpickle(kv._2))
//      SPAttributes(res:_*).asInstanceOf[V]
//    case Js.Obj(x@_*) if x.contains((tagName, Js.Str(n.split('.').takeRight(2).mkString(".")))) =>
//      rw.read(Js.Obj(x.filter(_._1 != tagName):_*))
//  }
//
//  override def annotate[V: ClassTag](rw: Writer[V], n: String) = Writer[V]{
//    case x: SPValue =>
//      toUpickle(x)
//    case x: V =>
//      val filter = n.split('.').takeRight(2).mkString(".")
//      Js.Obj((tagName, Js.Str(filter)) +: rw.write(x).asInstanceOf[Js.Obj].value:_*)
//  }
//
//
//  def toUpickle(value: SPValue): upickle.Js.Value = value match {
//    case x: JsonAST.JBool => upickle.default.writeJs(x.values)
//    case x: JsonAST.JDecimal => upickle.default.writeJs(x.values)
//    case x: JsonAST.JDouble => upickle.default.writeJs(x.values)
//    case x: JsonAST.JInt => upickle.default.writeJs(x.values)
//    case x: JsonAST.JLong => upickle.default.writeJs(x.values)
//    case x: JsonAST.JString => upickle.default.writeJs(x.values)
//    case x: JsonAST.JObject =>
//      val res = x.obj.map(kv => kv._1 -> toUpickle(kv._2))
//      upickle.Js.Obj(res:_*)
//    case x: JsonAST.JArray => upickle.Js.Arr(x.arr.map(toUpickle):_*)
//    case x => upickle.Js.Null
//  }
//  def fromUpickle(value: upickle.Js.Value): SPValue = {
//    val json = upickle.json.write(value)
//    SPValue.fromJson(json).getOrElse(SPValue("ERROR_UPICKLE"))
//  }
//
//
//
//}
//
//case class Hej(p1: String)
//
///**
//  * Testing API handling
//  */
//class ModelMakerAPITest extends FreeSpec with Matchers {
//  "CompTest" - {
//    "is it working" in {
//      import upickle.default._
//
//      val t = APITEST.Test1("hej", "då")
//      val t2 = APITEST.Test2(10, 2)
//      val t3 = APITEST.Test3(10.0, APITEST.Tom("tom"))
//
//      val j1 = APIParser.write(t)
//      val j2 = APIParser.write(t2)
//      val j3 = APIParser.write(t3)
//
//      println(j1)
//      println(j2)
//      println(j3)
//
//      println(APIParser.read[APITEST.API](j1))
//      println(APIParser.read[APITEST.API](j2))
//      println(APIParser.read[APITEST.API](j3))
//
//      println(APITEST.apiJson)
//
//
//    }
//    "testing more" in {
//      val t = APITEST.Test1("hej", "då")
//      val json = APIParser.write(t)
//
//      val test = APIParser.read[Hej](json)
//      println(test)
//
//      val text = """{"p1":"pp", "p2":"pp2"}"""
//      println(APIParser.read[APITEST.Test1](text))
//
//    }
//
//    "testing even more on SP Messages" in {
//      import sp.domain.Logic._
//      val t1 = APITEST.Test1("hej", "då")
//      val t2 = APITEST.Test4(SPAttributes("hej"->SPAttributes("kalle"->1)).addTimeStamp)
//      val json = APIParser.write(t2)
//      println(json)
//
//
//      val reOrderJson =
//        """
//          |{"time":"2017-02-03T08:25:56.859+0100", "p1":{"hej":{"kalle":"1"}}, "isa":"APITEST.Test4"}
//        """.stripMargin
//
//
//      val r = APIParser.read[APITEST.API](reOrderJson)
//      println(r)
//    }
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
