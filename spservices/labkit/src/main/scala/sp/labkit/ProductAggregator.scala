package sp.labkit

import akka.actor._
import akka.cluster.pubsub.DistributedPubSubMediator.Publish

import scala.concurrent.duration._
import sp.domain._
import sp.domain.Logic._
import org.joda.time._

import scala.util.{Failure, Success, Try}
import com.github.nscala_time.time.Imports._
import org.threeten.bp.ZonedDateTime
import play.api.libs.json._



sealed trait APILabKitWidget
object APILabKitWidget {
  implicit lazy val f1 = deriveFormatISA[APILabKitWidget_ProdStat]
  implicit lazy val readResp = deriveReadISA[APILabKitWidget]
  implicit lazy val writeResp = deriveWriteISA[APILabKitWidget]
}

  case class APILabKitWidget_OperationStarted(name: String, resource: String, product: String, operationType: String, time: String) extends APILabKitWidget
  case class APILabKitWidget_OperationFinished(name: String, resource: String, product: String, operationType: String, time: String) extends APILabKitWidget
  case class APILabKitWidget_ResourcePies(data: Map[String, Map[String, Int]]) extends APILabKitWidget
  case class APILabKitWidget_ProductPies(data: List[(String, List[(String, Int)])]) extends APILabKitWidget
  case class APILabKitWidget_ProdStat(name: String, leadtime: Int, processingTime: Int, waitingTime: Int, noOfOperations: Int, noOfPositions: Int) extends APILabKitWidget
  case class APILabKitWidget_ProductStats(data: List[APILabKitWidget_ProdStat]) extends APILabKitWidget




class ProductAggregator extends Actor with ActorLogging with NamesAndValues {
  import context.dispatcher

  import akka.cluster.pubsub._
  import DistributedPubSubMediator.{ Put, Subscribe, Publish }
  val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe("ops", self)
  mediator ! Subscribe("pos", self)


  case class Pos(name: String, time: DateTime, duration: Option[Long])

  case class Prod(name: String,
                   ops: List[APIOPMaker_OP],
                   positions: List[Pos],
                   startTime: DateTime,
                   endTime: Option[DateTime] = None,
                   currentDuration: Long = 0,
                   waited: Long = 0,
                   processed: Long = 0
                  )

  var currentPositions: Map[String, String] = Map()
  var currentProds: Map[String, Prod] = Map()
  var completedProds: Map[String, Prod] = Map()

  var liveProd: String = ""
  var latestTimeStamp: DateTime = org.joda.time.DateTime.now
  var newestProd = ""

  val ticker = context.system.scheduler.schedule(300 milliseconds, 300 milliseconds, self, "tick")



  import com.github.nscala_time.time.Imports._

  def receive= {
    case "tick" =>
      sendProds(latestTimeStamp)
      latestTimeStamp = latestTimeStamp.plus(300)
    case op: APIOPMaker_OP if op.end.nonEmpty =>
      op.start.product.foreach { name =>
        val updP = if (currentProds.contains(name)) {
          addOPToProd(currentProds(name), op)
        } else if (completedProds.contains(name)) {
          addOPToProd(completedProds(name), op)
        } else {
          newestProd = name
          Prod(name, List(op), List(), toDateTime(op.start.time))  // new product
        }

        if (!completedProds.contains(name))
          currentProds += name -> updP
        else
          completedProds += name -> updP

        latestTimeStamp = lastTime(op)
      }

    case APIOPMaker_Positions(positions, ztime) =>
      println("AGGREGATOR pos:")
      println("posLine: "+positions)

      val time = toDateTime(ztime)

      val upd = positions.filter{case (pos, prod) =>
        ! (currentPositions.contains(pos) && currentPositions(pos) == prod)
      }
      val removed = currentProds.collect{
        case kv if !positions.values.toSet.contains(kv._1) && !completedProds.contains(kv._1) => kv
      }
      currentPositions = positions

      val updProdPositions = upd.map{case (pos, prod) => prod -> Pos(pos, time, None)}.filter(_._1.nonEmpty)

      val updProds = currentProds.flatMap{case (name, prod) =>
        val updP = updProdPositions.get(name).map{ pos =>
          val uP = updPosInProd(prod, time)
          uP.copy(positions = pos :: uP.positions)
        }
        updP.map(name -> _)
      }

      currentProds ++= updProds

      val removedProds = currentProds.collect{
        case (name, prod) if removed.contains(name) => name-> {
          updPosInProd(prod, time).copy(endTime = Some(time))
        }
      }
      currentProds = currentProds.filter(kv => !removedProds.contains(kv._1))

      completedProds ++= removedProds

//      currentProds.foreach(x => println(s"posLine_prod: $x"))
//      completedProds.foreach(x => println(s"posLine_compl: $x"))
//
//      println()

      latestTimeStamp = time

  }


