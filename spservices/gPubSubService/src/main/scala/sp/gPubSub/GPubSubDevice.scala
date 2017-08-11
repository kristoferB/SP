package sp.gPubSub

import java.time.ZonedDateTime
import java.io.FileWriter
import java.text.SimpleDateFormat

import akka.actor._
import akka.persistence._

import scala.util.{Failure, Random, Success, Try}
import sp.messages._
import sp.messages.Pickles._
import java.util

import scala.collection.mutable.ListBuffer
import sp.domain._
import sp.domain.Logic._
import datahandler.ElvisDataHandlerDevice
import sp.gPubSub.API_Data.ElvisPatient
import sp.gPubSub.{API_Data => api}
import sp.gPubSub.{API_PatientEvent => sendApi}


import com.google.protobuf.Timestamp
import com.google.pubsub.v1.PubsubMessage


object GPubSubDevice {
  def props = Props(classOf[GPubSubDevice])
}


case object Ticker
case class APubSubMess(mess: PubsubMessage)
case object FailedPubSub

class GPubSubDevice extends PersistentActor with DiffMagic {
  override def persistenceId = "gPubSub"

  import akka.stream.scaladsl._
  import akka.stream._
  import akka.NotUsed

  implicit val system = context.system


  val project = "double-carport-162512"
  val topic = "erica-snap"
  val subscription = "snapper_temp"





  var state: List[api.EricaEvent] = List()
  val elvisActor = context.actorOf(ElvisDataHandlerDevice.props, "elvisdatahandler")
  var timeWhenMessageReceived = System.currentTimeMillis

  def makeNewElvisPubSub = new ElvisPubSuber(project, topic, subscription, self)
  var elvisPubSub: ElvisPubSuber = makeNewElvisPubSub



  def checkTimeSinceMessageReceived() {
    val timeDiff = Math.abs(timeWhenMessageReceived - System.currentTimeMillis)
    val timeLimit = 1000*60*5 // 5 minutes currently
    var sendFailed = true
    if (timeDiff > timeLimit) {
      elvisActor ! "NoElvisDataFlowing"
    }
    if (timeDiff > timeLimit*4 && sendFailed) {
      self ! FailedPubSub
      sendFailed = false
    }
    if (timeDiff < timeLimit) sendFailed = true


  }

  // A "ticker" that sends a "tick" string to self every 1 minute
  import scala.concurrent.duration._
  import context.dispatcher
  val timeout = 10 second
  val ticker = context.system.scheduler.schedule(0 seconds, 1 minutes, self, "tick")


  val receiveRecover: Receive = {
    case x: String =>
      elvisActor ! "recovered" // dummy
 }


  var messT: Option[Timestamp] = None
  var prev: List[ElvisPatient] = List()

   val receiveCommand: Receive = {
     case "tick" => checkTimeSinceMessageReceived()

     case APubSubMess(mess) =>
       elvisActor ! "ElvisDataFlowing"
       timeWhenMessageReceived = System.currentTimeMillis
       if (messT.isEmpty) messT = Some(mess.getPublishTime)

       val res = for {
         pT <- messT if mess.getPublishTime.getSeconds > pT.getSeconds
       } yield {
         messT = Some(mess.getPublishTime)
         log.debug("Recieved elvis snapshot is newer than the previous one.")
         mess.getData().toStringUtf8()
       }

       val newEvents: List[api.EricaEvent] = (for {
        json <- res
        attr <- SPAttributes.fromJson(json)
        xs <- attr.tryAs[List[api.ElvisPatient]]("patients").toOption
       } yield {
         log.debug("An elvis snapshot with " + xs.size + " ElvisPatients was recieved.")
         val res = checkTheDiff(xs, prev)
         prev = xs
         res.flatten
       }).getOrElse(List())

       if (newEvents.nonEmpty){
         val eventsJson = write(newEvents)
         persist(eventsJson) { events =>
           state = newEvents ++ state
           state = filterOldEvents(state)
           newEvents.foreach(e => log.debug("New EricaEvent persisted: " + e))
           log.debug("Number of EricaEvents in current state: " + state.size)

           elvisActor ! state

         }
       }

     case FailedPubSub =>
       elvisPubSub.res.map(_.stopAsync())
       elvisPubSub = makeNewElvisPubSub


   }

