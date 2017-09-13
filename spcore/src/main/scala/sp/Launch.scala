package sp

import akka.actor._
import sp.domain._
import Logic._
import sp.models.{ModelActor, ModelMaker}
import sp.server._



object Launch extends App {
  implicit val system = ActorSystem("SP")
  val cluster = akka.cluster.Cluster(system)


  cluster.registerOnMemberUp {
    println("SPCore node has joined the cluster")
    SPCore.launch(system)


  }
  cluster.registerOnMemberRemoved{
    println("SPCore node has been removed from the cluster")
  }


  scala.io.StdIn.readLine("Press ENTER to exit cluster.\n")
  cluster.leave(cluster.selfAddress)

  scala.io.StdIn.readLine("Press ENTER to exit application.\n")
  // terminates in SPCore launch



}

object SPCore {
  def launch(system: ActorSystem): Unit = {
    implicit val sys = system
    implicit val ec = sys.dispatcher
    val webServer = new LaunchGUI(sys)
    val f = webServer.launch
    val cluster = akka.cluster.Cluster(sys)

    sys.actorOf(sp.service.ServiceHandler.props)
    system.actorOf(ModelMaker.props(ModelActor.props), "modelMaker")

    cluster.registerOnMemberRemoved{
      println("SP core will terminate")
      f.flatMap(_.unbind()) // trigger unbinding from the port
        .onComplete{_ =>
        sys.terminate()
      } // and shutdown when done
    }
  }
}



//sealed trait APITesting
//object APITesting {
//  sealed trait SUB
//
//  case class ServiceCall(param1: String) extends APITesting
//  case class RequestCall(param1: String) extends APITesting
//
//  case class AnAnswer(from: String) extends APITesting
//  case class Hi(from: String) extends APITesting
//
//  val api = sp.macros.MacroMagic.info[APITesting, SUB]
//  val service = "testingWidget"
//}



//// testing actor
//class TestingWidget extends Actor with ActorLogging {
//  import akka.cluster.pubsub._
//  import DistributedPubSubMediator._
//  val mediator = DistributedPubSub(context.system).mediator
//  mediator ! Subscribe("services", self)
//  mediator ! Subscribe("requests", self)
//  mediator ! Put(self)
//
//  var mess: Option[SPAttributes] = None
//  println("I'm running")
//  var c = 0
//
//  def matchMessage(x: String) = {
//    getMessage(x).nonEmpty
//  }
//
//  def isItToMe(m: SPMessage) = {
//    println("spmess: " + m)
//    println(s"is to me: ${m.header.getAs[String]("to")} - ${Some(APITesting.service)}")
//     m.header.getAs[String]("to").contains(APITesting.service)
//  }
//
//  def getMessage(x: String) = {
//    for {
//      m <- Try{read[SPMessage](x)}.toOption if isItToMe(m)
//      pOK <- Try{readJs[APITesting](m.body)}.toOption
//    } yield {
//      (m.header, pOK)
//    }
//  }
//
//  def makeTheMessage(header: SPAttributes, body: APITesting) = {
//    val b = writeJs(body)
//    val mess = SPMessage(header, b)
//    write(mess)
//  }
//
//  def makeTheMessage(header: SPAttributes, body: APISP) = {
//    val b = writeJs(body)
//    val mess = SPMessage(header, b)
//    write(mess)
//  }
//
//  def sendTheMessage(topic: String, mess: String) = {
//    mediator ! Publish(topic, mess)
//  }
//
//
//  def receive = {
//    case x: String if matchMessage(x) =>
//      val (header, mess) = getMessage(x).get
//      mess match {
//        case APITesting.ServiceCall(p) =>
//          println("got a ServiceCall")
//          val newH: SPAttributes = header merge SPAttributes("replyFrom"->APITesting.service, "replyID" -> ID.newID)
//          sendTheMessage("answers",  makeTheMessage(newH, APISP.SPACK()))
//
//          sendTheMessage("answers",  makeTheMessage(newH, APITesting.AnAnswer(p + "1")))
//          sendTheMessage("answers",  makeTheMessage(newH, APITesting.AnAnswer(p + "2")))
//          sendTheMessage("answers",  makeTheMessage(newH, APITesting.AnAnswer(p + "3")))
//
//
//         sendTheMessage("answers",  makeTheMessage(newH, APISP.SPDone()))
//        case APITesting.RequestCall(p) =>
//          println("got a RequestCall")
//          val newH = header merge SPAttributes("replyFrom"->APITesting.service, "replyID" -> ID.newID)
//          sender() ! makeTheMessage(newH, APITesting.Hi(p))
//      }
//
//
//
//
//    case "tick" =>
//      println("Testing widget got tick: "+mess)
//      mess = updMess(mess)
//      mess.map(x => mediator ! Publish("answers", x.toJson))
//      tick
//    case x: String =>
//      println("Testing widget got: "+x)
//      println("Testing widget got: "+mess)
//
////      if (mess.isEmpty) tick
////      mess = updMess(SPAttributes.fromJson(x))
//
////      scala.util.Try(new java.io.File(s"./gui/sp-example-widget/$x")) match {
////        case Success(file) => sender() ! file
////        case Failure(e) => sender() ! s"failed reading file: ${e.getMessage}"
////      }
//    case x => println("Testing widget got not as string: "+x)
//
//  }
//
//  def updMess(newMess: Option[SPAttributes]) = {
//    newMess.map(x => {
//      val header = x.getAs[SPAttributes]("header").map(_ + ("c"->c))
//      val body = x.getAs[SPAttributes]("body")
//      c = c+1
//      SPAttributes("header"->header, "body"->body)
//    })
//  }
//
//  import context.dispatcher
//  import scala.concurrent.duration._
//  import akka.util.Timeout
//  import scala.util._
//  def tick = context.system.scheduler.scheduleOnce(500 milliseconds, self, "tick")
//
//}



class ClusterMonitor extends Actor with ActorLogging {

  // special setup to monitor cluster
  import akka.cluster.Cluster
  import akka.cluster.ClusterEvent._
  import akka.cluster.pubsub._

  val cluster = Cluster(context.system)
  import DistributedPubSubMediator.{ Put, Subscribe, Publish }
  val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe("modelMessages", self)
  mediator ! Subscribe("modelEvents", self)

  def receive = {
    case MemberUp(member) =>
      log.info("Member is Up: {}", member.address)
    case UnreachableMember(member) =>
      log.info("Member detected as unreachable: {}", member)
    case MemberRemoved(member, previousStatus) =>
      log.info("Member is Removed: {} after {}",
        member.address, previousStatus)
    case _: MemberEvent => // ignore
    case mess =>
      println(s"Message was sent: $mess")
  }

  // subscribe to cluster changes, re-subscribe when restart
  override def preStart(): Unit = {
    //#subscribe
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents,
      classOf[MemberEvent], classOf[UnreachableMember])
    //#subscribe
  }
  override def postStop(): Unit = cluster.unsubscribe(self)
}

object ClusterMonitor {
  def props = Props(classOf[ClusterMonitor])
}
