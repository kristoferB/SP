package sp.merger

import org.scalatest._
import org.scalatest.concurrent.ScalaFutures


/**
 * Created by kristofer on 19/11/14.
 */
class ProductAbilityMergerTest extends FreeSpec with Matchers with ScalaFutures{

  import sp.domain._

  val modelID = ID.makeID("596f6a75-9419-458d-a703-410043d3c54b").get
  val prodID = ID.makeID("596f6a75-9419-458d-a703-410043d3c54c").get
  val abilityID = ID.makeID("596f6a75-9419-458d-a703-410043d3c54d").get

  val pam = new ProductAbilityMerger(null)

  "when getting extracting attributes" - {
    "we will get a correct future" in {
      val a = Attr(
        "model" -> IDPrimitive(modelID),
        "product" -> IDPrimitive(prodID),
        "abilities" -> IDPrimitive(abilityID)
      )

      val res = pam.extractAttr(a)
      res.futureValue shouldEqual (modelID, prodID, abilityID)
    }
  }

}
