package sp.services.relations

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import sp.domain._
import sp.system.messages._

import scala.concurrent.duration._

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
      val params = extract(attr) map {
        case (model, opsID, init, groups, iterations, goal) =>

          val currentRelations = (modelHandler ? GetResults(model, _.isInstanceOf[RelationResult])).
            mapTo[List[RelationResult]] map(_.sortWith(_.modelVersion > _.modelVersion))

          val opsF = modelHandler ? GetIds(opsID, model)
          val modelInfoF = modelHandler ? GetModelInfo(model)
          val svsF = modelHandler ? GetStateVariables(model)

          // just one actor per request initially
          val relationFinder = context.actorOf(RelationFinder.props)

          for {
            ops <- opsF.mapTo[SPIDs]
            modelInfo <- modelInfoF.mapTo[ModelInfo]
            svs <- svsF.mapTo[SPSVs]
          } yield {
            val stateVarsMap = svs.svs map(sv => sv.id -> sv) toMap
            val theOps = ops.items map(_.asInstanceOf[Operation])
            val goalfunction: State => Boolean = if (goal == None) (s: State) => false else {
              val goalState = goal.get
              (s: State) => s == goalState
            }

            val findRels = FindRelations(theOps, stateVarsMap, init, groups, iterations, goalfunction)
            val res = (relationFinder ? findRels).mapTo[RelationMap]
            res map {relMap =>
              println(s"We Got relations: $relMap")
              val result = RelationResult("RelationMap", relMap, model, modelInfo.version)
              reply ! result
              modelHandler ! result
            }
          }
      }
      if (params == None) reply ! SPError("The request is missing parameters: \n" +
        s"model: ${attr.getAsString("model")}" + "\n" +
        s"ops: ${attr.getAsList("operations") map( _.flatMap(_.asID))}" + "\n" +
        s"initstate: ${attr.getStateAttr("initstate")}" + "\n" +
        s"groups(optional): ${attr.getAsList("groups")}" + "\n" +
        s"goal(optional): ${attr.getStateAttr("goal")}" + "\n" +
        s"iterations(optional): ${attr.getAsInt("iteration")}" + "\n"

      )
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
}


object RelationService{
  def props = Props(classOf[RelationService])
}