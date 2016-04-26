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
 * Created by Kristofer on 2016-03-02.
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
      val o1 = Operation("o1", List(), SPAttributes(), ID.makeID("a0f565e2-e44b-4017-a24e-c7d01e970dec").get)
      val o2 = Operation("o2",List(), SPAttributes(), ID.makeID("b0f565e2-e44b-4017-a24e-c7d01e970dec").get)
      val o3 = Operation("o3",List(), SPAttributes(), ID.makeID("c0f565e2-e44b-4017-a24e-c7d01e970dec").get)
      val o4 = Operation("o4",List(), SPAttributes(), ID.makeID("d0f565e2-e44b-4017-a24e-c7d01e970dec").get)
      val o5 = Operation("o5",List(), SPAttributes(), ID.makeID("e0f565e2-e44b-4017-a24e-c7d01e970dec").get)
      val sop = Parallel(Sequence(o1, Parallel(Sequence(o2, o3), o4), o5))

      val sopSpec =  SOPSpec("theSOPSpec", List(sop), SPAttributes())

      val longList: List[IDAble] = List(o1, o2, o3, o4, o5, sopSpec
      val r = Request("RunnerService",
        SPAttributes(
          "SOP" -> sopSpec.id
        ),
        longList
      )

      sh ! r

      fishForMessage(10 seconds){
        case x: Response => println(x); true;
        //case x => println(x); false // anv denna om progress skall skrivas ut
        // lägg till tester här baserat på progress.
        case x => false
      }
    }
  }
}
