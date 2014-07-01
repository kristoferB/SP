package sp.system

import akka.actor.ActorSystem
import akka.actor.Actor
import akka.actor.Props
import akka.testkit.TestKit
import org.scalatest.WordSpecLike
import org.scalatest.Matchers
import org.scalatest.BeforeAndAfterAll
import akka.testkit.ImplicitSender
import com.typesafe.config._
import sp.domain.SPAttributes
import sp.system.messages._

/**
 * Created by Kristofer on 2014-06-17.
 */
class ModelHandlerTest(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  def this() = this(ActorSystem("myTest", ConfigFactory.parseString(
    """
      |akka.persistence.journal.plugin = "akka.persistence.journal.leveldb"
      |akka.persistence.journal.leveldb.native = on
      |akka.persistence.journal.leveldb.dir = "target/journaltest"
      |akka.persistence.snapshot-store.local.dir = "target/snapshotstest/"
      |akka.loglevel = DEBUG
    """.stripMargin)))

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "The Model Handler" must {
    val mh = system.actorOf(ModelHandler.props, "modelHandler")

    "create a new model and return success" in {
      mh ! CreateModel("test2")
      expectMsg(ModelInfo("test2",1, SPAttributes(Map())))
    }
  }
}


