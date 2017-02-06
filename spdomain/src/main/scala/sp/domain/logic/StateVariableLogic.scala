// Handled by the new case class extraction from attributes.


//package sp.domain.logic
//
///**
// * Created by kristofer on 07/10/14.
// */
//object StateVariableLogic {
//  import sp.domain._
//
//  // attribute data
//  case class StateVarInfo(domain: SVDomain,
//                          init: Option[SPValue] = None,
//                          goal: Option[SPValue] = None)
//
//  sealed trait SVDomain
//  case class DomainList(domain: List[SPValue]) extends SVDomain
//  case class DomainRange(range: Range) extends SVDomain
//  case object DomainBool extends SVDomain
//
//  implicit class svLogic(th: Thing) {
//    def inDomain: SPValue => Boolean = {
//      v => true
////      getStateVarInfo.domain match {
////        case DomainList(xs) => {
////          value => {
////            println(s"$value in $xs = " + xs.contains(value))
////            xs.contains(value)
////          }
////        }
////        case DomainRange(r) => _.asInt.map(r.contains) getOrElse false
////        case DomainBool => v => {
////          v.isInstanceOf[BoolPrimitive] || v.asString.map(s => s=="true" || s == "false").getOrElse(false)
////        }
////      }
//
//    }
//
//    lazy val getStateVarInfo = {
//      val info = th.attributes.get("stateVariable") flatMap {
//        case attr @ MapPrimitive(sv) => {
//          val dom = extractDomain(attr.asSPAttributes)
//          val init = sv.get("init")
//          val goal = sv.get("goal")
//          Some(StateVarInfo(dom, init, goal))
//        }
//        case _ => None
//      }
//      info.getOrElse(StateVarInfo(DomainBool, Some(false), None))
//    }
//
//    def addStateVar(sv: StateVarInfo) = {
//      val domAttr = kevVal(sv.domain)
//      val init = sv.init.map(x => Map("init"-> x)).getOrElse(Map())
//      val goal = sv.goal.map(x => Map("goal"-> x)).getOrElse(Map())
//
//      val stateVar = init ++ goal + domAttr
//      th.copy(attributes = th.attributes + ("stateVariable" -> MapPrimitive(stateVar)))
//    }
//
//
//    private def kevVal(domain: SVDomain): (String, SPValue) = {
//      domain match {
//        case DomainList(xs) => "domain" -> ListPrimitive(xs)
//        case DomainRange(r) => "range" -> MapPrimitive(Map(
//          "start" -> r.start,
//          "end" -> r.end,
//          "step" -> r.step
//        ))
//        case DomainBool => "boolean" -> true
//      }
//    }
//
//    private def extractDomain(attr: SPAttributes): SVDomain = {
//      val dom = attr.getAsList("domain") map(DomainList.apply)
//      val start = attr.getAttribute(List("range", "start")) flatMap(_.asInt)
//      val end = attr.getAttribute(List("range", "end")) flatMap(_.asInt)
//      val step = attr.getAttribute(List("range", "step")) flatMap(_.asInt)
//      val range = for {
//        i <- start
//        e <- end
//        s <- step
//      } yield new DomainRange(Range(i, e, s))
//
//      dom getOrElse (range getOrElse DomainBool)
//    }
//
//
//  }
//
//}