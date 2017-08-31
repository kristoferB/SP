package sp.domain.logic

import sp.domain._

/**
 * Created by kristofer on 15-05-27.
 */
object ThingLogic extends ThingLogics

trait ThingLogics {

    implicit class svLogic(th: Thing) {
      def inDomain: SPValue => Boolean = {
        v => true
//        getStateVarInfo.domain match {
//          case DomainList(xs) => {
//            value => {
//              println(s"$value in $xs = " + xs.contains(value))
//              xs.contains(value)
//            }
//          }
//          case DomainRange(r) => _.asInt.map(r.contains) getOrElse false
//          case DomainBool => v => {
//            v.isInstanceOf[BoolPrimitive] || v.asString.map(s => s=="true" || s == "false").getOrElse(false)
//          }
//        }

      }

//      lazy val getStateVarInfo = {
//        val info = th.attributes.get("stateVariable") flatMap {
//          case attr @ MapPrimitive(sv) => {
//            val dom = extractDomain(attr.asSPAttributes)
//            val init = sv.get("init")
//            val goal = sv.get("goal")
//            Some(StateVarInfo(dom, init, goal))
//          }
//          case _ => None
//        }
//        info.getOrElse(StateVarInfo(DomainBool, Some(false), None))
//      }
//
//      def addStateVar(sv: StateVarInfo) = {
//        val domAttr = kevVal(sv.domain)
//        val init = sv.init.map(x => Map("init"-> x)).getOrElse(Map())
//        val goal = sv.goal.map(x => Map("goal"-> x)).getOrElse(Map())
//
//        val stateVar = init ++ goal + domAttr
//        th.copy(attributes = th.attributes + ("stateVariable" -> MapPrimitive(stateVar)))
//      }
//
//
//      private def kevVal(domain: SVDomain): (String, SPValue) = {
//        domain match {
//          case DomainList(xs) => "domain" -> ListPrimitive(xs)
//          case DomainRange(r) => "range" -> MapPrimitive(Map(
//            "start" -> r.start,
//            "end" -> r.end,
//            "step" -> r.step
//          ))
//          case DomainBool => "boolean" -> true
//        }
//      }
//
//      private def extractDomain(attr: SPAttributes): SVDomain = {
//        val dom = attr.getAsList("domain") map(DomainList.apply)
//        val start = attr.getAttribute(List("range", "start")) flatMap(_.asInt)
//        val end = attr.getAttribute(List("range", "end")) flatMap(_.asInt)
//        val step = attr.getAttribute(List("range", "step")) flatMap(_.asInt)
//        val range = for {
//          i <- start
//          e <- end
//          s <- step
//        } yield new DomainRange(Range(i, e, s))
//
//        dom getOrElse (range getOrElse DomainBool)
//      }


    }

}
