package sp.abilityhandler

import org.scalatest._
import sp.domain._
import sp.domain.Logic._

/**
  * Created by kristofer on 2017-03-06.
  */
class AbilityActorLogicTest extends FreeSpec with Matchers{
  import sp.abilityhandler.{APIAbilityHandler => api}

  "Methods tests" - {

    "extractVariables" in {
      val logic = new AbilityActorLogic {
        override val ability = APIAbilityHandler.Ability("test", ID.newID)
      }
      val v1 = Thing("v1")
      val v2 = Thing("v2")
      val v3 = Thing("v3")
      val v4 = Thing("v4")

      val g = AND(List(OR(List(EQ(v1.id, 3), EQ(v4.id, 40))), NEQ(v2.id, ValueHolder(false))))
      val a = Action(v3.id, ValueHolder(2))

      val res = logic.extractVariables(PropositionCondition(g, List(a)))
      res.toSet shouldEqual Set(v1.id, v2.id, v3.id, v4.id)
    }

    "updstate" in {
      val v1 = Thing("v1")
      val pre = PropositionCondition(EQ(v1.id, 1), List(Action(v1.id, ValueHolder(2))))
      val post = PropositionCondition(EQ(v1.id, 3), List(Action(v1.id, ValueHolder(4))))
      val started = PropositionCondition(EQ(v1.id, 2), List())
      val reset = PropositionCondition(AlwaysTrue, List(Action(v1.id, ValueHolder(1))))
      val a = api.Ability("test", ID.newID, pre, started, post, reset)
      val logic = new AbilityActorLogic {
        override val ability = a
      }

      import AbilityState._


      logic.state shouldEqual unavailable

      val init: Map[ID, SPValue] = Map(v1.id -> 0)
      logic.evalState(init)._1 shouldEqual Some(notEnabled)

      logic.start(init) shouldEqual  None

      logic.start(Map(v1.id -> 1)) shouldEqual Some(Map(v1.id -> SPValue(2)))
      logic.state shouldEqual starting

      logic.evalState(Map(v1.id -> 2))._1 shouldEqual Some(executing)



    }

  }
}
