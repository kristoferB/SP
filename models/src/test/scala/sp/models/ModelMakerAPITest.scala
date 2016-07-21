package sp.models

import org.scalatest.{FreeSpec, Matchers}
import sp.domain.{ID, SPAttributes}
import sp.messages.StatusRequest

import scala.util.Success


/**
  * Testing API handling
  */
class ModelMakerAPITest extends FreeSpec with Matchers {
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

