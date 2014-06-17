package sp.domain

import org.scalatest._

/**
 * Created by Kristofer on 2014-06-10.
 */
class StateTest extends WordSpec with Matchers  {

    val sv1: StateVariable = RestrictedIntRangeVariable("sv1", 1 to 10)
    val sv2: StateVariable = RestrictedStringVariable("sv2", Set("1", "två", "tre"))
    val initState = State(Map(sv1->1, sv2->"1"))


  "A State" when {
    "created" should {
      "return empty when out of domain" in {
        assert(State(Map(sv1->100)) == State(Map()))
      }
      "return state when in domain" in {
        assert(State(Map(sv1->1)).stateMap == Map[StateVariable, Any](sv1->1))
      }
    }

    "evaluated" should {
      val s = State(Map(sv1-> 1, sv2->"1"))
      "return true when matches by a map" in {
        val test = Map(sv2->"1")
        assert(s.evaluate(test))

      }

      "return false when they do not match" in {
        val test = Map(sv2->"två")
        assert(!s.evaluate(test))
      }

    }
  }

}
