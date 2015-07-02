package sp.system

import akka.actor._
import akka.testkit._
import com.typesafe.config._
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import sp.domain._
import sp.system.messages._
import sp.domain.Logic._

import scala.concurrent.duration._

/**
 * Created by Kristofer on 2014-06-17.
 */
class ServiceHandlerTest(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
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

  "The Service handler" must {

    "Add the service" in {
        val p = TestProbe()
        val serviceDef = SPAttributes()
        val sh = system.actorOf(ServiceHandler.props(p.ref))
        sh ! RegisterService("hej", p.ref, serviceDef)
        p.expectMsgPF(100 milliseconds){case r: RegisterService => println(r)}
    }
    "Remove the service" in {
        val p = TestProbe()
        val serviceDef = SPAttributes()
        val sh = system.actorOf(ServiceHandler.props(p.ref))
        sh ! RegisterService("hej", p.ref, serviceDef)
        sh ! RemoveService("hej")
        sh ! Request("hej", SPAttributes())

        p.fishForMessage(100 milliseconds){
          case r: RegisterService => false
          case r: RemoveService => true
        }

        fishForMessage(100 milliseconds){
          case r: RegisterService => false
          case r: RemoveService => false
          case SPErrorString(s) =>  true
        }
    }
    "Validate request" in {
      val definition = SPAttributes("key1" -> KeyDefinition("kk", List(), None),
        "key2" -> SPAttributes("key3" -> KeyDefinition("kk", List(), None)))
      val attr = SPAttributes("key1" -> "hej", "key3" -> "kalle")
      val p = TestProbe()
      val sh = system.actorOf(ServiceHandler.props(p.ref))
      sh ! RegisterService("hej", p.ref, definition)
      sh ! Request("hej", attr)

      p.fishForMessage(100 milliseconds){
        case r: RegisterService => false
        case r: Request => true
      }
    }
    "Validate request and fail" in {
      val definition = SPAttributes("key1" -> KeyDefinition("kk", List(), None),
        "key2" -> SPAttributes("key3" -> KeyDefinition("kk", List(), None)))
      val attr = SPAttributes("key1" -> "hej")
      val p = TestProbe()
      val sh = system.actorOf(ServiceHandler.props(p.ref))
      sh ! RegisterService("hej", p.ref, definition)
      sh ! Request("hej", attr)

      p.expectMsgPF(100 milliseconds){
        case r: RegisterService => "yes"
      }

      fishForMessage(100 milliseconds){
        case r: RegisterService => false
        case r: SPErrors => println(r); true
      }
    }
  }
}


class testService(o: IDAble) extends Actor {
  import context.dispatcher
  def receive = {
    case GetIds(_, Nil) => sender() ! SPIDs(List(o))
    case Request("reply", a, xs) => {
      sender() ! Response(xs, a)
    }
    case Request("delay", a, xs) => {
      val reply = sender()
      context.system.scheduler.scheduleOnce(500 milliseconds, reply, Progress(SPAttributes()))
      context.system.scheduler.scheduleOnce(1500 milliseconds, reply, Response(List(), SPAttributes()))
    }
  }
}