package sp.domain.logic

import org.scalatest._
import sp.system.messages.UpdateID

/**
 * Created by kristofer on 01/10/14.
 */
class IDAbleLogicTest extends FreeSpec with Matchers {

  import IDAbleLogic._
  import sp.domain._

  val id = ID.newID
  val otherid = ID.newID
  val ids = Set(id)

  "when removing ids from IDAbles" - {
    "removing IDs in attributes should" - {
      "return the same attributes if no match" - {
        "when empty input" in {
          val testAttr = Attr()
          removeIDFromAttribute(ids, testAttr) shouldEqual testAttr
        }
        "when simple attr input" in {
          val testAttr = Attr("hej"->"kalle", "id" -> otherid)
          removeIDFromAttribute(ids, testAttr) shouldEqual testAttr
        }
        "when req attr input" in {
          val testAttr = Attr(
            "hej"-> ListPrimitive(List("hej", otherid)),
            "map" -> MapPrimitive(Map(
              "1" -> ListPrimitive(List(5, otherid, MapPrimitive(Map("3"-> otherid)))),
              "2" -> "kalle"
            )))
          removeIDFromAttribute(ids, testAttr) shouldEqual testAttr
        }
      }
      "remove the id from attributes" - {
        "when simple attr input" in {
          val testAttr = Attr("hej"->"kalle", "id" -> id)
          removeIDFromAttribute(ids, testAttr) shouldEqual Attr("hej"->"kalle")
        }
        "when req attr input" in {
          val testAttr = Attr(
            "hej"-> ListPrimitive(List(id)),
            "hej2" -> IDPrimitive(id),
            "map" -> MapPrimitive(Map(
              "1" -> ListPrimitive(List(5, id, MapPrimitive(Map("3"-> otherid)))),
              "2" -> "kalle"
            )))
          removeIDFromAttribute(ids, testAttr) shouldEqual Attr(
            "hej"-> ListPrimitive(List()),
            "map" -> MapPrimitive(Map(
              "1" -> ListPrimitive(List(5, MapPrimitive(Map("3"-> otherid)))),
              "2" -> "kalle"
            )))
        }
      }

    }
    "Removing IDs from Action" - {
      "should not change the Action if no id" - {
        "for empty action" in {
          removeIDFromAction(ids, List()) shouldEqual List()
        }
        "for simple action" in {
          val a = Action(otherid, ValueHolder("1"))
          removeIDFromAction(ids, List(a)) shouldEqual List(a)
        }
        "for list of actions" in {
          val actions = List(Action(otherid, ValueHolder("1")), Action(otherid, ASSIGN(otherid)))
          removeIDFromAction(ids, actions) shouldEqual actions
        }
      }
      "should remove Actions if include id" - {
        "return empty list if all is removed" in {
          val a = Action(id, ValueHolder("1"))
          removeIDFromAction(ids, List(a)) shouldEqual List()
        }
        "for list of actions using assign" in {
          val actions = List(Action(otherid, ValueHolder("1")), Action(otherid, ASSIGN(id)))
          removeIDFromAction(ids, actions) shouldEqual List(Action(otherid, ValueHolder("1")))
        }
        "for list of actions using svid" in {
          val actions = List(Action(id, ValueHolder("1")), Action(otherid, ASSIGN(otherid)))
          removeIDFromAction(ids, actions) shouldEqual List(Action(otherid, ASSIGN(otherid)))
        }
      }
    }
    "Removing IDs from Propositions" - {
      "should not change the proposition if no id" - {
        "for empty proposition" in {
          removeIDFromProposition(ids, AND(List())) shouldEqual AlwaysTrue
        }
        "for simple proposition" in {
          val p = EQ(otherid, ValueHolder("1"))
          removeIDFromProposition(ids, p) shouldEqual p
        }
        "for list of propositions" in {
          val prop = AND(List(EQ(otherid, ValueHolder("1")), OR(List(EQ(otherid, ValueHolder("1"))))))
          removeIDFromProposition(ids, prop) shouldEqual prop
        }
      }
      "should remove props if include id" - {
        "return always true if all is removed" in {
          val p = AND(List(EQ(id, ValueHolder("1")), EQ(id, ValueHolder("1"))))
          removeIDFromProposition(ids, p) shouldEqual AlwaysTrue
        }
        "for list of propositions" in {
          val p = OR(List(NEQ(id, ValueHolder("1")), EQ(ValueHolder(id), otherid), EQ(otherid, ValueHolder("1"))))
          removeIDFromProposition(ids, p) shouldEqual OR(List(EQ(otherid, ValueHolder("1"))))
        }
        "for req struct of propositions" in {
          val p = AND(List(
            NOT(EQ(id, ValueHolder("1"))),
            EQ(otherid, ValueHolder("1")),
            OR(List(
              EQ(otherid, ValueHolder("1")),
              AND(List(
                EQ(otherid, ValueHolder("1")),
                EQ(id, ValueHolder("1"))))
            ))))

          val res = AND(List(
            EQ(otherid, ValueHolder("1")),
            OR(List(
              EQ(otherid, ValueHolder("1")),
              AND(List(
                EQ(otherid, ValueHolder("1"))
                ))
            ))))
          removeIDFromProposition(ids, p) shouldEqual res
        }
      }
    }
    "Removing IDs from Conditions" - {
      "should not change the condition if no id" - {
        "for empty condition" in {
          val empty = PropositionCondition(AlwaysTrue, List())
          removeIDFromCondition(ids, empty) shouldEqual empty
        }
        "for simple condition" in {
          val c = PropositionCondition(EQ(otherid, ValueHolder(1)), List(Action(otherid, ValueHolder(1))))
          removeIDFromCondition(ids, c) shouldEqual c
        }
        "for complex conditions" in {
          val p = PropositionCondition(
            AND(List(EQ(otherid, ValueHolder("1")), OR(List(EQ(otherid, ValueHolder("1")))))),
            List(Action(otherid, ValueHolder(1))),
            Attr("hej"->MapPrimitive(Map("hej"->otherid)))
          )
          removeIDFromCondition(ids, p) shouldEqual p
        }
      }
      "should remove props if include id" - {
        "return always true if all is removed" in {
          val c = PropositionCondition(EQ(id, ValueHolder(1)), List(Action(id, ValueHolder(1))))
          removeIDFromCondition(ids, c) shouldEqual PropositionCondition(AlwaysTrue, List())
        }
        "for a condition" in {
          val c = PropositionCondition(
            AND(List(EQ(id, ValueHolder(1)), EQ(otherid, ValueHolder(1)))),
            List(Action(id, ValueHolder(1)), Action(otherid, ValueHolder(1))),
            Attr("hej"->1, "då"->id))
          val res = PropositionCondition(
            AND(List(EQ(otherid, ValueHolder(1)))),
            List(Action(otherid, ValueHolder(1))),
            Attr("hej"->1))

          removeIDFromCondition(ids, c) shouldEqual res
        }
      }
    }
    "Removing IDs from Operation" - {
      "should not change the operation if no id" - {
        "for empty operation" in {
          val empty = Operation("hej")
          removeIDFromOperation(ids, empty) shouldEqual empty
        }
        "for simple operation" in {
          val o = Operation("hej", List(PropositionCondition(EQ(otherid, ValueHolder(1)), List(Action(otherid, ValueHolder(1))))), Attr("hej"-> 1))
          removeIDFromOperation(ids, o) shouldEqual o
        }
        "for complex operations" in {
          val o = Operation("hej",
            List(PropositionCondition(
              AND(List(EQ(otherid, ValueHolder("1")), OR(List(EQ(otherid, ValueHolder("1")))))),
              List(Action(otherid, ValueHolder(1))),
              Attr("hej"->MapPrimitive(Map("hej"->otherid)))
            )),
            Attr("hej"-> 1, "nej"->otherid))

          removeIDFromOperation(ids, o) shouldEqual o
        }
      }
      "should remove props if include id" - {
        "for an operation" in {
          val o = Operation("hej",
            List(PropositionCondition(
              AND(List(EQ(id, ValueHolder("1")), OR(List(EQ(otherid, ValueHolder("1")))))),
              List(Action(otherid, ValueHolder(1)), Action(id, ValueHolder(1))),
              Attr("hej"->MapPrimitive(Map("hej"->id)))
            )),
            Attr("hej"-> 1, "nej"->id))
          val res = Operation("hej",
            List(PropositionCondition(
              AND(List(OR(List(EQ(otherid, ValueHolder("1")))))),
              List(Action(otherid, ValueHolder(1))),
              Attr("hej"->MapPrimitive(Map()))
            )),
            Attr("hej"-> 1))

          Operation.unapply(removeIDFromOperation(ids, o)) shouldEqual Operation.unapply(res)
        }
      }
    }
    "Removing IDs from Thing" - {
      "should not change the thing if no id" - {
        "for empty thing" in {
          val empty = Thing("hej")
          removeIDFromThing(ids, empty) shouldEqual empty
        }
        "for simple thing" in {
          val t = Thing("hej", List(StateVariable("hej", Attr(), otherid)), Attr("hej"-> 1))
          removeIDFromThing(ids, t) shouldEqual t
        }
        "for complex things" in {
          val t = Thing("hej", List(StateVariable("hej", Attr("hej" -> otherid), otherid)), Attr("hej"-> ListPrimitive(List(otherid))))

          removeIDFromThing(ids, t) shouldEqual t
        }
      }
      "should remove props if include id" - {
        "for an thing" in {
          val t = Thing("hej", List(
            StateVariable("hej", Attr("hej" -> otherid, "nej"->id), otherid),
            StateVariable("hej", Attr("hej" -> otherid), id)
          ),
            Attr("hej"-> ListPrimitive(List(otherid, id))))

          val res = Thing("hej", List(
            StateVariable("hej", Attr("hej" -> otherid), otherid)
          ),
            Attr("hej"-> ListPrimitive(List(otherid))))

          Thing.unapply(removeIDFromThing(ids, t)) shouldEqual Thing.unapply(res)
        }
      }
    }
    "Removing IDs from SOP" - {
      "should not change the sop if no id" - {
        "for empty sop" in {
          val empty = Parallel()
          removeIDFromSOP(ids, empty).get shouldEqual empty
        }
        "for simple sop" in {
          val t = Parallel(otherid, otherid)
          removeIDFromSOP(ids, t).get shouldEqual t
        }
        "for complex sop" in {
          val t = Parallel(
            otherid,
            otherid,
            Sequence(Parallel(otherid, otherid, otherid), otherid, otherid)
          )

          removeIDFromSOP(ids, t).get shouldEqual t
        }
      }
      "should remove props if include id" - {
        "return none if all is removed" in {
          val t = Parallel(id, id)
          removeIDFromSOP(ids, t) shouldEqual None
        }

        "for a sop" in {
          val t = Parallel(
            otherid,
            id,
            Sequence(Parallel(id, id, id), otherid, otherid)
          )

          val res = Parallel(
            otherid,
            Sequence(otherid, otherid)
          )

          removeIDFromSOP(ids, t).get shouldEqual res
        }
        "for a sop 2" in {
          val t = Parallel(Sequence(
            otherid,
            id
          ))

          val res = Hierarchy(otherid, EmptySOP)

          removeIDFromSOP(ids, t).get shouldEqual res
        }
      }
    }
    "Removing IDs from List of IDAbles" - {
      "should not change the items if no id" - {
        val o = Operation("hej", List(PropositionCondition(EQ(otherid, ValueHolder(1)), List(Action(otherid, ValueHolder(1))))), Attr("hej"-> 1))
        val s = SOPSpec(List(Parallel(otherid, otherid)), "hej")
        val t = Thing("hej", List(StateVariable("hej", Attr(), otherid)), Attr("hej"-> 1))

        removeID(ids, List(o, s, t)) shouldEqual List()
      }
      "should return updated items if id" - {
        val o = Operation("hej", List(PropositionCondition(EQ(otherid, ValueHolder(1)), List(Action(otherid, ValueHolder(1))))), Attr("hej"-> 1))
        val s = SOPSpec(List(Parallel(otherid, otherid)), "hej")
        val t = Thing("hej", List(StateVariable("hej", Attr(), id)), Attr("hej"-> 1))

        val res = Thing.unapply(Thing("hej", List(), Attr("hej"-> 1)))


        val test = removeID(ids, List(o, s, t))
        Thing.unapply(removeID(ids, List(o, s, t)).head.item.asInstanceOf[Thing]) shouldEqual res
      }
      "should return updated items if id 2" - {
        val o = Operation("hej", List(PropositionCondition(EQ(otherid, ValueHolder(1)), List(Action(otherid, ValueHolder(1))))), Attr("hej"-> 1, "resource"->otherid))
        val s = SOPSpec(List(Parallel(otherid, otherid, id)), "hej")
        val t = Thing("hej", List(StateVariable("hej", Attr(), otherid)), Attr("hej"-> 1))

        val res = SOPSpec.unapply(SOPSpec(List(Parallel(otherid, otherid)), "hej"))


        val test = removeID(ids, List(o, s, t))
        SOPSpec.unapply(removeID(ids, List(o, s, t)).head.item.asInstanceOf[SOPSpec]) shouldEqual res
      }
    }
  }
}
