package sp.runners

import akka.actor._
import akka.testkit._
import com.typesafe.config._
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import sp.domain.Logic._
import sp.domain._

import scala.concurrent.duration._
import scala.util.Try


/**
  * Created by kristofer on 2016-05-04.
  */
class OPMakerTest(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  def this() = this(ActorSystem("SP", ConfigFactory.parseString(
    """
      |akka.persistence.journal.plugin = "akka.persistence.journal.inmem"
      |akka.persistence.snapshot-store.plugin = "akka.persistence.snapshot-store.local"
      |akka.persistence.snapshot-store.local.dir = "target/snapshotstest/"
      |akka.loglevel = "OFF"
      |akka.actor.provider = "akka.cluster.ClusterActorRefProvider"
      |akka.remote.netty.tcp.hostname="127.0.0.1"
      |akka.remote.netty.tcp.port=2555
      |akka.cluster.seed-nodes=["akka.tcp://SP@127.0.0.1:2555"]
    """.stripMargin)))


  import akka.cluster.pubsub.DistributedPubSubMediator._
  val mediator = akka.cluster.pubsub.DistributedPubSub(system).mediator
  val id = ID.newID

  import sp.runners.{API_OperationRunner => api}
  import sp.messages._
  import Pickles._


  override def beforeAll: Unit = {
    val exampleS = system.actorOf(OperationRunner.props, "runner")
  }

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }






  "The example service logic" must {
    "include a new pie" in {

    }


  }
}
