package sp.TobbeG

import akka.actor._
import sp.domain.logic.IDAbleLogic
import scala.concurrent._
import sp.system.messages._
import sp.system._
import sp.domain._
import sp.domain.Logic._


object TobbeG extends SPService {

  // Define the service interface used by other services or the UI
  // The KeyDefinitions are used by a UI to know what type the service expects in the json
  // Use the scala type names as description.
  val specification = SPAttributes(
    "service" -> SPAttributes(
      "group"-> "aMenuGroup" // to organize in gui. maybe use "hide" to hide service in gui
    ),
    "number" -> KeyDefinition("Int", List(), None)
  )

  // Include transformations when validating the request
  val transformTuple  = (
    TransformValue("number", _.getAs[Int]("a")),
    TransformValue("b", _.getAs[Int]("b"))
  )
  val transformation = transformToList(transformTuple.productIterator.toList)


    // incl ServiceLauncher if you want unique actors per request
  def props = ServiceLauncher.props(Props(classOf[TobbeG]))

  // use this line if you only want one actor
  //def props = Props(classOf[ExampleService])
}

case class MyServiceParams(param1: Boolean, param2: String)

// Add constructor parameters if you need access to modelHandler and ServiceHandler etc
// Inkluderar eventHandler och namnet på servicen operationController. Skickas med i SP.scala
//(eventHandler: ActorRef, serviceHandler: ActorRef, operationController: String)
class TobbeG extends Actor with ServiceSupport {
  import context.dispatcher

  def receive = {
    // The service always get the attributes as well as the IDAbles (ids) that the service should work on.
    case r @ Request(service, attr, ids, reqID) => {
      // Always include the following lines. Are used by the helper functions
      val replyTo = sender()
      implicit val rnr = RequestNReply(r, replyTo)

      // Lyssna på events från alla
      //eventHandler ! SubscribeToSSE(self)

      // include this if you want to send progress messages. Send attributes to it during calculations
      val progress = context.actorOf(progressHandler)

      println(s"example service got: $r")

      // Terminate self and progress. If you do not use ServiceLauncher, only terminate progress
      self ! PoisonPill
      progress ! PoisonPill
    }
  }
}



