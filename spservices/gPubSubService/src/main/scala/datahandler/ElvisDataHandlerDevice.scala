package datahandler

import akka.actor._

import scala.util.{Failure, Random, Success, Try}
import scala.util.matching.Regex
import sp.domain._
import sp.domain.Logic._
import sp.messages._
import sp.messages.Pickles._

import akka.cluster.pubsub.DistributedPubSubMediator.Publish

import scala.collection.mutable.ListBuffer

import java.util.Date
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.chrono.ChronoLocalDate

import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization._
import org.json4s._
import org.json4s.JsonDSL._

import sp.gPubSub.{API_PatientEvent => api}
import sp.gPubSub.{API_Data => dataApi}
import datahandler.{API_Patient => patientApi}
//import spgui.circuit.{SPGUICircuit, UpdateGlobalAttributes}
//import japgolly.scalajs.react._


/**
*  This is the actor (the service) that listens for messages on the bus
*/
class ElvisDataHandlerDevice extends Actor with ActorLogging {
  implicit val formats = org.json4s.DefaultFormats ++ org.json4s.ext.JodaTimeSerializers.all // for json serialization

  val externalFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
  val externalSpecialFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
  val internalFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS")
  val timeFormat = new SimpleDateFormat("HH:mm")


  import akka.cluster.pubsub._
  import DistributedPubSubMediator.{ Put, Send, Subscribe, Publish }
  val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe("services", self)
  mediator ! Subscribe("spevents", self)
  mediator ! Subscribe("elvis-diff", self)

  var state: Map[Int, dataApi.EricaPatient] = Map()
  var guiState: Map[String, patientApi.Patient] = Map()

