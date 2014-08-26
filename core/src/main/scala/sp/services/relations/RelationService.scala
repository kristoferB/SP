package sp.services.relations

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import sp.domain._
import sp.system.messages._

import scala.concurrent.duration._
import scala.util._

/**
 * Created by Kristofer on 2014-08-04.
 */
class RelationService extends Actor {

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
          val opsF = modelHandler ? GetIds(opsID, model)
          val modelInfoF = modelHandler ? GetModelInfo(model)
          val svsF = modelHandler ? GetStateVariables(model)
          val currentRelationsF = (modelHandler ? GetResults(model, _.isInstanceOf[RelationResult]))
          //            .mapTo[List[RelationResult]] map (_.sortWith(_.modelVersion > _.modelVersion))

          val resultFuture = for {
            opsAnswer <- opsF
            modelInfoAnswer <- modelInfoF
            stateVarsAnswer <- svsF
            currentRelAnswer <- currentRelationsF
          } yield {
            List(opsAnswer, modelInfoAnswer, stateVarsAnswer, currentRelAnswer) match {
              case SPIDs(opsIDAble) :: ModelInfo(_, mVersion, _) :: SPSVs(svsIDAble) :: SPIDs(olderRelsIDAble) :: Nil => {
                val ops = opsIDAble map (_.asInstanceOf[Operation])
                val svs = svsIDAble map (_.asInstanceOf[StateVariable])
                val olderRels = olderRelsIDAble map (_.asInstanceOf[RelationResult]) sortWith (_.modelVersion > _.modelVersion)

                val stateVarsMap = svs map (sv => sv.id -> sv) toMap
                val goalfunction: State => Boolean = if (goal == None) (s: State) => false else (s: State) => s == goal.get

                // just one actor per request initially
                val relationFinder = context.actorOf(RelationFinder.props)
                val inputToAlgo = FindRelations(ops, stateVarsMap, init, groups, iterations, goalfunction)

                //TODO: Handle long running algorithms better
                val answer = relationFinder ? inputToAlgo
                answer onComplete {
                  case Success(res: RelationMap) => {
                    val relation = RelationResult("RelationMap", res, model, mVersion)
                    reply ! relation
                    modelHandler ! UpdateIDs(model, List(UpdateID(relation.id, -1, relation)))
                  }
                  case Success(res) => println("WHAT IS THIS RELATION FINDER RETURNS: " + res)
                  case Failure(error) => reply ! SPError(error.getMessage)
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
      model <- attr.getAsString("model")
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
      s"model: ${attr.getAsString("model")}" + "\n" +
      s"ops: ${attr.getAsList("operations") map (_.flatMap(_.asID))}" + "\n" +
      s"initstate: ${attr.getStateAttr("initstate")}" + "\n" +
      s"groups(optional): ${attr.getAsList("groups")}" + "\n" +
      s"goal(optional): ${attr.getStateAttr("goal")}" + "\n" +
      s"iterations(optional): ${attr.getAsInt("iteration")}")
  }
}


object RelationService{
  def props = Props(classOf[RelationService])
}