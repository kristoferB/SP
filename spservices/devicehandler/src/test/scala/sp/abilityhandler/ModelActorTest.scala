package sp.abilityhandler

import akka.actor._
import scala.concurrent._
import scala.concurrent.duration._
import akka.cluster.pubsub._
import akka.testkit._
import com.typesafe.config._
import org.scalatest._
import sp.domain._
import sp.domain.Logic._

import sp.abilityhandler.{APIAbilityHandler => api}


/**
 * Testing AbilityActor
 */
class AbilityActorTest(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
  with FreeSpecLike with Matchers with BeforeAndAfterAll {

  def this() = this(ActorSystem("SP", ConfigFactory.parseString(
    """
      |akka.persistence.journal.plugin = "akka.persistence.journal.inmem"
      |akka.persistence.snapshot-store.plugin = "akka.persistence.snapshot-store.local"
      |akka.persistence.snapshot-store.local.dir = "target/snapshotstest/"
      |akka.loglevel = "INFO"
      |akka.actor.provider = "akka.cluster.ClusterActorRefProvider"
      |akka.remote.netty.tcp.hostname="127.0.0.1"
      |akka.remote.netty.hostname.port=2551
      |akka.cluster.seed-nodes=["akka.tcp://SP@127.0.0.1:2551"]
    """.stripMargin)))
  val mediator = DistributedPubSub(system).mediator
  val id = ID.newID

  val v1 = Thing("v1")
  val pre = PropositionCondition(EQ(v1.id, 1), List(Action(v1.id, ValueHolder(2))))
  val post = PropositionCondition(EQ(v1.id, 3), List(Action(v1.id, ValueHolder(4))))
  val started = PropositionCondition(EQ(v1.id, 2), List())
  val reset = PropositionCondition(AlwaysTrue, List(Action(v1.id, ValueHolder(1))))
  val ability = api.Ability("test", ID.newID, pre, started, post, reset)

  var mh: ActorRef = system.actorOf(AbilityActor.props(ability), id.toString)
  override def beforeAll: Unit = {

  }

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }



  "Ability is created" - {
    "get ids" in {
      val probe = TestProbe()
      mh.tell(GetIds, probe.ref)


      probe.fishForMessage(1 second){
        case x => println("HEJ: " + x); false
      }
    }
  }

//  "Model maker api should" - {
//    "Create a model"  in {
//      val probe = TestProbe()
//      mediator ! Subscribe("modelevents", probe.ref)
//      mediator ! Publish("modelmessages", ModelMakerAPI.write(CreateModel("kalle", None, None)))
//
//      probe.fishForMessage(3 second){
//        case s: SubscribeAck => false
//        case x: String =>
//          println("fish got: "+x)
//          api.read(x).isSuccess && api.read(x).get.isInstanceOf[ModelCreated]
//      }
//    }
//
//
//
//  }





}


