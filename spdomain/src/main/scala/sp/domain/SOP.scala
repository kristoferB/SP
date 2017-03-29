package sp.domain


sealed trait SOP {
  val sop: List[SOP]
}


case object EmptySOP extends SOP {val sop  = List[SOP]()}
case class Parallel(sop: List[SOP]) extends SOP
case class Alternative(sop: List[SOP]) extends SOP
case class Arbitrary(sop: List[SOP]) extends SOP
case class Sequence(sop: List[SOP]) extends SOP
case class SometimeSequence(sop: List[SOP]) extends SOP
case class Other(sop: List[SOP]) extends SOP
case class Hierarchy(operation: ID, conditions: List[Condition] = List(), sop: List[SOP] = List()) extends SOP

object Hierarchy{
  def apply(id: ID): Hierarchy = Hierarchy(id, List())
}


// Also create implicit conversion for operations

object SOP {
  
  def apply(children: SOP*): SOP = SOP(children.toList)
  def apply(children: List[SOP]): SOP = {
    if (children.isEmpty) EmptySOP
    else if (children.size == 1) children.head
    else Parallel(children)
  }

  def apply(op: Operation) = Hierarchy(op.id)
  def apply(op: Operation, children: SOP) = Hierarchy(op.id, List(), List(children))
  def apply(op: ID) = Hierarchy(op)

}

