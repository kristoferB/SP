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
  val ability = api.Ability("test", id, pre, started, post, reset)


  override def beforeAll: Unit = {

  }

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  import sp.abilityhandler.AbilityState._

  "Ability is created" - {
    "get ids" in {
      var mh: ActorRef = system.actorOf(AbilityActor.props(ability))
      val probe = TestProbe()
      mh.tell(GetIds, probe.ref)

      probe.fishForMessage(1 second){
        case AbilityIds(aid, xs) => aid == id && xs.contains(v1.id)
      }
    }

    "newState" in {
      var mh: ActorRef = system.actorOf(AbilityActor.props(ability))
      val probe = TestProbe()
      mh.tell(NewState(Map(v1.id -> 1)), probe.ref)
      mh.tell(NewState(Map(v1.id -> 2)), probe.ref)

      probe.fishForMessage(1 second){
        case x @ AbilityStateChange(aid, s, cnt, reqID) if aid == id && s == enabled && cnt == 0 => false
        case x @ AbilityStateChange(aid, s, cnt, reqID) if aid == id && s == executing && cnt == 1 => true
      }
    }
    "start" in {
      var mh: ActorRef = system.actorOf(AbilityActor.props(ability))
      val probe = TestProbe()
      val req = ID.newID
      mh.tell(StartAbility(Map(v1.id -> 1), req, Map()), probe.ref)

      probe.fishForMessage(1 second){
        case x @ AbilityStateChange(aid, s, cnt, reqID) if aid == id && s == starting && cnt == 0 => false
        case x @ StateUpdReq(aid, s) if aid == id && s == Map(v1.id -> SPValue(2)) => true
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


