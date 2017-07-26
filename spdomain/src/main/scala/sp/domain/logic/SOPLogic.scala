package sp.domain.logic

import sp.domain._



object SOPLogic extends SOPLogics

trait SOPLogics {

  implicit def operationToSOP(o: Operation): SOP = OperationNode(o.id)
  implicit def operationIDToSOP(o: ID): SOP = OperationNode(o)

  implicit class sopLogic(sop: SOP) {
    def modifySOP(children: List[SOP]): SOP = {
      sop match {
        case s: OperationNode => s.copy(sop = children)
        case s: Parallel => s.copy(sop = children)
        case s: Other => s.copy(sop = children)
        case s: Alternative => s.copy(sop = children)
        case s: Arbitrary => s.copy(sop = children)
        case s: Sequence => s.copy(sop = children)
        case s: SometimeSequence => s.copy(sop = children)
        case EmptySOP => EmptySOP
      }
    }

    def isEmpty = {sop == EmptySOP || (sop.sop.isEmpty && !sop.isInstanceOf[OperationNode])}

    def addChildren(children: Seq[SOP]): SOP = sop.modifySOP(sop.sop ++ children)

  }



    /**
      * this method extract all guards that the sop specify
      * @param group What group the generated condition belong to. Usually the SOPSpec name
      * @param getAllConditions Set true if "redundant" conditions should be extracted. (o1->o2->o3 will set o1f && o2f on o3)
      * @return a map between a operation id and a condition that the SOP specify.
      */
    def extractOperationCondition(sop: SOP, group: String, getAllConditions: Boolean = false): Map[ID, Condition] = {
      val props = findOpProps(sop, Map(), getAllConditions)
      props map{ case (id, props) =>
        val propList = if (props.size == 1) props.head else AND(props.toList)
        id -> Condition(propList, List(), SPAttributes("group" -> group, "kind" -> "precondition") )
      }
    }




    def extractOperationConditions(sops: List[SOP], group: String, getAllConditions: Boolean = false): Map[ID, Condition] = {
      extractOperationCondition(Parallel(sops),group, getAllConditions)
    }

    /**
      * If you end up with a List[Map[ID, Condition]], then this will merge them
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
      case x: OperationNode => Set(x.operation)
      case x: SOP if x.isEmpty => Set()
      case x: Sequence => seqEval(x.sop).flatMap(s => getOps(s, seqEval)) toSet
      case x: SometimeSequence => seqEval(x.sop).flatMap(s => getOps(s, seqEval)) toSet
      case x: SOP => x.sop.flatMap(s => getOps(s, seqEval)) toSet
    }
  }


    def getCompleteProposition(sop: SOP): Proposition = {
      sop match {
        case x: OperationNode => EQ(x.operation, "f")
        case x: SOP if x.isEmpty => AlwaysTrue
        case x: Sequence => getCompleteProposition(x.sop.last)
        case x: SometimeSequence => getCompleteProposition(x.sop.last)
        case x: Alternative => OR(x.sop map getCompleteProposition toList)
        case x: SOP => AND(x.sop map getCompleteProposition toList)
      }
    }

    def getStartProposition(sop: SOP): Proposition = {
      sop match {
        case x: OperationNode => EQ(x.operation, "i")
        case x: SOP if x.isEmpty => AlwaysTrue
        case x: Sequence => getStartProposition(x.sop.head)
        case x: SometimeSequence => getStartProposition(x.sop.head)
        case x: SOP => AND(x.sop map getStartProposition toList)
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
        case x: OperationNode => map // impl Hierarchy here later
        case x: SOP => {
          val childProps = x.sop.foldLeft(map) { (map, child) =>
            val props = findOpProps(child, map)
            updateMap(props, map)
          }
          x match {
            case alt: Alternative => {
              val startProps = alt.sop.map(c => c -> getStartProposition(c))
              val propsToAdd = startProps map { case (sop, prop) =>
                val otherProps = startProps.filter((kv) => kv._1 != sop) map (_._2)
                if (otherProps.size == 1) sop -> otherProps.head
                else sop -> AND(otherProps toList)
              }
              val newProps = propsToAdd.map { case (sop, prop) => addPropToOps(sop, prop, addToAll)}
              newProps.foldLeft(childProps) { case (oldMap, newMap) => updateMap(newMap, oldMap)}
            }
            case arbi: Arbitrary => {
              val startProps = arbi.sop.map(c => c -> getStartProposition(c))
              val complProps = arbi.sop.map(c => c -> getCompleteProposition(c)) toMap


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
              val res = req(AlwaysTrue, seq.sop.toList, Map())
              updateMap(res, childProps)
            }


            case _ => childProps
          }
        }
      }
    }


    /**
      * This method takes a sop and extract all relations defined by that SOP
      * @param sops
      */
    def extractRelations(sops: List[SOP]): Map[Set[ID], SOP] = {
      val result: Map[Set[ID], SOP] = sops match {
        case Nil => Map()
        case EmptySOP :: Nil => Map()
        case x :: xs => {
          val reqChildren = x.sop map extractOps toList
          val relMap = foldThem(x, reqChildren)
          val chMap = extractRelations(x.sop.toList)
          val rest = extractRelations(xs)
          relMap ++ chMap ++ rest
        }
      }
      result.filter(kv => !kv._2.isInstanceOf[Parallel])
    }

