package sp.models

import org.scalatest.{FreeSpec, Matchers}
import sp.domain.{ID, SPAttributes}
import sp.messages._

import scala.util.Success


object APITEST extends SPCommunicationAPI {
  sealed trait API
  case class Test1(p1: String, p2: String) extends API
  case class Test2(p1: Int, p2: Int) extends API
  case class Test3(p1: Double, p2: Tom) extends API
  case class Tom(str: String)

  import sp.domain.LogicNoImplicit._
  import org.json4s._


  override type MessageType = API


  // Automatically generate using macros
  override val apiFormats = List(
    classOf[Test1],
    classOf[Test2],
    classOf[Test3]
  )

  override val jsonFormats = List(

  )

}


/**
  * Testing API handling
  */
class ModelMakerAPITest extends FreeSpec with Matchers {
  "CompTest" - {
    "is it working" in {
      implicit val f = APITEST.formats
      val t = APITEST.Test1("hej", "då")

      val json = APITEST.write(t)
      val json2 = APITEST.write(APITEST.Test3(2, APITEST.Tom("hej")))
      println(json)
      println(json2)


      val res = APITEST.read(json)
      println(res)
      val res2 = APITEST.read(json2)
      println(res2)


    }
  }

  "Json serialization" - {
    "convert and add isa to modelmaker messages" in {
      implicit val formats = ModelMakerAPI.formats
      val cm = CreateModel("hej", Some(SPAttributes("attr" -> "hej")), None)
      val dm = DeleteModel(ID.makeID("2c99c220-2f72-45e0-b926-3d8e7a08114c").get)

      val jsonCM = ModelMakerAPI.write(cm)
      val jsonDM = ModelMakerAPI.write(dm)

      jsonCM shouldEqual """{"isa":"CreateModel","name":"hej","attributes":{"attr":"hej"}}"""
      jsonDM shouldEqual """{"isa":"DeleteModel","model":"2c99c220-2f72-45e0-b926-3d8e7a08114c"}"""
    }

    "read modelmaker messages" in {
      implicit val formats = ModelMakerAPI.formats
      val cm = CreateModel("hej", Some(SPAttributes("attr" -> "hej")), None)
      val dm = DeleteModel(ID.makeID("2c99c220-2f72-45e0-b926-3d8e7a08114c").get)

      val jsonCM = """{"isa":"CreateModel","name":"hej","attributes":{"attr":"hej"}}"""
      val jsonDM = """{"isa":"DeleteModel","model":"2c99c220-2f72-45e0-b926-3d8e7a08114c"}"""

      ModelMakerAPI.read(jsonCM) shouldEqual Success(cm)
      ModelMakerAPI.read(jsonDM) shouldEqual Success(dm)

    }

    "read pf modelmaker messages" in {
      implicit val formats = ModelMakerAPI.formats
      val cm = CreateModel("hej", Some(SPAttributes("attr" -> "hej")), None)
      val dm = DeleteModel(ID.makeID("2c99c220-2f72-45e0-b926-3d8e7a08114c").get)

      val jsonCM = """{"isa":"CreateModel","name":"hej","attributes":{"attr":"hej"}}"""

      var test = false
      ModelMakerAPI.readPF(jsonCM){
        case cmNew: CreateModel => test = cm == cmNew
      }
      {PartialFunction.empty}
      {x: String => test = false}
      assert(test)
    }

    "read pf modelmaker messages and fail" in {
      implicit val formats = ModelMakerAPI.formats
      val cm = CreateModel("hej", Some(SPAttributes("attr" -> "hej")), None)
      val dm = DeleteModel(ID.makeID("2c99c220-2f72-45e0-b926-3d8e7a08114c").get)

      val jsonCM = """{"isa":"CreateModel2","name":"hej","attributes":{"attr":"hej"}}"""

      var test = false
      ModelMakerAPI.readPF(jsonCM){
        case cmNew: CreateModel => test = false
      }
      {PartialFunction.empty}
      {x: String =>
        test = true
      }
      assert(test)
    }

    "read pf SPMessage messages" in {
      implicit val formats = ModelMakerAPI.formats
      import sp.messages._
      val req = StatusRequest()
      val jsonR = ModelMakerAPI.write(req)

      var test = false
      ModelMakerAPI.readPF(jsonR){
        case cmNew: ModelMakerMessages => test = false
      }
      {case StatusRequest(param) => test = true}
      {x: String => test = false}
      assert(test)
    }

  }
//  "Macro" - {
//    "should work" in {
//      val test = SealedExample.values[ModelMakerMessages]
//      println("macro test:")
//      test.foreach(println)
//    }
//  }

}

