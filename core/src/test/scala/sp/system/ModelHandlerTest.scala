package sp.system

import akka.actor._
import org.scalatest.concurrent.Futures
import scala.concurrent._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import akka.testkit.TestKit
import org.scalatest.WordSpecLike
import org.scalatest.Matchers
import org.scalatest.BeforeAndAfterAll
import akka.testkit.ImplicitSender
import com.typesafe.config._
import sp.system.messages._
import sp.domain._

/**
 * Created by Kristofer on 2014-06-17.
 */
class ModelHandlerTest(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  def this() = this(ActorSystem("myTest", ConfigFactory.parseString(
    """
      |akka.persistence.journal.plugin = "akka.persistence.journal.inmem"
      |akka.persistence.snapshot-store.plugin = "akka.persistence.snapshot-store.local"
      |akka.persistence.snapshot-store.local.dir = "target/snapshotstest/"
      |akka.loglevel = DEBUG
    """.stripMargin)))

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }


  "The Model Handler" must {
    val mh = system.actorOf(ModelHandler.props, "modelHandler")

    "create a new model and return success" in {
      val mid = sp.domain.ID.newID
      mh ! CreateModel(mid, "test2")
      expectMsgType[ModelInfo]
    }

    "create a new model and add content" in {
      val mid = sp.domain.ID.newID
      mh ! CreateModel(mid, "test2")
      val o = Operation("hej")
      var count = 0
      fishForMessage(3 seconds) {
        case m:ModelInfo => mh ! UpdateIDs(mid, 0, List(o)); false
        case SPIDs(ids) if count == 0 => mh ! GetIds(mid,List()); count +=1; false
        case SPIDs(ids) if count == 1 => ids shouldEqual List(o); true
      }
    }

    // add more test on the model and views
  }
}


