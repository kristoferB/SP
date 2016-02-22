package sp.rasmus

import akka.actor._
import sp.domain.logic.IDAbleLogic
import scala.concurrent._
import sp.system.messages._
import sp.system._
import sp.domain._
import sp.domain.Logic._


object rasmus extends SPService {

  // Define the service interface used by other services or the UI
  // The KeyDefinitions are used by a UI to know what type the service expects in the json
  // Use the scala type names as description.
  val specification = SPAttributes(
    "service" -> SPAttributes(
      "group" -> "calculator" // to organize in gui. maybe use "hide" to hide service in gui
    ),
    "a" -> KeyDefinition("Int", List(), None),
    "b" -> KeyDefinition("Int", List(), None),
    "sign" -> KeyDefinition("String", List("+", "-", "*"), Some("+"))
  )

  // Include transformations when validating the request
  val transformTuple = (
    TransformValue("a", _.getAs[Int]("a")),
    TransformValue("b", _.getAs[Int]("b")),
    TransformValue("sign", _.getAs[String]("sign"))
    )
  val transformation = transformToList(transformTuple.productIterator.toList)

  // incl ServiceLauncher if you want unique actors per request
  def props = ServiceLauncher.props(Props(classOf[rasmus]))

  class rasmus extends Actor with ServiceSupport {

    import context.dispatcher

    def receive = {
      // The service always get the attributes as well as the IDAbles (ids) that the service should work on.
      case r@Request(service, attr, ids, reqID) => {
        val replyTo = sender()
        implicit val rnr = RequestNReply(r, replyTo)

        val a: Int = transform(rasmus.transformTuple._1)
        val b: Int = transform(rasmus.transformTuple._2)
        val sign: String = transform(rasmus.transformTuple._3)

        val res: Int = sign match {
          case "+" => a + b
          case "-" => a - b
          case "*" => a * b
        }

        replyTo ! Response(List(), SPAttributes("result" -> res), rnr.req.service, rnr.req.reqID)
        self ! PoisonPill

      }
    }


  }

}



