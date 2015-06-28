package sp.system

import akka.actor._
import akka.testkit._
import com.typesafe.config._
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import sp.domain._
import sp.domain.Logic._
import sp.system.messages._

import scala.concurrent.duration._

/**
 * Created by Kristofer on 2014-06-17.
 */
class ServiceExampleTest(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  def this() = this(ActorSystem("myTest", ConfigFactory.parseString(
    """
      |akka.loglevel = INFO
    """.stripMargin)))

  val p = TestProbe()
  val sh = system.actorOf(ServiceHandler.props(p.ref))
  val s = system.actorOf(ServiceExample.props)
  sh ! RegisterService("test", s, ServiceExample.specification)

  override def beforeAll: Unit = {

  }

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "The Service Example" must {
    "filter the items" in {
      val t1 = Thing("t1")
      val t2 = Thing("t2", SPAttributes("gillar"-> t1.id))
      val t3 = Thing("t3", SPAttributes("gillar"-> t2.id))
      val o1 = Operation("o1", List(PropositionCondition(EQ(SVIDEval(t1.id), ValueHolder(SPValue(false))), List())))
      val o2 = Operation("o2", List(), SPAttributes("gillar"-> t2.id))

      val longList: List[IDAble] = List(t1, t2, t3, o1, o2) ++ (0 to 1000).map(x => t3)

      val r = Request("test",
        SPAttributes(
          "findID" -> t1.id,
          "setup" -> Map("onlyOperations"-> false, "searchMethod"->"theGood")
        ),
        longList
      )

      sh ! r

      fishForMessage(3 seconds){
        case x: Response => println(x); x.ids == List(t2, o1)
        case x => println(x); false
      }
    }
  }
}
