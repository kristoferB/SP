package sp.labkit

import akka.actor._
import akka.testkit._
import com.typesafe.config._
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import sp.domain.Logic._
import sp.domain._
import sp.labkit.OPMakerLogic

import scala.concurrent.duration._


/**
  * Created by kristofer on 2016-05-04.
  */
class OPMakerTest(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
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


  val logic = new OPMakerLogic {}
  val aState = Map[String, SPValue](
      logic.feeder_exec       -> false  ,
      logic.newCylinder_var   -> false  ,
      logic.pnp1_mode         -> 1      ,
      logic.pnp1from_var      -> false  ,
      logic.pnp1to_var        -> false  ,
      logic.p1_mode           -> 1      ,
      logic.p1Transport_var   -> false  ,
      logic.p1Process_var     -> false  ,
      logic.convFree_var      -> false  ,
      logic.convMove_var      -> false  ,
      logic.convAtOut_var     -> false  ,
      logic.pnp2_mode         -> 1      ,
      logic.pnp2to3_var       -> false  ,
      logic.pnp2to4_var       -> false  ,
      logic.p3_mode           -> 1      ,
      logic.p3Process_var     -> false  ,
      logic.p4_mode           -> 1      ,
      logic.p4Process_var     -> false,
      "time" -> SPValue(org.joda.time.DateTime.now)
  )

  "The OPMaker service" must {
    "make no new ops" in {
      val t = logic.makeMeOps(aState, Map())
      t shouldEqual List[APIOPMaker.OP]()
    }
    "make a start op" in {
      val t = logic.makeMeOps(aState + (logic.p3_mode->2), Map())
      println("RESULT")
      println(t)
      assert(t.nonEmpty)
    }
    "make an end op" in {
      val updS: Map[String, SPValue] = aState + (logic.p3_mode->2)
      val t = logic.makeMeOps(updS, Map())
      val tt = logic.makeMeOps(updS + (logic.p3_mode->3), Map(t.head.start.name->t.head))

      val l = logic.makeMeOps(updS, Map())
      val ll = logic.makeMeOps(updS + (logic.p3_mode->3), Map(l.head.start.name->l.head))


      println("RESULT")
      println(ll)

      assert(tt.nonEmpty)
    }
  }

  val prodLogic = new TrackProducts {}
  def time = org.joda.time.DateTime.now
  "The product tracker" must {
    "add a new cylinder" in {
      val start = APIOPMaker.OPEvent(logic.feedCylinder, time, "hej", "feeder", None)
      val end = None
      val op = APIOPMaker.OP(start, end)

      val res = prodLogic.updPositionsAndOps(op)
      println("RESULT PROD")
      println(res.start.product)
      println("The products:")
      println(prodLogic.positions)

      assert(res.start.product.nonEmpty)

    }
    "add a new cylinder and add at end" in {
      val start = APIOPMaker.OPEvent(logic.feedCylinder, time, "hej", "feeder", Some("cyl1"))
      val end = Some(APIOPMaker.OPEvent(logic.feedCylinder, time, "hej", "feeder", None))
      val op = APIOPMaker.OP(start, end)

      val res = prodLogic.updPositionsAndOps(op)
      println("RESULT PROD")
      println(res.end.get.product)
      println("The products:")
      println(prodLogic.positions)

      assert(prodLogic.positions(logic.inLoader).nonEmpty)
    }

    "remove the cylinder at the end" in {
      val start = APIOPMaker.OPEvent(logic.p3move, time, "hej", logic.p3, Some("cyl1"))
      val end = Some(APIOPMaker.OPEvent(logic.p3move, time, "hej", logic.p3, None))
      val op = APIOPMaker.OP(start, end)

      prodLogic.positions = prodLogic.positions + (logic.inP3 -> "cyl1")
      val res = prodLogic.updPositionsAndOps(op)
      println("RESULT PROD")
      println(res.end.get.product)
      println("The products:")
      println(prodLogic.positions)

      assert(prodLogic.positions(logic.inP3).isEmpty)
    }


    "move a cylinder via positions fromFeedToP1" in {
      val start = APIOPMaker.OPEvent(logic.fromFeedToP1, time, "hej", logic.pnp1, None)
      val end = Some(APIOPMaker.OPEvent(logic.fromFeedToP1, time, "hej", logic.pnp1, None))
      val op = APIOPMaker.OP(start, None)

      // init position
      prodLogic.positions = prodLogic.positions + (logic.inLoader -> "cyl1")

      println("START")
      println("from: "+prodLogic.positions(logic.inLoader) +
        ", via: " + prodLogic.positions(logic.inPnp1)+
        ", to: " + prodLogic.positions(logic.inP1)
      )


      val updStart = prodLogic.updPositionsAndOps(op)

      println("VIA")
      println("from: "+prodLogic.positions(logic.inLoader) +
        ", via: " + prodLogic.positions(logic.inPnp1)+
        ", to: " + prodLogic.positions(logic.inP1)
      )

      val updEnd = prodLogic.updPositionsAndOps(op.copy(start = updStart.start, end = end))

      println("END")
      println("from: "+prodLogic.positions(logic.inLoader) +
        ", via: " + prodLogic.positions(logic.inPnp1)+
        ", to: " + prodLogic.positions(logic.inP1)
      )

      println("The products:")
      println(prodLogic.positions)

      assert(prodLogic.positions(logic.inP1).nonEmpty)
    }

    "move a cylinder via positions fromFeedToC" in {
      val start = APIOPMaker.OPEvent(logic.fromFeedToC, time, "hej", logic.pnp1, None)
      val end = Some(APIOPMaker.OPEvent(logic.fromFeedToC, time, "hej", logic.pnp1, None))
      val op = APIOPMaker.OP(start, None)

      // init position
      prodLogic.positions = prodLogic.positions + (logic.inLoader -> "cyl1")

      println("START")
      println("from: "+prodLogic.positions(logic.inLoader) +
        ", via: " + prodLogic.positions(logic.inPnp1)+
        ", to: " + prodLogic.positions(logic.inConvIn)
      )


      val updStart = prodLogic.updPositionsAndOps(op)

      println("VIA")
      println("from: "+prodLogic.positions(logic.inLoader) +
        ", via: " + prodLogic.positions(logic.inPnp1)+
        ", to: " + prodLogic.positions(logic.inConvIn)
      )

      val updEnd = prodLogic.updPositionsAndOps(op.copy(start = updStart.start, end = end))

      println("END")
      println("from: "+prodLogic.positions(logic.inLoader) +
        ", via: " + prodLogic.positions(logic.inPnp1)+
        ", to: " + prodLogic.positions(logic.inConvIn)
      )

      println("The products:")
      println(prodLogic.positions)

      assert(prodLogic.positions(logic.inConvIn).nonEmpty)
    }

    "move a cylinder non via positions transport" in {
      val start = APIOPMaker.OPEvent(logic.transport, time, "hej", logic.pnp1, None)
      val end = Some(APIOPMaker.OPEvent(logic.transport, time, "hej", logic.pnp1, None))
      val op = APIOPMaker.OP(start, None)

      // init position
      prodLogic.positions = prodLogic.positions + (logic.inConvIn -> "cyl1")

      println("START")
      println("from: "+prodLogic.positions(logic.inConvIn) +
        ", via: " + prodLogic.positions+
        ", to: " + prodLogic.positions(logic.inConvOut)
      )


      val updStart = prodLogic.updPositionsAndOps(op)

      println("VIA")
      println("from: "+prodLogic.positions(logic.inConvIn) +
        ", via: " + prodLogic.positions+
        ", to: " + prodLogic.positions(logic.inConvOut)
      )

      val updEnd = prodLogic.updPositionsAndOps(op.copy(start = updStart.start, end = end))

      println("END")
      println("from: "+prodLogic.positions(logic.inConvIn) +
        ", via: " + prodLogic.positions+
        ", to: " + prodLogic.positions(logic.inConvOut)
      )

      println("The products:")
      println(prodLogic.positions)

      assert(prodLogic.positions(logic.inConvOut).nonEmpty)
    }
  }
}
