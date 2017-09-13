package sp.models

import akka.actor._
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import akka.cluster.pubsub._
import akka.testkit._
import com.typesafe.config._
import org.scalatest._
import sp.domain.Logic._
import sp.domain._
import sp.models.{APIModel => api}

import scala.concurrent.duration._


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
  import DistributedPubSubMediator.Subscribe
  val mediator: ActorRef = DistributedPubSub(system).mediator

  val mh = system.actorOf(ModelMaker.props(ModelActor.props), "modelHandler")
  val p = TestProbe()
  mediator ! Subscribe(APISP.answers, p.ref)
  mediator ! Subscribe(APISP.spevents, p.ref)

  override def beforeAll: Unit = {

  }

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }




  "Communicating with the model actors" in {
    val x1 = APIModelMaker.CreateModel("hej")
    val x2 = APIModelMaker.CreateModel("då")
    val h = SPHeader(from = "test", to = APIModelMaker.service, reply = SPValue("test"))

    mediator ! Publish(APISP.services, SPMessage.makeJson(h, x1))
    mediator ! Publish(APISP.services, SPMessage.makeJson(h, x2))

    var go1 = true
    var modelInfoMess = false
    var modelModelUpd = false
    var modelMess = false
    var modelItemsMess = false
    var modelItemMess = false

    p.fishForMessage(3 seconds) {
      case m: String =>
        //println("create got reply: "+m)
        val aMess = SPMessage.fromJson(m).toOption
        val aH = aMess.flatMap(_.getHeaderAs[SPHeader].toOption)

        for {
          mess <- aMess
          h <- aH
        } yield {
          println("ModelActorTesting got")
          println(s"from: ${h.from}")
          val bodyType = mess.body.get("_type").orElse(mess.body.get("isa"))
          println(s"got a $bodyType message")
        }


        val o = Operation("Kalle")
        for {
          mess <- SPMessage.fromJson(m).toOption
          h <- mess.getHeaderAs[SPHeader].toOption if h.from == x1.id.toString && go1
        } yield {
          go1 = false
          val h2 = SPHeader(from = "test", to = x1.id.toString, reply = SPValue("test"))
          mediator ! Publish(APISP.services, SPMessage.makeJson(h2, api.PutItems(List(o, Operation("Kalle2")))))
          mediator ! Publish(APISP.services, SPMessage.makeJson(h2, api.PutItems(List(Operation("Kalle2")))))
        }



        modelCreatedMess && modelListMess && modelDeleted
    }
  }


//  val cm = APIModelMaker.CreateModel("hej")
//  "Model actor" - {
//    val ma = system.actorOf(ModelActor.props(cm))
//
//    "create and initial model" in {
//      println("STARTING")
//      send(api.GetModel)
//      println("Sent GetModel from TEST")
//      val o = Operation("o1")
//      send(api.PutItems(List(o, Thing("t1"))))
//      send(api.DeleteItems(List(o.id)))
//      println("Sending some more")
//      p.fishForMessage(3 seconds) {
//        case m: String =>
//          println("fishing got reply: "+m)
//          m.contains("APISP.SPDone")
//          false
//      }
//
//
//    }
//  }



//def send(mess: api.Request) = {
//  val h = SPHeader(from = "test", to = cm.id.toString, reply = SPValue("test"))
//  mediator ! Publish(APISP.services, SPMessage.makeJson(h, mess))
//}


}



