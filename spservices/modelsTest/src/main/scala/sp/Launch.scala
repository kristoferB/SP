package sp

import akka.actor._
import sp.domain._
import sp.messages._

import scala.util.{Failure, Success, Try}




object Launch extends App {

  implicit val system = ActorSystem("SP")

  // Add root actors used in node here
  //system.actorOf(ModelMaker.props(ModelActor.props), "modelHandler")

  // Used only at one place in cluster for testing
  //system.actorOf(ClusterMonitor.props)

  // only for testing. Remove
    system.actorOf(Props(classOf[TestingWidget]), "testingWidget")
}

import sp.domain.Logic._



sealed trait APITesting
object APITesting {
  sealed trait SUB

  case class ServiceCall(param1: String) extends APITesting
  case class RequestCall(param1: String) extends APITesting

  case class AnAnswer(from: String) extends APITesting
  case class Hi(from: String) extends APITesting

  val api = sp.macros.MacroMagic.info[APITesting, SUB]
  val service = "testingWidget"
}



// testing actor
class TestingWidget extends Actor with ActorLogging {
  import akka.cluster.pubsub._
  import DistributedPubSubMediator._
  val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe("services", self)
  mediator ! Subscribe("requests", self)
  mediator ! Put(self)

  var mess: Option[SPAttributes] = None
  println("I'm running")
  var c = 0

  def matchMessage(x: String) = {
    getMessage(x).nonEmpty
  }

  def isItToMe(m: SPMessage) = {
    println("spmess: " + m)
    println(s"is to me: ${m.header.getAs[String]("to")} - ${Some(APITesting.service)}")
     m.header.getAs[String]("to").contains(APITesting.service)
  }

  def getMessage(x: String) = {
    for {
      m <- Try{APIParser.read[SPMessage](x)}.toOption if isItToMe(m)
      pOK <- Try{APIParser.readJs[APITesting](m.body)}.toOption
    } yield {
      (m.header, pOK)
    }
  }

  def makeTheMessage(header: SPAttributes, body: APITesting) = {
    val b = APIParser.writeJs(body)
    val mess = SPMessage(header, b)
    APIParser.write(mess)
  }

  def makeTheMessage(header: SPAttributes, body: APISP) = {
    val b = APIParser.writeJs(body)
    val mess = SPMessage(header, b)
    APIParser.write(mess)
  }

  def sendTheMessage(topic: String, mess: String) = {
    mediator ! Publish(topic, mess)
  }


  def receive = {
    case x: String if matchMessage(x) =>
      val (header, mess) = getMessage(x).get
      mess match {
        case APITesting.ServiceCall(p) =>
          println("got a ServiceCall")
          val newH: SPAttributes = header merge SPAttributes("replyFrom"->APITesting.service, "replyID" -> ID.newID)
          sendTheMessage("answers",  makeTheMessage(newH, APISP.SPACK()))

          sendTheMessage("answers",  makeTheMessage(newH, APITesting.AnAnswer(p + "1")))
          sendTheMessage("answers",  makeTheMessage(newH, APITesting.AnAnswer(p + "2")))
          sendTheMessage("answers",  makeTheMessage(newH, APITesting.AnAnswer(p + "3")))


         sendTheMessage("answers",  makeTheMessage(newH, APISP.SPDone()))
        case APITesting.RequestCall(p) =>
          println("got a RequestCall")
          val newH = header merge SPAttributes("replyFrom"->APITesting.service, "replyID" -> ID.newID)
          sender() ! makeTheMessage(newH, APITesting.Hi(p))
      }




    case "tick" =>
      println("Testing widget got tick: "+mess)
      mess = updMess(mess)
      mess.map(x => mediator ! Publish("answers", x.toJson))
      tick
    case x: String =>
      println("Testing widget got: "+x)
      println("Testing widget got: "+mess)

//      if (mess.isEmpty) tick
//      mess = updMess(SPAttributes.fromJson(x))

//      scala.util.Try(new java.io.File(s"./gui/sp-example-widget/$x")) match {
//        case Success(file) => sender() ! file
//        case Failure(e) => sender() ! s"failed reading file: ${e.getMessage}"
//      }
    case x => println("Testing widget got not as string: "+x)

  }

  def updMess(newMess: Option[SPAttributes]) = {
    newMess.map(x => {
      val header = x.getAs[SPAttributes]("header").map(_ + ("c"->c))
      val body = x.getAs[SPAttributes]("body")
      c = c+1
      SPAttributes("header"->header, "body"->body)
    })
  }

  import context.dispatcher
  import scala.concurrent.duration._
  import akka.util.Timeout
  import scala.util._
  def tick = context.system.scheduler.scheduleOnce(500 milliseconds, self, "tick")

}



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
  def props = Props(classOf[TestingWidget])
}
