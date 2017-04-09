package sp.elvisDataHandler

import akka.actor._

import scala.util.{Failure, Random, Success, Try}
import sp.domain._
import sp.domain.Logic._
import sp.messages._
import sp.messages.Pickles._

import java.io.File
import java.io.IOException
import java.util

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import scala.collection.immutable.ListMap

import com.codemettle.reactivemq._
import com.codemettle.reactivemq.ReActiveMQMessages._
import com.codemettle.reactivemq.model._

import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization._
import org.json4s._
import org.json4s.JsonDSL._

import elastic.PatientsToElastic
import elastic.GetFromElastic

import sp.elvisDataHandler.{API_PatientEvent => api}


/**
*  This is the actor (the service) that listens for messages on the bus
*/
class ElvisDataHandlerDevice extends Actor with ActorLogging {
  implicit val formats = org.json4s.DefaultFormats ++ org.json4s.ext.JodaTimeSerializers.all // for json serialization

  val patientsToElastic = new PatientsToElastic
  val getFromElastic = new GetFromElastic

  import akka.cluster.pubsub._
  import DistributedPubSubMediator.{ Put, Send, Subscribe, Publish }
  val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe("services", self)
  mediator ! Subscribe("spevents", self)
  mediator ! Subscribe("felvis-data-topic", self)



  // The metod that receive messages. Add service logic in a trait so you can test it. Here the focus is on parsing
  // and on the messages on the bus
  def receive = {
    case mess @ _ if {log.debug(s"ElvisDataHandlerService MESSAGE: $mess from $sender"); false} => Unit

    case x: String => handleRequests(x)

  }

  def handleRequests(x: String): Unit = {
    val mess = SPMessage.fromJson(x)
    matchRequests(mess)
 }

 def matchRequests(mess: Try[SPMessage]) = {
  ElvisDataHandlerComm.extractElvisEvent(mess) map { case (h, b) =>
    b match {
      case api.ElvisData(s) =>
        // Insert data to database
        patientsToElastic.messageReceived(s)
        // Check if new, diff or removed and send corresponding case classes as bodies on akka-bus
        handleMessage(s)
    }
  }
}

val info = SPAttributes(
   "service" -> api.attributes.service,
   "group" -> "runtime"
 )

  def handleMessage(message: String) {
    // figure out what sort of message we just received
    val json: JValue = parse(message) // this jsons the String.
    json.mapField(k => (k._1, k._2)).extract[Map[String, _ ]].keys.head match {
      case "newLoad" | "new"          => newPatient(json)
      case "diff"                     => diffPatient(json)
      case "removed"                  => removedPatient(json)
      case _ => println("WARNING: Searcher received an unrecognized message format. json: "+json)
    }
  }

  def newPatient(json: JValue) {
    val header = SPHeader(from = "elvisDataHandlerService")
    val patientJson = patientsToElastic.initiatePatient(json \ "new" \ "patient")
    val careContactId = (patientJson \ "CareContactId").values.toString
    val patientData = extractNewPatientData(patientJson)
    val events = extractNewPatientEvents(patientJson)
    val body = api.NewPatient(careContactId, patientData, events)
    publishOnAkka(header, body)
  }

  def diffPatient(json: JValue) {
    val header = SPHeader(from = "elvisDataHandlerService")
    val careContactId = (json \ "diff" \ "updates" \ "CareContactId").values.toString
    val patientData = extractDiffPatientData(json \ "diff" \ "updates")
    val newEvents = extractNewEvents(json \ "diff" \ "updates")
    val removedEvents = extractRemovedEvents(json \ "diff" \ "updates")
    val body = api.DiffPatient(careContactId, patientData, newEvents, removedEvents)
    publishOnAkka(header, body)
  }

  def removedPatient(json: JValue) {
    val header = SPHeader(from = "elvisDataHandlerService")
    val careContactId = (json \ "removed" \ "patient" \ "CareContactId").values.toString
    val body = api.RemovedPatient(careContactId)
    publishOnAkka(header, body)
  }

  def extractNewEvents(patient: JValue): List[Map[String, String]] = {
    var tmpList = new ListBuffer[Map[String,String]]()
    val events = castJValueToList[Map[String, JValue]](patient \ "newEvents")
    events.foreach{ m =>
      var tmpMap = Map[String, String]()
      m.foreach{ kv => tmpMap += kv._1 -> kv._2.toString
      }
      tmpList += tmpMap
    }
    return tmpList.toList
  }

