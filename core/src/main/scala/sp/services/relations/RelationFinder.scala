package sp.services.relations

import akka.actor._
import sp.domain._
import sp.domain.Logic._

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
                          stateVars: Map[ID, SPValue => Boolean],
                          init: State,
                          groups: List[SPValue] = List(),
                          iterations: Int = 100,
                          goal: State => Boolean = _ => true)

case class FindRelationResult(map: Option[RelationMap], deadlocks: Option[NoRelations])

class RelationFinder extends Actor with RelationFinderAlgorithms {
  def receive = {
    case FindRelations(ops, svs, init, groups, itr, goal) => {
      implicit val setup = Setup(ops, svs, groups, init, goal)
      println(s"init: $init")
      println(s"goal: $goal")
      println(s"groups: $groups")
      ops.foreach(o => println(o.name + " " + o.id + " " + o.conditions))
      val sm = findWhenOperationsEnabled(itr)
      val relM = sm._1 map findOperationRelations
      val addedArbis = for {
        en <- sm._1
        rel <- relM
      } yield changeParaToArbi(rel, en)
      sender ! FindRelationResult(addedArbis, sm._2)
    }
  }
}

object RelationFinder {
  def props = Props(classOf[RelationFinder])
}

//TODO: Move these to domain.logic. RelationsLogic
trait RelationFinderAlgorithms {

  /**
   *
   * @param ops The operations that are part of the relation identification. Usually all operations
   * @param stateVars All variables that are used by the operations
   * @param groups The condition groups that should be used. If empty, all groups are used
   * @param init The initial state
   * @param goal The goal function, when the execution has reached the goal.
   */
  case class Setup(ops: List[Operation],
              stateVars: Map[ID, SPValue => Boolean],
              groups: List[SPValue],
              init: State,
              goal: State => Boolean)


  case class SeqResult(seq: List[Operation], goalState: State, stateMap: Map[State, IndexedSeq[Operation]])
  case class NoSeqResult(Seq: List[Operation], finalState: State, opsLeft: IndexedSeq[Operation])


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
    def req(ops: IndexedSeq[Operation],
            s: State, seq: IndexedSeq[Operation],
            stateMap: Map[State, IndexedSeq[Operation]]): Either[NoSeqResult, SeqResult] = {
      val enabled = ops.filter(o => o.eval(s))
      if (ops.isEmpty || goal(s)){
        Right(SeqResult(seq.reverse toList, s, stateMap))
      } else if (enabled.isEmpty)
        Left(NoSeqResult(seq.reverse toList, s, ops))
      else {
        val i = scala.util.Random.nextInt(enabled.size)
        val o = enabled(i)
        val newState = o next s
        val remainingOps = if (props.defs.completed(newState(o.id)))
          ops.filterNot(_ == o)
        else ops
        req(remainingOps, newState, o +: seq, stateMap + (s->enabled))
      }
    }

