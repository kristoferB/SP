//package sp.server
//
//import akka.actor._
//import akka.cluster.pubsub._
//import akka.testkit._
//import com.typesafe.config._
//import org.scalatest._
//import sp.domain._
//import sp.messages._
//import akka.actor._
//import akka.cluster.pubsub.DistributedPubSubMediator._
//import akka.stream._
//import akka.stream.scaladsl._
//import akka.http.scaladsl.model.ws.{Message, TextMessage}
//import scala.util._
//
//import scala.concurrent.Await
//import scala.concurrent.duration._
//
//
///**
// * Testing ModelCommands
// */
//class WebsocketAPITest(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
//  with FreeSpecLike with Matchers with BeforeAndAfterAll {
//
//  def this() = this(ActorSystem("SP", ConfigFactory.parseString(
//    """
//
//    """.stripMargin)))
//
//
//
//
//  implicit val materializer = ActorMaterializer()
//
//  override def beforeAll: Unit = {
//  }
//
//  override def afterAll {
//    TestKit.shutdownActorSystem(system)
//  }
//
//
//  val messHeader = upickle.default.writeJs(DummyHeader(java.util.UUID.randomUUID()))
//  val messBody = upickle.default.writeJs(DummyBody("hej", "då"))
//  //val mess = UPickleMessage(messHeader, messBody)
//
//
//  "Testing transformers" - {
////    "Message to API" in {
////      val da = system.actorOf(Props(classOf[DummyActor]))
////      val test = new WebsocketHandler(da, "answers")
////
////      val in1 = TextMessage(write(APIWebSocket.Subscribe("requests")))
////      val in2 = TextMessage(APIParser.write(APIWebSocket.Unsubscribe("2")))
////      val in3 = TextMessage(APIParser.write(APIWebSocket.PublishMessage(mess, "services")))
////      val in4 = TextMessage("Hej Hej")
////      val out1 = Success(APIWebSocket.Subscribe("requests"))
////      val out2 = Success(APIWebSocket.Unsubscribe("2"))
////      val out3 = Success(APIWebSocket.PublishMessage(mess, "services"))
////
////      val res = test.transformMessages.runWith(Source(List(in1, in2, in3, in4)), Sink.seq)
////      val result = Await.result(res._2, 1.seconds)
////      result.toList.filter(_.isSuccess) shouldEqual List(out1, out2, out3)
////    }
////
////    "API to Bus Messages and Ack" in {
////      val da = system.actorOf(Props(classOf[DummyActor]))
////      val test = new WebsocketHandler(da, "answers")
////
////      val in1 = Success(APIWebSocket.Subscribe("requests"))
////      val in2 = Success(APIWebSocket.Unsubscribe("requests"))
////      val in3 = Success(APIWebSocket.PublishMessage(mess, "services"))
////
////      val out1 = test.MessageAndAck(None, APIWebSocket.SPACK("Subscribing to topic requests"))
////      val out2 = test.MessageAndAck(None, APIWebSocket.SPACK("Unsubscribing from topic requests"))
////      val out3 = test.MessageAndAck(Some(Publish("services", APIParser.write(mess))), APIWebSocket.SPACK("Message sent to topic services"))
////
////
////
////      val res = test.matchWebSocketMessages.runWith(Source(List(in1, in2, in3)), Sink.seq)
////      val result = Await.result(res._2, 1.seconds)
////      result.toList shouldEqual List(out1, out2, out3)
////    }
//
////    "API to Bus Messages and Ack" in {
////      val da = system.actorOf(Props(classOf[DummyActor]))
////      val test = new WebsocketHandler(da, "answers")
////
////      val in1 = Success(APIWebSocket.Subscribe("requests"))
////      val in2 = Success(APIWebSocket.Unsubscribe("requests"))
////      val in3 = Success(APIWebSocket.PublishMessage(mess, "services"))
////
////      val out1 = test.MessageAndAck(None, APIWebSocket.SPACK("Subscribing to topic requests"))
////      val out2 = test.MessageAndAck(None, APIWebSocket.SPACK("Unsubscribing from topic requests"))
////      val out3 = test.MessageAndAck(Some(Publish("services", APIParser.write(mess))), APIWebSocket.SPACK("Message sent to topic services"))
////
////
////
////      val res = test.matchWebSocketMessages.runWith(Source(List(in1, in2, in3)), Sink.seq)
////      val result = Await.result(res._2, 1.seconds)
////      result.toList shouldEqual List(out1, out2, out3)
////    }
//
//
//    import Pickles._
//
//
////    "Create a model"  in {
////      val probe = TestProbe()
////      val da = system.actorOf(Props(classOf[DummyActor]))
////
////      val test = new WebsocketHandler(da, "answers")
////
////      val message = TextMessage(write(APIWebSocket.PublishMessage("requests")))
////      val in3 = TextMessage(write(APIWebSocket.PublishMessage(mess, "services")))
////      val s = Source(List(message, in3))
////
////      test.webSocketHandler.runWith(s, Sink.actorRef(probe.ref, "done"))
////
////
////      probe.fishForMessage(3 second){
////        case "done" => true
////        case x =>
////          println("")
////          println(x)
////          println("")
////          false
////      }
////    }
//
//
//
//  }
//
//
//}
//
//case class DummyHeader(reqID: java.util.UUID, sender: String = "DUMMY")
//case class DummyBody(p1: String, p2: String)
//case class DummyMessage(header: DummyHeader, body: DummyBody)
//
//
//class DummyActor extends Actor {
//  val mess = upickle.default.write(DummyMessage(DummyHeader(java.util.UUID.randomUUID()), DummyBody("hej", "då")))
//  println("DUMMY STARTS")
//  var repl: ActorRef = self
//  def receive = {
//    case Subscribe(_, _, r) => repl = r
//    case x =>
//      println(s"Dummy got: " +x)
//      import context.dispatcher
//      context.system.scheduler.scheduleOnce(500 milliseconds, self, mess)
//      repl ! mess
//  }
//}
//
//
//
//