  def filterOldEvents(ev: List[api.EricaEvent]) = {
    val threeDaysAgo = getNow.minusDays(3)
    val reEv = ev.filter(e => isAfter(e, threeDaysAgo))
    reEv.sortWith(isLatest)
  }


  override def postStop(): Unit = {
    elvisPubSub.res.map(_.stopAsync())
    log.debug(this + " said: OFF")
  }


}




trait DiffMagic {
  import org.json4s._
  import org.json4s.native.Serialization
  import org.json4s.native.Serialization.{read, write}
  import com.github.nscala_time.time.Imports._
  //implicit val formats = org.json4s.DefaultFormats ++ org.json4s.ext.JodaTimeSerializers.all
  val log = org.slf4j.LoggerFactory.getLogger(getClass.getName)

  var currentState: List[api.ElvisPatient] = List()

  def clearState() = currentState = List()

  def checkTheDiff(ps: List[api.ElvisPatient], currentState: List[api.ElvisPatient]): List[List[api.EricaEvent]] = {
    val dataAggregation = new elastic.DataAggregation
    if (currentState.isEmpty) {
      ps.map{p =>
        dataAggregation.convertToEricaEvents(p)
      }
    } else if (currentState != ps)  {
      val changes = ps.filterNot(currentState.contains)
      val removed = currentState.filterNot(p => ps.exists(_.CareContactId == p.CareContactId))

      val upd = changes.map{ p =>
        val diffP = diffPat(p, currentState.find(_.CareContactId == p.CareContactId))
        diffP match {
          case None => {
            dataAggregation.convertToEricaEvents(p)
          }
          case Some(d) => {
            dataAggregation.convertDiffToEricaEvents(d._1, d._2, d._3)
          }
        }
      }

      val rem = removed.map{p =>
        List(api.EricaEvent(
              p.CareContactId,
              "RemovedPatient",
              "NA",
              getNowString,
              "",
              "",
              "",
              p.VisitId,
              getNowString))
      }
      upd ++ rem

    } else {
      List()
    }
  }

  def sendToEvah(json: String) = {
    val header = SPHeader(from = "gPubSubService")
  }


  def diffPat(curr: api.ElvisPatient, old: Option[api.ElvisPatient]) = {
    old.map { prev =>
      newLocation(curr, old)
      (Map(
        "CareContactId" -> Some(Extraction.decompose(curr.CareContactId)),
        "CareContactRegistrationTime" -> diffThem(prev.CareContactRegistrationTime, curr.CareContactRegistrationTime),
        "DepartmentComment" -> diffThem(prev.DepartmentComment, curr.DepartmentComment),
        "Location" -> diffThem(prev.Location, curr.Location),
        "PatientId" -> Some(Extraction.decompose(curr.PatientId)),
        "ReasonForVisit" -> diffThem(prev.ReasonForVisit, curr.ReasonForVisit),
        "Team" -> diffThem(prev.Team, curr.Team),
        "VisitId" -> diffThem(prev.VisitId, curr.VisitId),
        "VisitRegistrationTime" -> diffThem(prev.VisitRegistrationTime, curr.VisitRegistrationTime),
        "timestamp" -> Some(Extraction.decompose(getNowString))
      ).filter(kv=> kv._2 != None).map(kv=> kv._1 -> kv._2.get),
        curr.Events.filterNot(prev.Events.contains),
        prev.Events.filterNot(curr.Events.contains))

    }
  }

  def newLocation(curr: api.ElvisPatient, old: Option[api.ElvisPatient]) = {
    old.foreach{o =>
      if (curr.Location != o.Location){
        log.info("Patient " + curr.CareContactId + " moved from location " + o.Location +" to "+ curr.Location )
      }
    }
  }

  def diffThem[T](prev: T, current: T): Option[JValue]= {
    if (prev == current) None
    else Some(Extraction.decompose(current))
  }



  val timePattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
  def getNow = {
    DateTime.now(DateTimeZone.forID("Europe/Stockholm"))
  }

  def getNowString = {
    getNow.toString(timePattern)
  }


