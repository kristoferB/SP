package sp.services.relations

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import sp.domain._
import sp.domain.logic.StateLogic
import sp.system.messages._
import sp.services.specificationconverters._

import scala.concurrent.duration._
import scala.util._

/**
 * Created by Kristofer on 2014-08-04.
 */
class RelationService(modelHandler: ActorRef, serviceHandler: ActorRef, conditionsFromSpecService: String) extends Actor {

  import sp.system.SPActorSystem._

  private implicit val to = Timeout(1 seconds)

  import context.dispatcher
  import Attribs._

  def receive = {
    case Request(_, attr) => {
      val reply = sender
      extract(attr) match {
        case Some((model, opsID, init, groups, iterations, goal)) => {

          // Retreive from model
          // todo: Handle this in a more general way soon
          val opsF = modelHandler ? GetIds(model, opsID)
          val modelInfoF = modelHandler ? GetModelInfo(model)
          val svsF = modelHandler ? GetThings(model)
          val currentRelationsF = (modelHandler ? GetResults(model, _.isInstanceOf[RelationResult]))
          val specsCondsF = serviceHandler ? Request(conditionsFromSpecService, Attr("model"-> model))

          //            .mapTo[List[RelationResult]] map (_.sortWith(_.modelVersion > _.modelVersion))

          val resultFuture = for {
            opsAnswer <- opsF
            modelInfoAnswer <- modelInfoF
            stateVarsAnswer <- svsF
            currentRelAnswer <- currentRelationsF
            specsConds <- specsCondsF

          } yield {
            List(opsAnswer, modelInfoAnswer, stateVarsAnswer, currentRelAnswer, specsConds) match {
              case SPIDs(opsIDAble) ::
                ModelInfo(_, _ , mVersion, _) ::
                SPIDs(svsIDAble) ::
                SPIDs(olderRelsIDAble) ::
                ConditionsFromSpecs(condMap) ::
                Nil => {

//                println(s"relationsSerice got:")
//                println(s"ops: $opsIDAble ")
//                println(s"mVersion: $mVersion ")
//                println(s"svsIdAble: $svsIDAble ")
//                println(s"olderRels: $olderRelsIDAble ")
//                println(s"condMap: $condMap ")

                val ops = opsIDAble map (_.asInstanceOf[Operation])
                val svs = svsIDAble map (_.asInstanceOf[Thing])
                val olderRels = olderRelsIDAble map (_.asInstanceOf[RelationResult]) sortWith (_.modelVersion > _.modelVersion)

                if (olderRels.nonEmpty && olderRels.head.modelVersion == mVersion) reply ! olderRels.head
                else {


                  import sp.domain.logic.StateVariableLogic._
                  val stateVarsMap = svs.map(sv => sv.id -> sv.inDomain).toMap ++ createOpsStateVars(ops)
                  val goalState = goal.getOrElse(State(Map()))
                  val goalfunction: State => Boolean = (s: State) => {
                    goalState.state.nonEmpty &&
                    goalState.state.forall{case (sv, value) => s(sv) == value}
                  }

                  println(s"GOAL STATE: $goal")
                  println(s"conditions from spec $condMap")

                  val temp = IDPrimitive(ID.newID)
                  val filterCondMap = condMap.map { case (id, conds) =>
                    val filtered = if (groups.isEmpty) conds else conds.filter(c => {
                      val group = c.attributes.get("group").getOrElse(temp)
                      groups.contains(group) || group == temp
                    })
                    id -> filtered
                  }

                  println(s"filterCondMap $filterCondMap")

                  val updatedOps = ops.map{ o =>
                    val updatedConds = filterCondMap.getOrElse(o.id, List[Condition]()) ++ o.conditions
                    o.copy(conditions = updatedConds)
                  }

                  //                  println(s"updated ops: $updatedOps")
                  //                  println(s"init: $init")
                  //                  println(s"added: ${addOpsToState(updatedOps, init)}")

                  // just one actor per request initially
                  val relationFinder = context.actorOf(RelationFinder.props)
                  val inputToAlgo = FindRelations(updatedOps, stateVarsMap ++ createOpsStateVars(updatedOps), addOpsToState(updatedOps, init), groups, iterations, goalfunction)

                  val relID = if (olderRels.nonEmpty) olderRels.head.id else ID.newID
                  val attr = Attr(
                    "settings" -> MapPrimitive(Map(
                      "goal" -> StateLogic.convertStateToListPrimitive(goalState),
                      "init" -> StateLogic.convertStateToListPrimitive(init),
                      "groups" -> ListPrimitive(groups),
                      "operations" -> ListPrimitive(opsID.map(IDPrimitive.apply))
                    ))
                  )

                  //TODO: Handle long running algorithms better
                  val answer = relationFinder ? inputToAlgo
                  answer onComplete {
                    case Success(res: FindRelationResult) => {
                      val relation = RelationResult("RelationMap", res.map, res.deadlocks, model, mVersion + 1, attr, relID)
                      reply ! relation
                      modelHandler ! UpdateIDs(model, mVersion, List(relation))
                      relationFinder ! PoisonPill
                    }
                    case Success(res) => println("WHAT IS THIS RELATION FINDER RETURNS: " + res); relationFinder ! PoisonPill
                    case Failure(error) => reply ! SPError(error.getMessage); relationFinder ! PoisonPill
                  }
                }
              }
              //TODO: This error handling should also be part of the general solution when extracting
              case error @ x :: xs => {
                val respond = error.foldLeft("- ")(_ + "\n- " + _.toString)
                reply ! respond
              }
            }
          }
          resultFuture.recover { case e: Exception => println("Resultfuture Fail: " + e.toString)}
        }
        case None => reply ! errorMessage(attr)
      }
    }
  }



  //TODO: I will fix this in a more general way so we can return errors if something is missing (probably using HList)
  def extract(attr: SPAttributes) = {
    for {
      model <- attr.getAsID("model")
      ops <- attr.getAsList("operations") map( _.flatMap(_.asID))
      initState <- attr.getStateAttr("initstate")
    } yield {
      val groups = attr.getAsList("groups").getOrElse(List())
      val goalState = attr.getStateAttr("goal")
      val iterations = attr.getAsInt("iteration").getOrElse(100)
      (model, ops, initState, groups, iterations, goalState)
    }
  }

  def errorMessage(attr: SPAttributes) = {
    SPError("The request is missing parameters: \n" +
      s"model: ${attr.getAsID("model")}" + "\n" +
      s"ops: ${attr.getAsList("operations") map (_.flatMap(_.asID))}" + "\n" +
      s"initstate: ${attr.getStateAttr("initstate")}" + "\n" +
      s"groups(optional): ${attr.getAsList("groups")}" + "\n" +
      s"goal(optional): ${attr.getStateAttr("goal")}" + "\n" +
      s"iterations(optional): ${attr.getAsInt("iteration")}")
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
    ops.map(o => o.id -> sp.domain.logic.OperationLogic.OperationState.inDomain).toMap
  }
}


object RelationService{
  def props(modelHandler: ActorRef, serviceHandler: ActorRef, conditionsFromSpecService: String) = Props(classOf[RelationService], modelHandler, serviceHandler, conditionsFromSpecService)

}