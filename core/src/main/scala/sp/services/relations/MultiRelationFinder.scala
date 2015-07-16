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


private[relations] case class Setup(twoOrThreeStates: String, simpleThreeState: Boolean, iterations: Int, maxDepth: Int, maxResets: Int, breakOnSameState: Boolean)
private[relations] case class Input(operations: List[ID], groups: List[String], initState: State, goal: Proposition)
private[relations] case class RelationStatus(iterations: Int)



/**
 * Created by kristofer on 15-06-22.
 */
class MultiRelationFinder(serviceHandler: ActorRef,
                          condFromSpecsService: String,
                          flattenOperationService: String
                           ) extends sp.system.ServiceRunner  {


  type ServiceInput = (Setup, Input)

  def extractServiceInput(attr: SPAttributes): Option[ServiceInput] = {
    val setup = attr.getAs[Setup]("setup")
    val input = attr.getAs[Input]("input")
    for (s <- setup; x <- input) yield {(s, x)}
  }

  import context.dispatcher

  def request(attr: ServiceInput, ids: List[IDAble]): Response = {
    val condFromSpecsF = askAService(Request(condFromSpecsService, SPAttributes(), ids.filter(_.isInstanceOf[Specification])), serviceHandler)
    val flattenOpsF = askAService(Request(flattenOperationService, SPAttributes("someInput"->"yes"), ids), serviceHandler)
    val f = for {cond <- condFromSpecsF; ops <- flattenOpsF} yield {


      Response(List(), SPAttributes())
    }
    Await.result(f, 3 seconds)
  }




  }



trait MultiRelationFinderLogic {

  def findOneSequence(ops: List[Operation], init: State, goal: State => Boolean) = {

  }

  def eval(o: Operation, s: State) = {
    //o.conditions.
  }


}
