package sp.domain

sealed trait Relation
case object SequenceRel extends Relation
case object SometimeSeqRel extends Relation
case object ParallelRel extends Relation
case object AlternativeRel extends Relation
case object HierarchyRel extends Relation
case object ArbitraryRel extends Relation
//case class OtherRel(source: TransitionStateMap, target: TransitionStateMap) extends Relation
case class UndefinedRel(relations: Set[Relation]) extends Relation

//case class TransitionStateMap(rel: (ConditionType, Set[State])*)
//object TransitionStateMap {
//  val empty = TransitionStateMap(Seq[(ConditionType, Set[State])]():_*)
//}
//
//
//
//case class RelationArc[S,T](source: S, relation: Relation, target: T)
//
//trait RelationMap[S, T] {
//  val relMap: Set[RelationArc[S,T]]
//  def relation(obj1: Any, obj2: Any): Option[RelationArc[S,T]] = mapping get(Set(obj1,obj2))
//  def isEmpty = relMap.isEmpty
//
//  private lazy val mapping = (relMap.map(r => Set(r.source, r.target) -> r)) toMap
//}
//
//case class OpRelationMap(relMap: Set[RelationArc[Operation, Operation]]) extends RelationMap[Operation, Operation]
//case class VarRelationMap(relMap: Set[RelationArc[Operation, StateVariable[_]]]) extends RelationMap[Operation, StateVariable[_]]
//case class SOPRelationMap(relMap: Set[RelationArc[SOP, SOP]]) extends RelationMap[SOP, SOP] {
//  def +(arc: RelationArc[SOP, SOP]): SOPRelationMap = SOPRelationMap(relMap + arc)
//}