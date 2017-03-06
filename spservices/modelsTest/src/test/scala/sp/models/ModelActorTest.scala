//package sp.models
//
//import akka.actor._
//import akka.cluster.pubsub._
//import akka.testkit._
//import com.typesafe.config._
//import org.scalatest._
//import sp.domain._
//import sp.messages._
//
//import scala.concurrent.duration._
//
//
///**
// * Testing ModelCommands
// */
//class ModelActorTest(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
//  with FreeSpecLike with Matchers with BeforeAndAfterAll {
//
//  def this() = this(ActorSystem("SP", ConfigFactory.parseString(
//    """
//      |akka.persistence.journal.plugin = "akka.persistence.journal.inmem"
//      |akka.persistence.snapshot-store.plugin = "akka.persistence.snapshot-store.local"
//      |akka.persistence.snapshot-store.local.dir = "target/snapshotstest/"
//      |akka.loglevel = "INFO"
//      |akka.actor.provider = "akka.cluster.ClusterActorRefProvider"
//      |akka.remote.netty.tcp.hostname="127.0.0.1"
//      |akka.remote.netty.hostname.port=2551
//      |akka.cluster.seed-nodes=["akka.tcp://SP@127.0.0.1:2551"]
//    """.stripMargin)))
//
//
//  import akka.cluster.pubsub.DistributedPubSubMediator._
//  val mediator = DistributedPubSub(system).mediator
//  val id = ID.newID
//
//
//
//  override def beforeAll: Unit = {
//    val mh = system.actorOf(ModelActor.props(id), id.toString)
//    val maker = system.actorOf(ModelMaker.props(ModelActor.props), "modelMaker")
//  }
//
//  override def afterAll {
//    TestKit.shutdownActorSystem(system)
//  }
//
//  val api = ModelMessagesAPI
//  implicit val format = ModelMessagesAPI.formats join ModelMakerAPI.formats
//
//  import sp.models.ModelMakerAPI._
//  import ModelMessagesAPI._
//
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
//    "Update attributes and reply"  in {
//      val probe = TestProbe()
//      mediator ! Subscribe("modelevents", probe.ref)
//      mediator ! Publish("modelmessages", CreateModel("kalle", None, None))
//      mediator ! Publish("modelmessages", api.write(
//        PutAttributes(id, Some("ja"), Some(SPAttributes("attr"->0)))
//      ))
//
//      probe.fishForMessage(3 second){
//        case s: SubscribeAck => false
//        case x: String =>
//          println("fish got: "+x)
//          api.read(x).isSuccess && api.read(x).get.isInstanceOf[AttributesChanged]
//      }
//    }
//
//
//    "Not update attributes when not your model id"  in {
//      val probe = TestProbe()
//      mediator ! Subscribe("modelevents", probe.ref)
//      mediator ! Publish("modelmessages", CreateModel("kalle", None, None))
//      mediator ! Publish("modelmessages", api.write(
//        PutAttributes(ID.newID, Some("ja"), Some(SPAttributes("attr"->0)))
//      ))
//
//      probe.expectMsgType[String]
//
//      probe.fishForMessage(3 second){
//        case s: SubscribeAck => false
//        case x: String =>
//          println("fish got: "+x)
//          api.read(x).isSuccess && api.read(x).get.isInstanceOf[AttributesChanged]
//      }
//    }
//
//    "Update items and reply"  in {
//      val probe = TestProbe()
//      mediator ! Subscribe("modelevents", probe.ref)
//      mediator ! Publish("modelmessages", CreateModel("kalle", None, None))
//      mediator ! Publish("modelmessages", api.write(
//        PutItems(id, List(Operation("hej")), SPAttributes("info"->"added op"))
//      ))
//
//      probe.fishForMessage(3 second){
//        case s: SubscribeAck => false
//        case x: String =>
//          println("fish got: "+x)
//          api.read(x).isSuccess && api.read(x).get.isInstanceOf[ItemsChanged]
//      }
//    }
//
//    "delete items and reply"  in {
//      val probe = TestProbe()
//      mediator ! Subscribe("modelevents", probe.ref)
//      mediator ! Publish("modelmessages", CreateModel("kalle", None, None))
//      val o = Operation("hej")
//      mediator ! Publish("modelmessages", api.write(
//        PutItems(id, List(o), SPAttributes("info"->"added op"))
//      ))
//      mediator ! Publish("modelmessages", api.write(
//        DeleteItems(id, List(o.id), SPAttributes("info"->"deleted op"))
//      ))
//
//      probe.fishForMessage(3 second){
//        case s: SubscribeAck => false
//        case x: String =>
//          println("fish got: "+x)
//          api.read(x).isSuccess && api.read(x).get.isInstanceOf[ItemsDeleted]
//      }
//    }
//
//    "reply to status request on eventbus"in {
//      val chatMember1 = TestProbe()
//      mediator ! Subscribe("modelevents", chatMember1.ref)
//      mediator ! Publish("modelmessages", CreateModel("kalle", None, None))
//      mediator ! Publish("modelmessages", ModelMessagesAPI.write(StatusRequest()))
//
//      chatMember1.fishForMessage(1 second){
//        case s: SubscribeAck => false
//        case x: String =>
//          println("fish event got: "+x)
//          api.readSPMessage(x).isSuccess && api.readSPMessage(x).get.isInstanceOf[StatusResponse]
//      }
//    }
//
//
//  }
//
//
//
//
//
//}
//
//
