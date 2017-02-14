package sp.example

import akka.actor._
import akka.cluster.pubsub.DistributedPubSubMediator.Publish

import scala.util.{Failure, Random, Success, Try}
import sp.domain._
import sp.domain.Logic._
import sp.example.API_ExampleService.{GetTheTickers, SetTheTicker, StartTheTicker, StopTheTicker}
import sp.messages._



// The messages that this service can send and receive is
// is defined using this API structure

sealed trait API_ExampleService
object API_ExampleService {
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
  case object GetTheTickers extends API_ExampleService
  case class ResetAllTickers() extends API_ExampleService

  // included here for simplicity
  case object StartThePLC extends API_ExampleService


  // Messages that I will send as answer
  case class TickerEvent(map: Map[String, Int], id: java.util.UUID) extends API_ExampleService
  case class TheTickers(ids: List[java.util.UUID]) extends API_ExampleService

  val service = "exampleService"
}


/**
  *  This is the actor (the service) that listens for messages on the bus
  *  It keeps track of a set of Pie diagrams that is updated every second
  */
class ExampleService extends Actor with ActorLogging with ExampleServiceLogic {

  // conneting to the pubsub bus using the mediator actor
  import akka.cluster.pubsub._
  import DistributedPubSubMediator.{ Put, Send, Subscribe, Publish }
  val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe("services", self)
  mediator ! Subscribe("spevents", self)



  // A "ticker" that sends a "tick" string to self every 2 second
  import scala.concurrent.duration._
  import context.dispatcher
  val ticker = context.system.scheduler.schedule(2 seconds, 2 seconds, self, "tick")


  // The metod that receve messages. Add service logic in a trait so you can test it. Here the focus in on parsing
  // and on the messages on the bus
  def receive = {
    case mess @ _ if {log.debug(s"ExampleService MESSAGE: $mess from $sender"); false} => Unit

    case "tick" =>
      val upd = tick  // Updated the pies on a tick
      upd.foreach{e =>
        val header = SPAttributes(
          "from" -> API_ExampleService.service,
          "reqID" -> e.id
        ).addTimeStamp
        val mess = SPMessage(header, APIParser.writeJs(e)).toJson
        mediator ! Publish("answers", mess)  // sends out the updated pies
      }
    case x: String =>
      // SPMessage uses the APIParser to parse the json string
      SPMessage.fromJson(x) match {
        case Success(mess) =>

          // forward the API to another method if it is my API
          // It returns the json AST (upickle) that will be used in SPMessage
          val res = getMyMessage(mess).map(commands).getOrElse(List())

          // If the message is a status request. This method extract it and creates a response
          val statusResp = answerToStatusRequest(mess)

          // fixing the header, by adding a replyFrom key
          // The normal case is not to change the header, but to return the key-values as is
          // this may change in the futre
          val newH = mess.header + SPAttributes("replyFrom" -> API_ExampleService.service, "replyID" -> ID.newID)

          // If it was a message to me with my api, i will reply here
          res.foreach{ body =>
            val replyMessage = APIParser.write(SPMessage(newH, body))
            if (mess.header.getAs[Boolean]("answerDirect").getOrElse(false))  // a special flag from the sender to answer directly to the sender via akka instead of the bus
              sender() ! replyMessage
            else {
              mediator ! Publish("answers", replyMessage)
            }
          }

          // If the message was a status request, we reply on the spevent bus
          statusResp.foreach{body =>
            val replyMessage = APIParser.write(SPMessage(newH, body))
            mediator ! Publish("spevents", replyMessage)
          }

        case Failure(err) => {}
      }


  }


  // Matches if the message is to me
  // by cheking the header.to field and if the body is of my type.
  def getMyMessage(spMess : SPMessage) = {
    val to = spMess.header.getAs[String]("to").getOrElse("") // extracts the header.to, if it is to me
    val body = Try{APIParser.readJs[API_ExampleService](spMess.body)}
    if (body.isSuccess && to == API_ExampleService.service)
      Some(body.get)
    else
      None
  }

  def answerToStatusRequest(spMess: SPMessage) = {
    val body = Try{APIParser.readJs[APISP.StatusRequest](spMess.body)}
    body.map{r =>
      APIParser.writeJs(
        APISP.StatusResponse(statusResponse)
      )
    }.toOption
  }

  val statusResponse = SPAttributes(
    "service" -> API_ExampleService.service,
    "api" -> "to be added with macros later",
    "groups" -> List("examples"),
    "allowRequests" -> true
  )

  // Sends a status response when the actor is started so service handlers finds it
  override def preStart() = {
    mediator ! Publish("spevents", APIParser.write(SPMessage(SPAttributes("from"->API_ExampleService.service), APIParser.writeJs(statusResponse))))
  }

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
  def commands(body: API_ExampleService) = {
    body match {
      case API_ExampleService.StartTheTicker(id) =>
        thePies += id -> Map("first"->10, "second"-> 30, "third" -> 60)
        List(APIParser.writeJs(APISP.SPACK()), APIParser.writeJs(getTheTickers))
      case API_ExampleService.StopTheTicker(id) =>
        thePies -= id
        List(APIParser.writeJs(APISP.SPDone()), APIParser.writeJs(getTheTickers))
      case API_ExampleService.SetTheTicker(id, map) =>
        thePies += id -> map
        List(APIParser.writeJs(APISP.SPACK()), APIParser.writeJs(getTheTickers))
      case API_ExampleService.GetTheTickers =>
        List(APIParser.writeJs(getTheTickers))
      case API_ExampleService.ResetAllTickers() =>
        thePies = Map()
        List(APIParser.writeJs(getTheTickers))
      case x => List(APIParser.writeJs(APISP.SPError(s"ExampleService can not understand: $x")))
    }


  }

  def tick = {
    thePies = thePies.map(kv => kv._1 -> updPie(kv._2))
    thePies.map{case (id, p) =>
      API_ExampleService.TickerEvent(p, id)
    }.toList
  }

  def getTheTickers = API_ExampleService.TheTickers(thePies.keys.toList)


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