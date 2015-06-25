package sp.system

import akka.actor._
import akka.testkit._
import com.typesafe.config._
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import sp.domain._
import sp.system.messages._
import sp.domain.Logic._

import scala.concurrent.duration._

/**
 * Created by Kristofer on 2014-06-17.
 */
class ServiceHandlerTest(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  def this() = this(ActorSystem("myTest", ConfigFactory.parseString(
    """
      |akka.loglevel = DEBUG
    """.stripMargin)))


  val mhP = TestProbe()
  val seP = TestProbe()
  val bP = TestProbe()

  val mh = system.actorOf(ServiceTalker.props(), "serviceTalker")
  val mid = ID.newID
  val o = Operation("hej")

  override def beforeAll: Unit = {

  }

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }


  "The Service Talker" must {
    "analyse request attributes an pass if ok" in {
      val attr = SPAttributes("key1"-> KeyDefinition("String", None, None),
        "key2"-> SPAttributes("key3"-> KeyDefinition("String", None, None)))


      val mid = sp.domain.ID.newID
      mh ! CreateModel(mid, "test2")
      expectMsgType[ModelInfo]
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


