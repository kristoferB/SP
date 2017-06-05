package sp.gPubSub

import akka.actor._

import scala.util.{Failure, Random, Success, Try}
import sp.messages._
import sp.messages.Pickles._
import java.util

import scala.collection.mutable.ListBuffer

import sp.domain._
import sp.domain.Logic._
import datahandler.ElvisDataHandlerDevice
import sp.gPubSub.{API_Data => api}
import sp.gPubSub.{API_PatientEvent => sendApi}

object GPubSubDevice {
  def props = Props(classOf[GPubSubDevice])
}


case object Ticker

class GPubSubDevice extends Actor with ActorLogging with DiffMagic {

  import akka.stream.scaladsl._
  import akka.stream._
  import akka.NotUsed
  import com.qubit.pubsub.akka._
  import com.qubit.pubsub.client._
  import com.qubit.pubsub.client.retry._
  import scala.concurrent.Await
  import scala.concurrent.duration._
  import com.google.common.base.Charsets

  implicit val system = context.system


  val decider: Supervision.Decider = {
    case x =>
      println("Stream problems in gPubSub")
      println(x.getMessage)
      Supervision.Restart
  }

  implicit val materializer = ActorMaterializer(
    ActorMaterializerSettings(system).withSupervisionStrategy(decider)
  )


  // connecting to the pubsub bus using the mediator actor
  import akka.cluster.pubsub._
  import DistributedPubSubMediator.{ Put, Send, Subscribe, Publish }
  val mediator = DistributedPubSub(context.system).mediator

  var state: List[api.EricaEvent] = List()
  val elvisActor = context.actorOf(ElvisDataHandlerDevice.props, "elvisdatahandler")

  def receive = {
    case Ticker => clearState() // Propably used for testing. Only locally present.
    case mess @ _ if {println(s"GPubSubService MESSAGE: $mess from $sender"); false} => Unit
  }

  val timeout = 10 second
  val project = "intelligentaakuten-158811"
  val testTopic = PubSubTopic(project, s"elvis-snap")
  val testSubscription = PubSubSubscription(project, s"getSnap")

  import com.qubit.pubsub.akka.attributes._
  val attributes = Attributes(List())
//    PubSubStageBufferSizeAttribute(100),
//    PubSubStageMaxRetriesAttribute(100),
//    PubSubPublishTimeoutAttribute(10.seconds)))

  val client = com.qubit.pubsub.client.retry.RetryingPubSubClient(com.qubit.pubsub.client.grpc.PubSubGrpcClient())
  client.createSubscription(testSubscription, testTopic)


  val toJsonString: Flow[PubSubMessage, String, NotUsed] = Flow[PubSubMessage]
    .map{m => {
      new String(m.payload, Charsets.UTF_8)
      }
    }

  val jsonToList: Flow[String, List[api.ElvisPatient], NotUsed] = Flow[String]
    .map{json => SPAttributes.fromJson(json)}
    .collect{
      case Some(attr) => attr.tryAs[List[api.ElvisPatient]]("patients")
    }
    .collect{
      case Success(xs) => {
        xs
      }
    }

  val makeDiff: Flow[List[api.ElvisPatient], String, NotUsed] = Flow[List[api.ElvisPatient]]
    .mapConcat(checkTheDiff)


  val s = Source.fromGraph(new PubSubSource(testSubscription)).withAttributes(attributes)

  val mediatorSink = Sink.foreach{ s: String =>
    val dataAggregation = new elastic.DataAggregation
    var newState = dataAggregation.handleMessage(s)
    if (!newState.isEmpty) {
      state = state ++ newState

      println("")
      newState.foreach(println)

      elvisActor ! state
      //val h = SPHeader(from = "gPubSubDevice")
      //val b = sendApi.ElvisData(state)
      //val mess = SPMessage.makeJson(h, b).get
      //mediator ! Publish("elvis-diff", mess)
    }
  }

  val test = s via toJsonString via jsonToList via makeDiff runWith(mediatorSink)

  override def postStop(): Unit = {
    materializer.shutdown()
    println("OFF")
    println(test)
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

  def checkTheDiff(ps: List[api.ElvisPatient]): List[String] = {
    val res = if (currentState.isEmpty) {
      currentState = ps
      ps.map{p =>
        val newP = api.NewPatient(getNow, p)
        val json = write(Map("data"->newP, "isa"->"newLoad"))
        json
      }
    }
    else if (currentState != ps)  {

      println("A change identified")
      println("no of pats in snap: " + ps.size)


      val changes = ps.filterNot(currentState.contains)
      val removed = currentState.filterNot(p => ps.exists(_.CareContactId == p.CareContactId))




      val upd = changes.map{ p =>
        val diffP = diffPat(p, currentState.find(_.CareContactId == p.CareContactId))
        diffP match {
          case None => {
            val newP = api.NewPatient(getNow, p)
            val json = write(Map("data"->newP, "isa"->"new"))
            json
          }
          case Some(d) => {
            val diffPatient = api.PatientDiff(d._1, d._2, d._3)
            val json = write(Map("data"->diffPatient, "isa"->"diff"))
            json
          }
        }
      }

      val rem = removed.map{p =>
        val removedPat = api.RemovedPatient(getNow, p)
        val json = write(Map("data"->removedPat, "isa"->"removed"))
        json
      }


      println("")
      println("no of pats changed: " + changes.size)
      println("no of pats removed: " + removed.size)
      println("no of pats actually changed: " + upd.size)
      upd.foreach(println)
      println("")

      upd ++ rem

    }
    else {
      List()
    }
    currentState = ps
    res
  }

  def sendToEvah(json: String) = {
    val header = SPHeader(from = "gPubSubService")
  }


  def diffPat(curr: api.ElvisPatient, old: Option[api.ElvisPatient])={
    old.map {
      case prev: api.ElvisPatient => {
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
          "timestamp" -> Some(Extraction.decompose(getNow))
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

  def getNow = {
    DateTime.now(DateTimeZone.forID("Europe/Stockholm")).toString("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
  }
}
