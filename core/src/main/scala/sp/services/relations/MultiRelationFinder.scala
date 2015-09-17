package sp.services.relations

import akka.actor._
import sp.domain._
import sp.domain.Logic._
import sp.system.messages._
import scala.concurrent._
import scala.concurrent.duration._


object MultiRelationFinder {
  val specification = SPAttributes(
    "service" -> SPAttributes(
      "group"-> "Relations"
    ),
    "setup" -> SPAttributes(
      "twoOrThreeStates" -> KeyDefinition("String", List("two", "three"), Some("three")),
      "simpleThreeState" -> KeyDefinition("Boolean", List(), Some(true)),
      "iterations" -> KeyDefinition("Int", List(), Some(100)),
      "maxDepth" -> KeyDefinition("Int", List(), Some(-1)),
      "maxResets" -> KeyDefinition("Int", List(), Some(0)),
      "breakOnSameState" -> KeyDefinition("Boolean", List(), Some(true))
    ),
    "input" -> SPAttributes(
      "operations" -> KeyDefinition("List[ID]", List(), Some(org.json4s.JArray(List()))),
      "groups" -> KeyDefinition("List[String]", List(), Some(org.json4s.JArray(List()))),
      "initState" -> KeyDefinition("State", List(), None),
      "goal" -> KeyDefinition("Proposition", List(), None)
    )
  )

  def props(serviceHandler: ActorRef,
            condFromSpecsService: String,
            flattenOperationService: String) =
    Props(classOf[MultiRelationFinder], serviceHandler, condFromSpecsService, flattenOperationService)
}


case class Setup(twoOrThreeStates: String, simpleThreeState: Boolean, iterations: Int, maxDepth: Int, maxResets: Int, breakOnSameState: Boolean)
case class Input(operations: List[ID], groups: List[String], initState: State, goal: Proposition)

/**
 * Created by kristofer on 15-06-22.
 */
class MultiRelationFinder(serviceHandler: ActorRef,
                          condFromSpecsService: String,
                          flattenOperationService: String
                           ) extends Actor with sp.system.ServiceSupport with MultiRelationFinderLogic  {

  import context.dispatcher
  implicit val timeout = akka.util.Timeout(1 seconds)

  def receive = {
    case r @ Request(service, attr, ids, reqID) => {
      val replyTo = sender()
      implicit val rnr = RequestNReply(r, replyTo)
      val progress = context.actorOf(progressHandler)

      val res = for {
        setup <- getAttr(_.getAs[Setup]("setup"), "Couldn't convert the setup json")
        input <- getAttr(_.getAs[Input]("input"), "Couldn't convert the input json")
      } yield {
          val condFromSpecs = askAService(Request(condFromSpecsService, SPAttributes(), ids.filter(_.isInstanceOf[Specification])), serviceHandler)
          val flattenOpsF = askAService(Request(flattenOperationService, SPAttributes("someInput"->"yes"), ids), serviceHandler)

          progress ! SPAttributes("status"-> "Received initial answer from external services")

        }

      res.foreach(replyTo ! _)
    }

    }

  import context.dispatcher

//  def request(attr: ServiceInput, ids: List[IDAble]): Response = {
//    val setup = attr._1
//    val input = attr._2
//    val condFromSpecsF = askAService(Request(condFromSpecsService, SPAttributes(), ids.filter(_.isInstanceOf[Specification])), serviceHandler)
//    val flattenOpsF = askAService(Request(flattenOperationService, SPAttributes("someInput"->"yes"), ids), serviceHandler)
//    val f = for {cond <- condFromSpecsF; ops <- flattenOpsF} yield {
//      updateProgress(SPAttributes("status"-> "Received initial answer from external services"))
//
//      // identify relations
//
//      //loop
//        // find a sequence
//        // identify enabled and arbitrary
//        // update relations
//        // handle deadlocks
//      // end loop
//
//
//
//      Response(List(), SPAttributes())
//    }
//    Await.result(f, 3 seconds)
//  }




  }



trait MultiRelationFinderLogic {

  def findOneSequence(ops: List[Operation], init: State, goal: State => Boolean) = {

  }

  def eval(o: Operation, s: State) = {
    //o.conditions.
  }


}
