package sp.domain

// TODO: Clean this and move logic. Maybe also make sop: List[SOP]

trait SOP {
  val sop: Seq[SOP]
  def +(sop: SOP) = SOP.addChildren(this, Seq(sop))
  def ++(sops: Seq[SOP]) = SOP.addChildren(this,sops)
  def modify(sops: Seq[SOP]) = SOP.modifySOP(this,sops)
  def <--(sops: Seq[SOP]) = modify(sops)
  lazy val isEmpty: Boolean = (this == EmptySOP) || sop.isEmpty || sop.contains(EmptySOP)
}


case object EmptySOP extends SOP {val sop  = Seq[SOP]()}
case class Parallel(sop: SOP*) extends SOP
case class Alternative(sop: SOP*) extends SOP
case class Arbitrary(sop: SOP*) extends SOP
case class Sequence(sop: SOP*) extends SOP
case class SometimeSequence(sop: SOP*) extends SOP
case class Other(sop: SOP*) extends SOP
case class Hierarchy(operation: ID, conditions: List[Condition], sop: SOP*) extends SOP

object Hierarchy{
  def apply(id: ID): Hierarchy = Hierarchy(id, List())
}


// Also create implicit conversion for operations

object SOP {
  
  def apply(children: SOP*): SOP = {
    if (children.isEmpty) EmptySOP
    else if (children.size == 1) children.head
    else Parallel(children:_*) 
  }
  def apply(op: Operation) = Hierarchy(op.id)
  def apply(op: ID) = Hierarchy(op)

//  def createFromOps(children: Seq[Operation]): SOP = {
//    val sops = children map (o => operationToSOP(o))
//    apply(sops:_*)
//  }

  def addChildren(sop: SOP, children: Seq[SOP]): SOP = {
    sop match {
      case Hierarchy(o,conds, child) => Hierarchy(o,conds, addChildren(child, children))
      case _ => modifySOP(sop, sop.sop ++ children)
    }
  }

  def modifySOP(sop: SOP, children: Seq[SOP]): SOP = {
    sop match {
      case s: Hierarchy => Hierarchy(s.operation, s.conditions, children:_*)
      case s: Parallel => Parallel(children:_*)
      case s: Other => Other(children:_*)
      case s: Alternative => Alternative(children:_*)
      case s: Arbitrary => Arbitrary(children:_*)
      case s: Sequence => Sequence(children:_*)
      case s: SometimeSequence => SometimeSequence(children:_*)
      case EmptySOP => apply(children:_*)
      case _ => apply(children:_*)
    }
  }
  
  
  implicit def operationToSOP(o: Operation): SOP = Hierarchy(o.id)
  implicit def operationIDToSOP(o: ID): SOP = Hierarchy(o)

}

