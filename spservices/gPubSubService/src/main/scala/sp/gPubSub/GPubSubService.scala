package sp.gPubSub

import akka.actor._

import scala.util.{Failure, Random, Success, Try}
import sp.domain._
import sp.domain.Logic._
import sp.messages._
import sp.messages.Pickles._

import java.io.File
import java.io.IOException
import java.util

import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.http.HttpTransport
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.services.pubsub.model.PublishResponse
import com.google.api.services.pubsub.model._
import com.google.api.services.pubsub.{Pubsub, PubsubScopes}
import com.typesafe.scalalogging.LazyLogging

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import scala.collection.immutable.ListMap

trait PubSubEndpoints {
  self =>
  val projectName: String

  def getFQTopicName(topicName: String)(implicit projectName: String): String = s"projects/$projectName/topics/$topicName"
  def getFQSubscriptionName(subName: String)(implicit projectName: String) = s"projects/$projectName/subscriptions/$subName"
}

class GPubSub(applicationName: String, implicit val projectName: String) extends PubSubEndpoints with LazyLogging {

  val Transport = GoogleNetHttpTransport.newTrustedTransport()
  val JsonFactory = JacksonFactory.getDefaultInstance()


  //Ellen: changed the application default credentials file (for gcloud)
  var creds = GoogleCredential.getApplicationDefault()

  def main(): List[String] = {
    val sub = getSubscription("arto-topic", "arto-sub")
    var listOfMessages = new ListBuffer[String]()
    for (x <- sub.pullMessages()) {
      x.foreach { m =>
        val mDecoded = new String(m.getMessage.decodeData())
        listOfMessages += mDecoded
        println(mDecoded)
        sub.ackMessage(Seq(m.getAckId))
      }
    }
    listOfMessages.toList
  }


  class TopicSubscription(val underlying: Subscription)(implicit val ps: Pubsub, implicit val projectName: String)
      extends RichSubscription

  implicit lazy val pubsubClient: Pubsub = getPubsubClient()

  def getPubsubClient(): Pubsub = {
    if (creds.createScopedRequired()) {
      creds = creds.createScoped(PubsubScopes.all())
    }
    val requestFactory = Transport.createRequestFactory(creds)
    val initializer = requestFactory.getInitializer()
    new Pubsub.Builder(Transport, JsonFactory, initializer).setApplicationName(applicationName).build()
  }

  def getSubscription(topicName: String, subName: String): TopicSubscription = {
    new TopicSubscription(pubsubClient.projects().subscriptions().get(getFQSubscriptionName(subName)).execute())
  }


  trait RichSubscription extends PubSubEndpoints {
    val underlying: Subscription
    val SubscriptionName: String = underlying.getName

    implicit val ps: Pubsub

    def pullMessages(batchSize: Int = 5, block: Boolean = false): Option[util.List[ReceivedMessage]] = {
      val pullRequest = new PullRequest()
        .setReturnImmediately(!block)
        .setMaxMessages(batchSize)

      val pullResponse = ps.projects()
        .subscriptions()
        .pull(SubscriptionName, pullRequest).execute()

      val receivedMessages: Option[util.List[ReceivedMessage]] = Option(pullResponse.getReceivedMessages())
      receivedMessages
    }

    def ackMessage(acks: Seq[String]) : Unit = {
      val ackRequest = new AcknowledgeRequest().setAckIds(acks)
      ps.projects().subscriptions().acknowledge(SubscriptionName, ackRequest).execute()
    }
  }
}



// The messages that this service can send and receive is
// is defined using this API structure

package API_GPubSubService {
  sealed trait API_GPubSubService
  // Messages you can send to me
  /**
    * Adds a new pie to the memory with an id
    * @param id an UUID identifying the pie
    */
  case class StartTheTicker(id: java.util.UUID) extends API_GPubSubService

  /**
    * removes the pie with the id
    * @param id an UUID identifying the pie
    */
  case class StopTheTicker(id: java.util.UUID) extends API_GPubSubService

  /**
    * Changes the pie to the given map
    * @param id  an UUID identifying the pie
    * @param map A map representing a pie
    */
  case class SetTheTicker(id: java.util.UUID, map: Map[String, Int]) extends API_GPubSubService
  case class GetTheTickers() extends API_GPubSubService
  case class ResetAllTickers() extends API_GPubSubService

  // included here for simplicity
  case object StartThePLC extends API_GPubSubService


  // Messages that I will send as answer
  case class TickerEvent(map: Map[String, Int], id: java.util.UUID) extends API_GPubSubService
  case class TheTickers(ids: List[java.util.UUID]) extends API_GPubSubService

  object attributes {
    val service = "exampleService"
    val version = 1.0
    val api = "to be fixed by macros"
  }
}
import sp.gPubSub.{API_GPubSubService => api}


/**
  *  This is the actor (the service) that listens for messages on the bus
  *  It keeps track of a set of Pie diagrams that is updated every second
  */
class GPubSubService extends Actor with ActorLogging with ExampleServiceLogic {

  // connecting to the pubsub bus using the mediator actor
  import akka.cluster.pubsub._
  import DistributedPubSubMediator.{ Put, Send, Subscribe, Publish }
  val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe("services", self)
  mediator ! Subscribe("spevents", self)


  // The metod that receve messages. Add service logic in a trait so you can test it. Here the focus in on parsing
  // and on the messages on the bus
  def receive = {
    case mess @ _ if {log.debug(s"GPubSubService MESSAGE: $mess from $sender"); false} => Unit

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
        b <- m.getBodyAs[api.API_GPubSubService]
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
          case mess @ _ if {println(s"GPubSubService sends: $mess"); false} => Unit
          case x: api.API_GPubSubService =>
            oldMess.makeJson(h, x.asInstanceOf[api.API_GPubSubService]).map { b =>
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
  val ticker = context.system.scheduler.schedule(0 seconds, 0.1 seconds, self, "tick")

}

object GPubSubService {
  def props = Props(classOf[GPubSubService])
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
  def commands(body: api.API_GPubSubService) = {
    body match {
      case api.StartTheTicker(id) =>
      var aMap: Map[String,Int] = Map()
      aMap += (" " -> 0)
      println("Listening...")
      thePies += id -> aMap
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
      case x => List(APISP.SPError(s"GPubSubService can not understand: $x"))
    }


  }

  def tick = {
    // Get new messages
    thePies = thePies.map{ kv =>
      var aMap = kv._2
      if (aMap.keys.size != 0) {
        val inst: GPubSub = new GPubSub("Intelligentaakuten","intelligentaakuten-158811")
        val receivedMessagesList = inst.main()
        val mapSize = aMap.size
        var sortedMap = ListMap(aMap.toSeq.sortBy(_._1):_*)
        val key = sortedMap.keys.toList(mapSize-1)
        var counter: Int = sortedMap(key)
        receivedMessagesList.foreach { mess =>
          counter += 1
          if (aMap.contains(mess)) {
            aMap += (mess + counter -> counter)
          } else {
            aMap += (mess -> counter)
          }
        }
      }
      kv._1 -> aMap
    }
    thePies.map{case (id, p) =>
      api.TickerEvent(p, id)
    }.toList
  }

  def getTheTickers = api.TheTickers(thePies.keys.toList)

}




// Trying to handle incoming HTTP POST requests
class TestService extends HttpServlet {

}
