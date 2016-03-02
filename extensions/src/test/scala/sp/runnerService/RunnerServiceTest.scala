package sp.runnerService

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
class RunnerServiceTest(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  def this() = this(ActorSystem("myTest", ConfigFactory.parseString(
    """
      |akka.loglevel = INFO
    """.stripMargin)))

  val p = TestProbe()
  val e = TestProbe()
  val sh = system.actorOf(RunnerService.props(p.ref, "OperationControl"))

  override def beforeAll: Unit = {

  }

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "The Service Example" must {
    "filter the items" in {
      val o1 = Operation("o1")
      val o2 = Operation("o1")
      val o3 = Operation("o1")
      val o4 = Operation("o1")
      val o5 = Operation("o1")
      val sop = Parallel(Sequence(o1, Parallel(Sequence(o2, o3), o4), o5))

      val sopSpec =  SOPSpec("theSOPSpec", List(sop), SPAttributes())

      val longList: List[IDAble] = List(o1, o2, o3, o4, o5, sopSpec)

      val r = Request("RunnerService",
        SPAttributes(
          "SOP" -> sopSpec.id,
          "setup" -> Map("onlyOperations"-> false, "searchMethod"->"theGood")
        ),
        longList
      )

      sh ! r

      fishForMessage(10 seconds){
        case x: Response => println(x); true;
        case x => println(x); false
      }
    }
  }
}
