package sp.patientcardsservice

import sp.domain._
import sp.domain.Logic._
import sp.messages._
import sp.messages.Pickles._

import akka.actor._
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import scala.util.{ Try, Success, Failure }
import scala.util.Random.nextInt
import scala.util.matching.Regex
import scala.collection.mutable.ListBuffer

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

import sp.patientcardsservice.{API_PatientEvent => api}

class PatientCardsDevice extends Actor with ActorLogging {
  import akka.cluster.pubsub._
  import DistributedPubSubMediator.{ Subscribe, Publish }
  val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe("services", self)
  mediator ! Subscribe("spevents", self)
  mediator ! Subscribe("patient-event-topic", self)

  def receive = {
    case mess @ _ if {log.debug(s"ExampleService MESSAGE: $mess from $sender"); false} => Unit

    case x: String => handleRequests(x)

  }

  def handleRequests(x: String): Unit = {
    val mess = SPMessage.fromJson(x)
    matchRequests(mess)
  }

  def matchRequests(mess: Try[SPMessage]) = {
    val header = SPHeader(from = "patientCardsService")
    PatientCardsComm.extractPatientEvent(mess) map { case (h, b) =>
      b match {
        case api.NewPatient(careContactId, patientData, events) => {
          println("+ New patient: " + careContactId)
          //extractNewPatient(api.NewPatient(careContactId, patientData, events))
          for (ne <- extractNewPatient(api.NewPatient(careContactId, patientData, events))){
            publishOnAkka(header, ne)
          }
        }
        case api.DiffPatient(careContactId, patientData, newEvents, removedEvents) => {
          println("Diff patient: " + careContactId)
          //publishOnAkka(header, api.DiffPatient(careContactId, patientData, newEvents, removedEvents))
        }
        case api.RemovedPatient(careContactId) => {
          println("- Removed patient: " + careContactId)
          //publishOnAkka(header, api.RemovedPatient(careContactId))
        }
      }
    }
  }
  def extractDiffPatient(dp: api.DiffPatient): List[api.PatientProperty] = {
    var tempList = new ListBuffer[api.PatientProperty]()
    np.events.foreach{ e => if(e("Category") == ("T"|"U")){
      val tmpp: api.PatientProperty = e("Type") match {
        case "EJKÖLAPP" =>
        case "EXT MOTT" =>
        case "KLAR" =>
        case "KÖLAPP" =>
        case "LÄKARE" =>
        case "OMSKOORD" =>
        case "OMVÅRDNAD" =>
        case "RTG-REMISS" =>
        case "TRIAGE" =>
        case "VPLKOORD" =>
        case "VÄND" =>

        // case "Priority" => updatePriority(np.patientData("Priority"), np.patientData("TimeOfTriage")) //println(np.patientData("CareContactId")+" : will have propety: "+np.patientData(k))
        // case "TimeOfDoctor" => updateAttended( !("0000-01-24T00:00:00.000Z" == np.patientData("TimeOfDoctor")), ( for ( e <- np.events; if (e("Title") == "Läkare") ) yield e("Value") ).mkString, np.patientData("TimeOfDoctor"))
        // case "Location" => updateLocation(np.patientData("Location")) // Should this have timestamp
        // case "Team" => updateTeam(np.patientData("Team"), np.patientData("ReasonForVisit"), np.patientData("Location"))
        case _ => api.Undefined() // ???
      }
    }
    tempList = tempList ++ List(tmpp)
    //println(tempList+": TMPLIST")
    }
    val listigt: List[api.PatientProperty] = tempList.toList
    //println(listigt.filter(_ != (api.Undefined())).filter(_ != api.Attended(false,"","0000-01-24T00:00:00.000Z") )+" : LISTIGT")
    listigt.filter(_ != (api.Undefined())).filter(_ != api.Attended(false,"","0000-01-24T00:00:00.000Z")
  }

  def extractNewPatient(np: api.NewPatient): List[api.PatientProperty] = {
    var tempList = new ListBuffer[api.PatientProperty]()
    np.patientData.keys.foreach{ k =>
      val tmpp: api.PatientProperty = k match {
        case "Priority" => updatePriority(np.patientData("Priority"), np.patientData("TimeOfTriage")) //println(np.patientData("CareContactId")+" : will have propety: "+np.patientData(k))
        case "TimeOfDoctor" => updateAttended( !("0000-01-24T00:00:00.000Z" == np.patientData("TimeOfDoctor")), ( for ( e <- np.events; if (e("Title") == "Läkare") ) yield e("Value") ).mkString, np.patientData("TimeOfDoctor"))
        case "Location" => updateLocation(np.patientData("Location")) // Should this have timestamp
        case "Team" => updateTeam(np.patientData("Team"), np.patientData("ReasonForVisit"), np.patientData("Location"))
        case _ => api.Undefined() // ???
      }
      tempList = tempList ++ List(tmpp)
      //println(tempList+": TMPLIST")
    }
    val listigt: List[api.PatientProperty] = tempList.toList
    println(listigt.filter(_ != (api.Undefined())).filter(_ != api.Attended(false,"","0000-01-24T00:00:00.000Z") )+" : LISTIGT")
    listigt.filter(_ != (api.Undefined())).filter(_ != api.Attended(false,"","0000-01-24T00:00:00.000Z"))
  }

  def happenedAfter(laterTimestamp: String, timestamp: String ): Boolean = {
    timestampToODT(laterTimestamp).isAfter(timestampToODT(timestamp))
  }

  def timestampToODT(timestamp: String): OffsetDateTime = {
    OffsetDateTime.parse(timestamp, DateTimeFormatter.ISO_INSTANT)
  }

  def updatePriority(newPriority: String, timestamp: String): api.PriorityEvent = {
    newPriority match {
      case "Grön" => api.Green(timestamp)
      case "Gul" =>  api.Yellow(timestamp)
      case "Orange" => api.Orange(timestamp)
      case "Röd" => api.Red(timestamp)
      case _ => api.NotTriaged(timestamp)
    }
  }

  def updateAttended(att: Boolean, attId: String, timestamp: String): api.Attended = {
    api.Attended(att, attId, timestamp)
  }

  def updateLocation(roomNr: String): api.RoomNr = {
    api.RoomNr(roomNr.replaceAll("[^0-9ivr]",""))
  }

  def updateTeam(klinik: String, reasonForVisit: String, roomNr: String): api.Team = {
    api.Team(decodeTeam(reasonForVisit, roomNr), decodeKlinik(klinik))
  }

  def decodeTeam(reasonForVisit: String, roomNr: String): String = {
    (reasonForVisit, roomNr.charAt(0)) match {
      case ("APK", roomNr) => "Streamteam"
      case (reasonForVisit, 'B') => "Blå"
      case (reasonForVisit, 'G') => "Gul"
      case (reasonForVisit, 'P') => "Process"
      case _ => "Röd"
    }
  }
  def decodeKlinik(klinik: String): String = {
    klinik match {
      case "NAKKI" => "kirurgi"
      case "NAKME" => "medicin"
      case "NAKOR" => "ortopedi"
      case "NAKBA" | "NAKGY" | "NAKÖN" => "bgö"
    }
  }

  def updateLatestEvent(newE: api.LatestEvent, oldE: api.LatestEvent): api.LatestEvent = {
    if (happenedAfter(newE.timestamp, oldE.timestamp))
    api.LatestEvent(newE.latestEvent, newE.timestamp)
    else api.LatestEvent(oldE.latestEvent, oldE.timestamp)
  }

  def updateArrivalTime(ts: String): api.ArrivalTime = {
    api.ArrivalTime(ts)
  }


  def publishOnAkka(header: SPHeader, body: api.PatientProperty) {
    val toSend = PatientCardsComm.makeMess(header, body)
    toSend match {
      case Success(v) =>
      mediator ! Publish("patient-cards-widget-topic", v) // Publishes on bus for widget to receive
      case Failure(e) =>
      println("Failed")
    }
  }

}

object PatientCardsDevice {
  def props = Props(classOf[PatientCardsDevice])
}
