package sp.system

import akka.actor._
import akka.testkit._
import com.typesafe.config._
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import sp.domain.Logic._
import sp.domain._
import sp.system.messages._

import scala.concurrent.duration._

/**
 * Created by Kristofer on 2014-06-17.
 */
class ServiceTalkerTest(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  def this() = this(ActorSystem("myTest", ConfigFactory.parseString(
    """
      |akka.loglevel = INFO
    """.stripMargin)))



  val id = ID.newID
  val o = Operation("hej")

  override def beforeAll: Unit = {

  }

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }


  "The Service Talker" must {
    "analyse request attributes an pass if ok" in {
      val definition = SPAttributes("key1" -> KeyDefinition("kk", List("hej", "kalle"), None),
        "key2" -> SPAttributes("key3" -> KeyDefinition("kk", List("hej", "kalle"), None))) +
      ServiceTalker.serviceHandlerAttributes

      val attr = SPAttributes("key1" -> "hej", "key2" -> Map("key3" -> "kalle"))
      val request = Request("temp", attr, List())

      val x = ServiceTalker.validateRequest(request, definition, List())
      x shouldEqual Right(Request("temp", SPAttributes("key1" -> "hej", "key2" -> Map("key3" -> "kalle"), "core"-> ServiceHandlerAttributes(None, false, false, List())), List(), request.reqID))
    }

    "analyse request attributes an fail if missing" in {
      val definition = SPAttributes("key1" -> KeyDefinition("String", List("hej", "kalle"), None),
        "key2" -> SPAttributes("key3" -> KeyDefinition("String", List("hej", "kalle"), None)))
      val attr = SPAttributes("key1" -> "hej")
      val request = Request("temp", attr, List())
      val x = ServiceTalker.validateRequest(request, definition, List())
      x shouldEqual Left(List(SPErrorString(s"Required key key3 is missing")))
    }

    "analyse request attributes an fill if missing" in {
      val definition = SPAttributes("key1" -> KeyDefinition("String", List("hej", "kalle"), Some("japp")),
        "key2" -> SPAttributes("key3" -> KeyDefinition("String", List("hej", "kalle"), None)))
      val attr = SPAttributes("key2" -> Map("key3" -> "kalle"))
      val request = Request("temp", attr, List())
      val x = ServiceTalker.validateRequest(request, definition, List())
      x shouldEqual Right(Request("temp", SPAttributes("key1" -> "japp", "key2" -> Map("key3" -> "kalle")), List(), request.reqID))
    }

    "forward a request" in {
      val p = TestProbe()
      val r = Request("", SPAttributes(), List())
      val st = system.actorOf(
        ServiceTalker.props(
          service = p.ref,
          modelHandler = p.ref,
          replyTo = p.ref,
          serviceAttributes = SPAttributes(),
          request = r,
          toBus = None))

      st ! r
      p.expectMsg(100 millis, r)
    }

    "Expect a response and SPTalker dies" in {
      val p = TestProbe()
      val dead = TestProbe()
      val r = Request("reply", SPAttributes(), List())
      val testA = system.actorOf(Props(classOf[testService], o))
      val st = system.actorOf(
        ServiceTalker.props(
          service = testA,
          modelHandler = p.ref,
          replyTo = p.ref,
          serviceAttributes = SPAttributes(),
          request = r,
          toBus = None))
      dead watch st

      st ! r
      p.expectMsg(500 millis, Response(r.ids, r.attributes, r.service, r.reqID))
      dead.expectTerminated(st, 2000 milliseconds)
    }
    "Expect a progress and then response" in {
      val p = TestProbe()
      val r = Request("delay", SPAttributes(), List())
      val testA = system.actorOf(Props(classOf[testService], o))
      val st = system.actorOf(
        ServiceTalker.props(
          service = testA,
          modelHandler = p.ref,
          replyTo = p.ref,
          serviceAttributes = SPAttributes(),
          request = r,
          toBus = None))

      st ! r
      var b = false
      p.fishForMessage(2 second){
        case x: Progress if !b => b = true; false
        case x: Response if b =>  true
      }
    }
    "Expect only response if onlyResponse attribute" in {
      val p = TestProbe()
      val r = Request("delay", SPAttributes("core"-> ServiceHandlerAttributes(None, false, true, List())), List())
      val testA = system.actorOf(Props(classOf[testService], o))
      val st = system.actorOf(
        ServiceTalker.props(
          service = testA,
          modelHandler = p.ref,
          replyTo = p.ref,
          serviceAttributes = SPAttributes(),
          request = r,
          toBus = None))

      st ! r
      p.fishForMessage(2 second){
        case x: Response =>  true
      }
    }
    "Expect SPTalker to fill model" in {
      val p = TestProbe()
      val r = Request("reply", SPAttributes("core"-> ServiceHandlerAttributes(Some(id), true, false, List())), List())
      val testA = system.actorOf(Props(classOf[testService], o))
      val st = system.actorOf(
        ServiceTalker.props(
          service = testA,
          modelHandler = testA,
          replyTo = p.ref,
          serviceAttributes = SPAttributes(),
          request = r,
          toBus = None))

      st ! r
      p.expectMsg(1600 millis, Response(List(o), r.attributes, r.service, r.reqID))
    }

    "Fail and die if model is wrong" in {
      val p = TestProbe()
      val empty = TestProbe()
      val dead = TestProbe()
      val r = Request("reply", SPAttributes("core"-> ServiceHandlerAttributes(Some(id), true, false, List())), List())
      val testA = system.actorOf(Props(classOf[testService], o))
      val st = system.actorOf(
        ServiceTalker.props(
          service = testA,
          modelHandler = empty.ref,
          replyTo = p.ref,
          serviceAttributes = SPAttributes(),
          request = r,
          toBus = None))

      dead watch st
      st ! r
      p.expectMsgPF(3 seconds){
        case SPErrorString(s) => true
      }
      dead.expectTerminated(st, 3 seconds)
    }


    "Expect timeout and SPError if service not responding" in {
      val p = TestProbe()
      val empty = TestProbe()
      val r = Request("delay", SPAttributes(), List())
      val testA = system.actorOf(Props(classOf[testService], o))
      val st = system.actorOf(
        ServiceTalker.props(
          service = empty.ref,
          modelHandler = p.ref,
          replyTo = p.ref,
          serviceAttributes = SPAttributes(),
          request = r,
          toBus = None))

      st ! r
      p.expectMsgPF(4 seconds){
        case SPErrorString(s) => println(s)
      }
    }

  }

}



class testServiceSH(o: IDAble) extends Actor {
  import context.dispatcher
  def receive = {
    case GetIds(_, Nil) => sender() ! SPIDs(List(o))
    case Request("reply", a, xs, id) => {
      sender() ! Response(xs, a, "reply", id)
    }
    case Request("delay", a, xs, id) => {
      val reply = sender()
      context.system.scheduler.scheduleOnce(500 milliseconds, reply, Progress(SPAttributes(), "delay", id))
      context.system.scheduler.scheduleOnce(1500 milliseconds, reply, Response(List(), SPAttributes(), "delay", id))
    }
  }
}