package sp.system

import akka.actor._
import akka.cluster.pubsub._
import akka.testkit.{ImplicitSender, TestKit}
import com.typesafe.config._
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import sp.domain._
import sp.system.messages._

import scala.concurrent.duration._

/**
 * Created by Kristofer on 2014-06-17.
 */
class JsonAPITest(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  def this() = this(ActorSystem("SP", ConfigFactory.parseString(
    """
      |akka.persistence.journal.plugin = "akka.persistence.journal.inmem"
      |akka.persistence.snapshot-store.plugin = "akka.persistence.snapshot-store.local"
      |akka.persistence.snapshot-store.local.dir = "target/snapshotstest/"
      |akka.loglevel = "DEBUG"
      |akka.actor.provider = "akka.cluster.ClusterActorRefProvider"
      |akka.extensions = ["akka.cluster.pubsub.DistributedPubSub"]
    """.stripMargin)))


  system.actorOf(ModelHandler.props, "modelHandler")
  system.actorOf(ServiceHandler.props, "serviceHandler")
  system.actorOf(EventHandler.props, "eventHandler")
  val api = system.actorOf(OldModelAPIService.props, "jsonAPI")
  //val apiEvents = system.actorOf(JsonAPIEvents.props, "jsonAPIEvents")

  import DistributedPubSubMediator.{ Put, Subscribe, Publish }
  val mediator = DistributedPubSub(system).mediator

  val id = ID.newID

  override def beforeAll: Unit = {

  }

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "Running the Json API" must {
    "Create actors and listens to messages" in {

      implicit val f = OldModelMessageAPI.formats

      val mess = OldModelMessageAPI.write(OldModelMessageAPI.CreateModel(id, "hej"))

      println("MY MESSAGE: " + mess)

      //mediator ! Publish("modelHandler", CreateModel(id, "hej"))
      mediator ! Publish("models", mess)



      fishForMessage(2 seconds) {
        case x => println("The test got: \n"+x); false

      }
    }


    // add more test on the model and views
  }
}


