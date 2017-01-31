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


   import com.github.nscala_time.time.Imports._


  "The Product and position aggregator" must {
    "fix the times correct" in {
      val intervals = List(
        new org.joda.time.DateTime("2016-11-27T05:39:45.675+01:00") to
        new org.joda.time.DateTime("2016-11-27T05:39:46.192+01:00"),
        new org.joda.time.DateTime("2016-11-27T05:40:45.675+01:00") to
          new org.joda.time.DateTime("2016-11-27T05:40:46.192+01:00"),
        new org.joda.time.DateTime("2016-11-27T05:41:41.675+01:00") to
          new org.joda.time.DateTime("2016-11-27T05:41:46.192+01:00"),
        new org.joda.time.DateTime("2016-11-27T05:42:42.675+01:00") to
          new org.joda.time.DateTime("2016-11-27T05:42:46.192+01:00"),
        new org.joda.time.DateTime("2016-11-27T05:43:43.675+01:00") to
          new org.joda.time.DateTime("2016-11-27T05:46:46.192+01:00"),
        new org.joda.time.DateTime("2016-11-27T05:44:45.675+01:00") to
          new org.joda.time.DateTime("2016-11-27T05:44:46.192+01:00"),
        new org.joda.time.DateTime("2016-11-27T05:46:45.675+01:00") to
          new org.joda.time.DateTime("2016-11-27T05:47:46.192+01:00"),
        new org.joda.time.DateTime("2016-11-27T05:48:48.675+01:00") to
          new org.joda.time.DateTime("2016-11-27T05:49:46.192+01:00"),
        new org.joda.time.DateTime("2016-11-27T05:50:45.675+01:00") to
          new org.joda.time.DateTime("2016-11-27T05:51:46.192+01:00")
      ).sortWith(_.start < _.start)

      val kalle: Option[org.joda.time.Interval] = None
      val k = intervals.foldLeft( 0L, kalle){(a, b) =>
        val act = a._1
        val prev = a._2
        val updInter = for {
          p <- prev if p.overlaps(b)
        } yield {
          val x = p.start to {if (p.end > b.end) p.end else b.end}
          println("it overlaps "+ x.millis)
          x
        }



        val int = updInter.getOrElse(b)
        val actUpd = int.toDurationMillis + act


        println(a)
        println(b)
        println(actUpd, int)
        println("")

        (actUpd, Some(int))
      }


      println(k)
      println((intervals.head.start to intervals.last.end).toDurationMillis)


    }

  }


}
