package sp.domain

case class EnabledStates(pre: States, post: States = States(Map()))
case class EnabledStatesMap(map: Map[ID, EnabledStates])

case class RelationMap(relations: Map[Set[ID], SOP], enabledStates: EnabledStatesMap) {
  def apply(o1: ID, o2: ID) = relations(Set(o1, o2))
}

