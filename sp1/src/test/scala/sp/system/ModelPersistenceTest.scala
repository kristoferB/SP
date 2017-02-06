package sp.system

import akka.actor._
import akka.testkit.{ImplicitSender, TestKit}
import com.typesafe.config._
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import sp.domain._
import sp.system.messages._

import scala.concurrent.duration._

/**
 * Created by Kristofer on 2014-06-17.
 */
class ModelPersistenceTest(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  def this() = this(ActorSystem("myTest", ConfigFactory.parseString(
    """
      |akka.persistence.journal.plugin = "akka.persistence.journal.leveldb"
      |akka.persistence.journal.leveldb.native = off
      |akka.persistence.journal.leveldb.dir = "target/journaltest"
      |akka.persistence.snapshot-store.plugin = "akka.persistence.snapshot-store.local"
      |akka.persistence.snapshot-store.local.dir = "target/snapshotstest/"
      |akka.loglevel = DEBUG
    """.stripMargin)))

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

   //preload
  val modelID = ID.makeID("b8d7c330-0562-11e5-b939-0800200c9a66").get
//  val o1 = Operation("o1")
//  val o2 = Operation("o2")
//  val mh = system.actorOf(ModelHandler.props, "modelHandler")
//  mh ! CreateModel(modelID, "hej")
//  mh ! UpdateIDs(modelID, 0, List(o1, o2))
  //mh ! PoisonPill


  "The Model Handler" must {
    val mh = system.actorOf(ModelHandler.props, "modelHandler")
    "Recover when started" in {
      mh ! GetIds(modelID, List())
      fishForMessage(3 seconds) {
        case m: ModelInfo => false
        case SPIDs(xs) => println(xs); true
      }
    }

//    "create a new model and add content" in {
//      val mid = sp.domain.ID.newID
//      mh ! CreateModel(mid, "test2")
//      val o = Operation("hej")
//      var count = 0
//      fishForMessage(3 seconds) {
//        case m:ModelInfo => mh ! UpdateIDs(mid, 0, List(o)); false
//        case SPIDs(ids) if count == 0 => mh ! GetIds(mid,List()); count +=1; false
//        case SPIDs(ids) if count == 1 => ids shouldEqual List(o); true
//      }
//    }

    // add more test on the model and views
  }
}


