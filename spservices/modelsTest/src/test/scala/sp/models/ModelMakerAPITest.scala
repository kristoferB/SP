//package sp.models
//
//import org.json4s.ShortTypeHints
//import org.scalatest.{FreeSpec, Matchers}
//import sp.domain._
//import sp.messages._
//
//import scala.util.Success
//
//
//
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
//
//  // Result json fín apiJson, copy paste in http://jsoneditoronline.org to view
//  /**
//    * [{"isa":"APITEST.Test1","p1":{"key":"p1","ofType":"String"},"p2":{"key":"p2","ofType":"String"}},{"isa":"APITEST.Test2","p1":{"key":"p1","ofType":"Int"},"p2":{"key":"p2","ofType":"Int"}},{"isa":"APITEST.Test3","p1":{"key":"p1","ofType":"Double"},"p2":{"key":"p2","ofType":"APITEST.Tom"}},{"subs":{"APITEST.Tom":{"str":{"key":"str","ofType":"String"}}}}]
//    */
//
//
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
//    "convert and add isa to modelmaker messages" in {
//      implicit val formats = ModelMakerAPI.formats
//      val cm = ModelMakerAPI.CreateModel("hej", Some(SPAttributes("attr" -> "hej")), None)
//      val dm = ModelMakerAPI.DeleteModel(ID.makeID("2c99c220-2f72-45e0-b926-3d8e7a08114c").get)
//
//      val jsonCM = ModelMakerAPI.write(cm)
//      val jsonDM = ModelMakerAPI.write(dm)
//
//      jsonCM shouldEqual """{"isa":"ModelMakerAPI$CreateModel","name":"hej","attributes":{"attr":"hej"}}"""
//      jsonDM shouldEqual """{"isa":"ModelMakerAPI$DeleteModel","model":"2c99c220-2f72-45e0-b926-3d8e7a08114c"}"""
//    }
//
//    "read modelmaker messages" in {
//      implicit val formats = ModelMakerAPI.formats
//      val cm = ModelMakerAPI.CreateModel("hej", Some(SPAttributes("attr" -> "hej")), None)
//      val dm = ModelMakerAPI.DeleteModel(ID.makeID("2c99c220-2f72-45e0-b926-3d8e7a08114c").get)
//
//      val jsonCM = """{"isa":"ModelMakerAPI$CreateModel","name":"hej","attributes":{"attr":"hej"}}"""
//      val jsonDM = """{"isa":"ModelMakerAPI$DeleteModel","model":"2c99c220-2f72-45e0-b926-3d8e7a08114c"}"""
//
//      ModelMakerAPI.read(jsonCM) shouldEqual Success(cm)
//      ModelMakerAPI.read(jsonDM) shouldEqual Success(dm)
//
//    }
//
//    "read pf modelmaker messages" in {
//      implicit val formats = ModelMakerAPI.formats
//      val cm = ModelMakerAPI.CreateModel("hej", Some(SPAttributes("attr" -> "hej")), None)
//      val dm = ModelMakerAPI.DeleteModel(ID.makeID("2c99c220-2f72-45e0-b926-3d8e7a08114c").get)
//
//      val jsonCM = """{"isa":"ModelMakerAPI$CreateModel","name":"hej","attributes":{"attr":"hej"}}"""
//
//      var test = false
//      ModelMakerAPI.readPF(jsonCM){
//        case cmNew: ModelMakerAPI.CreateModel => test = cm == cmNew
//      }
//      {PartialFunction.empty}
//      {x: String => test = false}
//      assert(test)
//    }
//
//    "read pf modelmaker messages and fail" in {
//      implicit val formats = ModelMakerAPI.formats
//      val cm = ModelMakerAPI.CreateModel("hej", Some(SPAttributes("attr" -> "hej")), None)
//      val dm = ModelMakerAPI.DeleteModel(ID.makeID("2c99c220-2f72-45e0-b926-3d8e7a08114c").get)
//
//      val jsonCM = """{"isa":"ModelMakerAPI$CreateModel2","name":"hej","attributes":{"attr":"hej"}}"""
//
//      var test = false
//      ModelMakerAPI.readPF(jsonCM){
//        case cmNew: ModelMakerAPI.CreateModel => test = false
//      }
//      {PartialFunction.empty}
//      {x: String =>
//        test = true
//      }
//      assert(test)
//    }
//
//    "read pf SPMessage messages" in {
//      implicit val formats = ModelMakerAPI.formats
//      import sp.messages._
//      val req = StatusRequest()
//      val jsonR = ModelMakerAPI.write(req)
//
//      var test = false
//      ModelMakerAPI.readPF(jsonR){
//        case cmNew: ModelMakerAPI.API => test = false
//      }
//      {case StatusRequest(param) => test = true}
//      {x: String => test = false}
//      assert(test)
//    }
//
//  }
//
//
//}
//
