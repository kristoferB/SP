package sp.services.sopmaker

import org.scalatest._
import sp.domain._
import sp.domain.logic.OperationLogic.EvaluateProp

/**
 * Created by Kristofer on 2014-08-06.
 */
class SOPMakerTest extends FreeSpec with Matchers with Defs {
  "The SOPMaker" - {
    "when grouping " - {
      "should convert ops to sop " in {
        val sops = makeSOPsFromOpsID(ops)
        sops shouldEqual List(Hierarchy(o1), Hierarchy(o2), Hierarchy(o3), Hierarchy(o4))
      }

    }


  }
}

trait Defs extends Groupify {


  val o1 = Operation("o1").id
  val o2 = Operation("o2").id
  val o3 = Operation("o2").id
  val o4 = Operation("o2").id

  val ops = List(o1,o2,o3,o4)

  val o1o2 = OperationPair(o1,o2)
  val o1o3 = OperationPair(o1,o3)
  val o1o4 = OperationPair(o1,o4)
  val o2o3 = OperationPair(o2,o3)
  val o2o4 = OperationPair(o2,o4)
  val o3o4 = OperationPair(o3,o4)

  val es = EnabledStatesMap(Map())
  val rm = RelationMap(Map(), es)
}
