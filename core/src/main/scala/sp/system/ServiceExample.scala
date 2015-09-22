package sp.system

import akka.actor._
import sp.domain.logic.IDAbleLogic
import scala.concurrent._
import sp.system.messages._
import sp.domain._
import sp.domain.Logic._


object ServiceExample extends SPService {
  val specification = SPAttributes(
    "service" -> SPAttributes(
      "group"-> "aMenuGroup" // to organize in gui. maybe use "hide" to hide service in gui
    ),
    "setup" -> SPAttributes(
      "onlyOperations" -> KeyDefinition("Boolean", List(), Some(false)),
      "searchMethod" -> KeyDefinition("String", List("theGood", "theBad"), Some("theGood"))
    ),
    "findID" -> KeyDefinition("ID", List(), Some(""))
  )

  val transformTuple  = (
    TransformValue("setup", _.getAs[ExampleSetup]("setup")),
    TransformValue("findID", _.getAs[ID]("findID"))
  )

  val transformation = transformToList(transformTuple.productIterator.toList)

  // important to incl ServiceLauncher if you want unique actors per request
  def props = ServiceLauncher.props(Props(classOf[ServiceExample]))


  // Alla får även "core" -> ServiceHandlerAttributes

//  case class ServiceHandlerAttributes(model: Option[ID],
//                                      responseToModel: Boolean,
//                                      onlyResponse: Boolean,
//                                      includeIDAbles: List[ID])

}


case class ExampleSetup(onlyOperations: Boolean, searchMethod: String)

// Add constructor parameters if you need access to modelHandler and ServiceHandler etc
class ServiceExample extends Actor with ServiceSupport {
  import context.dispatcher

  def receive = {
    case r @ Request(service, attr, ids, reqID) => {

      // Always include the following lines. Are used by the helper functions
      val replyTo = sender()
      implicit val rnr = RequestNReply(r, replyTo)

      // include this if you whant to send progress messages. Send attributes to it during calculations
      val progress = context.actorOf(progressHandler)

      println(s"I got: $r")

      val s = transform(ServiceExample.transformTuple._1)
      val id = transform(ServiceExample.transformTuple._2)

      val core = r.attributes.getAs[ServiceHandlerAttributes]("core").get
      println(s"core är även med: $core")

      s.searchMethod match {
        case "theBad" => {
          println("HEJ")
          var iterations = 0
          val filter = ids.filter { x =>
            progress ! SPAttributes("iterations" -> iterations)
            iterations += 1
            if (s.onlyOperations && !x.isInstanceOf[Operation] || x.id == id) false
            else {
              val jsonID = SPValue(id)
              SPValue(x).find(_ == jsonID).isDefined
            }
          }
          sendResp(Response(filter, SPAttributes("setup" -> s), service, reqID), progress)
        }
        case "theGood" => {
          println("HEJ den bra")
          var iterations = 0
          val f2 = IDAbleLogic.removeID(Set(id), ids).map(_.id)
          val filter = ids.filter { x =>
            progress ! SPAttributes("iterations" -> iterations)
            iterations += 1
            f2.contains(x.id)
          }
          sendResp(Response(filter, SPAttributes("setup" -> s), service, reqID), progress)
        }
      }
    }
    case (r : Response, reply: ActorRef) => {
      reply ! r
    }
    case x => {
      sender() ! SPError("What do you whant me to do? "+ x)
      self ! PoisonPill
    }
  }

  import scala.concurrent._
  import scala.concurrent.duration._
  def sendResp(r: Response, progress: ActorRef)(implicit rnr: RequestNReply) = {
    context.system.scheduler.scheduleOnce(2000 milliseconds, self, (r, rnr.reply))

//    rnr.reply ! r
//    progress ! PoisonPill
//    self ! PoisonPill
  }

}



