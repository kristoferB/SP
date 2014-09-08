package sp.domain.logic

/**
 * Created by kristofer on 04/09/14.
 */
case object SOPLogic {

  import sp.domain._

  /**
   * Given a sop, this method extract all guards that the sop specify
   * @param sop The specification sop
   * @param group What group the generated condition belong to. Usually the SOPSpec name
   * @param getAllConditions Set true if "redundant" conditions should be extracted. (o1->o2->o3 will set o1f && o2f on o3)
   * @return a map between a operation id and a condition that the SOP specify.
   */
  def extractOperationCondition(sop: SOP, group: String, getAllConditions: Boolean = false): Map[ID, Condition] = {
    val props = findOpProps(sop, Map(), getAllConditions)
    props map{ case (id, props) =>
      val propList = if (props.size == 1) props.head else AND(props.toList)
      id -> PropositionCondition(propList, List(), SPAttributes(Map("group" -> group, "kind"-> "precondition")))
    }
  }

  def extractOperationConditions(sops: List[SOP], group: String, getAllConditions: Boolean = false): Map[ID, Condition] = {
    extractOperationCondition(Parallel(sops:_*),group, getAllConditions)
  }

  /**
   * If you end up with a list of Map[ID, Condition], then this will merge them
   * @param maps
   * @return
   */
  def mergeConditionMaps(maps: List[Map[ID, Condition]]): Map[ID, List[Condition]] = {
    maps.foldLeft(Map[ID, List[Condition]]())((aggr, condMap) => {
      val update = condMap.map{case (id, cond) =>
        val conds = aggr.getOrElse(id, List[Condition]())
        id -> (cond :: conds)
      }
      aggr ++ update
    })
  }


  def getStartOperations(sop: SOP): Set[ID] = {
    getOps(sop,sops => Seq(sops.head))
  }

  def getFinalOperations(sop: SOP): Set[ID] = {
    getOps(sop, sops => Seq(sops.last))
  }

  def getAllOperations(sop: SOP): Set[ID] = {
    getOps(sop, sops => sops)
  }


  private def getOps(sop: SOP, seqEval: Seq[SOP] => Seq[SOP]) : Set[ID] = {
    sop match{
      case x: Hierarchy => Set(x.operation)
      case x: SOP if x.isEmpty => Set()
      case x: Sequence => seqEval(x.children).flatMap(s => getOps(s, seqEval)) toSet
      case x: SometimeSequence => seqEval(x.children).flatMap(s => getOps(s, seqEval)) toSet
      case x: SOP => x.children.flatMap(s => getOps(s, seqEval)) toSet
    }
  }


  def getCompleteProposition(sop: SOP): Proposition = {
    sop match {
      case x: Hierarchy => EQ(x.operation, "f")
      case x: SOP if x.isEmpty => AlwaysTrue
      case x: Sequence => getCompleteProposition(x.children.last)
      case x: SometimeSequence => getCompleteProposition(x.children.last)
      case x: Alternative => OR(x.children map getCompleteProposition toList)
      case x: SOP => AND(x.children map getCompleteProposition toList)
    }
  }

  def getStartProposition(sop: SOP): Proposition = {
    sop match {
      case x: Hierarchy => EQ(x.operation, "i")
      case x: SOP if x.isEmpty => AlwaysTrue
      case x: Sequence => getStartProposition(x.children.head)
      case x: SometimeSequence => getStartProposition(x.children.head)
      case x: SOP => AND(x.children map getStartProposition toList)
    }
  }

  def updateMap(newMap: Map[ID, Set[Proposition]], oldMap: Map[ID, Set[Proposition]]) = {
    val updates = newMap map{case (id, set) =>
      id -> (oldMap.getOrElse(id, Set()) ++ set)
    }
    oldMap ++ updates
  }

  def addPropToStartOps(sop: SOP, prop: Proposition): Map[ID, Set[Proposition]] = {
    getStartOperations(sop) map (_ -> Set(prop)) toMap
  }
  def addPropToOps(sop: SOP, prop: Proposition, toAllOps: Boolean): Map[ID, Set[Proposition]] = {
    val ops = if (toAllOps) getAllOperations(sop) else getStartOperations(sop)
    ops map (_ -> Set(prop)) toMap
  }

  def findOpProps(sop: SOP, map: Map[ID, Set[Proposition]], addToAll: Boolean = false): Map[ID, Set[Proposition]] = {
    sop match {
      case x: SOP if x.isEmpty => map
      case x: Hierarchy => map // impl Hierarchy here later
      case x: SOP => {
        val childProps = x.children.foldLeft(map) { (map, child) =>
          val props = findOpProps(child, map)
          updateMap(props, map)
        }
        x match {
          case alt: Alternative => {
            val startProps = alt.children.map(c => c -> getStartProposition(c))
            val propsToAdd = startProps map { case (sop, prop) =>
              val otherProps = startProps.filter((kv) => kv._1 != sop) map (_._2)
              if (otherProps.size == 1) sop -> otherProps.head
              else sop -> AND(otherProps toList)
            }
            val newProps = propsToAdd.map { case (sop, prop) => addPropToOps(sop, prop, addToAll)}
            newProps.foldLeft(childProps) { case (oldMap, newMap) => updateMap(newMap, oldMap)}
          }
          case arbi: Arbitrary => {
            val startProps = arbi.children.map(c => c -> getStartProposition(c))
            val complProps = arbi.children.map(c => c -> getCompleteProposition(c)) toMap


            val props = startProps.map { case (sop, prop) =>
              sop -> OR(List(prop, complProps(sop)))
            }
            val propsToAdd = props map { case (sop, prop) =>
              val otherProps = props.filter((kv) => kv._1 != sop) map (_._2)
              if (otherProps.size == 1) sop -> otherProps.head
              else sop -> AND(otherProps toList)
            }
            val newProps = propsToAdd.map { case (sop, prop) => addPropToOps(sop, prop, addToAll)}
            newProps.foldLeft(childProps) { case (oldMap, newMap) => updateMap(newMap, oldMap)}

          }
          //TODO: Add sometime in sequence as well, but maybe we should not allow that specification? 140906
          case seq: Sequence => {
            def req(prevProp: Proposition, sops: List[SOP], res: Map[ID, Set[Proposition]]): Map[ID, Set[Proposition]] = {
              sops match {
                case Nil => res
                case x :: xs => {
                  val prop = getCompleteProposition(x)
                  if (prevProp == AlwaysTrue) req(prop, xs, res)
                  else {
                    val update = addPropToOps(x, prevProp, addToAll)
                    val accumulatedProp = if (addToAll) AND(List(prevProp, prop)) else prop
                    req(accumulatedProp, xs, updateMap(update, res))
                  }
                }
              }
            }
            val res = req(AlwaysTrue, seq.children.toList, Map())
            updateMap(res, childProps)
          }


          case _ => childProps
        }
      }
    }
  }

}
