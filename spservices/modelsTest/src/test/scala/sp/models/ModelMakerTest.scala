//package sp.models
//
//import akka.actor._
//import akka.testkit._
//import com.typesafe.config._
//import org.scalatest._
//import sp.domain._
//import sp.messages._
//
//import scala.util._
//import akka.cluster.pubsub._
//import DistributedPubSubMediator.{Publish, Subscribe}
//import org.json4s.ShortTypeHints
//
//import scala.concurrent.duration._
//
//
//
//
///**
// * Testing ModelMakers
// */
//class ModelMakerTest(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
//  with FreeSpecLike with Matchers with BeforeAndAfterAll {
//
//  def this() = this(ActorSystem("SP", ConfigFactory.parseString(
//    """
//      |akka.persistence.journal.plugin = "akka.persistence.journal.inmem"
//      |akka.persistence.snapshot-store.plugin = "akka.persistence.snapshot-store.local"
//      |akka.persistence.snapshot-store.local.dir = "target/snapshotstest/"
//      |akka.loglevel = "OFF"
//      |akka.actor.provider = "akka.cluster.ClusterActorRefProvider"
//      |akka.remote.netty.tcp.hostname="127.0.0.1"
//      |akka.remote.netty.hostname.port=2551
//      |akka.cluster.seed-nodes=["akka.tcp://SP@127.0.0.1:2551"]
//    """.stripMargin)))
//
//
//
//  val mh = system.actorOf(ModelMaker.props(MockMaker.props), "modelHandler")
//  val mm = system.actorOf(MockMaker.props(ID.newID), "mockMakerSub")
//
//
//  override def beforeAll: Unit = {
//    //mh ! CreateModel(mid, "generalModel")
//    //mh ! UpdateIDs(mid, List(o))
//  }
//
//  override def afterAll {
//    TestKit.shutdownActorSystem(system)
//  }
//
//  implicit val format = ModelMakerAPI.formats
//
//  "Model maker api should" - {
//    "reject illegal messages" in {
//      val jsonCM = """{"name":"hej","attributes":{"attr":"hej"}}"""
//
//      mh ! jsonCM
//      fishForMessage(0.5 seconds) {
//        case m: String =>
//          println("got reply: "+m)
//          val se = ModelMakerAPI.readSPMessage(m).get
//          se.isInstanceOf[SPError]
//      }
//    }
//    "reject illegal messages 2" in {
//      val jsonCM = """{"isa":"CreateModel","attributes":{"attr":"hej"}}"""
//
//      mh ! jsonCM
//      fishForMessage(0.5 seconds) {
//        case m: String =>
//          println("2 got reply: "+m)
//          val se = ModelMakerAPI.readSPMessage(m).get
//          se.isInstanceOf[SPError]
//      }
//    }
//    "create Model and SPOK" in {
//      val jsonCM = """{"isa":"CreateModel","name":"hej"}"""
//
//      mh ! jsonCM
//      fishForMessage(0.5 seconds) {
//        case m: String =>
//          println("create got reply: "+m)
//          val se = ModelMakerAPI.readSPMessage(m).get
//          se.isInstanceOf[SPOK]
//      }
//    }
//    "create and delete Model" in {
//      val id = ID.newID
//      val cm = ModelMakerAPI.CreateModel("hej", None, Some(id))
//      val dm = ModelMakerAPI.DeleteModel(id)
//
//      val mediator = DistributedPubSub(system).mediator
////      val chatMember1 = TestProbe()
////      mediator ! Subscribe("modelevents", chatMember1.ref)
////
////      chatMember1.fishForMessage(1 second){
////        case x => println("fishing got: "+x); true
////      }
//
//
//      mediator ! Publish("modelmessages", ModelMakerAPI.write(cm))
//      mediator ! Publish("modelmessages", ModelMakerAPI.write(StatusRequest()))
//      mediator ! Publish("modelmessages", dm)
//
//      //mh ! ModelMakerAPI.write(cm)
//      //mh ! ModelMakerAPI.write(dm)
//
//
////      chatMember1.fishForMessage(1 second){
////        case x => println("fishing got: "+x); true
////      }
//
//      fishForMessage(3 seconds) {
//        case m: String =>
//          println("create got reply: "+m)
//          val se = ModelMakerAPI.readSPMessage(m).get
//          se.isInstanceOf[SPOK]
//          true
//      }
//    }
//  }
//
//
//
//
//
//}
//
//class MockMaker(id: ID) extends Actor {
//
//  val mediator = DistributedPubSub(context.system).mediator
//  mediator ! Subscribe("modelevents", self)
//  def receive = {
//    case x => println("sp.models.MockMaker got : "+ x)
//  }
//}
//
//object MockMaker {
//  def props(maker: ID) = Props(classOf[MockMaker], maker)
//}
//
