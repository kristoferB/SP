package sp.domain.logic

/**
 * Created by kristofer on 30/09/14.
 */
object IDAbleLogic {

  import sp.domain._
  import sp.system.messages._

  def removeID(id: ID, ids: List[IDAble]) = {
    def removeFrom(item: IDAble): Option[UpdateID] = {
      val newItem = item match {
        case x: Operation => removeIDFromOperation(id, x)
        case x: Thing => removeIDFromThing(id, x)
        case x: SPObject => removeIDFromSPObject(id, x)
        case x: SPSpec => removeIDFromSPSpec(id, x)
        case x: SOPSpec => removeIDFromSOPSpec(id, x)
        case _ => item
      }
      if (newItem == item) None
      else {
        Some(UpdateID(item.id, item.version, newItem))
      }
    }

    ids flatMap removeFrom
  }




  def removeIDFromThing(id: ID, th: Thing): Thing = {
    val newAttr = removeIDFromAttribute(id, th.attributes)
    val newSV = th.stateVariables flatMap(sv => removeIDFromStateVariable(id, sv))
    if (newAttr == th.attributes && newSV == th.stateVariables)
      th
    else
      th.copy(stateVariables = newSV, attributes = newAttr)
  }

  def removeIDFromOperation(id: ID, op: Operation): Operation = {
    val newAttr = removeIDFromAttribute(id, op.attributes)
    val removeIt = removeIDFromCondition(id, (_: Condition))
    val newCond = op.conditions map removeIt
    if (newAttr == op.attributes && newCond == op.conditions)
      op
    else
      op.copy(conditions = newCond, attributes = newAttr)
  }

  def removeIDFromSPObject(id: ID, obj: SPObject): SPObject = {
    val newAttr = removeIDFromAttribute(id, obj.attributes)
    if (newAttr == obj.attributes)
      obj
    else
      obj.copy(attributes = newAttr)
  }
  def removeIDFromSPSpec(id: ID, obj: SPSpec): SPSpec = {
    val newAttr = removeIDFromAttribute(id, obj.attributes)
    if (newAttr == obj.attributes)
      obj
    else
      obj.copy(attributes = newAttr)
  }

  def removeIDFromSOPSpec(id: ID, obj: SOPSpec): SOPSpec = {
    val newAttr = removeIDFromAttribute(id, obj.attributes)
    val newSOP = obj.sop flatMap(sop => removeIDFromSOP(id, sop))

    if (newAttr == obj.attributes && newSOP == obj.sop)
      obj
    else
      obj.copy(sop = newSOP, attributes = newAttr)
  }

  def removeIDFromSOP(id: ID, sop: SOP): Option[SOP] = {
    def filter(xs: Seq[SOP]) = {
      val t = xs flatMap(x => removeIDFromSOP(id, x))
      if (t == xs) xs else t
    }

    sop match {
      case Hierarchy(opid, x) => {
        if (opid == id) None
        else {
          removeIDFromSOP(id, x) match {
            case Some(s) => if (s == x) Some(sop) else Some(Hierarchy(id, s))
            case None => Some(Hierarchy(id, EmptySOP))
          }
        }
      }
      case EmptySOP => Some(EmptySOP)
      case _ => {
        val newChildren = filter(sop.children)
        if (newChildren == sop.children)
          Some(sop)
        else if (newChildren.isEmpty)
          None
        else if (newChildren.size == 1)
          Some(newChildren.head)
        else
          Some(sop.modify(newChildren))
      }
    }
  }

  def removeIDFromStateVariable(id: ID, sv: StateVariable): Option[StateVariable] = {
    if (sv.id == id) None
    else {
      val newAttr = removeIDFromAttribute(id, sv.attributes)
      Some(sv.copy(attributes = newAttr))
    }
  }

  def removeIDFromCondition(id: ID, cond: Condition): Condition = {
    cond match {
      case c @ PropositionCondition(guard, action, attr) => {
        val newGuard = removeIDFromProposition(id, guard)
        val newActions = removeIDFromAction(id, action)
        val newAttr = removeIDFromAttribute(id, attr)
        PropositionCondition(newGuard, newActions, newAttr)
      }
      case _ => cond
    }
  }

  def removeIDFromProposition(id: ID, prop: Proposition): Proposition = {
    def clean(xs: List[Proposition], make: List[Proposition] => Proposition) = xs match {
      case Nil => None
      case _ => Some(make(xs))
    }
    def removeIDFromStateEvaluator(sv: StateEvaluator): Option[StateEvaluator] = {
      sv match {
        case SVIDEval(svid) => if (id == svid) None else Some(sv)
        case ValueHolder(v) => {
          removeIDFromAttrValue(id, v) map ValueHolder
        }
        case _ => Some(sv)
      }
    }
    def req(prop: Proposition): Option[Proposition] = {
      prop match {
        case AND(xs) => clean(xs flatMap req, AND.apply)
        case OR(xs) => clean(xs flatMap req, OR.apply)
        case NOT(x) => clean(List(x) flatMap req, AND.apply) // AND will never be used
        case EQ(left, right) => {
          for {
            l <- removeIDFromStateEvaluator(left)
            r <- removeIDFromStateEvaluator(right)
          } yield EQ(l, r)
        }
        case NEQ(left, right) => {
          for {
            l <- removeIDFromStateEvaluator(left)
            r <- removeIDFromStateEvaluator(right)
          } yield NEQ(l, r)
        }
        case _ => Some(prop)
      }

    }

    req(prop) match {
      case Some(x) => x
      case None => AlwaysTrue
    }
  }


  def removeIDFromAction(id: ID, actions: List[Action]): List[Action] = {
    actions flatMap {
      case a @ Action(svid, ValueHolder(v)) => {
        val newV = removeIDFromAttrValue(id, v)
        if (svid == id || newV == None) None
        else Some(Action(svid, ValueHolder(newV.get)))
      }
      case a @ Action(svid, ASSIGN(assignID)) => {
        if (svid == id || assignID == id) None
        else Some(a)
      }
      case a @ Action(svid, _) => {
        if (svid == id) None
        else Some(a)
      }
    }
  }

  def removeIDFromAttribute(id: ID, attr: SPAttributes): SPAttributes = {
    val updated = reqRemoveFromAttr(id, attr.attrs)
    if (updated == attr.attrs) attr else SPAttributes(updated)
  }
  def removeIDFromAttrValue(id: ID, attrVal: SPAttributeValue): Option[SPAttributeValue] = {
    attrVal match {
      case IDPrimitive(x) => if (x == id) None else Some(attrVal)
      case MapPrimitive(x) => {
        val upd = reqRemoveFromAttr(id, x)
        Some(MapPrimitive(upd))
      }
      case ListPrimitive(xs) => {
        val upd  = xs flatMap ((v => removeIDFromAttrValue(id, v)))
        Some(ListPrimitive(upd))
      }
      case OptionAsPrimitive(x) => x flatMap (v => removeIDFromAttrValue(id, v))
      case _ => Some(attrVal)
    }
  }
  private def reqRemoveFromAttr(id: ID, keyVal: Map[String, SPAttributeValue]): Map[String, SPAttributeValue] = {
    val markID = keyVal map { case (key, attr) =>
      val newAttr = removeIDFromAttrValue(id, attr)
      newAttr match {
        case Some(x) => key -> x
        case None => "remove!!!!" -> StringPrimitive("")
      }
    }
    markID - "remove!!!!"
  }

}
