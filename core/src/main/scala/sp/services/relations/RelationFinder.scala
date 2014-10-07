package sp.services.relations

import akka.actor._
import sp.domain._

import scala.annotation.tailrec

/**
 * This message starts the identification. Returns the
 * relations identified.
 * TODO: I need to update this later for better performance. Mainly using
 * ints and arrays instead of all the objects.
 *
 * @param ops The ops should have all conditions
 *            that should be used. So add Specs
 *            before
 */
case class FindRelations( ops: List[Operation],
                          stateVars: Map[ID, SPAttributeValue => Boolean],
                          init: State,
                          groups: List[SPAttributeValue] = List(),
                          iterations: Int = 100,
                          goal: State => Boolean = _ => true)


class RelationFinder extends Actor with RelationFinderAlgotithms {
  def receive = {
    case FindRelations(ops, svs, init, groups, itr, goal) => {
      implicit val setup = Setup(ops, svs, groups, init, goal)
      val sm = findWhenOperationsEnabled(itr)
      val res = findOperationRelations(sm)
      sender ! res
    }
  }
}

object RelationFinder {
  def props = Props(classOf[RelationFinder])
}

//TODO: Move these to domain.logic. RelationsLogic
trait RelationFinderAlgotithms {

  /**
   *
   * @param ops The operations that are part of the relation identification. Usually all operations
   * @param stateVars All variables that are used by the operations
   * @param groups The condition groups that should be used. If empty, all groups are used
   * @param init The initial state
   * @param goal The goal function, when the execution has reached the goal.
   */
  case class Setup(ops: List[Operation],
              stateVars: Map[ID, SPAttributeValue => Boolean],
              groups: List[SPAttributeValue],
              init: State,
              goal: State => Boolean)


  case class SeqResult(seq: List[Operation], goalState: State, stateMap: Map[State, IndexedSeq[Operation]])


  /**
   * Finds a random seq of operations from the initial state
   * until a goal is reached or no operations are enabled
   *
   * @param setup
   * @return
   */
  def findASeq(setup: Setup) = {
    //val setup = prepairSetup(setMeUp)
    val ops = setup.ops
    val stateVars = setup.stateVars
    val init = setup.init
    val groups = setup.groups
    val goal = setup.goal

    import sp.domain.logic.OperationLogic._
    implicit val props = EvaluateProp(stateVars, groups.toSet)

    @tailrec
    def req(ops: IndexedSeq[Operation], s: State, seq: IndexedSeq[Operation], stateMap: Map[State, IndexedSeq[Operation]]): SeqResult = {
      val enabled = ops.filter(o => o.eval(s))
      if (enabled.isEmpty || goal(s)){
        SeqResult(seq.reverse toList, s, stateMap)
      }
      else {
        val i = scala.util.Random.nextInt(enabled.size)
        val o = enabled(i)
        val newState = o next s
        val remainingOps = ops.filterNot(_ == o)
        req(remainingOps, newState, o +: seq, stateMap + (s->enabled))
      }
    }

    req(ops.toIndexedSeq, init, IndexedSeq(), Map())
  }

  /**
   * Find when operations are enabled. The more iterations, the better
   * @param iterations No of random sequences that are generated
   * @param opsToTest The operations that are returned in the map
   * @param setup The definition for the algorithm
   * @return
   */
  def findWhenOperationsEnabled(iterations: Int, opsToTest: Set[Operation] = Set())(implicit setup: Setup) = {
    //val setup = prepairSetup(setMeUp)


    @tailrec
    def req(n: Int, esm: EnabledStatesMap): EnabledStatesMap  = {
      if (n <= 0) esm
      else {
        val seqRes = findASeq(setup)
        val updateMap = seqRes.stateMap.foldLeft(esm.map)((m,sm)=> {
          val checkThese = if (!opsToTest.isEmpty) sm._2 filter opsToTest.contains else sm._2
          val updatedOps = checkThese.map(o => o.id -> mergeAState(o, m(o.id), sm._1)).toMap
          m ++ updatedOps
        })
        req(n-1, EnabledStatesMap(updateMap))
      }
    }
    def mergeAState(o: Operation, e: EnabledStates, s: State) = {
      val opstate = s(o.id) // fix three State here
      val newInit = e.pre.add(s) // remove full domain for speed
      e.copy(pre = newInit)
    }


    val emptyStates = States(setup.stateVars map (_._1 -> Set[SPAttributeValue]()))
    val oie = EnabledStates(emptyStates, emptyStates)
    val startMap = {if (opsToTest.isEmpty) setup.ops else opsToTest}.map(_.id -> oie)
    req(iterations, EnabledStatesMap(startMap toMap))
  }


  def findOperationRelations(sm: EnabledStatesMap) = {
    @tailrec
    def req(ops: List[ID],
            map: Map[ID, EnabledStates],
            res: Map[Set[ID], SOP] ): Map[Set[ID], SOP] = {
      ops match {
        case Nil => res
        case o1 :: rest => {
          val o1State = map(o1)
          val update = map.foldLeft(res){case (aggr, (o2, o2State)) =>
            if (o1 != o2 && !aggr.contains(Set[ID](o1, o2))) aggr + (Set[ID](o1, o2) -> matchOps(o1, o1State, o2, o2State))
            else aggr
          }
          req(rest, map, update)
        }
      }
    }

    val rels = req(sm.map.keys toList, sm.map, Map())
    RelationMap(rels, sm)
  }

  //TODO: Fix to match three state as well
  val opi = Set("i")
  val opf = Set("f")
  val opif = Set("i", "f")
  def matchOps(o1: ID, o1State: EnabledStates, o2: ID, o2State: EnabledStates): SOP = {
    val stateOfO2WhenO1Pre = o1State.pre(o2) flatMap (_.asString)
    val stateOfO1WhenO2pre = o2State.pre(o1) flatMap (_.asString)
    val pre = (stateOfO2WhenO1Pre, stateOfO1WhenO2pre)

    if (pre ==(opi, opi)) Alternative(o1, o2)
    else if (pre ==(opi, opf)) Sequence(o1, o2)
    else if (pre ==(opf, opi)) Sequence(o2, o1)
    else if (pre ==(opif, opf)) SometimeSequence(o2, o1)
    else if (pre ==(opi, opif)) SometimeSequence(o1, o2)
    else if (pre ==(opif, opif)) Parallel(o1, o2)
    else Other(o1, o2)
  }


  /**
   * Adds opertion statevariables to the state and the varMap
   * @param setup a setup from the user
   * @return an updated setup object
   */
  def prepairSetup(setup: Setup) = {
    if (setup.ops.isEmpty) setup
    else {
      import sp.domain.logic.OperationLogic._
      val opSV  = createOpsStateVars(setup.ops)
      val startState = setup.init.next(setup.ops.map(_.id -> OperationState.init).toMap)
      val upSV = setup.stateVars ++ opSV
      setup.copy(stateVars = upSV, init = startState)
    }

  }

  /**
   * Adds opertion init state ("i") to a state
   * @param ops
   * @param state
   * @return an updated state object
   */
  def addOpsToState(ops: List[Operation], state: State) = {
    state.next(ops.map(_.id -> StringPrimitive("i")).toMap)
  }

  /**
   * Adds opertion statevariables to the stateVarMap
   * @param ops The operations
   * @return a stateVar map
   */
  def createOpsStateVars(ops: List[Operation]) = {
    import sp.domain.logic.OperationLogic._
    val matchFunction = OperationState.domain.contains(_)
    ops.map(o => o.id -> matchFunction).toMap
  }

  
  

}
