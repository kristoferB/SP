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
      |akka.loglevel = "DEBUG"
      |akka.actor.provider = "akka.cluster.ClusterActorRefProvider"
      |akka.remote.netty.tcp.hostname="127.0.0.1"
      |akka.remote.netty.hostname.port=2551
      |akka.cluster.seed-nodes=["akka.tcp://SP@127.0.0.1:2551"]
    """.stripMargin)))


  import akka.cluster.pubsub.DistributedPubSubMediator._
  val mediator = DistributedPubSub(system).mediator
  val id = ID.newID



  override def beforeAll: Unit = {
    val mh = system.actorOf(ModelActor.props(id), id.toString)
  }

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  val api = ModelCommandAPI
  implicit val format = ModelCommandAPI.formats

  "Model maker api should" - {
    "Update attributes and reply"  in {
      val probe = TestProbe()
      mediator ! Subscribe("modelevents", probe.ref)
      mediator ! Publish("modelmessages", CreateModel("kalle", None, None))
      mediator ! Publish("modelmessages", api.write(
        PutAttributes(id, Some("ja"), Some(SPAttributes("attr"->0)))
      ))

      probe.fishForMessage(3 second){
        case s: SubscribeAck => false
        case x: String =>
          println("fish got: "+x)
          api.read(x).isSuccess && api.read(x).get.isInstanceOf[AttributesChanged]
      }
    }

    "reply to status request on eventbus"in {
      val chatMember1 = TestProbe()
      mediator ! Subscribe("modelevents", chatMember1.ref)
      mediator ! Publish("modelmessages", CreateModel("kalle", None, None))
      mediator ! Publish("modelmessages", ModelCommandAPI.write(StatusRequest()))

      chatMember1.fishForMessage(1 second){
        case s: SubscribeAck => false
        case x: String =>
          println("fish event got: "+x)
          api.readSPMessage(x).isSuccess && api.readSPMessage(x).get.isInstanceOf[StatusResponse]
      }
    }


  }





}


