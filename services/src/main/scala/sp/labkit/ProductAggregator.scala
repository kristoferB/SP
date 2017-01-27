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


  case class Pos(name: String, time: DateTime)

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




  def receive= {
    case op: APIOPMaker.OP =>
      op.start.product.map { name =>
        if (!currentProds.contains(name)){
          // new product
          val pos = currentProdPosition.getOrElse(name, inLoader)
          val prod = Prod(name, List(op), List(Pos(pos, op.start.time)), op.start.time)
          currentProds += name -> prod
        } else {
          val prod = currentProds(name)
          val firstOP = prod.start
        }

      }

    case APIOPMaker.Positions(positions) =>



  }







}

object ProductAggregator {
  def props() = Props(classOf[ProductAggregator])

}