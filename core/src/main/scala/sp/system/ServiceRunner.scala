package sp.system

import akka.actor._
import sp.domain._
import sp.domain.Logic._
import sp.system.messages._
import scala.concurrent._
import scala.concurrent.duration._
import akka.pattern.ask
import akka.util.Timeout
import scala.util._



trait ServiceSupport {

  type RequestAndReply = (Request, ActorRef)

  def getAttr[T](transform: SPAttributes => Option[T], error: String)(implicit rnr: RequestAndReply): Option[T] = {
    val result = transform(rnr._1.attributes)
    if (result.isEmpty) rnr._2 ! SPError(error)
    result
  }

  def askAService(service: String, serviceHandler: ActorRef)(implicit rnr: RequestAndReply, ec: ExecutionContext, timeout: Timeout) = {
    val p = Promise[Response]()
    val request = rnr._1
    val replyTo = rnr._2
    val f = serviceHandler ? request.copy(attributes = request.attributes + ("onlyResponse"->true))
    f.onSuccess{
      case x: Response => p.success(x)
      case x: SPError => {
        replyTo ! SPErrors(List(SPError(s"$service failed when asking $request"), x))
        p.failure(new RuntimeException(x.toString))
      }
      case x => {
        replyTo ! SPError(s"$service failed when asking $request, got $x")
        p.failure(new RuntimeException(x.toString))
      }
    }
    f.onFailure{ case e =>
      replyTo ! SPErrors(List(SPError(s"$service failed when asking $request"), SPError(e.getMessage)))
      p.failure(e)
    }

    p.future
  }

  def progressHandler(implicit rnr: RequestAndReply) = {
    Props(classOf[ProgressHandler], rnr._1, rnr._2)
  }

}

class ProgressHandler(request: Request, replyTo: ActorRef) extends Actor {
  import context.dispatcher
  var count = 0
  var progress = Progress(SPAttributes(), request.service, request.reqID)
  self ! "tick"
  def receive = {
    case attr: SPAttributes => {
      progress = progress.copy(attributes = attr)
    }
    case "tick" => {
      count += 1
      sendProgress
      nextTick
    }
  }

  def sendProgress = {
    replyTo ! progress.copy(attributes = progress.attributes + ("progress", count))
  }

  def nextTick = {
    println("hej")
    context.system.scheduler.scheduleOnce(500 milliseconds, self, "tick")
  }
}

class ServiceLauncher(runner: Props) extends Actor {
  def receive = {
    case RegisterService(s, _, _) => println(s"Service $s is registered")
    case RemoveService(s) => println(s"Service $s is removed")
    case req: Request => {
      try {
        context.actorOf(runner).tell(req, sender())
      } catch { case e: Throwable =>
        sender() ! SPError(s"Runner actor in service ${req.service} could not be created: ${e.getMessage}")
      }
    }
  }
}

object ServiceLauncher {
  def props(runner: Props) = Props(classOf[ServiceLauncher], runner)
}


///**
// * A base class for a service that launches a unique actor for every new request.
// * Implement the extractServiceInput where you define how to convert the attribute
// * input. Then define a good type for ServiceInput which is the type of the result of
// * extractServiceInput.
// *
// * Define the request method where the logic of the service is defined. Do not forget
// * to call updateProgress multiple times during the execution. Return a response.
// *
// * Remember to also incl a companion object defining:
// * val specification = SPAttributes(...)
// *
// * and
// *
// * def props(p1: T, p2, V) = ServiceLauncher.props(Props(classOf[ServiceExample], p1, p2))
// *
// * where p1, p2 are constructor parameters to your class (for example names of other services
// * and actorRef to modelHandler and serviceHandler). important to incl ServiceLauncher if you
// * want unique actors per request
// */
//abstract class ServiceRunner extends Actor {
//  type ServiceInput
//  def extractServiceInput(attr: SPAttributes): Option[ServiceInput]
//
//  /**
//   * The method where the response is calculated. If you end up with a future[Response],
//   * then block and await the result and return or throw an exception.
//   * @param attr The input to the service transformed by extractServiceInput
//   * @param ids a list of the ids to the service
//   * @return the final Response
//   */
//  def request(attr: ServiceInput, ids: List[IDAble]): Response
//
//
//  // API to use
//
//  // call this multiple time during execution to update the progress
//  //def updateProgress(attr: SPAttributes)
//
//  // If you want to ask another service, use this method
//  // def askAService(req: Request, serviceHandler: ActorRef)
//
//
//  var count = 0
//  var progress = Progress(SPAttributes("count"->count))
//  def updateProgress(attr: SPAttributes) = {
//    val attrNoCount = SPAttributes(attr.obj.filter(_._1 != "count"))
//    progress = Progress(attrNoCount + ("count" -> count))
//  }
//
//
//  var replyTo = context.parent
//  var service = ""
//  def receive = {
//    case Request(s, attr, ids) => {
//      replyTo = sender()
//      service = s
//      val extr = extractServiceInput(attr)
//      if (!extr.isDefined) {
//        sender() ! SPError(s"couldn't understand the attributes: $attr")
//      } else {
//        val progressRunner = startProgress
//        try {
//          val resp = request(extr.get, ids)
//          replyTo ! resp
//        } catch {
//          case e: Throwable => {
//            replyTo ! SPError(s"$s failed with an exception: ${e.getMessage}")
//          }
//        }
//        progressRunner.cancel
//        self ! PoisonPill
//      }
//    }
//  }
//
//
//  import context.dispatcher
//  implicit val timeout = Timeout(10 seconds)
//  def askAService(req: Request, serviceHandler: ActorRef) = {
//    val p = Promise[Response]()
//    val f = serviceHandler ? req.copy(attributes = req.attributes + ("onlyResponse"->true))
//    f.onSuccess{
//      case x: Response => p.success(x)
//      case x: SPError => {
//        replyTo ! SPErrors(List(SPError(s"$service failed when asking $req"), x))
//        p.failure(new RuntimeException(x.toString))
//      }
//      case x => {
//        replyTo ! SPError(s"$service failed when asking $req, got $x")
//        p.failure(new RuntimeException(x.toString))
//      }
//    }
//    f.onFailure{ case e =>
//      replyTo ! SPErrors(List(SPError(s"$service failed when asking $req"), SPError(e.getMessage)))
//      p.failure(e)
//    }
//
//    p.future
//  }
//
//  def startProgress = {context.system.scheduler.schedule(
//    500 milliseconds, 500 milliseconds){
//    count += 1
//    updateProgress(progress.attributes)
//    replyTo ! progress
//  }
//  }
//
//}
//
//
