package sp.domain.logic

import sp.domain._

/**
 * Created by kristofer on 14/11/14.
 */


// Handled by attribute + "state" -> state
// and attribute.getAs[State]("state")


object StateLogic extends StateLogics

trait StateLogics {
  implicit class extState(s: SPState) {
    def apply(id: ID): SPValue = s.state(id)
    def get(id: ID): Option[SPValue] = s.state.get(id)
    def next(idValueMap: (ID, SPValue)) = s.copy(state = s.state + idValueMap)
    def next(idValueMap:  Map[ID, SPValue]) = s.copy(state = s.state ++ idValueMap)
    def add(idValueMap:  Map[ID, SPValue]) = next(idValueMap)
  }
}

