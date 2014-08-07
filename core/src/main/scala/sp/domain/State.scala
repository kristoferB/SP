package sp.domain


trait State {
  def apply(id: ID): SPAttributeValue
  def next(idValueMap: (ID, SPAttributeValue)): State
  def next(idValueMap: Map[ID, SPAttributeValue]): State
  def get(id: ID): Option[SPAttributeValue]
}

object State {
  def apply(state: Map[ID, SPAttributeValue]) = MapState(state)
}

case class MapState(state: Map[ID, SPAttributeValue]) extends State {
  def apply(id: ID): SPAttributeValue = state(id)
  def get(id: ID): Option[SPAttributeValue] = state.get(id)
  def next(idValueMap: (ID, SPAttributeValue)) = MapState(state + idValueMap)
  def next(idValueMap:  Map[ID, SPAttributeValue]) = MapState(state ++ idValueMap)
}

//TODO: Add logic somewere else, or when needed
//trait State {
//  val stateMap: Map[StateVariable, Any]
//  private lazy val nameValueMap: Map[String, Any] = stateMap map {case (k,v)=>(k.name, v)}
//  private lazy val nameVarMap: Map[String, StateVariable] = stateMap map {case (k,v)=>(k.name, k)}
//
//  def evaluate(map: Map[StateVariable, Any]) = {
//    map.foldLeft(true)({case (res,(sv,v))=> {
//      res && stateMap(sv) == v
//    }})
//  }
//
//  def evaluateWithString(map: Map[String, Any]) = {
//    map.foldLeft(true)({case (res,(name,v))=> {
//      val sv = nameVarMap(name)
//      res && stateMap(sv) == v
//    }})
//  }
//
//  def update(updateStates: Map[StateVariable, Any]): State = {
//    State(stateMap map {case (k,v)=>(k, updateStates.getOrElse(k,v))})
//  }
//
//  def updateWithStrings(updateStates: Map[String, Any]): State = {
//    val newMap = for {kv <- updateStates; sv <- nameVarMap.get(kv._1)} yield (sv, kv._2)
//    if (newMap.size == updateStates.size) update(newMap) else State.empty
//  }
//
//  def v(sv: StateVariable): Any = stateMap(sv)
//  def v(sv: String): Any = nameValueMap(sv)
//  def get(sv: StateVariable) = v(sv)
//
//  def ++(otherState: State): State = State((stateMap ++ otherState.stateMap))
//  def +(kv : (StateVariable, Any)) = {
//    if (stateMap.contains(kv._1)) update(Map(kv))
//    else State(stateMap + kv)
//  }
//
//  override def equals(obj: Any): Boolean = {
//    obj match {
//      case s: State => s.stateMap == this.stateMap
//      case _ => false
//    }
//  }
//  override def hashCode = this.stateMap.hashCode
//}
//
//object State {
//  def apply(map: Map[StateVariable, Any]) = {
//    if (notInDomain(map).isEmpty){
//      new State {
//        override val stateMap = map
//      }
//    } else State.empty
//  }
//
//  def notInDomain(map: Map[StateVariable, Any]) = {
//    map filter{case (k,v)=> !k.valueInDomain(v)}
//  }
//
//
//  def empty = new State{val stateMap = Map[StateVariable, Any]()}
//}



