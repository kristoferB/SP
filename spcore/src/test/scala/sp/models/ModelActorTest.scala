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
class ModelActorTest(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
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
    var modelUpd = false
    var modelMess = false
    var modelItemsMess = false
    var modelItemMess = false

    p.fishForMessage(3 seconds) {
      case m: String =>
//        //println("create got reply: "+m)
//        val aMess = SPMessage.fromJson(m)
//        val aH = aMess.flatMap(_.getHeaderAs[SPHeader])
//
//        for {
//          mess <- aMess
//          h <- aH
//        } yield {
//          println("ModelActorTesting got")
//          println(s"from: ${h.from}")
//          val bodyType = mess.body.get("_type").orElse(mess.body.get("isa"))
//          println(s"got a $bodyType message")
//        }


        val o = Operation("Op1")
        for {
          mess <- SPMessage.fromJson(m)
          h <- mess.getHeaderAs[SPHeader] if h.from == x1.id.toString && go1
        } yield {
          go1 = false
          val h2 = SPHeader(from = "test", to = x1.id.toString, reply = SPValue("test"))
          mediator ! Publish(api.topicRequest, SPMessage.makeJson(h2, api.PutItems(List(o, Operation("Kalle2")))))
          mediator ! Publish(api.topicRequest, SPMessage.makeJson(h2, api.ExportModel))
          mediator ! Publish(api.topicRequest, SPMessage.makeJson(h2, api.GetItemList(0, 1)))
          mediator ! Publish(api.topicRequest, SPMessage.makeJson(h2, api.GetItemList(filter = api.ItemFilter(regexName = ".*ka.*"))))
          mediator ! Publish(api.topicRequest, SPMessage.makeJson(h2, api.GetItem(o.id)))
          mediator ! Publish(api.topicRequest, SPMessage.makeJson(h2, api.DeleteItems(List(o.id))))
          mediator ! Publish(api.topicRequest, SPMessage.makeJson(h2, api.GetItem(o.id)))
        }

        for {
          mess <- SPMessage.fromJson(m)
          h <- mess.getHeaderAs[SPHeader] if h.from == x1.id.toString && !go1
          b <- mess.getBodyAs[api.Response]
        } yield {
          println("ModelActorTesting got")
          println(s"from: ${h.from}")
          val bodyType = mess.body.get("_type").orElse(mess.body.get("isa"))
          println(s"got a $bodyType message")
          b match {
            case x: api.ModelToExport => modelMess = true
            case x: api.ModelInformation => modelInfoMess = true
            case x: api.ModelUpdate => modelUpd = true
            case x: api.SPItem => modelItemMess = true
            case x: api.SPItems =>
              println(x)

              modelItemsMess = true
            case _ =>
          }
        }

        for {
          mess <- SPMessage.fromJson(m)
          h <- mess.getHeaderAs[SPHeader] if h.from == x1.id.toString && !go1
          b <- mess.getBodyAs[APISP]
        } yield {
          b match {
            case x: APISP.SPError =>
              println("go an error response in model actor test, which is good")
              println(x)
            case _ =>
          }
        }



        modelInfoMess && modelItemMess && modelItemsMess && modelMess && modelUpd
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



