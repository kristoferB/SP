package sp.domain

trait SOP {
  val children: Seq[SOP]
  def +(sop: SOP) = SOP.addChildren(this, Seq(sop))
  def ++(sops: Seq[SOP]) = SOP.addChildren(this,sops)
  def modify(sops: Seq[SOP]) = SOP.modifySOP(this,sops)
  def <--(sops: Seq[SOP]) = modify(sops)
  lazy val isEmpty: Boolean = (this == EmptySOP) || children.isEmpty || children.contains(EmptySOP)
}


case object EmptySOP extends SOP {val children  = Seq[SOP]()}
case class Parallel(children: SOP*) extends SOP
case class Alternative(children: SOP*) extends SOP
case class Arbitrary(children: SOP*) extends SOP
case class Sequence(children: SOP*) extends SOP
case class SometimeSequence(children: SOP*) extends SOP
case class Other(children: SOP*) extends SOP
case class Hierarchy(operation: ID, conditions: List[Condition], children: SOP*) extends SOP

object Hierarchy{
  def apply(id: ID): Hierarchy = Hierarchy(id, List())
}


// Also create implicit conversion for operations

object SOP {
  
  def apply(children: SOP*): SOP = {
    if (children isEmpty) EmptySOP
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
      case _ => modifySOP(sop, sop.children ++ children)
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

