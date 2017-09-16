package sp.example

import akka.actor._

import scala.util.{Failure, Random, Success, Try}
import sp.domain._
import sp.domain.Logic._


/**
  *  This is the actor (the service) that listens for messages on the bus
  *  It keeps track of a set of Pie diagrams that is updated every second
  */
class ExampleService extends Actor
  with ActorLogging with
  ExampleServiceLogic with
  sp.service.ServiceSupport {

  val instanceID = ID.newID


  // Setting up the status response that is used for identifying the service in the cluster
  val statusResponse = ExampleServiceInfo.attributes.copy(
    instanceID = Some(this.instanceID)
  )
  // starts waiting for ping requests from service handler
  triggerServiceRequestComm(statusResponse)

  // subscribe to the topic where requests are sent for this API
  subscribe(APIExampleService.topicRequest)


  // The method that receive messages. Add service logic in a trait so you can test it. Here the focus in on parsing
  // and on the messages on the bus
  def receive = {
    // enable the line below for troubleshooting
    //case mess @ _ if {println(s"ExampleService MESSAGE: $mess from $sender"); false} => Unit

    case x: String =>
      // extract the body if it is a case class from my api as well as the header.to has my name
      // act on the messages from the API. Always add the logic in a trait to enable testing
      val bodyAPI = for {
        mess <- SPMessage.fromJson(x)
        h <- mess.getHeaderAs[SPHeader] if h.to == instanceID.toString || h.to == APIExampleService.service  // only extract body if it is to me
        b <- mess.getBodyAs[APIExampleService.Request]
      } yield {
        val spHeader = h.swapToAndFrom
        sendAnswer(SPMessage.makeJson(spHeader, APISP.SPACK()))

        val toSend = commands(b) // doing the logic
        sendAnswer(SPMessage.makeJson(spHeader, toSend))
        sendAnswer(SPMessage.makeJson(spHeader, APISP.SPACK()))
      }

    case Tick =>
      val upd = tick  // Updated the pies on a tick
      tick.foreach{ e =>
        val header = SPHeader(
          from = ExampleServiceInfo.attributes.service,
          reqID = e.id
        )
        val mess = SPMessage.makeJson(header, e)
        sendAnswer(mess)  // sends out the updated pies
      }
  }

  def sendAnswer(mess: String) = publish(APIExampleService.topicResponse, mess)



  // A "ticker" that sends a "tick" string to self every 2 second
  import scala.concurrent.duration._
  import context.dispatcher
  val ticker = context.system.scheduler.schedule(2 seconds, 2 seconds, self, Tick)

}

object ExampleService {
  def props = Props(classOf[ExampleService])
}

case object Tick


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
  def commands(body: APIExampleService.Request) = {
    body match {
      case APIExampleService.StartTheTicker(id) =>
        thePies += id -> Map("first"->10, "second"-> 30, "third" -> 60)
      case APIExampleService.StopTheTicker(id) =>
        thePies -= id
      case APIExampleService.SetTheTicker(id, map) =>
        thePies += id -> map
      case APIExampleService.ResetAllTickers =>
        thePies = Map()
      case APIExampleService.GetTheTickers => 
    }
    getTheTickers
  }

  def tick = {
    thePies = thePies.map(kv => kv._1 -> updPie(kv._2))
    thePies.map{case (id, p) =>
      APIExampleService.TickerEvent(p, id)
    }.toList
  }

  def getTheTickers = APIExampleService.TheTickers(thePies.keys.toList)


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

