package sp.models

import akka.actor._
import akka.testkit._
import com.typesafe.config._
import org.scalatest._
import sp.domain._
import Logic._
import scala.util._
import akka.cluster.pubsub._
import DistributedPubSubMediator.{Publish, Subscribe}


import scala.concurrent.duration._

import sp.models.{APIModel => api}



/**
 * Testing ModelMakers
 */
class ModelMakerTest(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
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


  import akka.cluster.pubsub._
  import DistributedPubSubMediator.{ Put, Subscribe }
  val mediator = DistributedPubSub(system).mediator

  val mh = system.actorOf(ModelMaker.props(MockMaker.props), "modelHandler")
  val p = TestProbe()
  mediator ! Subscribe(APISP.answers, p.ref)
  mediator ! Subscribe(APISP.spevents, p.ref)

  override def beforeAll: Unit = {

  }

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }




//  "create Model and SPOK" in {
//    val x = api.CreateModel("hej")
//    val h = SPHeader(from = "test", to = api.attributes.service, reply = SPValue("test"))
//    mediator ! Publish(APISP.services, SPMessage.makeJson(h, x))
//
//      p.fishForMessage(1 seconds) {
//        case m: String =>
//          println("create got reply: "+m)
//          false
//      }
//    }


  val cm = api.CreateModel("hej")
  "Model actor" - {
    val ma = system.actorOf(ModelActor.props(cm))

    "create and initial model" in {
      val h = SPHeader(from = "test", to = api.service, reply = SPValue("test"))
      send(api.GetModel())
      val o = Operation("o1")
      send(api.PutItems(List(o, Thing("t1"))))
      send(api.DeleteItems(List(o.id)))
      p.fishForMessage(3 seconds) {
        case m: String =>
          println("fishing got reply: "+m)
          m.contains("APISP.SPDone")
          false
      }


    }
  }



def send(mess: api.Request) = {
  val h = SPHeader(from = "test", to = cm.id.toString, reply = SPValue("test"))
  mediator ! Publish(APISP.services, SPMessage.makeJson(h, mess))
}


}

class MockMaker(cm: api.CreateModel) extends Actor {

  def receive = {
    case x => println("sp.models.MockMaker got : "+ x)
  }
}

object MockMaker {
  def props(cm : api.CreateModel) = Props(classOf[MockMaker], cm)
}

