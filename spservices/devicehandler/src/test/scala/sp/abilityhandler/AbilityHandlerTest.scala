package sp.abilityhandler

import akka.actor._
import akka.cluster.pubsub._
import akka.testkit._
import com.typesafe.config._
import org.scalatest._
import sp.abilityhandler.{APIAbilityHandler => api}
import sp.domain.Logic._
import sp.domain._
import sp.messages._
import Pickles._
import akka.cluster.pubsub.DistributedPubSubMediator.{Publish, Put, Subscribe}
import scala.concurrent.duration._


/**
 * Testing AbilityActor
 */
class AbilityHandlerTest(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
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


  import sp.abilityhandler.{APIAbilityHandler => api}
  import sp.abilityhandler.AbilityState._



  "Ability Handler " - {
    val handlerID = ID.newID
    val vdID = ID.newID
    val h = SPHeader(from = "test", to = "kalle", toID = Some(handlerID), replyTo = "test")

    "create" in {
      var mh: ActorRef = system.actorOf(AbilityHandler.props("kalle", handlerID, vdID))
      val probeAnswers = TestProbe()
      val probeEvents = TestProbe()
      val probeServices = TestProbe()
      mediator ! Subscribe("answers", probeAnswers.ref)
      mediator ! Subscribe("events", probeEvents.ref)
      mediator ! Subscribe("services", probeServices.ref)

      val rID = ID.newID
      val mess = SPMessage.makeJson(h.copy(reqID = rID), api.SetUpAbility(ability))
      mh.tell(mess, probeAnswers.ref)





      probeAnswers.fishForMessage(1 second){
        case x => println("answers: " +x); false
      }
      probeEvents.fishForMessage(1 second){
        case x => println("answers: " +x); false
      }
      probeServices.fishForMessage(1 second){
        case x => println("answers: " +x); false
      }
    }


  }







}


