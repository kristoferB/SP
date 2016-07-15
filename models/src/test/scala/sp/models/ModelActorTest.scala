package sp.models

import akka.actor._
import akka.cluster.pubsub._
import akka.testkit._
import com.typesafe.config._
import org.scalatest._
import sp.domain._
import sp.messages._

import scala.concurrent.duration._


/**
 * Testing ModelCommands
 */
class ModelCommandTest(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
  with FreeSpecLike with Matchers with BeforeAndAfterAll {

  def this() = this(ActorSystem("SP", ConfigFactory.parseString(
    """
      |akka.persistence.journal.plugin = "akka.persistence.journal.inmem"
      |akka.persistence.snapshot-store.plugin = "akka.persistence.snapshot-store.local"
      |akka.persistence.snapshot-store.local.dir = "target/snapshotstest/"
      |akka.loglevel = "OFF"
      |akka.actor.provider = "akka.cluster.ClusterActorRefProvider"
      |akka.remote.netty.tcp.hostname="127.0.0.1"
      |akka.remote.netty.hostname.port=2551
      |akka.cluster.seed-nodes=["akka.tcp://SP@127.0.0.1:2551"]
    """.stripMargin)))


  import akka.cluster.pubsub.DistributedPubSubMediator._
  val mediator = DistributedPubSub(system).mediator


  override def beforeAll: Unit = {
    val id = ID.newID
    val mh = system.actorOf(ModelActor.props(ID.newID), id.toString)
  }

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  implicit val format = ModelCommandAPI.formats

  "Model maker api should" - {
    "reply to status request on eventbus" in {

      val chatMember1 = TestProbe()
      mediator ! Subscribe("modelevents", chatMember1.ref)

      mediator ! Publish("modelmessages", CreateModel("kalle", None, None))

      within(200 millis) {}

      mediator ! Publish("modelmessages", ModelCommandAPI.write(StatusRequest()))

      chatMember1.fishForMessage(1 second){
        case s: SubscribeAck => false
        case x: String => println("fishing got: "+x); true
      }


    }


  }





}