  def getEventTime(e: api.EricaEvent) = {
    val res = Try{DateTime.parse(e.TimeEvent, DateTimeFormat.forPattern(timePattern));}
    if (res.isFailure) log.warn("" + res)
    res
  }
  def isLatest(f: api.EricaEvent, s: api.EricaEvent): Boolean = {
    (for {
      first <- getEventTime(f)
      last <- getEventTime(s)
    } yield last.isAfter(first)
    ).getOrElse(false)
  }

  def isAfter(f: api.EricaEvent, t: DateTime): Boolean = {
    (for {
      first <- getEventTime(f)
    } yield first.isAfter(t)
      ).getOrElse(false)
  }

}


class ElvisPubSuber(project: String, topic: String, subscription: String, sendTo: ActorRef) {

  import com.google.cloud.pubsub.v1.SubscriptionAdminClient
  import com.google.pubsub.v1.PushConfig
  import com.google.pubsub.v1.Subscription
  import com.google.pubsub.v1.SubscriptionName
  import com.google.pubsub.v1.TopicName
  import com.google.api.core.ApiService.Listener
  import com.google.protobuf.Timestamp
  import com.google.cloud.pubsub.v1.Subscriber
  import com.google.api.core._
  import com.google.common.util.concurrent.MoreExecutors

  val log = org.slf4j.LoggerFactory.getLogger(getClass.getName)

  /** Example of deleting a subscription. */
  def deleteSubscription(subscriptionId: String) = { // [START pubsub_delete_subscription]
    Try {
      val subscriptionAdminClient = SubscriptionAdminClient.create
      try {
        val subscriptionName = SubscriptionName.create(project, subscriptionId)
        subscriptionAdminClient.deleteSubscription(subscriptionName)
        log.debug("Subscription " + subscriptionId + " DELETED")
        subscriptionName
      } finally if (subscriptionAdminClient != null) subscriptionAdminClient.close()
    }
    // [END pubsub_delete_subscription]
  }


  /** Example of creating a pull subscription for a topic. */
  @throws[Exception]
  def createSubscription(topicId: String, subscriptionId: String) = { // [START pubsub_create_pull_subscription]
    Try {
      val subscriptionAdminClient = SubscriptionAdminClient.create
      try { // eg. projectId = "my-test-project", topicId = "my-test-topic"
        val topicName = TopicName.create(project, topicId)
        // eg. subscriptionId = "my-test-subscription"
        val subscriptionName = SubscriptionName.create(project, subscriptionId)
        // create a pull subscription with default acknowledgement deadline
        val subscription = subscriptionAdminClient.createSubscription(subscriptionName, topicName, PushConfig.getDefaultInstance, 0)
        log.debug("Subscription " + subscriptionId + " CREATED")

        subscription
      } finally if (subscriptionAdminClient != null) subscriptionAdminClient.close()
    }
    // [END pubsub_create_pull_subscription]
  }





  log.info("Connecting to Google pubsub. Subscription: " + subscription + " Topic: " + topic)
  println("Connecting to Google pubsub. Subscription: " + subscription + " Topic: " + topic)
  val x = deleteSubscription(subscription)
  println("delete:")
  println(x)

  val subTry = createSubscription(topic, subscription)
  log.debug("Subscription attempt resulted in: " + subTry)
  val res = subTry.map{s =>

    import com.google.cloud.pubsub.v1.AckReplyConsumer
    import com.google.cloud.pubsub.v1.MessageReceiver
    import com.google.pubsub.v1.PubsubMessage
    val receiver = new MessageReceiver() {
      override def receiveMessage(message: PubsubMessage, consumer: AckReplyConsumer): Unit = {
        log.info("New elvis snapshot recieved from Google pubsub.")
        sendTo ! APubSubMess(message)
        consumer.ack()
      }
    }


    val x = Subscriber.defaultBuilder(s.getNameAsSubscriptionName, receiver).build()
    x.addListener(new Listener() {
      override def failed(from: ApiService.State, failure: Throwable ) {
        log.warn("pubSub failed: "+ failure.getMessage)
        log.warn("pubSub failed state: "+ from)
        sendTo ! FailedPubSub
      }
    }, MoreExecutors.directExecutor())

    x

  }

  res.map(_.startAsync())
}
