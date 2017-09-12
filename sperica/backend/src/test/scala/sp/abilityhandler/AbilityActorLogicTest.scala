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

      val res = logic.extractVariables(Condition(g, List(a)))
      res.toSet shouldEqual Set(v1.id, v2.id, v3.id, v4.id)
    }

    val v1 = Thing("v1")
    val pre = Condition(EQ(v1.id, 1), List(Action(v1.id, ValueHolder(2))))
    val post = Condition(EQ(v1.id, 3), List(Action(v1.id, ValueHolder(4))))
    val started = Condition(EQ(v1.id, 2), List())
    val reset = Condition(AlwaysTrue, List(Action(v1.id, ValueHolder(1))))
    val a = api.Ability("test", ID.newID, pre, started, post, reset)

    import AbilityState._
    "updstate" in {
      val logic = new AbilityActorLogic {
        override val ability = a
      }
      logic.state shouldEqual unavailable
      logic.evalState(Map(v1.id -> 0))._1 shouldEqual Some(notEnabled)
      logic.evalState(Map(v1.id -> 2))._1 shouldEqual Some(executing)
      logic.evalState(Map(v1.id -> 3))._1 shouldEqual Some(finished)
      // Auto restart
      logic.evalState(Map(v1.id -> 3))._1 shouldEqual Some(notEnabled)
      logic.evalState(Map(v1.id -> 2))._1 shouldEqual Some(executing)

    }
    "updstate when missing state ids" in {
      val logic = new AbilityActorLogic {
        override val ability = a
      }
      logic.state shouldEqual unavailable
      println(logic.evalState(Map()))

    }

    "startNReset" in {
      val logic = new AbilityActorLogic {
        override val ability = a
      }
      logic.state shouldEqual unavailable

      val init: Map[ID, SPValue] = Map(v1.id -> 0)
      logic.evalState(init)._1 shouldEqual Some(notEnabled)
      logic.start(init) shouldEqual  None
      logic.start(Map(v1.id -> 1)) shouldEqual Some(Map(v1.id -> SPValue(2)))
      logic.state shouldEqual starting
      logic.evalState(Map(v1.id -> 2))._1 shouldEqual Some(executing)
      logic.reset(Map(v1.id -> 2)) shouldEqual Some(Map(v1.id -> SPValue(1)))
      logic.evalState(Map(v1.id -> 3))
      logic.state shouldEqual notEnabled

    }


    "sendCmc" in {
      val logic = new AbilityActorLogic {
        override val ability = a
      }

      logic.start(Map(v1.id -> 0)) shouldEqual None
      logic.start(Map(v1.id -> 1)) shouldEqual Some(Map(v1.id -> SPValue(2)))

      logic.state = starting

      logic.start(Map(v1.id -> 1)) shouldEqual None


    }



  }
}