    req(ops.toIndexedSeq, init, IndexedSeq(), Map())
  }

  def arbitraryFinder(sequence: Seq[Operation], setup: Setup) = {
    val stateVars = setup.stateVars
    val init = setup.init
    val groups = setup.groups

    import sp.domain.logic.OperationLogic._
    
    @tailrec
    def findNotPar(s: State, seq: Seq[Operation]): Set[Operation] = {
    implicit val props = EvaluateProp(stateVars, groups.toSet, TwoStateDefinition)
      if (seq.isEmpty) Set() 
      else if (!seq.head.eval(s)) Set(seq.head)
      else {
        findNotPar(seq.head.next(s), seq.tail)
      }
    }
    
    implicit val props = EvaluateProp(stateVars, groups.toSet, ThreeStateDefinition)
    @tailrec
    def req(s: State, seq: Seq[Operation], res: Set[Set[ID]]): Set[Set[ID]] = {
      if (seq.isEmpty) res
      else {
        val o = seq.head
        if (!o.eval(s)) println("SOMETHING IS WRONG IN OVERLAP FINDER OP "+o+s)
        val newState = o.next(s)
        val parallelTo = findNotPar(newState, seq.tail).map(x=> Set(o.id,x.id))
        req(o.next(newState), seq.tail, res ++ parallelTo)
      }
    }
    req(init, sequence, Set())
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
    def req(n: Int, esm: EnabledStatesMap, deadLocks: NoRelations): (EnabledStatesMap, NoRelations)  = {
      if (n <= 0) (esm, deadLocks)
      else {
        findASeq(setup) match {
          case Left(noSeq) => {
            val idSeq = noSeq.Seq map(_.id)
            val newSeqs = if (deadLocks.sequences.size > 10) deadLocks.sequences else deadLocks.sequences + idSeq
            val newStates = if (deadLocks.states.size > 10) deadLocks.states else deadLocks.states + noSeq.finalState
            req(n-1, esm, NoRelations(newSeqs, newStates, deadLocks.finalState.add(noSeq.finalState)))
          }
          case Right(seqRes) => {
            val updateMap = seqRes.stateMap.foldLeft(esm.map)((m,sm)=> {
              val checkThese = if (!opsToTest.isEmpty) sm._2 filter opsToTest.contains else sm._2
              val updatedOps = checkThese.map(o => o.id -> mergeAState(o, m(o.id), sm._1)).toMap
              m ++ updatedOps
            })
            val arbiMap = arbitraryFinder(seqRes.seq, setup)
            val updatedArbiMap = arbiMap ++ esm.arbiMap
            req(n-1, EnabledStatesMap(updateMap, updatedArbiMap), deadLocks)
          }
        }
      }
    }
    def mergeAState(o: Operation, e: EnabledStates, s: State) = {
      val opstate = s(o.id) // fix three State here
      val newInit = e.pre.add(s) // remove full domain for speed
      e.copy(pre = newInit)
    }


    val emptyStates = States(setup.stateVars map (_._1 -> Set[SPValue]()))
    val oie = EnabledStates(emptyStates, emptyStates)
    val startMap = {if (opsToTest.isEmpty) setup.ops else opsToTest}.map(_.id -> oie)
    val emptyEnabledStates = EnabledStatesMap(startMap toMap)
    val res = req(iterations, emptyEnabledStates, NoRelations(Set(), Set(), emptyStates))
    val enM = if (res._1 == emptyEnabledStates) None else Some(res._1)
    val noRes = if (res._2.sequences.isEmpty) None else Some(res._2)
    (enM, noRes)
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
  // TODO: Temporary disabled sometimeSeq
  val opi = Set("i")
  val opf = Set("f")
  val opif = Set("i", "f")
  def matchOps(o1: ID, o1State: EnabledStates, o2: ID, o2State: EnabledStates): SOP = {
    val stateOfO2WhenO1Pre = o1State.pre(o2) flatMap (_.getAs[String])
    val stateOfO1WhenO2pre = o2State.pre(o1) flatMap (_.getAs[String])
    val pre = (stateOfO2WhenO1Pre, stateOfO1WhenO2pre)

    if (pre ==(opi, opi)) Alternative(o1, o2)
    else if (pre ==(opi, opf)) Sequence(o1, o2)
    else if (pre ==(opf, opi)) Sequence(o2, o1)
    else if (pre ==(opif, opi)) Sequence(o2, o1) //SometimeSequence(o2, o1)
    else if (pre ==(opi, opif)) Sequence(o1, o2) //SometimeSequence(o1, o2)
    else if (pre ==(opif, opif)) Parallel(o1, o2)
    else Other(o1, o2)
  }

  def changeParaToArbi(rels: RelationMap, enabled: EnabledStatesMap) = {
    val arbMap = enabled.arbiMap
    val arbisar = rels.relations.map{
      case (ops, p: Parallel) if arbMap.contains(ops) =>
        ops -> Arbitrary(p.sop:_*)
      case x @ (ops, sop) => x
    }

    rels.copy(relations = arbisar)


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
    val i: SPValue = "i"
    state.next(ops.map(_.id -> i).toMap)
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
