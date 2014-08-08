package sp.domain

case class EnabledStates(pre: States, post: States = MapStates(Map()))
case class EnabledStatesMap(map: Map[Operation, EnabledStates])

case class RelationMap(relations: Map[OperationPair, SOP], enabledStates: EnabledStatesMap)
case class OperationPair(o1: ID, o2: ID) {
  val set = Set(o1,o2)
  override def equals(obj: Any) = obj match {
    case x: Set[_] => x == set
    case x: OperationPair => x.set == set
    case (o1, o2) => Set(o1,o2) == set
    case _ => false
  }

  override def hashCode(): Int = set.hashCode()
}
