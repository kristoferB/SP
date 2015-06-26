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
      |akka.loglevel = INFO
    """.stripMargin)))


  val mhP = TestProbe()
  val seP = TestProbe()
  val bP = TestProbe()

  //val mh = system.actorOf(ServiceTalker.props(), "serviceTalker")
  val mid = ID.newID
  val o = Operation("hej")

  override def beforeAll: Unit = {

  }

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }


  "The Service Talker" must {
    "analyse request attributes an pass if ok" in {
      val definition = SPAttributes("key1" -> KeyDefinition("kk", List(), None),
        "key2" -> SPAttributes("key3" -> KeyDefinition("kk", List(), None)))
      val attr = SPAttributes("key1" -> "hej", "key3" -> "kalle")

      val x = ServiceTalker.validateRequest(Request("temp", attr, List()), definition)
      x shouldEqual Right(Request("temp", SPAttributes("key1" -> "hej", "key3" -> "kalle")))
    }

    "analyse request attributes an fail if missing" in {
      val definition = SPAttributes("key1" -> KeyDefinition("String", List(), None),
        "key2" -> SPAttributes("key3" -> KeyDefinition("String", List(), None)))
      val attr = SPAttributes("key1" -> "hej")
      val x = ServiceTalker.validateRequest(Request("temp", attr, List()), definition)
      x shouldEqual Left(List(SPErrorString(s"required key key3 is missing")))
    }

    "analyse request attributes an fill if missing" in {
      val definition = SPAttributes("key1" -> KeyDefinition("String", List(), Some("japp")),
        "key2" -> SPAttributes("key3" -> KeyDefinition("String", List(), None)))
      val attr = SPAttributes("key3" -> "kalle")
      val x = ServiceTalker.validateRequest(Request("temp", attr, List()), definition)
      x shouldEqual Right(Request("temp", SPAttributes("key1" -> "japp", "key3" -> "kalle")))
    }




      //      val mid = sp.domain.ID.newID
//      mh ! CreateModel(mid, "test2")
//      expectMsgType[ModelInfo]
//    }

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


