package sp.gPubSub

import akka.actor._

import scala.util.{Failure, Random, Success, Try}
import sp.messages._
import sp.messages.Pickles._
import java.util

import sp.domain._
import sp.domain.Logic._
import sp.gPubSub.{API_PatientEvent => api}




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

  println("hej")

  implicit val system = context.system
  implicit val materializer = ActorMaterializer()


  // connecting to the pubsub bus using the mediator actor
  import akka.cluster.pubsub._
  import DistributedPubSubMediator.{ Put, Send, Subscribe, Publish }
  val mediator = DistributedPubSub(context.system).mediator
//  mediator ! Subscribe("services", self)
//  mediator ! Subscribe("spevents", self)
//  mediator ! Subscribe("elvis-diff", self)

  import context.dispatcher
  val ticker = context.system.scheduler.schedule(100 seconds, 100 seconds, self, Ticker)


  def receive = {
    case Ticker => clearState()
    case mess @ _ if {println(s"GPubSubService MESSAGE: $mess from $sender"); false} => Unit

  }

  val timeout = 10 second
  val project = "intelligentaakuten-158811"
  val testTopic = PubSubTopic(project, s"elvis-snap")
  val testSubscription = PubSubSubscription(project, s"getSnap")

  import com.qubit.pubsub.akka.attributes._
  val attributes = Attributes(List(
    PubSubStageBufferSizeAttribute(10),
    PubSubStageMaxRetriesAttribute(10),
    PubSubPublishTimeoutAttribute(10.seconds)))

  val client = com.qubit.pubsub.client.retry.RetryingPubSubClient(com.qubit.pubsub.client.grpc.PubSubGrpcClient())
  client.createSubscription(testSubscription, testTopic)


  val toJsonString:Flow[PubSubMessage, String, NotUsed] = Flow[PubSubMessage]
    .map{m => new String(m.payload, Charsets.UTF_8)}

  val jsonToList: Flow[String, List[ElvisPatient], NotUsed] = Flow[String]
    .map{json => SPAttributes.fromJson(json)}
    .collect{
      case Some(attr) => attr.tryAs[List[ElvisPatient]]("patients")
    }
    .collect{
      case Success(xs) => xs
    }

  val makeDiff: Flow[List[ElvisPatient], String, NotUsed] = Flow[List[ElvisPatient]]
    .mapConcat(checkTheDiff)

  val s = Source.fromGraph(new PubSubSource(testSubscription)).withAttributes(attributes)

  val testSink = Sink.foreach{ x: Any =>
    println()
    println(x)
    println()
  }

  val mediatorSink = Sink.foreach{ s: String =>
    val h = SPHeader(from = "gPubSubDevice")
    val b = api.ElvisData(s)
    val mess = SPMessage.makeJson(h, b).get
    println("Sending a patient diff")
    mediator ! Publish("elvis-diff", mess)
  }

  val test = s via toJsonString via jsonToList via makeDiff runWith(mediatorSink)

  override def postStop(): Unit = {
    println("OFF")
    println(materializer.isShutdown)
  }


}


trait DiffMagic {
  import org.json4s._
  import org.json4s.native.Serialization
  import org.json4s.native.Serialization.{read, write}
  import com.github.nscala_time.time.Imports._
  //implicit val formats = org.json4s.DefaultFormats ++ org.json4s.ext.JodaTimeSerializers.all


  var currentState: List[ElvisPatient] = List()

  def clearState() = currentState = List()


  def checkTheDiff(ps: List[ElvisPatient]): List[String] = {
    if (currentState.isEmpty) {
      currentState = ps
      ps.map{p =>
        val newP = NewPatient(getNow, p)
        val json = write(Map("data"->newP, "isa"->"newLoad"))
        json
      }
    }
    else if (currentState != ps)  {
      val changes = ps.filterNot(currentState.contains)
      val removed = currentState.filterNot(p => ps.exists(_.CareContactId == p.CareContactId))

      val upd = changes.map{ p =>
        val diffP = diffPat(p, currentState.find(_.CareContactId == p.CareContactId))
        diffP match {
          case None => {
            val newP = NewPatient(getNow, p)
            val json = write(Map("data"->newP, "isa"->"new"))
            json
          }
          case Some(d) => {
            val diffPatient = PatientDiff(d._1, d._2, d._3)
            val json = write(Map("data"->diffPatient, "isa"->"diff"))
            json
          }
        }
      }

      val rem = removed.map{p =>
        val removedPat = RemovedPatient(getNow, p)
        val json = write(Map("data"->removedPat, "isa"->"removed"))
        json
      }
      currentState = ps

      upd ++ rem
    }
    else List()
  }

  def sendToEvah(json: String) = {
    val header = SPHeader(from = "gPubSubService")
    println(json)
    /**
    val elvisDataSPMessage = GPubSubComm.makeMess(header, api.ElvisData(json))
  elvisDataSPMessage match {
    case Success(v) =>
      println(s"Publishing elvis message: $v")
      mediator ! Publish("elvis-data-topic", v)
    case Failure(e) =>
      println("Failed")
  }*/
  }

  def toNewPat(p: ElvisPatient)= {
    val t = p.CareContactRegistrationTime
    NewPatient(t,p)
  }


  def diffPat(curr: ElvisPatient, old: Option[ElvisPatient])={
    old.map {
      case prev: ElvisPatient => {
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