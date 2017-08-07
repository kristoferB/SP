package sp.abilityhandler

import akka.actor._
import akka.cluster.pubsub._
import akka.testkit._
import com.typesafe.config._
import org.scalatest._
import sp.abilityhandler.{APIAbilityHandler => api}
import sp.domain._
import sp.domain.Logic._
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
  val pre = Condition(EQ(v1.id, 1), List(Action(v1.id, ValueHolder(2))))
  val post = Condition(EQ(v1.id, 3), List(Action(v1.id, ValueHolder(4))))
  val started = Condition(EQ(v1.id, 2), List())
  val reset = Condition(AlwaysTrue, List(Action(v1.id, ValueHolder(1))))
  val ability = api.Ability("test", id, pre, started, post, reset)


  override def beforeAll: Unit = {

  }

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }


  import sp.abilityhandler.{APIAbilityHandler => api}
  import sp.devicehandler.{APIVirtualDevice => vdAPI}

  import sp.abilityhandler.AbilityState._



  "Ability Handler " - {
    val handlerID = ID.newID
    val vdID = ID.newID
    val rID = ID.newID
    val h = SPHeader(from = "test", to = handlerID.toString, reply = "test")

    "create" in {
      var mh: ActorRef = system.actorOf(AbilityHandler.props("kalle", handlerID, vdID))
      val probeAnswers = TestProbe()

      mediator ! Subscribe("answers", probeAnswers.ref)
      mediator ! Subscribe("events", probeAnswers.ref)
      mediator ! Subscribe("services", probeAnswers.ref)
      mediator ! Subscribe("spevents", probeAnswers.ref)

      val rID = ID.newID
      val stateUpd = SPMessage.makeJson(h.copy(from = vdID.toString, reply = vdID.toString), vdAPI.StateEvent("r", rID, Map(v1.id -> 1)))
      val mess = SPMessage.makeJson(h.copy(reqID = rID), api.SetUpAbility(ability))
      mediator ! Publish("events", stateUpd)
      Thread.sleep(100)
      mediator ! Publish("services", mess)

      val start = SPMessage.makeJson(h, api.StartAbility(ability.id))
      mediator ! Publish("services", start)

      mediator ! Publish("events", SPMessage.makeJson(h.copy(from = vdID.toString, reply = vdID.toString), vdAPI.StateEvent("r", rID, Map(v1.id -> 2))))
      mediator ! Publish("events", SPMessage.makeJson(h.copy(from = vdID.toString, reply = vdID.toString), vdAPI.StateEvent("r", rID, Map(v1.id -> 3))))
      mediator ! Publish("events", SPMessage.makeJson(h.copy(from = vdID.toString, reply = vdID.toString), vdAPI.StateEvent("r", rID, Map(v1.id -> 4))))
      mediator ! Publish("events", SPMessage.makeJson(h.copy(from = vdID.toString, reply = vdID.toString), vdAPI.StateEvent("r", rID, Map(v1.id -> 1))))
      mediator ! Publish("events", SPMessage.makeJson(h.copy(from = vdID.toString, reply = vdID.toString), APISP.StatusRequest))


      probeAnswers.fishForMessage(1 second){
        case x =>
          println("-------: ")
          println(x)
          println("-------: ")

          false
      }
    }


  }







}


