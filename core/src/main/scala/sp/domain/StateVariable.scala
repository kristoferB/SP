package sp.domain

case class StateVariable(name: String, attributes: SPAttributes = SPAttributes(Map()), id: ID = ID.newID)

case object StateVariable {

  def apply(o: Operation): StateVariable = operationVariable(o)
  def apply(t: Thing): StateVariable = thingVariable(t)

  //val operationDomain = ListPrimitive(List(StringPrimitive("i"), StringPrimitive("e"), StringPrimitive("f")))
  private val attr = SPAttributes(Map())
  implicit def operationVariable(o: Operation) = StateVariable(o.name, attr, o.id)

  private val attrT = SPAttributes(Map("boolean"-> true))
  implicit def thingVariable(t: Thing) = StateVariable(t.name, attrT, t.id)
}


//trait StateVariable {
//  val name: String
//  val attributes: SPAttributes
//
//}
//
//case class IntVariable(name: String, attributes: SPAttributes = SPAttributes(Map())) extends StateVariable
//case class RestrictedIntRangeVariable(name: String, domain: Range, attributes: SPAttributes = SPAttributes(Map())) extends StateVariable
//case class RestrictedIntVariable(name: String, domain: Set[Int], attributes: SPAttributes = SPAttributes(Map())) extends StateVariable
//case class StringVariable(name: String, attributes: SPAttributes = SPAttributes(Map())) extends StateVariable
//case class RestrictedStringVariable(name: String, domain: Set[String], attributes: SPAttributes = SPAttributes(Map())) extends StateVariable
//case class BooleanVariable(name: String, attributes: SPAttributes = SPAttributes(Map())) extends StateVariable


//
//trait StateVariable {
//  def valueInDomain(v: Any): Boolean
//  val name: String
//  val attributes: SPAttributes
//
//  def assign(v: Any): Option[(StateVariable, Any)] = if (valueInDomain(v)) Some((this, v)) else None
//  def <--(v:Any) = assign(v)
//
//  override def equals(obj: Any)={
//    if (obj.isInstanceOf[StateVariable]){
//      obj.asInstanceOf[StateVariable].name == name
//    } else false
//  }
//
//  override def hashCode = name.hashCode
//}
//
//
//case class IntVariable(name: String, attributes: SPAttributes = SPAttributes(Map())) extends StateVariable {
//  def valueInDomain(v: Any): Boolean = v.isInstanceOf[Int]
//}
//case class RestrictedIntRangeVariable(name: String, domain: Range, attributes: SPAttributes = SPAttributes(Map())) extends StateVariable {
//  def valueInDomain(v: Any): Boolean = v match {
//    case i: Int => domain contains i
//    case _ => false
//  }
//}
//case class RestrictedIntVariable(name: String, domain: Set[Int], attributes: SPAttributes = SPAttributes(Map())) extends StateVariable {
//  def valueInDomain(v: Any): Boolean = v match {
//    case i: Int => domain contains i
//    case _ => false
//  }
//}
//case class StringVariable(name: String, attributes: SPAttributes = SPAttributes(Map())) extends StateVariable {
//  def valueInDomain(v: Any): Boolean = v.isInstanceOf[String]
//}
//case class RestrictedStringVariable(name: String, domain: Set[String], attributes: SPAttributes = SPAttributes(Map())) extends StateVariable {
//  def valueInDomain(v: Any): Boolean = v match {
//    case s: String => domain contains s
//    case _ => false
//  }
//}
//case class BooleanVariable(name: String, attributes: SPAttributes = SPAttributes(Map())) extends StateVariable {
//  def valueInDomain(v: Any): Boolean = v.isInstanceOf[Boolean]
//}
//




