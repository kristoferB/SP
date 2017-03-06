package sp.abilityhandler

import org.scalatest._
import sp.domain._
import sp.domain.Logic._

/**
  * Created by kristofer on 2017-03-06.
  */
class AbilityActorLogicTest extends FreeSpec with Matchers{

  "Methods tests" - {
    val logic = new AbilityActorLogic {
      override val ability = APIAbilityHandler.Ability("test", ID.newID)
      override var state = AbilityState.State("", 0, SPAttributes())
    }
    "extractVariables" in {
      val v1 = Thing("v1")
      val v2 = Thing("v2")
      val v3 = Thing("v3")
      val v4 = Thing("v4")

      val g = AND(List(OR(List(EQ(v1.id, 3), EQ(v4.id, 40))), NEQ(v2.id, ValueHolder(false))))
      val a = Action(v3.id, ValueHolder(2))

      val res = logic.extractVariables(PropositionCondition(g, List(a)))
      res.toSet shouldEqual Set(v1.id, v2.id, v3.id, v4.id)
    }

  }
}
