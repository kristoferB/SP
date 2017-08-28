package sp.models

import org.scalatest.{FreeSpec, Matchers}
import sp.domain._
import sp.domain.Logic._
import scala.util.Success






/**
  * Testing API handling
  */
class ModelMakerAPITest extends FreeSpec with Matchers {

    "Testing some message handling" - {
      "model schema" in {


        val s = ModelInfo.apischema
        println(s)
        assert(s.get("error").isEmpty)
      }

      "model API" in {
        val m = APIModel.CreateModel("hej")

        println(SPValue(m))

      }
    }


}

