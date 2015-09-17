package sp.system

import akka.actor._
import sp.domain.logic.IDAbleLogic
import scala.concurrent._
import sp.system.messages._
import sp.domain._
import sp.domain.Logic._



object ServiceExample {
  val specification = SPAttributes(
    "service" -> SPAttributes(
      "group"-> "aMenuGroup" // to organize in gui. maybe use "hide" to hide service in gui
    ),
    "setup" -> Map(
      "onlyOperations" -> KeyDefinition("Boolean", List(), Some(false)),
      "searchMethod" -> KeyDefinition("String", List("theGood", "theBad"), Some("theGood"))
    ),
    "findID" -> KeyDefinition("ID", List(), None)
  )

  // important to incl ServiceLauncher if you want unique actors per request
  def props = ServiceLauncher.props(Props(classOf[ServiceExample]))
}


case class ExampleSetup(onlyOperations: Option[Boolean], searchMethod: Option[String])

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

      // ask a service with the following. You need to send in the service Handler in the constructor
      // of the actor. Avoid using sp.system.SPActorSystem.serviceHandler as below since it
      // makes it hard to test
      //val serviceAnswer: Future[Response] = askAService("myService", serviceHandler)

      // Use the getAttr to simplify extraction of parameters with error support
      val res = for {
        s <- getAttr(_.getAs[ExampleSetup]("setup"), error = "Can not convert the setup")
        id <- getAttr(_.getAs[ID]("findID"), error = "An ID was not defined in findID")
      } yield {
          s.searchMethod match {
            case Some("theBad") => {
              println("HEJ")
              var iterations = 0
              val filter = ids.filter { x =>
                progress ! SPAttributes("iterations" -> iterations)
                iterations += 1
                if (s.onlyOperations.get && !x.isInstanceOf[Operation] || x.id == id) false
                else {
                  val jsonID = SPValue(id)
                  SPValue(x).find(_ == jsonID).isDefined
                }
              }
              Response(filter, SPAttributes("setup" -> s), service, reqID)
            }
            case Some("theGood") => {
              println("HEJ den bra")
              var iterations = 0
              val f2 = IDAbleLogic.removeID(Set(id), ids).map(_.id)
              val filter = ids.filter { x =>
                progress ! SPAttributes("iterations" -> iterations)
                iterations += 1
                f2.contains(x.id)
              }
              Response(filter, SPAttributes("setup" -> s), service, reqID)
            }
          }
      }

      res.foreach{ resp =>
        replyTo ! resp
        self ! PoisonPill
        //progress ! PoisonPill  // behövs om du inte skapar en actor per request
      }
    }
    case x => sender() ! SPError("What do you whant me to do? "+ x)
  }


}



