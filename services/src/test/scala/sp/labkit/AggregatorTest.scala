package sp.labkit

import akka.actor._
import akka.testkit._
import com.typesafe.config._
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import sp.domain.Logic._
import sp.domain._


/**
  * Created by kristofer on 2016-05-04.
  */
class AggregatorTest(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  def this() = this(ActorSystem("myTest", ConfigFactory.parseString(
    """
      |akka.loglevel = INFO
    """.stripMargin)))

  val p = TestProbe()
  val e = TestProbe()
  //val sh = system.actorOf(OperatorService.props(p.ref), "OperatorService")

  override def beforeAll: Unit = {

  }

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }




  "The Product and position aggregator" must {
    "testing som json messaging" in {
      val opS = APIOPMaker.OPEvent("OPStart", org.joda.time.DateTime.now, "1", "test", Some("Cyl1"))
      val op = APIOPMaker.OP(opS, None)
      // maybe skip datetime in api and encode as string
      //val json = APIParser.write(op)

    }

  }


}
