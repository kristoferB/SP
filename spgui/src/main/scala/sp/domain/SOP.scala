package sp.domain

sealed trait SOP {
  val sop: List[SOP]
  val nodeID: ID
}


case class Parallel(sop: List[SOP], nodeID: ID = ID.newID) extends SOP
case class Alternative(sop: List[SOP], nodeID: ID = ID.newID) extends SOP
case class Arbitrary(sop: List[SOP], nodeID: ID = ID.newID) extends SOP
case class Sequence(sop: List[SOP], nodeID: ID = ID.newID) extends SOP
case class SometimeSequence(sop: List[SOP], nodeID: ID = ID.newID) extends SOP
case class Other(sop: List[SOP], nodeID: ID = ID.newID) extends SOP
case class OperationNode(operation: ID, conditions: List[Condition] = List(), sop: List[SOP] = List(), nodeID: ID = ID.newID) extends SOP
case object EmptySOP extends SOP {val sop  = List[SOP](); val nodeID = ID.newID}

object OperationNode{
  def apply(id: ID): OperationNode = OperationNode(id, List())
}


// Also create implicit conversion for operations

object SOP {
  def apply(children: SOP*): SOP = SOP(children.toList)
  def apply(children: List[SOP]): SOP = {
    if (children.isEmpty) EmptySOP
    else if (children.size == 1) children.head
    else Parallel(children)
  }

  def apply(op: Operation) = OperationNode(op.id)
  def apply(op: Operation, children: SOP) = OperationNode(op.id, List(), List(children))
  def apply(op: ID) = OperationNode(op)

}

