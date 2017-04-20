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

import com.typesafe.scalalogging.LazyLogging

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import scala.collection.immutable.ListMap

import com.typesafe.config.ConfigFactory
import org.json4s._
import org.json4s.native.Serialization
import org.json4s.native.Serialization.{read, write}
import com.github.nscala_time.time.Imports._

import sp.gPubSub.{API_PatientEvent => api}

/**
  *  This is the actor (the service) that listens for messages on the bus
  *  It keeps track of a set of Pie diagrams that is updated every second
  */
class GPubSubDevice extends Actor with ActorLogging {
  implicit val formats = org.json4s.DefaultFormats ++ org.json4s.ext.JodaTimeSerializers.all // for json serialization

  var currentState: List[ElvisPatient] = List()

  // connecting to the pubsub bus using the mediator actor
  import akka.cluster.pubsub._
  import DistributedPubSubMediator.{ Put, Send, Subscribe, Publish }
  val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe("services", self)
  mediator ! Subscribe("spevents", self)

  def receive = {
    case mess @ _ if {log.debug(s"GPubSubService MESSAGE: $mess from $sender"); false} => Unit

    case "pull-mess" => {
      while(true) {
        val messList = getMessages
        if (!messList.isEmpty) {
        //  val body = api.ElvisData(messList(0))
          val patientMap: Map[String, List[ElvisPatient]]  = read[Map[String, List[ElvisPatient]]](messList(0))
          val patients = patientMap("patients") // will fail if wrong structure
          println(s"No of patients: ${patients.size}")
          checkTheDiff(patients)
          Thread.sleep(5000)
        }
      }
    }
  }

  def getMessages() = {
    val inst = new GPubSubClient("Intelligentaakuten","intelligentaakuten-158811")
    inst.main() // returns list of received messages
  }

  def checkTheDiff(ps: List[ElvisPatient]) = {
  if (currentState.isEmpty) {
    currentState = ps
    ps.foreach{p =>
      val newP = NewPatient(getNow, p)
      val json = write(Map("data"->newP, "isa"->"newLoad"))
      sendToEvah(json)
    }
  }
  else if (currentState != ps)  {
    val changes = ps.filterNot(currentState.contains)
    val removed = currentState.filterNot(p => ps.exists(_.CareContactId == p.CareContactId))

    changes.map{ p =>
      val diffP = diffPat(p, currentState.find(_.CareContactId == p.CareContactId))
      diffP match {
        case None => {
          val newP = NewPatient(getNow, p)
          val json = write(Map("data"->newP, "isa"->"new"))
          sendToEvah(json)
        }
        case Some(d) => {
          val diffPatient = PatientDiff(d._1, d._2, d._3)
          val json = write(Map("data"->diffPatient, "isa"->"diff"))
          sendToEvah(json)
          }
        }
      }

    removed.map{p =>
      val removedPat = RemovedPatient(getNow, p)
      val json = write(Map("data"->removedPat, "isa"->"removed"))
      sendToEvah(json)
    }
    currentState = ps
  }
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
  DateTime.now(DateTimeZone.forID("Europe/Stockholm"))
}

}

object GPubSubDevice {
  def props = Props(classOf[GPubSubDevice])
  implicit val system = ActorSystem("SP")
  val elvisPublisher = system.actorOf(Props[GPubSubDevice], "elvis-publisher")
  while (true) {
    Thread.sleep(500)
    elvisPublisher ! "pull-mess"
  }
}