    def extractOps(sop: SOP): List[ID] = {
      def extr(xs: Seq[SOP]): List[ID] = xs flatMap extractOps toList

      sop match {
        case x: OperationNode => x.operation :: extr(x.sop)
        case x: SOP => extr(x.sop)
      }
    }

    def foldThem(parent: SOP, children: List[List[ID]]):Map[Set[ID], SOP] = {
      children match {
        case Nil => Map()
        case x :: Nil => Map()
        case x :: xs => {
          val map = for {
            head <- x
            other <- xs.flatten
          } yield Set(head, other)-> parent.modifySOP(List(head, other))
          map.toMap ++ foldThem(parent, xs)
        }
      }
    }




    def addMissingRelations(sops: List[SOP], relations: Map[Set[ID], SOP]): List[SOP] = {
      val sopRels = extractRelations(sops)
      val ops = sops flatMap getAllOperations
      val missing = (for {
        o1 <- ops
        o2 <- ops if o1 != o2 && !sopRels.contains(Set(o1, o2))
        rel <- relations.get(Set(o1, o2))
      } yield Set(o1, o2) -> rel).toMap
      val cond = makeProps(missing, relations)

      val otherOps = relations.keys flatMap(_ map(id => id)) filter(id => !ops.contains(id)) toSet
      val missingOthers = (for {
        o1 <- ops
        o2 <- otherOps
        rel <- relations.get(Set(o1, o2))
      } yield Set(o1, o2) -> rel).toMap
      val otherCond = makeProps(missingOthers, relations)

      val conds = makeConds(cond, otherCond)

      //println(s"conds: $cond")

      sops map(updateSOP(_, conds))
    }

    def makeProps(missing: Map[Set[ID], SOP], relations: Map[Set[ID], SOP]): Map[ID, Proposition] = {
      val res = missing.toList flatMap {
        case (_, s: Parallel) => List()
        case (_, s: Other) => List()
        case (_, s: Alternative) => {
          val temp = relOrder(s).map{case (id1, id2) => List(id1 -> EQ(id2, "i"), id2 -> EQ(id1, "i"))}
          temp.getOrElse(List())
        }
        case (_, s: Arbitrary) => {
          val temp = relOrder(s).map{case (id1, id2) => List(id1 -> NEQ(id2, "e"), id2 -> NEQ(id1, "e"))}
          temp.getOrElse(List())
        }
        case (_, s: Sequence) => {
          val temp = relOrder(s).map{case (id1, id2) => List(id2 -> EQ(id1, "f"))}
          temp.getOrElse(List())
        }
        case (_, s: SometimeSequence) => {
          val temp = relOrder(s).map{case (id1, id2) => List(id2 -> OR(List(EQ(id1, "i"), EQ(id1, "f"))))}
          temp.getOrElse(List())
        }
        case (_, _) => List()
      }

      val temp = res.foldLeft(Map[ID, AND]()){case (aggr, (id, prop)) =>
        if (!aggr.contains(id)) aggr + (id -> AND(List(prop)))
        else aggr + (id -> AND(prop :: aggr(id).props))
      }

      val filteredMap = temp.map{case (id, and) =>
        val seqs = and.props.flatMap{
          case EQ(SVIDEval(op), ValueHolder(st)) if op == id && st.asOpt[String].contains("f") => Some(id)
          case _ => None
        }

        val removeIds = for {
          id1 <- seqs
          id2 <- seqs
          rels <- relations.get(Set(id1, id2)) if rels == Sequence(List(id1, id2))
        } yield {
          //println(s"remove $id1")
          id1
        }


        val filteredProps = and.props.filterNot{
          case EQ(SVIDEval(op), _) if op == id => removeIds.contains(id)
          case _ => false
        }
        id -> AND(filteredProps)
      }

      //println(s"temp: $temp" )
      //println(s"Filtered: $filteredMap" )

      filteredMap

    }


    def makeConds(c1: Map[ID, Proposition], c2: Map[ID, Proposition]): Map[ID, List[Condition]] = {
      val inC1AndBoth = c1 map{ case (id, prop) =>
        val cond1 = Condition(prop, List(), SPAttributes(
          "kind" -> "precondition",
          "group" -> "sop"
        ))
        id -> {
          if (c2.contains(id)){
            List(cond1, Condition(c2(id), List(), SPAttributes(
              "kind" -> "precondition",
              "group" -> "other"
            )))
          } else List(cond1)
        }
      }
      val onlyInC2 = c2.keySet.diff(c1.keySet)
      onlyInC2.foldLeft(inC1AndBoth){case (acc, id) =>
        acc + (id -> List(Condition(c2(id), List(), SPAttributes(
          "kind" -> "precondition",
          "group" -> "other"
        ))))
      }
    }

    def updateSOP(sop: SOP, conds: Map[ID, List[Condition]]): SOP = {
      val updCh = sop.sop.map(updateSOP(_, conds))
      val updSOP = if (updCh == sop.sop) sop else sop.modifySOP(updCh)
      updSOP match {
        case h: OperationNode => {
          if (conds.contains(h.operation)){
            OperationNode(h.operation, conds(h.operation), updCh)
          } else updSOP
        }
        case _ => updSOP
      }
    }

    def relOrder(sop: SOP): Option[(ID, ID)] = {
      sop.sop.toList match {
        case (h1: OperationNode) :: (h2: OperationNode) :: Nil => Some(h1.operation, h2.operation)
        case _ => None
      }
    }

  }
