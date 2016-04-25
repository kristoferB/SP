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
    //"b" -> KeyDefinition("Int", List(), None),
    //"sign" -> KeyDefinition("String", List("+","-"), Some("+"))
  )

  // Include transformations when validating the request
  val transformTuple  = (
    TransformValue("number", _.getAs[Int]("a")),
    TransformValue("b", _.getAs[Int]("b"))
    //TransformValue("sign", _.getAs[String]("sign"))
  )
  val transformation = transformToList(transformTuple.productIterator.toList)

  // incl ServiceLauncher if you want unique actors per request
  def props = ServiceLauncher.props(Props(classOf[TobbeG]))

  // use this line if you only want one actor
  //def props = Props(classOf[ExampleService])
}

case class MyServiceParams(param1: Boolean, param2: String)

// Add constructor parameters if you need access to modelHandler and ServiceHandler etc
class TobbeG extends Actor with ServiceSupport {
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

      val number: Int = transform(TobbeG.transformTuple._1)
      val b: Int = transform(TobbeG.transformTuple._2)
      //val sign: String = transform(TobbeG.transformTuple._3)

      val res: Int = number match {
        case 0 => 0
        case 1 => 1
      }
/*
      function getProperColor($number)
      {
        if ($var > 0 && $var <= 5)
          return '#00FF00';
        else if ($var >= 6 && $var <= 10)
        return = '#FF8000';
        else if ($var >= 11)
        return = '#FF0000';
      }
      */
      // Send progress if your calculations take some time (> 0.5 sec)
      progress ! SPAttributes("progress" -> "we are making it")


      replyTo ! Response(List(), SPAttributes("result"-> res), rnr.req.service, rnr.req.reqID)


      // Terminate self and progress. If you do not use ServiceLauncher, only terminate progress
      self ! PoisonPill
      progress ! PoisonPill

    }
  }


}



