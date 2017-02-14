package sp.labkit

import akka.actor._
import akka.testkit._
import com.typesafe.config._
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import sp.domain.Logic._
import sp.domain._
import sp.example.{API_ExampleService, ExampleService, ExampleServiceLogic}
import sp.messages.{APIParser, APISP, SPMessage}

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




  override def beforeAll: Unit = {
    val exampleS = system.actorOf(ExampleService.props, "exampleService")
  }

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }




  val logic = new ExampleServiceLogic {}


  "The example service logic" must {
    "include a new pie" in {
      val res = logic.commands(API_ExampleService.StartTheTicker(id))
      val map = logic.thePies

      assert(map.size == 1 && map.contains(id) && map(id) == Map("first"->10, "second"-> 30, "third" -> 60))
      res shouldEqual APIParser.writeJs(APISP.SPACK())
    }

    "remove a pie" in {
      logic.commands(API_ExampleService.StartTheTicker(id))
      assert(logic.thePies.size == 1)

      val res = logic.commands(API_ExampleService.StopTheTicker(id))
      assert(logic.thePies.isEmpty)
      res shouldEqual APIParser.writeJs(APISP.SPDone())

    }

    "removing non existing pie" in {

      val res = logic.commands(API_ExampleService.StopTheTicker(id))
      assert(logic.thePies.isEmpty)
      res shouldEqual APIParser.writeJs(APISP.SPDone())

    }

    "include a predef pie" in {
      val xs = Map("foo"->10, "bar"-> 30)
      val res = logic.commands(API_ExampleService.SetTheTicker(id, xs))
      val map = logic.thePies

      assert(map.size == 1 && map.contains(id) && map(id) == xs)
      res shouldEqual APIParser.writeJs(APISP.SPACK())
    }

    "multiple pies" in {
      val xs = Map("foo"->10, "bar"-> 30)
      val res = logic.commands(API_ExampleService.SetTheTicker(id, xs))
      logic.commands(API_ExampleService.StartTheTicker(ID.newID))
      val map = logic.thePies

      assert(map.size == 2)
    }

    "upd pie" in {
      val xs = Map("foo"->10, "bar"-> 30)
      val res = logic.updPie(xs)
      println(res)
      println(logic.updPie(res))
    }

  }

  val header = SPAttributes("to" -> API_ExampleService.service, "from" -> "testing")

  "The example service actor" must {
    val p = TestProbe()
    val e = TestProbe()
    mediator ! Subscribe("answers", p.ref)
    "start ticking on new pie" in {
      val body = APIParser.writeJs(API_ExampleService.StartTheTicker(id))
      val mess = SPMessage(header, body).toJson
      mediator ! Publish("services", mess)

      p.fishForMessage(10 seconds){
        case mess @ _ if {println(s"answers probe got: $mess"); false} => false

        case x: String if SPMessage.fromJson(x).isSuccess =>
          val mess = SPMessage.fromJson(x).get
          val b = Try{APIParser.readJs[API_ExampleService.TickerEvent](mess.body)}
          b.isSuccess
      }
    }

    mediator ! Subscribe("spevents", e.ref)
    "answer to status request" in {
      val body = APIParser.writeJs(APISP.StatusRequest())
      val mess = SPMessage(header, body).toJson
      mediator ! Publish("spevents", mess)

      e.fishForMessage(10 seconds){
        case mess @ _ if {println(s"spevents probe got: $mess"); false} => false

        case x: String if SPMessage.fromJson(x).isSuccess =>
          val mess = SPMessage.fromJson(x).get
          val b = Try{APIParser.readJs[APISP.StatusResponse](mess.body)}
          b.isSuccess
      }
    }

  }

  "The example service API" must {
    "parse the classes" in {

    }
  }
}
