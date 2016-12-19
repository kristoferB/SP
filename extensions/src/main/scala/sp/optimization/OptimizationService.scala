package sp.optimization

import akka.actor._
import sp.domain.logic.IDAbleLogic

import scala.concurrent._
import sp.system.messages._
import sp.system._
import sp.domain._
import sp.domain.Logic._


object OptimizationService extends SPService {


  // The KeyDefinitions are used by a UI to know what type the service expects in the json
  // Use the scala type names as description.

  val modelSelectionList = OptimizationModels.getSelectionList
  val specification = SPAttributes(
    "service" -> SPAttributes(
      "group"-> "optimization" // to organize in gui. maybe use "hide" to hide service in gui
    ),
    "modelParameters" -> SPAttributes(
      "model" -> KeyDefinition("String", modelSelectionList, modelSelectionList.headOption),
      "data" -> KeyDefinition("String", List(), None)
    )
  )

  // Include transformations when validating the request
  val transformTuple  = (
    TransformValue("modelParameters", _.getAs[ModelParams]("modelParameters"))
  )
  val transformation = transformToList(transformTuple.productIterator.toList)

  // incl ServiceLauncher if you want unique actors per request
  def props = ServiceLauncher.props(Props(classOf[OptimizationService]))

  // use this line if you only want one actor
  //def props = Props(classOf[ExampleService])
}


/*trait OptimizationInterface {
  def result: SPAttributes
}*/

case class ModelParams(model: String, data: String)

// Add constructor parameters if you need access to modelHandler and ServiceHandler etc
class OptimizationService extends Actor with ServiceSupport {
  //import context.dispatcher


  def receive = {
    // The service always get the attributes as well as the IDAbles (ids) that the service should work on.
    case r @ Request(service, attr, ids, reqID) => {
      // Always include the following lines. Are used by the helper functions
      val replyTo = sender()
      implicit val rnr = RequestNReply(r, replyTo)

      // include this if you want to send progress messages. Send attributes to it during calculations
      val progress = context.actorOf(progressHandler)

      val modelParams = transform(OptimizationService.transformTuple)

      val opt = new OptimizationCoordinator(modelParams)


      // Send progress if your calculations take some time (> 0.5 sec)
      progress ! SPAttributes("progress" -> "we are making it")


      replyTo ! Response(List(Thing("Solution"+System.currentTimeMillis, opt.getSolution)), SPAttributes(), rnr.req.service, rnr.req.reqID)

      // Terminate self and progress. If you do not use ServiceLauncher, only terminate progress
      self ! PoisonPill
      progress ! PoisonPill

    }
  }


}