  def extractRemovedEvents(patient: JValue): List[Map[String, String]] = {
    var tmpList = new ListBuffer[Map[String,String]]()
    val events = castJValueToList[Map[String, JValue]](patient \ "removedEvents")
    events.foreach{ m =>
      var tmpMap = Map[String, String]()
      m.foreach{ kv => tmpMap += kv._1 -> kv._2.toString }
      tmpList += tmpMap
    }
    return tmpList.toList
  }

  def extractNewPatientEvents(patient: JValue): List[Map[String, String]] = {
    var tmpList = new ListBuffer[Map[String,String]]()
    val events = castJValueToList[Map[String, JValue]](patient \ "Events")
    events.foreach{ m =>
      var tmpMap = Map[String,String]()
      m.foreach{ kv => tmpMap += kv._1 -> kv._2.toString }
      tmpList += tmpMap
    }
    return tmpList.toList
  }

  def extractDiffPatientData(patient: JValue): Map[String, String] = {
    var departmentComment = ""
    var location = ""
    var reasonForVisit = ""
    var team = ""

    val departmentCommentJson = (patient \ "DepartmentComment")
    val locationJson = (patient \ "Location")
    val reasonForVisitJson = (patient \ "ReasonForVisit")
    val teamJson = (patient \ "Team")

    if (departmentCommentJson != JNothing) {
      departmentComment = departmentCommentJson.values.toString
    }
    if (locationJson != JNothing) {
      location = locationJson.values.toString
    }
    if (reasonForVisitJson != JNothing) {
      reasonForVisit = reasonForVisitJson.values.toString
    }
    if (teamJson != JNothing) {
      team = teamJson.values.toString
    }

    val timestamp = (patient \ "timestamp").values.toString
    val careContactId = (patient \ "CareContactId").values.toString
    val patientId = (patient \ "PatientId").values.toString

    return Map("DepartmentComment" -> departmentComment, "Location" -> location, "ReasonForVisit" -> reasonForVisit,
    "Team" -> team, "timestamp" -> timestamp, "CareContactId" -> careContactId, "PatientId" -> patientId)
  }

  def extractNewPatientData(patient: JValue): Map[String, String] = {
    val careContactId = (patient \ "CareContactId").values.toString
    val careContactRegistrationTime = (patient \ "CareContactRegistrationTime").values.toString
    val departmentComment = (patient \ "DepartmentComment").values.toString
    val location = (patient \ "Location").values.toString
    val patientId = (patient \ "PatientId").values.toString
    val reasonForVisit = (patient \ "ReasonForVisit").values.toString
    val team = (patient \ "Team").values.toString
    val visitId = (patient \ "VisitId").values.toString
    val visitRegistrationTime = (patient \ "VisitRegistrationTime").values.toString
    val priority = (patient \ "Priority").values.toString
    val timeToDoctor = (patient \ "TimeToDoctor").values.toString
    val timeToTriage = (patient \ "TimeToTriage").values.toString
    val timeToFinished = (patient \ "TimeToFinished").values.toString
    val timeOfDoctor = (patient \ "TimeOfDoctor").values.toString
    val timeOfTriage = (patient \ "TimeOfTriage").values.toString
    val timeOfFinished = (patient \ "TimeOfFinished").values.toString
    val timestamp = (patient \ "timestamp").values.toString

    return Map("CareContactId" -> careContactId, "CareContactRegistrationTime" -> careContactRegistrationTime,
      "DepartmentComment" -> departmentComment, "Location" -> location, "PatientId" -> patientId,
      "ReasonForVisit" -> reasonForVisit, "Team" -> team, "VisitId" -> visitId,
      "VisitRegistrationTime" -> visitRegistrationTime, "Priority" -> priority, "TimeToDoctor" -> timeToDoctor,
      "TimeToTriage" -> timeToTriage, "TimeToFinished" -> timeToFinished, "TimeOfDoctor" -> timeOfDoctor,
      "TimeOfTriage" -> timeOfTriage, "TimeOfFinished" -> timeOfFinished, "timestamp" -> timestamp
    )
  }

  def publishOnAkka(header: SPHeader, body: api.PatientEvent) {
    val toSend = ElvisDataHandlerComm.makeMess(header, body)
    toSend match {
      case Success(v) =>
        //println(s"About to publish on akka: $v")
        mediator ! Publish("patient-event-topic", v)
      case Failure(e) =>
        println("Failed")
    }
  }

  /** Casts a jValue to a List[A] without crashing on empty lists */
  def castJValueToList[A](list:JValue): List[A] = {
    list match {
      case JNothing => List[A]()
      case _ => list.asInstanceOf[JArray].values.asInstanceOf[List[A]]
    }
  }

}

object ElvisDataHandlerDevice {
  def props = Props(classOf[ElvisDataHandlerDevice])
}
