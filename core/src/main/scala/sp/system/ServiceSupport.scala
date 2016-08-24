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


trait SPService {
  val specification: SPAttributes
  val transformation: List[TransformValue[_]]

  def transformToList(xs: List[Any]) = {
    for {
      x <- xs
      if x.isInstanceOf[TransformValue[_]]
    } yield x.asInstanceOf[TransformValue[_]]
  }
}


trait ServiceSupport {
  type transformed
  case class RequestNReply(req: Request, reply: ActorRef)


  def transform[T](tV: TransformValue[T])(implicit rnr: RequestNReply): T = {
    val replyTo = rnr.reply    
    val service = rnr.req.service
    val attr = rnr.req.attributes
    
    val transformO = tV.transform(attr)
    if (transformO.isEmpty) replyTo ! SPError(s"Couldn't transform the key ${tV.key}")
    transformO.get
  }

  def getAttr[T](transform: SPAttributes => Option[T], error: String = "couldn't translate the attributes in the service")(implicit rnr: RequestNReply): Option[T] = {
    val result = transform(rnr.req.attributes)
    if (result.isEmpty) rnr.reply ! SPError(error + s"\nreq: ${rnr.req}")
    result
  }

  def askAService(request: Request, serviceHandler: ActorRef)(implicit rnr: RequestNReply, ec: ExecutionContext, timeout: Timeout) = {
    val p = Promise[Response]()
    val service = request.service
    val replyTo = rnr.reply
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

  def askForIDAbles(mess: ModelCommand, modelHandler: ActorRef)(implicit rnr: RequestNReply, ec: ExecutionContext, timeout: Timeout) = {
    val p = Promise[List[IDAble]]()
    val model = mess.model
    val replyTo = rnr.reply
    val f = modelHandler ? mess
    f.onSuccess{
      case SPIDs(xs) => p.success(xs)
      case x: SPError => {
        replyTo ! SPErrors(List(SPError(s"$model failed when asked $mess"), x))
        p.failure(new RuntimeException(x.toString))
      }
      case x => {
        replyTo ! SPError(s"$model failed when asked $mess, got $x")
        p.failure(new RuntimeException(x.toString))
      }
    }
    f.onFailure{ case e =>
      replyTo ! SPErrors(List(SPError(s"$model failed when asked $mess"), SPError(e.getMessage)))
      p.failure(e)
    }

    p.future
  }

  def askForModelInfo(model: ID, modelHandler: ActorRef)(implicit rnr: RequestNReply, ec: ExecutionContext, timeout: Timeout) = {
    val p = Promise[ModelInfo]()
    val replyTo = rnr.reply
    val f = modelHandler ? GetModelInfo(model)
    f.onSuccess{
      case x: ModelInfo => p.success(x)
      case x: SPError => {
        replyTo ! SPErrors(List(SPError(s"$model failed when asked for info"), x))
        p.failure(new RuntimeException(x.toString))
      }
      case x => {
        replyTo ! SPError(s"$model failed when asked for info, got $x")
        p.failure(new RuntimeException(x.toString))
      }
    }
    f.onFailure{ case e =>
      replyTo ! SPErrors(List(SPError(s"$model failed when asked for info"), SPError(e.getMessage)))
      p.failure(e)
    }

    p.future
  }

  def progressHandler(implicit rnr: RequestNReply) = {
    Props(classOf[ProgressHandler], rnr.req, rnr.reply)
  }

}

class ProgressHandler(request: Request, replyTo: ActorRef) extends Actor {
  import context.dispatcher
  var count = 0
  var lastUpdate = 0
  var progress = Progress(SPAttributes(), request.service, request.reqID)
  nextTick

  def receive = {
    case attr: SPAttributes => {
      progress = progress.copy(attributes = attr)
      lastUpdate = count
    }
    case "tick" => {
      count += 1
      sendProgress

      if (lastUpdate > count + 30) {
        // terminate if no response from service in 15 sec
        self ! PoisonPill
        replyTo ! SPError("Service failed to respond!")
      } else
        nextTick
    }
  }

  def sendProgress = {
    replyTo ! progress.copy(attributes = progress.attributes + ("progressNo", count))
  }

  def nextTick = {
    context.system.scheduler.scheduleOnce(500 milliseconds, self, "tick")
  }
}

class ServiceLauncher(runner: Props) extends Actor {
  def receive = {
    case RegisterService(s, _, _,_) => println(s"Service $s is registered")
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

