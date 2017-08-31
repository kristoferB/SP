package sp.domain.logic

/**
 * Created by kristofer on 30/09/14.
 */
object IDAbleLogic {

  import sp.domain._
  import sp.domain.logic.SOPLogic._



  def removeID(id: Set[ID], ids: List[IDAble]) = {
    def removeFrom(item: IDAble): Option[IDAble] = {
      val newItem = item match {
        case x: Operation => removeIDFromOperation(id, x)
        case x: Thing => removeIDFromThing(id, x)
        case x: SPSpec => removeIDFromSPSpec(id, x)
        case x: SOPSpec => removeIDFromSOPSpec(id, x)
        case x:Struct => removeIDFromStruct(id, x)
        case _ => item
      }
      if (newItem == item) None
      else {
        Some(newItem)
      }
    }

    val t = ids flatMap removeFrom
    println(s"removeID: $t")
    t
  }




  def removeIDFromThing(id: Set[ID], th: Thing): Thing = {
    val newAttr = removeIDFromAttribute(id, th.attributes)
    if (newAttr == th.attributes)
      th
    else
      th.copy(attributes = newAttr)
  }

  def removeIDFromOperation(id: Set[ID], op: Operation): Operation = {
    val newAttr = removeIDFromAttribute(id, op.attributes)
    val removeIt = removeIDFromCondition(id, (_: Condition))
    val newCond = op.conditions map removeIt
    if (newAttr == op.attributes && newCond == op.conditions)
      op
    else
      op.copy(conditions = newCond, attributes = newAttr)
  }

  def removeIDFromSPSpec(id: Set[ID], obj: SPSpec): SPSpec = {
    val newAttr = removeIDFromAttribute(id, obj.attributes)
    if (newAttr == obj.attributes)
      obj
    else
      obj.copy(attributes = newAttr)
  }

  def removeIDFromSOPSpec(id: Set[ID], obj: SOPSpec): SOPSpec = {
    val newAttr = removeIDFromAttribute(id, obj.attributes)
    val newSOP = obj.sop flatMap(sop => removeIDFromSOP(id, sop))
    if (newAttr == obj.attributes && newSOP == obj.sop)
      obj
    else
      obj.copy(sop = newSOP, attributes = newAttr)
  }

  def removeIDFromStruct(id: Set[ID], obj: Struct): Struct = {
    val newAttr = removeIDFromAttribute(id, obj.attributes)
    val newCh = obj.items.flatMap((c => removeIDFromStructNode(id, c)))
    if (newAttr == obj.attributes && newCh == obj.items)
      obj
    else
      obj.copy(items = newCh, attributes = newAttr)
  }

  def removeIDFromStructNode(ids: Set[ID], obj: StructNode): Option[StructNode] = {
    val p = obj.parent.flatMap(x => if (ids.contains(x)) None else Some(x))
    val newAttr = removeIDFromAttribute(ids, obj.attributes)

    if (ids.contains(obj.item)) None else Some(obj.copy(parent = p, attributes = newAttr))
  }

  def removeIDFromSOP(id: Set[ID], sop: SOP): Option[SOP] = {
    def filter(xs: List[SOP]) = {
      println(s"id: $id, \n filter: $xs")
      val t = xs flatMap(x => removeIDFromSOP(id, x))
      println(s"res: $t")
      if (t == xs) xs else t
    }

    val t = sop match {
      case h: OperationNode => {
        if (id.contains(h.operation)) None
        else {
          val newChildren = filter(h.sop)
          if (newChildren == sop.sop) Some(h)
          else Some(h.copy(sop = newChildren))
        }
      }
      case EmptySOP => Some(EmptySOP)
      case _ => {
        val newChildren = filter(sop.sop)
        if (newChildren == sop.sop)
          Some(sop)
        else if (newChildren.isEmpty)
          None
        else if (newChildren.size == 1)
          Some(newChildren.head)
        else
          Some(sop.modifySOP(newChildren))
      }
    }

//    println(s"sop before: $sop")
//    println(s"sop after: $t")

    t
  }


  def removeIDFromCondition(id: Set[ID], cond: Condition): Condition = {
    cond match {
      case c @ Condition(guard, action, attr) => {
        val newGuard = removeIDFromProposition(id, guard)
        val newActions = removeIDFromAction(id, action)
        val newAttr = removeIDFromAttribute(id, attr)
        Condition(newGuard, newActions, newAttr)
      }
      case _ => cond
    }
  }

  def removeIDFromProposition(id: Set[ID], prop: Proposition): Proposition = {
    def clean(xs: List[Proposition], make: List[Proposition] => Proposition) = xs match {
      case Nil => None
      case _ => Some(make(xs))
    }
    def removeIDFromStateEvaluator(sv: StateEvaluator): Option[StateEvaluator] = {
      sv match {
        case SVIDEval(svid) => if (id.contains(svid)) None else Some(sv)
        case ValueHolder(v) => {
          removeIDFromAttrValue(id.map(_.toString), v) map ValueHolder
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


  def removeIDFromAction(id: Set[ID], actions: List[Action]): List[Action] = {
    actions flatMap {
      case a @ Action(svid, ValueHolder(v)) => {
        val newV = removeIDFromAttrValue(id.map(_.toString), v)
        if (id.contains(svid) || newV == None) None
        else Some(Action(svid, ValueHolder(newV.get)))
      }
      case a @ Action(svid, ASSIGN(assignID)) => {
        if (id.contains(svid) || id.contains(assignID)) None
        else Some(a)
      }
      case a @ Action(svid, _) => {
        if (id.contains(svid)) None
        else Some(a)
      }
    }
  }

  // TODO: Update this to play json transformers
  import play.api.libs.json._
  def removeIDFromAttribute(id: Set[ID], attr: SPAttributes): SPAttributes = {
    val idSet = id.map(_.toString())
    val updated = reqRemoveFromAttr(idSet, attr.value.toMap)
    if (updated == attr.value.toMap) attr else JsObject(updated)
  }
  def removeIDFromAttrValue(ids: Set[String], attrVal: SPValue): Option[SPValue] = {
    attrVal match {
      case x: JsString => if (ids.contains(x.value)) None else Some(attrVal)
      case x: JsObject => {
        val upd = reqRemoveFromAttr(ids, x.value.toMap)
        Some(JsObject(upd))
      }
      case JsArray(xs) => {
        val upd  = xs flatMap ((v => removeIDFromAttrValue(ids, v)))
        Some(JsArray(upd))
      }
      case _ => Some(attrVal)
    }
  }
  private def reqRemoveFromAttr(id: Set[String], keyVal: Map[String, JsValue]): Map[String, JsValue] = {
    keyVal.flatMap { case (key, attr) =>
      removeIDFromAttrValue(id, attr).map(x => key -> x)
    }
  }

}
