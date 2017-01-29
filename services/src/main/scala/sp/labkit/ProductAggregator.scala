package sp.labkit

import akka.actor._
import akka.cluster.pubsub.DistributedPubSubMediator.Publish

import scala.concurrent.duration._
import akka.persistence._
import sp.domain._
import sp.domain.Logic._
import sp.messages._
import org.joda.time._

import scala.util.{Failure, Success, Try}
import com.github.nscala_time.time.Imports._




class ProductAggregator extends Actor with ActorLogging with NamesAndValues {
  import context.dispatcher

  import akka.cluster.pubsub._
  import DistributedPubSubMediator.{ Put, Subscribe }
  val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe("ops", self)
  mediator ! Subscribe("pos", self)


  case class Pos(name: String, time: DateTime, duration: Option[Long])

  case class Prod(name: String,
                   ops: List[APIOPMaker.OP],
                   positions: List[Pos],
                   startTime: DateTime,
                   endTime: Option[DateTime] = None,
                   currentDuration: Long = 0,
                   waited: Long = 0,
                   processed: Long = 0
                  )

  var currentPositions: Map[String, String] = Map()
  var currentProdPosition: Map[String, String] = Map()
  var currentProds: Map[String, Prod] = Map()
  var completedProds: Map[String, Prod] = Map()





  def receive= {
    case op: APIOPMaker.OP if op.end.nonEmpty =>
      op.start.product.map { name =>
        if (!currentProds.contains(name)){
          // new product
          val pos = currentProdPosition.getOrElse(name, inLoader)
          val prod = Prod(name, List(op), List(Pos(pos, op.start.time, None)), op.start.time)
          currentProds += name -> prod
        } else {
          val prod = currentProds(name)
          val started = prod.startTime
          val current = started to lastTime(op) toDurationMillis
          val actW = calcWaitAndActive(prod.ops, current)
          val updP = prod.copy(ops = prod.ops :+ op, currentDuration = current, waited = actW._1, processed = actW._2)
          currentProds += name -> updP
          // removes when prod is removed from positions
//          if (op.end.nonEmpty && (op.start.name == p3move || op.start.name == p4move)){
//            // Prod done
//            val completed = updP.copy(endTime = Some(lastTime(op)))
//            completedProds += name -> updP
//            currentProds -= name
//          }
        }

        println("AGGREGATOR ops:")
        println(currentProds)


        println()

      }

    case APIOPMaker.Positions(positions, time) =>
      println("AGGREGATOR pos:")
      println("posLine: "+positions)

      val upd = positions.filter{case (pos, prod) =>
        ! (currentPositions.contains(pos) && currentPositions(pos) == prod)
      }


      println()

  }



  def lastTime(op: APIOPMaker.OP) = {
    op.end.getOrElse(op.start).time
  }

  def calcWaitAndActive(list: List[APIOPMaker.OP], currentDuration: Long) = {
    val active = list.foldLeft(0L){(a, b) =>
      a + b.attributes.getAs[Long]("duration").getOrElse(0L)
    }
    val waiting = currentDuration - active
    (waiting, active)
  }





}

object ProductAggregator {
  def props() = Props(classOf[ProductAggregator])

}