package sp.service

import akka.actor._
import akka.testkit._
import com.typesafe.config._
import org.scalatest._
import sp.domain.Logic._
import sp.domain._
import sp.messages._
import Pickles._
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Publish, Put, Subscribe}
import scala.concurrent.duration._
import sp.runners.{API_OperationRunner => api}


/**
  * Created by kristofer on 2016-05-04.
  */
class ServiceHandlerTest(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
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

  val p = TestProbe()
  val e = TestProbe()
  //val sh = system.actorOf(OperatorService.props(p.ref), "OperatorService")

  override def beforeAll: Unit = {

  }

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }


  "service handler logic" - {
    val l = new ServiceHandlerLogic{}

    val s1 = APISP.StatusResponse(SPAttributes(
      "service" -> "s1",
      "instanceID" -> ID.newID,
      "tags" -> List("test", "1")
    ))
    val s2 = APISP.StatusResponse(SPAttributes(
      "service" -> "s2",
      "instanceID" -> ID.newID,
      "tags" -> List("test", "2")
    ))




    }



//  "Ability Handler " - {
//    val handlerID = ID.newID
//    val vdID = ID.new
//    val rID = ID.newID
//    val h = SPHeader(from = "test", to = handlerID.toString, reply = "test")
//
//    "create" in {
//      var mh: ActorRef = system.actorOf(AbilityHandler.props("kalle", handlerID, vdID))
//      val probeAnswers = TestProbe()
//
//      mediator ! Subscribe("answers", probeAnswers.ref)
//      mediator ! Subscribe("events", probeAnswers.ref)
//      mediator ! Subscribe("services", probeAnswers.ref)
//      mediator ! Subscribe("spevents", probeAnswers.ref)
//
//      val rID = ID.newID
//      val stateUpd = SPMessage.makeJson(h.copy(from = vdID.toString, reply = vdID), vdAPI.StateEvent("r", rID, Map(v1.id -> 1)))
//      val mess = SPMessage.makeJson(h.copy(reqID = rID), api.SetUpAbility(ability))
//      mediator ! Publish("events", stateUpd)
//      Thread.sleep(100)
//      mediator ! Publish("services", mess)
//
//      val start = SPMessage.makeJson(h, api.StartAbility(ability.id))
//      mediator ! Publish("services", start)
//
//      mediator ! Publish("events", SPMessage.makeJson(h.copy(from = vdID.toString, reply = vdID), vdAPI.StateEvent("r", rID, Map(v1.id -> 2))))
//      mediator ! Publish("events", SPMessage.makeJson(h.copy(from = vdID.toString, reply = vdID), vdAPI.StateEvent("r", rID, Map(v1.id -> 3))))
//      mediator ! Publish("events", SPMessage.makeJson(h.copy(from = vdID.toString, reply = vdID), vdAPI.StateEvent("r", rID, Map(v1.id -> 4))))
//      mediator ! Publish("events", SPMessage.makeJson(h.copy(from = vdID.toString, reply = vdID), vdAPI.StateEvent("r", rID, Map(v1.id -> 1))))
//
//
//      probeAnswers.fishForMessage(1 second){
//        case x =>
//          println("-------: ")
//          println(x)
//          println("-------: ")
//
//          false
//      }
//    }
//
//
//  }


}
