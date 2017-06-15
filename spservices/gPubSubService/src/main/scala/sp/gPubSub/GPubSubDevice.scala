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

import com.google.protobuf.Timestamp
import com.google.cloud.pubsub.spi.v1.Subscriber
import com.google.pubsub.v1.PubsubMessage

import scala.collection.mutable.ListBuffer
import sp.domain._
import sp.domain.Logic._
import datahandler.ElvisDataHandlerDevice
import sp.gPubSub.API_Data.ElvisPatient
import sp.gPubSub.{API_Data => api}
import sp.gPubSub.{API_PatientEvent => sendApi}

object GPubSubDevice {
  def props = Props(classOf[GPubSubDevice])
}


case object Ticker
case class APubSubMess(mess: PubsubMessage)

class GPubSubDevice extends PersistentActor with ActorLogging with DiffMagic {
  override def persistenceId = "gPubSub"

  import akka.stream.scaladsl._
  import akka.stream._
  import akka.NotUsed
  //import com.qubit.pubsub.akka._
  //import com.qubit.pubsub.client._
  //import com.qubit.pubsub.client.retry._
  import scala.concurrent.Await
  import scala.concurrent.duration._
  import com.google.common.base.Charsets

  implicit val system = context.system


  val project = "double-carport-162512"
  val topic = "erica-snap"
  val subscription = "snapper_temp"





  // Testing google pubsub client

  import com.google.cloud.Identity
  import com.google.cloud.Role
  import com.google.cloud.ServiceOptions
  import com.google.cloud.pubsub.spi.v1.PagedResponseWrappers.ListSubscriptionsPagedResponse
  import com.google.cloud.pubsub.spi.v1.TopicAdminClient
  import com.google.cloud.pubsub.spi.v1.SubscriptionAdminClient
  import com.google.iam.v1.Binding
  import com.google.iam.v1.Policy
  import com.google.iam.v1.TestIamPermissionsResponse
  import com.google.pubsub.v1.ListSubscriptionsRequest
  import com.google.pubsub.v1.ProjectName
  import com.google.pubsub.v1.PushConfig
  import com.google.pubsub.v1.Subscription
  import com.google.pubsub.v1.SubscriptionName
  import com.google.pubsub.v1.TopicName


  /** Example of deleting a subscription. */
  def deleteSubscription(subscriptionId: String) = { // [START pubsub_delete_subscription]
    Try {
      val subscriptionAdminClient = SubscriptionAdminClient.create
      try {
        val subscriptionName = SubscriptionName.create(project, subscriptionId)
        subscriptionAdminClient.deleteSubscription(subscriptionName)
        println("subscriber deleted: "+ subscriptionId)
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
        println("subscriber created: "+ subscriptionId)

        subscription
      } finally if (subscriptionAdminClient != null) subscriptionAdminClient.close()
    }
    // [END pubsub_create_pull_subscription]
  }


  println("starting pubSub")
  deleteSubscription(subscription)
  val subTry = createSubscription(topic, subscription)
  println(subTry)
  val res = subTry.map{s =>

    import com.google.cloud.pubsub.spi.v1.AckReplyConsumer
    import com.google.cloud.pubsub.spi.v1.MessageReceiver
    import com.google.pubsub.v1.PubsubMessage
    val receiver = new MessageReceiver() {
      override def receiveMessage(message: PubsubMessage, consumer: AckReplyConsumer): Unit = {
        self ! APubSubMess(message)
        consumer.ack()
      }
    }

    Subscriber.defaultBuilder(s.getNameAsSubscriptionName, receiver).build()

  }

  res.map(_.startAsync())











//  val decider = {
//    case x =>
//      println("Stream problems in gPubSub")
//      println(x.getMessage)
//      Supervision.Restart
//  }

  implicit val materializer = ActorMaterializer()


  // connecting to the pubsub bus using the mediator actor
  import akka.cluster.pubsub._
  import DistributedPubSubMediator.{ Put, Send, Subscribe, Publish }
  val mediator = DistributedPubSub(context.system).mediator

  // A "ticker" that sends a "tick" string to self every 1 minute
  import scala.concurrent.duration._
  import context.dispatcher
  val ticker = context.system.scheduler.schedule(0 seconds, 1 minutes, self, "tick")

  var state: List[api.EricaEvent] = List()
  val elvisActor = context.actorOf(ElvisDataHandlerDevice.props, "elvisdatahandler")
  var timeWhenMessageReceived = System.currentTimeMillis






//  override def receive = {
//
//    //case Ticker => clearState() // Propably used for testing. Only locally present.
//    //case mess @ _ if {println(s"GPubSubService MESSAGE: $mess from $sender"); false} => Unit
//  }





  def checkTimeSinceMessageReceived() {
    val timeDiff = Math.abs(timeWhenMessageReceived - System.currentTimeMillis)
    val timeLimit = 1000*60*5 // 5 minutes currently
    if (timeDiff > timeLimit) {
      elvisActor ! "NoElvisDataFlowing"
    }
  }

  val timeout = 10 second
  //val testTopic = PubSubTopic(project, s"erica-snap")
  //val testSubscription = PubSubSubscription(project, s"testSub")

  //import com.qubit.pubsub.akka.attributes._
  //val attributes = Attributes(List())
  /**  PubSubStageBufferSizeAttribute(100),
    PubSubStageMaxRetriesAttribute(100),
    PubSubPublishTimeoutAttribute(10.seconds))*/

