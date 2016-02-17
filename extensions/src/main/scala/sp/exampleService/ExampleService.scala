package sp.exampleService

import akka.actor._
import sp.domain.logic.IDAbleLogic
import scala.concurrent._
import sp.system.messages._
import sp.system._
import sp.domain._
import sp.domain.Logic._


object ExampleService extends SPService {

  // Define the service interface used by other services or the UI
  // The KeyDefinitions are used by a UI to know what type the service expects in the json
  // Use the scala type names as description.
  val specification = SPAttributes(
    "service" -> SPAttributes(
      "group"-> "aMenuGroup" // to organize in gui. maybe use "hide" to hide service in gui
    ),
    "serviceParameters" -> SPAttributes(
      "param1" -> KeyDefinition("Boolean", List(), Some(false)),
      "param2" -> KeyDefinition("String", List("theGood", "theBad"), Some("theGood"))
    ),
    "anID" -> KeyDefinition("ID", List(), Some(""))
  )

  // Include transformations when validating the request
  val transformTuple  = (
    TransformValue("serviceParameters", _.getAs[MyServiceParams]("serviceParameters")),
    TransformValue("anID", _.getAs[ID]("anID"))
  )
  val transformation = transformToList(transformTuple.productIterator.toList)

  // incl ServiceLauncher if you want unique actors per request
  def props = ServiceLauncher.props(Props(classOf[ExampleService]))

  // use this line if you only want one actor
  //def props = Props(classOf[ExampleService])
}


case class MyServiceParams(param1: Boolean, param2: String)

// Add constructor parameters if you need access to modelHandler and ServiceHandler etc
class ExampleService extends Actor with ServiceSupport {
  import context.dispatcher

  def receive = {
    // The service always get the attributes as well as the IDAbles (ids) that the service should work on.
    case r @ Request(service, attr, ids, reqID) => {
      // Always include the following lines. Are used by the helper functions
      val replyTo = sender()
      implicit val rnr = RequestNReply(r, replyTo)

      // include this if you want to send progress messages. Send attributes to it during calculations
      val progress = context.actorOf(progressHandler)

      println(s"example service got: $r")

      val params: MyServiceParams = transform(ExampleService.transformTuple._1)
      val id: ID = transform(ExampleService.transformTuple._2)
      val core = r.attributes.getAs[ServiceHandlerAttributes]("core").get
      println(s"core is always included: $core")

      // Send progress if your calculations take some time (> 0.5 sec)
      progress ! SPAttributes("progress" -> "we are making it")


      replyTo ! Response(List(), SPAttributes("someKey"->"someValue"), rnr.req.service, rnr.req.reqID)


      // Terminate self and progress. If you do not use ServiceLauncher, only terminate progress
      self ! PoisonPill
      progress ! PoisonPill

    }
  }


}



