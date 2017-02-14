package sp.domain.logic

import org.json4s._
import org.scalatest._
import sp.system.messages._

/**
 * Created by kristofer on 01/10/14.
 */
//class IDAbleLogicTest extends FreeSpec with Matchers {
//
//  import IDAbleLogic._
//  import sp.domain._
//  import sp.domain.Logic._
//  import org.json4s.JsonDSL._
//
//  val id = ID.newID
//  val otherid = ID.newID
//  val ids = Set(id)
//
//  implicit f = jsonFormats
//  implicit def strToJ(x: String): JValue = JString(x)
//  implicit def intToJ(x: Int): JValue = JInt(x)
//  implicit def boolToJ(x: Boolean): JValue = JBool(x)
//  implicit def doubleToJ(x: Double): JValue = JDouble(x)
//  implicit def idToJ(x: ID): JValue = Extraction.decompose(x)
//
//
//  "when removing ids from IDAbles" - {
//    "removing IDs in attributes should" - {
//      "return the same attributes if no match" - {
//        "when empty input" in {
//          val testAttr = SPAttributes()
//          removeIDFromAttribute(ids, testAttr) shouldEqual testAttr
//        }
//        "when simple attr input" in {
//          val testAttr = SPAttributes("hej"-> "kalle", "id" -> otherid)
//          removeIDFromAttribute(ids, testAttr) shouldEqual testAttr
//        }
//        "when req attr input" in {
//          val json =
//            ("hej"-> ("hej", otherid)) ~
//            ("map" ->
//              ("1"->(5,otherid,("3"->otherid)))~
//              ("2"->"kalle")
//              )
//
//          val testAttr: SPAttributes = json
//          removeIDFromAttribute(ids, testAttr) shouldEqual testAttr
//        }
//      }
//      "remove the id from attributes" - {
//        "when simple attr input" in {
//          val testAttr = SPAttributes("hej"->"kalle", "id" -> id)
//          removeIDFromAttribute(ids, testAttr) shouldEqual SPAttributes("hej"->"kalle")
//        }
//        "when req attr input" in {
//          val testAttr = SPAttributes(
//            "hej"-> (List(id)),
//            "hej2" -> (id),
//            "map" -> (Map(
//              "1" -> (List(5, id, (Map("3"-> otherid)))),
//              "2" -> "kalle"
//            )))
//          removeIDFromAttribute(ids, testAttr) shouldEqual SPAttributes(
//            "hej"-> (List()),
//            "map" -> (Map(
//              "1" -> (List(5, (Map("3"-> otherid)))),
//              "2" -> "kalle"
//            )))
//        }
//      }
//
//    }
//    "Removing IDs from Action" - {
//      "should not change the Action if no id" - {
//        "for empty action" in {
//          removeIDFromAction(ids, List()) shouldEqual List()
//        }
//        "for simple action" in {
//          val a = Action(otherid, ValueHolder("1"))
//          removeIDFromAction(ids, List(a)) shouldEqual List(a)
//        }
//        "for list of actions" in {
//          val actions = List(Action(otherid, ValueHolder("1")), Action(otherid, ASSIGN(otherid)))
//          removeIDFromAction(ids, actions) shouldEqual actions
//        }
//      }
//      "should remove Actions if include id" - {
//        "return empty list if all is removed" in {
//          val a = Action(id, ValueHolder("1"))
//          removeIDFromAction(ids, List(a)) shouldEqual List()
//        }
//        "for list of actions using assign" in {
//          val actions = List(Action(otherid, ValueHolder("1")), Action(otherid, ASSIGN(id)))
//          removeIDFromAction(ids, actions) shouldEqual List(Action(otherid, ValueHolder("1")))
//        }
//        "for list of actions using svid" in {
//          val actions = List(Action(id, ValueHolder("1")), Action(otherid, ASSIGN(otherid)))
//          removeIDFromAction(ids, actions) shouldEqual List(Action(otherid, ASSIGN(otherid)))
//        }
//      }
//    }
//    "Removing IDs from Propositions" - {
//      "should not change the proposition if no id" - {
//        "for empty proposition" in {
//          removeIDFromProposition(ids, AND(List())) shouldEqual AlwaysTrue
//        }
//        "for simple proposition" in {
//          val p = EQ(otherid, ValueHolder("1"))
//          removeIDFromProposition(ids, p) shouldEqual p
//        }
//        "for list of propositions" in {
//          val prop = AND(List(EQ(otherid, ValueHolder("1")), OR(List(EQ(otherid, ValueHolder("1"))))))
//          removeIDFromProposition(ids, prop) shouldEqual prop
//        }
//      }
//      "should remove props if include id" - {
//        "return always true if all is removed" in {
//          val p = AND(List(EQ(id, ValueHolder("1")), EQ(id, ValueHolder("1"))))
//          removeIDFromProposition(ids, p) shouldEqual AlwaysTrue
//        }
//        "for list of propositions" in {
//          val p = OR(List(NEQ(id, ValueHolder("1")), EQ(ValueHolder(id), otherid), EQ(otherid, ValueHolder("1"))))
//          removeIDFromProposition(ids, p) shouldEqual OR(List(EQ(otherid, ValueHolder("1"))))
//        }
//        "for req struct of propositions" in {
//          val p = AND(List(
//            NOT(EQ(id, ValueHolder("1"))),
//            EQ(otherid, ValueHolder("1")),
//            OR(List(
//              EQ(otherid, ValueHolder("1")),
//              AND(List(
//                EQ(otherid, ValueHolder("1")),
//                EQ(id, ValueHolder("1"))))
//            ))))
//
//          val res = AND(List(
//            EQ(otherid, ValueHolder("1")),
//            OR(List(
//              EQ(otherid, ValueHolder("1")),
//              AND(List(
//                EQ(otherid, ValueHolder("1"))
//                ))
//            ))))
//          removeIDFromProposition(ids, p) shouldEqual res
//        }
//      }
//    }
//    "Removing IDs from Conditions" - {
//      "should not change the condition if no id" - {
//        "for empty condition" in {
//          val empty = PropositionCondition(AlwaysTrue, List())
//          removeIDFromCondition(ids, empty) shouldEqual empty
//        }
//        "for simple condition" in {
//          val c = PropositionCondition(EQ(otherid, ValueHolder(1)), List(Action(otherid, ValueHolder(1))))
//          removeIDFromCondition(ids, c) shouldEqual c
//        }
//        "for complex conditions" in {
//          val p = PropositionCondition(
//            AND(List(EQ(otherid, ValueHolder("1")), OR(List(EQ(otherid, ValueHolder("1")))))),
//            List(Action(otherid, ValueHolder(1))),
//            SPAttributes("hej"->(Map("hej"->otherid)))
//          )
//          removeIDFromCondition(ids, p) shouldEqual p
//        }
//      }
//      "should remove props if include id" - {
//        "return always true if all is removed" in {
//          val c = PropositionCondition(EQ(id, ValueHolder(1)), List(Action(id, ValueHolder(1))))
//          removeIDFromCondition(ids, c) shouldEqual PropositionCondition(AlwaysTrue, List())
//        }
//        "for a condition" in {
//          val c = PropositionCondition(
//            AND(List(EQ(id, ValueHolder(1)), EQ(otherid, ValueHolder(1)))),
//            List(Action(id, ValueHolder(1)), Action(otherid, ValueHolder(1))),
//            SPAttributes("hej"->1, "då"->id))
//          val res = PropositionCondition(
//            AND(List(EQ(otherid, ValueHolder(1)))),
//            List(Action(otherid, ValueHolder(1))),
//            SPAttributes("hej"->1))
//
//          removeIDFromCondition(ids, c) shouldEqual res
//        }
//      }
//    }
//    "Removing IDs from Operation" - {
//      "should not change the operation if no id" - {
//        "for empty operation" in {
//          val empty = Operation("hej")
//          removeIDFromOperation(ids, empty) shouldEqual empty
//        }
//        "for simple operation" in {
//          val o = Operation("hej", List(PropositionCondition(EQ(otherid, ValueHolder(1)), List(Action(otherid, ValueHolder(1))))), SPAttributes("hej"-> 1))
//          removeIDFromOperation(ids, o) shouldEqual o
//        }
//        "for complex operations" in {
//          val o = Operation("hej",
//            List(PropositionCondition(
//              AND(List(EQ(otherid, ValueHolder("1")), OR(List(EQ(otherid, ValueHolder("1")))))),
//              List(Action(otherid, ValueHolder(1))),
//              SPAttributes("hej"->(Map("hej"->otherid)))
//            )),
//            SPAttributes("hej"-> 1, "nej"->otherid))
//
//          removeIDFromOperation(ids, o) shouldEqual o
//        }
//      }
//      "should remove props if include id" - {
//        "for an operation" in {
//          val o = Operation("hej",
//            List(PropositionCondition(
//              AND(List(EQ(id, ValueHolder("1")), OR(List(EQ(otherid, ValueHolder("1")))))),
//              List(Action(otherid, ValueHolder(1)), Action(id, ValueHolder(1))),
//              SPAttributes("hej"->(Map("hej"->id)))
//            )),
//            SPAttributes("hej"-> 1, "nej"->id))
//          val res = Operation("hej",
//            List(PropositionCondition(
//              AND(List(OR(List(EQ(otherid, ValueHolder("1")))))),
//              List(Action(otherid, ValueHolder(1))),
//              SPAttributes("hej"->(Map()))
//            )),
//            SPAttributes("hej"-> 1))
//
//          Operation.unapply(removeIDFromOperation(ids, o)) shouldEqual Operation.unapply(res)
//        }
//      }
//    }
//    "Removing IDs from Thing" - {
//      "should not change the thing if no id" - {
//        "for empty thing" in {
//          val empty = Thing("hej")
//          removeIDFromThing(ids, empty) shouldEqual empty
//        }
//        "for simple thing" in {
//          val t = Thing("hej", SPAttributes("hej"-> 1))
//          removeIDFromThing(ids, t) shouldEqual t
//        }
//        "for complex things" in {
//          val t = Thing("hej", SPAttributes("hej"-> (List(otherid))))
//
//          removeIDFromThing(ids, t) shouldEqual t
//        }
//      }
//      "should remove props if include id" - {
//        "for an thing" in {
//          val tid = ID.newID
//          val t = Thing("hej",
//            SPAttributes("hej"-> (List(otherid, tid))),
//            tid
//          )
//
//          val res = Thing("hej",
//            SPAttributes("hej"-> (List(otherid))),
//            tid
//          )
//
//          removeIDFromThing(ids, t) shouldEqual res
//        }
//      }
//    }
//    "Removing IDs from SOP" - {
//      "should not change the sop if no id" - {
//        "for empty sop" in {
//          val empty = Parallel()
//          removeIDFromSOP(ids, empty).get shouldEqual empty
//        }
//        "for simple sop" in {
//          val t = Parallel(otherid, otherid)
//          removeIDFromSOP(ids, t).get shouldEqual t
//        }
//        "for complex sop" in {
//          val t = Parallel(
//            otherid,
//            otherid,
//            Sequence(Parallel(otherid, otherid, otherid), otherid, otherid)
//          )
//
//          removeIDFromSOP(ids, t).get shouldEqual t
//        }
//      }
//      "should remove props if include id" - {
//        "return none if all is removed" in {
//          val t = Parallel(id, id)
//          removeIDFromSOP(ids, t) shouldEqual None
//        }
//
//        "for a sop" in {
//          val t = Parallel(
//            otherid,
//            id,
//            Sequence(Parallel(id, id, id), otherid, otherid)
//          )
//
//          val res = Parallel(
//            otherid,
//            Sequence(otherid, otherid)
//          )
//
//          removeIDFromSOP(ids, t).get shouldEqual res
//        }
//        "for a sop 2" in {
//          val t = Parallel(Sequence(
//            otherid,
//            id
//          ))
//
//          val res = Hierarchy(otherid)
//
//          removeIDFromSOP(ids, t).get shouldEqual res
//        }
//      }
//    }
//    "Removing IDs from List of IDAbles" - {
//      "should not change the items if no id" - {
//        val o = Operation("hej", List(PropositionCondition(EQ(otherid, ValueHolder(1)), List(Action(otherid, ValueHolder(1))))), SPAttributes("hej"-> 1))
//        val s = SOPSpec("hej", List(Parallel(otherid, otherid)))
//        val t = Thing("hej", SPAttributes("hej"-> 1))
//
//        removeID(ids, List(o, s, t)) shouldEqual List()
//      }
//      "should return updated items if id" - {
//        val tid = ID.newID
//        val o = Operation("hej", List(PropositionCondition(EQ(otherid, ValueHolder(1)), List(Action(otherid, ValueHolder(1))))), SPAttributes("hej"-> 1))
//        val s = SOPSpec("hej", List(Parallel(otherid, otherid)))
//        val t = Thing("hej", SPAttributes("hej"-> 1, "nej"-> id), tid)
//
//        val res = Thing("hej", SPAttributes("hej"-> 1), tid)
//
//        removeID(ids, List(o, s, t)) shouldEqual res
//      }
//      "should return updated items if id 2" - {
//        val o = Operation("hej", List(PropositionCondition(EQ(otherid, ValueHolder(1)), List(Action(otherid, ValueHolder(1))))), SPAttributes("hej"-> 1, "resource"->otherid))
//        val s = SOPSpec("hej", List(Parallel(otherid, otherid, id)))
//        val t = Thing("hej", SPAttributes("hej"-> 1))
//
//        val res = s.copy(sop = List(Parallel(otherid, otherid)))
//
//        removeID(ids, List(o, s, t)) shouldEqual res
//      }
//    }
//  }
//}