  //val client = com.qubit.pubsub.client.retry.RetryingPubSubClient(com.qubit.pubsub.client.grpc.PubSubGrpcClient())
  //client.createSubscription(testSubscription, testTopic)


//  val toJsonString: Flow[PubSubMessage, String, NotUsed] = Flow[PubSubMessage]
//    .map{m => {
//      elvisActor ! "ElvisDataFlowing"
//      timeWhenMessageReceived = System.currentTimeMillis
//      if (messT.isEmpty) messT = m.publishTs
//
//      val res = for {
//        ct <- m.publishTs
//        pT <- messT if ct.isAfter(pT)
//      } yield {
//        messT = m.publishTs
//        println("is newer")
//        new String(m.payload, Charsets.UTF_8)
//      }
//      /*
//      val fw = new FileWriter("oldNALDataJSON.txt", true)
//      try {
//        fw.write(res.getOrElse("") + "\n\n")
//      }
//      finally fw.close()*/
//      res.getOrElse("")
//      }
//    }



  val jsonToList: Flow[String, List[api.ElvisPatient], NotUsed] = Flow[String]
    .map{json => SPAttributes.fromJson(json)}
    .collect{
      case Some(attr) => attr.tryAs[List[api.ElvisPatient]]("patients")
    }
    .collect{
      case Success(xs) => {
        println("Nr of pat: " + xs.length)
        xs
      }
    }

    val makeDiff: Flow[List[api.ElvisPatient], List[api.EricaEvent], NotUsed] = Flow[List[api.ElvisPatient]]
   .statefulMapConcat{ () =>
     var prev = List[api.ElvisPatient]()

     xs => {
       println("A snap: " + xs.size)
       val res = checkTheDiff(xs, prev)
       prev = xs
       res
     }
   }


  //val s = Source.fromGraph(new PubSubSource(testSubscription)).withAttributes(attributes)

  val mediatorSink = Sink.foreach{ s: List[api.EricaEvent] =>
    if (!s.isEmpty) {
      self ! s
      state = state ++ s
      println("")
      s.foreach(println)
      println("number of events: " + state.size)
      println("")
      elvisActor ! state
    }
  }

  val receiveRecover: Receive = {
    case x: String =>
      println("recover")
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
         println("is newer")
         mess.getData().toStringUtf8()
       }

       val newEvents: List[api.EricaEvent] = (for {
        json <- res
        attr <- SPAttributes.fromJson(json)
        xs <- attr.tryAs[List[api.ElvisPatient]]("patients").toOption
       } yield {
         println("A snap: " + xs.size)
         val res = checkTheDiff(xs, prev)
         prev = xs
         res.flatten
       }).getOrElse(List())

       if (newEvents.nonEmpty){
         val eventsJson = write(newEvents)
         persist(eventsJson) { events =>
           state = newEvents ++ state
           state = filterOldEvents(state)
           newEvents.foreach(println)
           println("number of events: " + state.size)
           println("")

           elvisActor ! state

         }
       }







   }

  def filterOldEvents(ev: List[api.EricaEvent]) = {
    val threeDaysAgo = getNow.minusDays(3)
    val reEv = ev.filter(e => isAfter(e, threeDaysAgo))
    ev.sortWith(isLatest)
  }

  //val test = s via toJsonString via jsonToList via makeDiff runWith(mediatorSink)

//  test.onComplete{
//    case Success(res) =>
//    case Failure(res) =>
//      println("stream failed:" + res.getMessage)
//      self
//  }

  override def postStop(): Unit = {
    materializer.shutdown()

    res.map(_.stopAsync())

    println("OFF")
  }


}


trait DiffMagic {
  import org.json4s._
  import org.json4s.native.Serialization
  import org.json4s.native.Serialization.{read, write}
  import com.github.nscala_time.time.Imports._
  //implicit val formats = org.json4s.DefaultFormats ++ org.json4s.ext.JodaTimeSerializers.all


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
    old.map {
      case prev: api.ElvisPatient => {
        (Map(
          "CareContactId" -> Some(Extraction.decompose(curr.CareContactId)),
          "CareContactRegistrationTime" -> diffThem(prev.CareContactRegistrationTime, curr.CareContactRegistrationTime),
          "DepartmentComment" -> diffThem(prev.DepartmentComment, curr.DepartmentComment),
          "Location" -> {
            val res = diffThem(prev.Location, curr.Location)
            if (res.nonEmpty) println("patient moved: " + prev.Location +" -> "+curr.Location)
            if (prev.Location.isEmpty && curr.Location.nonEmpty) println("prev loc empty: " + prev.Location +" -> "+curr.Location)
            res
          },
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
  }

  def diffThem[T](prev: T, current: T): Option[JValue]= {
    if (prev == current) None
    else Some(Extraction.decompose(current))
  }



  val timePattern = "yyyy-MM-dd'T'HH:mm:ss.SSS"
  def getNow = {
    DateTime.now(DateTimeZone.forID("Europe/Stockholm"))
  }

  def getNowString = {
    getNow.toString(timePattern)
  }


  def getEventTime(e: api.EricaEvent) = {
    Try{DateTime.parse(e.TimeEvent, DateTimeFormat.forPattern(timePattern));}
  }
  def isLatest(f: api.EricaEvent, s: api.EricaEvent): Boolean = {
    (for {
      first <- getEventTime(f)
      last <- getEventTime(s)
    } yield last.isAfter(last)
    ).getOrElse(false)
  }

  def isAfter(f: api.EricaEvent, t: DateTime): Boolean = {
    (for {
      first <- getEventTime(f)
    } yield first.isAfter(t)
      ).getOrElse(false)
  }

}
