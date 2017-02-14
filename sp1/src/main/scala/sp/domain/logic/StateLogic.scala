package sp.domain.logic

import sp.domain._

/**
 * Created by kristofer on 14/11/14.
 */


// Handled by attribute + "state" -> state
// and attribute.getAs[State]("state")


object StateLogic extends StateLogics

trait StateLogics {
  implicit class extState(s: State) {
    def apply(id: ID): SPValue = s.state(id)
    def get(id: ID): Option[SPValue] = s.state.get(id)
    def next(idValueMap: (ID, SPValue)) = State(s.state + idValueMap)
    def next(idValueMap:  Map[ID, SPValue]) = State(s.state ++ idValueMap)
  }
}
//
//  implicit class getStateAttr(attr: SPAttributes) {
//    def getStateAttr(key: String): Option[State] = {
//      attr.getAsList(key) map( li =>
//        State((li flatMap {
//          case MapPrimitive(keyValues) => {
//            val id = keyValues.get("id") flatMap(_.asID)
//            val value = keyValues.get("value")
//            for {
//              theID <- id
//              theValue <- value
//            } yield theID -> theValue
//          }
//          case _ => None
//        }).toMap)
//        )
//    }
//    def addStateAttr(key: String, state: State): SPAttributes = {
//      val map = convertStateToListPrimitive(state)
//      attr + (key, map)
//    }
//  }
//
//  def convertStateToListPrimitive(s: State) = {
//    ListPrimitive(s.state.map{case (id, value) =>
//      MapPrimitive(Map("id"->IDPrimitive(id), "value" -> value))
//    } toList)
//  }
//
//}
