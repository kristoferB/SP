package sp.models

import akka.actor._
import akka.testkit._
import com.typesafe.config._
import org.scalatest._
import sp.domain._
import sp.messages._
import scala.util._

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
      |akka.loglevel = DEBUG
      |akka.actor.provider = "akka.cluster.ClusterActorRefProvider"
    """.stripMargin)))



  val mh = system.actorOf(ModelMaker.props(MockMaker.props), "modelHandler")


  override def beforeAll: Unit = {
    //mh ! CreateModel(mid, "generalModel")
    //mh ! UpdateIDs(mid, List(o))
  }

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  implicit val format = ModelMakerAPI.formats

  "Model maker api should" - {
    "reject illegal messages" in {
      val jsonCM = """{"name":"hej","attributes":{"attr":"hej"}}"""

      mh ! jsonCM
      fishForMessage(0.5 seconds) {
        case m: String =>
          println("got reply: "+m)
          val se = ModelMakerAPI.readSPMessage(m).get
          se.isInstanceOf[SPError]
      }
    }
    "reject illegal messages 2" in {
      val jsonCM = """{"isa":"CreateModel","attributes":{"attr":"hej"}}"""

      mh ! jsonCM
      fishForMessage(0.5 seconds) {
        case m: String =>
          println("2 got reply: "+m)
          val se = ModelMakerAPI.readSPMessage(m).get
          se.isInstanceOf[SPError]
      }
    }
    "create Model and SPOK" in {
      val jsonCM = """{"isa":"CreateModel","name":"hej"}"""

      mh ! jsonCM
      fishForMessage(0.5 seconds) {
        case m: String =>
          println("create got reply: "+m)
          val se = ModelMakerAPI.readSPMessage(m).get
          se.isInstanceOf[SPOK]
      }
    }
  }





}

class MockMaker(id: ID) extends Actor {
  def receive = {
    case x => println("maker got : "+ x)
  }
}

object MockMaker {
  def props(maker: ID) = Props(classOf[MockMaker], maker)
}

