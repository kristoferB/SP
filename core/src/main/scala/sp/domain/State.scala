package sp.domain

case class State(state: Map[ID, SPValue])




// TODO: Move to internal relation identification
case class States(states: Map[ID, Set[SPValue]]) {
  def apply(id: ID): Set[SPValue] = states(id)
  def get(id: ID): Option[Set[SPValue]] = states.get(id)
  def add(id: ID, value: SPValue): States = add(id, Set(value))
  def add(id: ID, value: Set[SPValue]): States = {
    val xs = get(id).getOrElse(Set()) ++ value
    States(states + (id -> xs))
  }
  def add(s: State): States = {
    val newMap = s.asInstanceOf[State].state map {case (id, set) =>
      id -> (get(id).getOrElse(Set()) + set)
    }
    States(states ++ newMap)
  }
}

