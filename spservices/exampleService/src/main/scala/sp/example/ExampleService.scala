package sp.example

import akka.actor._

import scala.util.{Failure, Random, Success, Try}
import sp.domain._
import sp.domain.Logic._
import sp.messages._
import sp.messages.Pickles._



// The messages that this service can send and receive is
// is defined using this API structure

package API_ExampleService {
  sealed trait API_ExampleService
  // Messages you can send to me
  /**
    * Adds a new pie to the memory with an id
    * @param id an UUID identifying the pie
    */
  case class StartTheTicker(id: java.util.UUID) extends API_ExampleService

  /**
    * removes the pie with the id
    * @param id an UUID identifying the pie
    */
  case class StopTheTicker(id: java.util.UUID) extends API_ExampleService

  /**
    * Changes the pie to the given map
    * @param id  an UUID identifying the pie
    * @param map A map representing a pie
    */
  case class SetTheTicker(id: java.util.UUID, map: Map[String, Int]) extends API_ExampleService
  case class GetTheTickers() extends API_ExampleService
  case class ResetAllTickers() extends API_ExampleService

  // included here for simplicity
  case object StartThePLC extends API_ExampleService


  // Messages that I will send as answer
  case class TickerEvent(map: Map[String, Int], id: java.util.UUID) extends API_ExampleService
  case class TheTickers(ids: List[java.util.UUID]) extends API_ExampleService

  object attributes {
    val service = "exampleService"
    val version = 1.0
    val api = "to be fixed by macros"
  }
}
import sp.example.{API_ExampleService => api}


/**
  *  This is the actor (the service) that listens for messages on the bus
  *  It keeps track of a set of Pie diagrams that is updated every second
  */
class ExampleService extends Actor with ActorLogging with ExampleServiceLogic {

  // connecting to the pubsub bus using the mediator actor
  import akka.cluster.pubsub._
  import DistributedPubSubMediator.{ Put, Send, Subscribe, Publish }
  val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe("services", self)
  mediator ! Subscribe("spevents", self)


  // The metod that receve messages. Add service logic in a trait so you can test it. Here the focus in on parsing
  // and on the messages on the bus
  def receive = {
    case mess @ _ if {log.debug(s"ExampleService MESSAGE: $mess from $sender"); false} => Unit

    case "tick" =>
      val upd = tick  // Updated the pies on a tick
      tick.foreach{ e =>
        val header = SPAttributes(
          "from" -> api.attributes.service,
          "reqID" -> e.id
        ).addTimeStamp
        val mess = SPMessage.makeJson(header, e)
        mess.foreach(m=> mediator ! Publish("answers", m))  // sends out the updated pies
      }

    case x: String =>
      // SPMessage uses the APIParser to parse the json string

      val message = SPMessage.fromJson(x)

      // extract the header from the message
      val header = for {m <- message; h <- m.getHeaderAs[SPHeader]} yield h

      // extract the body if it is an case class from my api as well as the header.to has my name
      val bodyAPI = for {
        m <- message
        h <- header if h.to == api.attributes.service  // only extract body if it is to me
        b <- m.getBodyAs[api.API_ExampleService]
      } yield b

      // Extract the body if it is a StatusRequest
      val bodySP = for {m <- message; b <- m.getBodyAs[APISP.StatusRequest]} yield b


      // act on the messages from the API. Always add the logic in a trait to enable testing
      // we need to keep the old header, that is why we need to also extract the oldMess here and
      // use if the the oldMess.make(...) below
      for {
        body <- bodyAPI
        h <- header
        oldMess <- message
      } yield {
        val toSend = commands(body) // doing the logic
        val spHeader = h.copy(replyFrom = api.attributes.service, replyID = Some(ID.newID)) // upd header put keep most info
        // We must do a pattern match here to enable the json conversion (SPMessage.make. Or the command can return pickled bodies


        toSend.map{
          case mess @ _ if {println(s"ExampleService sends: $mess"); false} => Unit
          case x: api.API_ExampleService =>
            oldMess.makeJson(h, x.asInstanceOf[api.API_ExampleService]).map { b =>
              mediator ! Publish("answers", b)
            }
          case x: APISP =>
            oldMess.makeJson(h, x.asInstanceOf[APISP]).map { b =>
              mediator ! Publish("answers", b)
            }

        }
      }

      // reply to a statusresponse
      for {
        body <- bodySP
        oldMess <- message
      } yield {
        val mess = oldMess.makeJson(SPHeader(api.attributes.service, "serviceHandler"), APISP.StatusResponse(statusResponse))
        mess.map(m => mediator ! Publish("spevents", m))

      }


  }




  val statusResponse = SPAttributes(
    "service" -> api.attributes.service,
    "api" -> "to be added with macros later",
    "groups" -> List("examples"),
    "attributes" -> api.attributes
  )

  // Sends a status response when the actor is started so service handlers finds it
  override def preStart() = {
    val mess = SPMessage.makeJson(SPHeader(api.attributes.service, "serviceHandler"), statusResponse)
    mess.map(m => mediator ! Publish("spevents", m))
  }

  // A "ticker" that sends a "tick" string to self every 2 second
  import scala.concurrent.duration._
  import context.dispatcher
  val ticker = context.system.scheduler.schedule(2 seconds, 2 seconds, self, "tick")

}

object ExampleService {
  def props = Props(classOf[ExampleService])
}





/*
 * Using a trait to make the logic testable
 */
trait ExampleServiceLogic {

  // This variable stores the pies that are used by the different widgets
  // Initially, it is empty
  var thePies: Map[java.util.UUID, Map[String, Int]] = Map()

  // Matching and doing the stuff based on the message
  // This method returns multiple messages that will be sent out on the bus
  // Services should start and end with an SPACK and SPDONE if there is a
  // a clear start and end of the message stream (so listeners can unregister)
  def commands(body: api.API_ExampleService) = {
    body match {
      case api.StartTheTicker(id) =>
        thePies += id -> Map("first"->10, "second"-> 30, "third" -> 60)
        List(APISP.SPACK(), getTheTickers)
      case api.StopTheTicker(id) =>
        thePies -= id
        List(APISP.SPDone(), getTheTickers)
      case api.SetTheTicker(id, map) =>
        thePies += id -> map
        List(APISP.SPACK(), getTheTickers)
      case api.ResetAllTickers() =>
        thePies = Map()
        List(getTheTickers)
      case x => List(APISP.SPError(s"ExampleService can not understand: $x"))
    }


  }

  def tick = {
    thePies = thePies.map(kv => kv._1 -> updPie(kv._2))
    thePies.map{case (id, p) =>
      api.TickerEvent(p, id)
    }.toList
  }

  def getTheTickers = api.TheTickers(thePies.keys.toList)


  // Just some logic to make the pies change
  val r = Random
  def updPie(pie: Map[String, Int]) = {
    val no = r.nextInt(20)
    val part = r.nextInt(pie.size)
    val key = pie.keys.toList(part)
    val newPie = pie + (key -> (pie(key) + no))
    norm(newPie)
  }

  def norm(pie: Map[String, Int]) = {
    val sum = pie.foldLeft(1){(a, b) => a + b._2}
    pie.map{case (key, v) => key -> ((v.toDouble / sum)*100).toInt}
  }

}