  /**
  Receives incoming messages on the AKKA-bus.
  */
  def receive = {
    case "NoElvisDataFlowing" => {
      println("No updates in some time... Sending to front-end")
      publishOnAkka(SPHeader(from = "elvisDataHandler"), api.ElvisDataFlowing(false), "spevents")
    }
    case "ElvisDataFlowing" => {
      publishOnAkka(SPHeader(from = "elvisDataHandler"), api.ElvisDataFlowing(true), "spevents")
    }
    case x: List[dataApi.EricaEvent] => handleElvisData(x)
  }



/**
Initial handling of ERICA-events that came as messages on the bus.
*/
def handleElvisData(data: List[dataApi.EricaEvent]) {
  state = Map() // clear state before assembling new that was received
  var visited: Map[Int, Boolean] = Map()
  data.foreach{ e => {
    if (!visited.contains(e.CareContactId)) {
      val patientEvents = data.filter(_.CareContactId == e.CareContactId)
      val removedPatientEvents = patientEvents.filter(d => d.Category == "RemovedPatient")
      val patientEventsExclRemoved = patientEvents.filter(d => d.Category != "RemovedPatient")
      val removedEvent = getLatestEventByInternalFormatter(removedPatientEvents) // gets latest "RemovedPatient"-event for specific patient
      val latestEvent = getLatestEventByInternalFormatter(patientEventsExclRemoved) // gets latest event, except "RemovedPatient"-events, for specific patient
      if (removedPatientEvents.isEmpty) {
        // Patient has never been removed and will be constructed.
        state += e.CareContactId -> constructPatient(patientEvents.filter(p => !isRemovedEvent(p, removedPatientEvents)))
      } else if (!isLatest(removedEvent,latestEvent)) {
        // Patient has been removed, but has another event (not related to RemovedPatient-event) after the removed event, indicating that the patient is still present.
        state += e.CareContactId -> constructPatient(patientEvents.filter(p => (!isRemovedEvent(p, removedPatientEvents) || p.Category != "RemovedPatient")))
      } else {
        // Patient has RemovedPatient as latest event and should be removed from the local state.
        state -= e.CareContactId
      }
      visited += e.CareContactId -> true
      }
    }
  }
  println("Patients with valid clinic: " + state.filter( pa => (isValidClinic(pa._2.Clinic))).size)
  createVisualizablePatients(state.filter( pa => (isValidClinic(pa._2.Clinic))))
}

/**
Checks if ERICA-event has been removed by using identification based on event title and start time of event.
*/
def isRemovedEvent(event: dataApi.EricaEvent, events: List[dataApi.EricaEvent]): Boolean = {
  events.exists(e => (e.Title == event.Title && e.Start == event.Start))
}

/**
Checks if the event in the first argument were created/received before or after the event in the second argument.
Returns true if the first event was the latest, false otherwise.
(If first event is None, this method will never be called. See method handleElvisData)
*/
def isLatest(firstEvent: Option[dataApi.EricaEvent], secondEvent: Option[dataApi.EricaEvent]): Boolean = {
  val res = for {
    f <- firstEvent
    s <- secondEvent
  } yield internalFormatter.parse(f.TimeEvent).after(internalFormatter.parse(s.TimeEvent))
  res.getOrElse(true)
}

/**
Returns the latest event among the list of events given as argument, using the internal date formatter (i.e. date set in SP, not in ELVIS).
Returns None if the list is empty.
*/
def getLatestEventByInternalFormatter(events: List[dataApi.EricaEvent]): Option[dataApi.EricaEvent] = {
  val init: Option[dataApi.EricaEvent] = None
  events.foldLeft(init){(a, b) =>
    a match {
      case None => Some(b)
      case Some(e) => {
        val prevT = internalFormatter.parse(e.TimeEvent)
        val eventT = internalFormatter.parse(b.TimeEvent)
        if (eventT.after(prevT)) Some(b) else a
      }
    }
  }
}

/**
Returns the latest event among list of events, using the external date formatter (i.e. date set in ELVIS).
*/
def getLatestEventByExternalFormatter(events: List[dataApi.EricaEvent]): Option[dataApi.EricaEvent] = {
  val init: Option[dataApi.EricaEvent] = None
  events.foldLeft(init){(a, b) =>
    a match {
      case None => Some(b)
      case Some(e) => {
        val prevT = externalSpecialFormatter.parse(e.Start)
        val eventT = externalSpecialFormatter.parse(b.Start)
        if (eventT.after(prevT)) Some(b) else a
      }
    }
  }
}

/**
Checks if the clinic-string given as argument represents a valid clinic at NÄL.
Returns true if valid, false otherwise.
*/
def isValidClinic(clinic: String): Boolean = {
  clinic match {
    case "NAKME" | "NAKM" | "NAKKI" | "NAKOR" | "NAKBA" | "NAKOR" | "NAKÖN" => true
    case _ => false
  }
}

/**
Reconstructs the local state (gui-state) by converting data structured in EricaPatient:s to visualizable patient objects, whose structure is understood by front-end in SP.
*/
def createVisualizablePatients(patients: Map[Int, dataApi.EricaPatient]) {
  guiState = patients.values.foldLeft(Map.empty[String, patientApi.Patient]){ (a, p) =>
    a + (p.CareContactId.toString -> (patientApi.Patient(
      p.CareContactId.toString,
      patientApi.Priority(p.Priority, ""),
      patientApi.Attended(p.IsAttended, p.DoctorId, ""),
      patientApi.Location(decodeLocation(p.Location), ""),
      patientApi.Team(getTeam(p.Clinic, p.Location, p.ReasonForVisit), p.Clinic, p.ReasonForVisit, ""),
      patientApi.Examination(p.OnExamination, ""),
      patientApi.LatestEvent(p.LatestEvent, p.LatestEventTimeDiff, false, ""),
      patientApi.Plan(p.HasPlan, ""),
      patientApi.ArrivalTime(getArrivalFormat(p.VisitRegistrationTime), ""),
      patientApi.Debugging(p.Clinic, p.ReasonForVisit, p.Location),
      patientApi.Finished(p.IsFinished, p.IsFinished, "")
    )))
  }
  publishOnAkka(SPHeader(from = "elvisDataHandler"), api.State(guiState), "state-event-topic")
}

/**
Constructs patient information based on EricaEvent:s and gathers it in EricaPatient objects.
*/
def constructPatient(events: List[dataApi.EricaEvent]): dataApi.EricaPatient = {
  val (latestEvent, timeDiff) = getLatestEvent(events.filter( e => (e.Category == "T" || e.Category == "U" || e.Category == "Q")))
  val (isAttended, doctorId) = getIsAttended(events.filter(_.Category == "T"))
  dataApi.EricaPatient(
        events(0).CareContactId,
        getLatestEventValue(events.filter(_.Category == "DepartmentCommentUpdate")),
        getLatestEventValue(events.filter(_.Category == "LocationUpdate")),
        getLatestEventValue(events.filter(_.Category == "ReasonForVisitUpdate")),
        getLatestEventValue(events.filter(_.Category == "TeamUpdate")),
        getPriority(events.filter(_.Category == "P")),
        latestEvent,
        timeDiff,
        isAttended,
        doctorId,
        getNeedsAttention(timeDiff, getPriority(events.filter(_.Category == "P"))),
        getOnExamination(events),
        getHasPlan(events.filter(_.Category == "T")),
        getIsFinished(events.filter(_.Category == "T")),
        events(0).VisitId,
        getLatestEventValue(events.filter(_.Category == "VisitRegistrationTimeUpdate"))
      )
}

/**
Publishes messages on the AKKA-bus.
*/
def publishOnAkka(header: SPHeader, body: api.Event, topic: String) {
  val toSend = ElvisDataHandlerComm.makeMess(header, body)
  mediator ! Publish(topic, toSend)
}

/**
* Returns "hh:mm (day)"-format of argument date string in format yyyy-MM-dd'T'HH:mm:ssZ.
*/
def getArrivalFormat(startTimeString: String): String = {
  val startTime = externalFormatter.parse(startTimeString.replaceAll("Z$", "+0000"))
  val timeString = timeFormat.format(startTime)
  val diff = getTimeDiffExternalFormat(startTimeString)
  val days = (diff / (1000*60*60*24))
  var dayString = ""
  days match {
    case 0 => dayString = ""
    case 1 => dayString = "(igår)"
    case (n: Long) => dayString = "(+" + n + " d.)"
  }
  timeString + " " + dayString
}

/**
Returns the time difference (in milliseconds) between a given start time and now.
Argument startTimeString must be received in date-time-format: yyyy-MM-ddTHH:mm:ss.SSSZ
*/
def getTimeDiffExternalFormat(startTimeString: String): Long = {
  val now: Long = System.currentTimeMillis
  val startTime = Try(externalFormatter.parse(startTimeString.replaceAll("Z$", "+0000")))
  startTime match {
    case Success(s) => Math.abs(now - s.getTime()) // returns diff in millisec
    case _ => 0
  }
}

/**
Identifies the team based on clinic, location and reason for visit strings.
*/
def getTeam(clinic: String, location: String, reasonForVisit: String): String = {
  reasonForVisit match {
    case "AKP" => "stream"
    case "ALL" | "TRAU" => "process"
    case "B" | "MEP" | "HÖFT" => {
        if (location != "") {
          location.charAt(0) match {
            case 'G' => "medicin gul"
            case 'B' => "medicin blå"
            case 'P' => "process"
            case _ => {
              clinic match {
                case "NAKME" => "medicin"
                case "NAKKI" => "kirurgi"
                case "NAKOR" => "ortopedi"
                case "NAKOR" | "NAKBA" | "NAKÖN" => "jour"
                case "NAKM" => "NAKM"
                case _ => "no-match"
              }
            }
          }
        } else {
          clinic match {
            case "NAKME" => "medicin"
            case "NAKKI" => "kirurgi"
            case "NAKOR" => "ortopedi"
            case "NAKOR" | "NAKBA" | "NAKÖN" => "jour"
            case "NAKM" => "NAKM"
            case _ => "no-match"
          }
        }
    }
    case _ => "no-match"
  }
}

/**
* Checks if patient needs attention, according to some specific guidelines.
*/
def getNeedsAttention(timeDiff: Long, priority: String): Boolean = {
  priority match {
    case "Blue" | "Green" | "Yellow" => if (timeDiff > 3600000) true else false
    case "Orange" => if (timeDiff > 1200000) true else false
    case "Red" => true
    case _ => false
  }
}

/**
Checks if events contains event with title "Klar", indicating that patient is finished.
*/
def getIsFinished(events: List[dataApi.EricaEvent]): Boolean = {
  events.exists(e => (e.Title == "Klar"))
}

/**
Checks if events contains event with title "Plan", indicating that patient has a plan assigned.
*/
def getHasPlan(events: List[dataApi.EricaEvent]): Boolean = {
  events.exists(e => (e.Title == "Plan"))
}

/**
Checks if events contains event with title "Rö/klin" and unvalid end time, indicating that patient is on examination.
*/
def getOnExamination(events: List[dataApi.EricaEvent]): Boolean = {
  events.exists(e => (e.Title == "Rö/klin" && e.End == "0001-01-02T23:00:00Z"))
}

/**
Checks if events contains event with title "Läkare", indicating that patient has seen a doctor.
*/
def getIsAttended(events: List[dataApi.EricaEvent]): (Boolean, String) = {
  val latestDoctorEvent = getLatestEventByExternalFormatter(events.filter(f => (f.Title == "Läkare")))
  val res = for {
    e <- latestDoctorEvent
  } yield (true, e.Value)
  res.getOrElse((false, "NA"))
}

/**
Returns the value (as string) of the latest event among list of events.
*/
def getLatestEventValue(events: List[dataApi.EricaEvent]): String = {
  val latestEvent = getLatestEventByInternalFormatter(events)
  val res = for {
    l <- latestEvent
  } yield l.Value
  res.getOrElse("")
}

/**
Returns the title and time difference of the latest event among list of events.
*/
def getLatestEvent(events: List[dataApi.EricaEvent]): (String, Long) = {
  val latestEvent = getLatestEventByExternalFormatter(events)
  val res = for {
    l <- latestEvent
  } yield (l.Title, getTimeDiffExternalFormat(l.Start))
  res.getOrElse(("", 0))
}

/**
Checks events for specific patient and identifies and translates its priority.
Returns priority as string: "NotTriaged", "Blue", "Green", "Yellow", "Orange" or "Red".
*/
def getPriority(events: List[dataApi.EricaEvent]): String = {
  val latestPrioEvent: Option[dataApi.EricaEvent] = getLatestEventByExternalFormatter(events.filter(p => isValidTriageColor(p.Title)))
  val latestPrio = for {
    l <- latestPrioEvent
  } yield l.Title
  translate(latestPrio.getOrElse("Otriagerad"))
}

/**
Translates ELVIS-priority to priority-string understood by front-end in SP.
*/
def translate(str: String): String = {
  str match {
    case "Otriagerad" => "NotTriaged"
    case "Blå" => "Blue"
    case "Grön" => "Green"
    case "Gul" => "Yellow"
    case "Röd" => "Red"
    case _ => str
  }
}

/**
Filters out a room number from an ELVIS-location.
*/
def decodeLocation(location: String): String = {
  val locationWithTeam = """([GgBbPp])(.{0,2})""".r
  val infectionLocation = """([Ii])([Nn]*)([1-4])""".r
  val emergencyLocation = """([Aa])([Kk]*)([1-4])""".r
  location match {
    case locationWithTeam(team, optional) => optional
    case emergencyLocation(a, k, nr) => a + nr
    case infectionLocation(i, n, nr) => i + nr
    case _ => location
  }
}

/**
Checks if string is valid triage color.
*/
def isValidTriageColor(string: String): Boolean = {
  string match {
    case "Blå" | "Grön" | "Gul" | "Orange" | "Röd" => true
    case _ => false
  }
}

val info = SPAttributes(
   "service" -> api.attributes.service,
   "group" -> "runtime"
 )

}

object ElvisDataHandlerDevice {
  def props = Props(classOf[ElvisDataHandlerDevice])
}