  def sendProds(time: DateTime) = {

    if ((liveProd.isEmpty || !currentProds.contains(liveProd)) && currentProds.contains(newestProd))
      liveProd = newestProd

    val livepie = currentProds.get(liveProd).map(p =>
      makeMeAPie(updPosInProd(p, time))
    ).getOrElse(("No live", List()))
    val compl = newestCompleted.map(makeMeAPie)
    val pie = (livepie +: compl)


    // TODO: SEND WITH HEADER IN THE FUTURE
    if (pie.nonEmpty) mediator ! Publish("frontend", SPValue(APILabKitWidget_ProductPies(pie)).toJson)

    val pStats = createProdStats
    if (pStats.nonEmpty)  mediator ! Publish("frontend", SPValue(APILabKitWidget_ProductStats(pStats)).toJson)

  }

  def makeMeAPie(prod: Prod) = {
    prod.name -> prod.positions.flatMap(p => p.duration.map(d => p.name -> (d*1000/(prod.currentDuration+1)).toInt))
  }

  def newestCompleted = {
    completedProds.values.toList
      .filter(_.endTime.nonEmpty)
      .sortWith(_.endTime.get > _.endTime.get)
      .take(2)
  }

  def lastTime(op: APIOPMaker_OP) = {
    toDateTime(op.end.getOrElse(op.start).time)
  }

  def toDateTime(t: ZonedDateTime) = DateTime.parse(t.toString())

  def createProdStats = {
    val t = completedProds.toList.sortWith((a, b) => a._2.endTime.getOrElse(a._2.startTime) > b._2.endTime.getOrElse(b._2.startTime) ).map{case (name, prod) =>
      APILabKitWidget_ProdStat(name, prod.currentDuration/1000 toInt, prod.processed/1000 toInt, prod.waited/1000 toInt, prod.ops.size, prod.positions.size)
    }
    t.take(10)
  }


  def addOPToProd(prod: Prod, op: APIOPMaker_OP) = {
    reCalculateProd(prod.copy(ops = prod.ops :+ op), lastTime(op))
  }


  def updPosInProd(prod: Prod, time: DateTime) = {
    val oldP = prod.positions.headOption.map(p => p.copy(duration = Some((p.time to time).toDurationMillis))).toList
    val list: List[Pos] = if (oldP.nonEmpty) oldP ++ prod.positions.tail else oldP
    prod.copy(positions = list)
  }

  def reCalculateProd(prod: Prod, time: DateTime) = {
    val times = sortAndMakeInterval(prod)
    val current = (prod.startTime to time).toDurationMillis
      prod.copy(currentDuration = current, waited = current - times._1, processed = times._1)
  }

  def sortAndMakeInterval(prod: Prod) = {
    val intervals = prod.ops.filter(_.end.nonEmpty).map(x => toDateTime(x.start.time) to toDateTime(x.end.get.time)).sortWith(_.start < _.start)
    val kalle: Option[org.joda.time.Interval] = None
    intervals.foldLeft(0L, kalle){(a, b) =>
      val act = a._1
      val prev = a._2
      val updInter = for {
        p <- prev if p.overlaps(b)
      } yield {
        (b.millis - p.overlap(b).millis, p.start to {if (p.end > b.end) p.end else b.end})
      }

      val interval = updInter.getOrElse((b.millis, b))
      val actUpd = interval._1 + act

      (actUpd, Some(interval._2))
    }

  }







}

object ProductAggregator {
  def props() = Props(classOf[ProductAggregator])

}