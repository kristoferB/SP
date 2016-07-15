package sp.models

import org.scalatest.{FreeSpec, Matchers}
import sp.domain.{ID, SPAttributes}

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

  }
}
