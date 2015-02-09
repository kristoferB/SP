package sp.system

import akka.actor._
import akka.testkit.{ImplicitSender, TestKit, TestActorRef}
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import sp.domain._
import sp.system.messages._

/**
 * Created by kristofer on 09/01/15.
 */
class ServiceHandlerTest extends TestKit(ActorSystem("test")) with ImplicitSender
      with WordSpecLike with Matchers with BeforeAndAfterAll {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  class ServiceHelperImpl(modelHandler: ActorRef) extends ServiceHelper(modelHandler) {

    val interface: Map[String, SPAttributeValue] = Map("model"->DefinitionPrimitive("ID"))
    val fetchedFromModel: List[String] = List()


    def request(request: ServiceRequest): Unit = {
      println(s"REQUEST: $request")
    }
  }

  class ModelTemp extends Actor {
    def receive = {
      case m @ _ => println(s"modelTemp got: $m")
    }
  }


  "The Service Helper" must {
    val mTemp = system.actorOf(Props(classOf[ModelTemp], this), "mTemp")
    val p = Props(classOf[ServiceHelperImpl], this, mTemp)
    val actorRef = TestActorRef(p)
    val actor = actorRef.underlyingActor

    val service = system.actorOf(Props(classOf[ServiceHelperImpl], this, mTemp),"service")

    "when parsing attributes" in {

    }

    "ping" in {
      service ! "hej"
      expectMsg("då")
      service ! Request("", Attr(
        "model" -> ""
      ))
      expectMsg("då")
    }
  }
}
