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
            //println("fromNEW: "+header, ne)
            publishOnAkka(header, ne)
          }
        }
        case api.DiffPatient(careContactId, patientData, newEvents, removedEvents) => {
          println("Diff patient: " + careContactId)
          for (de <- extractDiffPatient(api.DiffPatient(careContactId, patientData, newEvents, removedEvents))){
            println("fromDIFF: "+header, de)
            publishOnAkka(header, de)
          }
        }
        case api.RemovedPatient(careContactId) => {
          println("- Removed patient: " + careContactId)
          //publishOnAkka(header, api.RemovedPatient(careContactId))
        }
      }
    }
  }

  // /*
  // Takes two LatestEvent and returns the most recent one
  // **/
  // def latestEventOf(newE: api.LatestEvent, oldE: api.LatestEvent): api.LatestEvent = {
  //   if (happenedAfter(newE.timestamp, oldE.timestamp))
  //   api.LatestEvent(newE.latestEvent, newE.timestamp)
  //   else api.LatestEvent(oldE.latestEvent, oldE.timestamp)
  // }
  //
  // /*
  //   Takes a list of events and returns a list containing only the most recent one of each type
  // **/
  // def filterMostRecent(pplist: List[api.LatestEvent]): List[api.LatestEvent] = {
  //
  //
  //   latestEventOf()
  // }

  /*
  Takes a DiffPatient and returns relevant events from fields "updates" and "newEvents"
  **/
  def extractDiffPatient(dp: api.DiffPatient): List[api.PatientProperty] = {
    var tempList = new ListBuffer[api.PatientProperty]()
    println("dp.newEvents: "+dp.newEvents.headOption)
    tempList = tempList ++ extractDiffPatientNewEvents(dp.careContactId, dp.newEvents)
    // tempList = tempList ++ List(extractDiffPatientUpdate(dp.patientData)) // atm not implemented
    val listigt: List[api.PatientProperty] = tempList.toList
    println("filterd EXTRDP"+listigt.filter(_ != (api.Undefined(dp.patientData("careContactId")))).filter(_ != api.Attended(dp.careContactId, false,"","0000-01-24T00:00:00.000Z") ))
    listigt.filter(_ != (api.Undefined(dp.careContactId)))
  }

  /*
  Takes the "newEvents"-field of a DiffPatient and returns corresponding
  list of PatientProperty
  **/
  def extractDiffPatientNewEvents(ccid: String, dp: List[Map[String, String]]): List[api.PatientProperty] = { // TODO may need to account for RemovedEvents. No idea how they work.
    //println("extractEvent: "+extractEvent(ccid, dp.head))
    //println("inside EXTRDPNE: "+ccid+ Option(dp.head))
    var tempList = new ListBuffer[api.PatientProperty]()
    dp.foreach{ e =>
      tempList = tempList ++ List(extractEvent(ccid, e))
    }
    val listigt: List[api.PatientProperty] = tempList.toList
    println("filterd EXTRDPNE"+listigt.filter(_ != (api.Undefined(ccid))).filter(_ != api.Attended(ccid, false,"","0000-01-24T00:00:00.000Z") ))
    listigt.filter(_ != (api.Undefined(ccid)))
  }

  // ### HOW TO SOLVE THE PROBLEM THAT UPDATES DON'T CONTAIN COMPLETE INFO?
  // /*
  // Takes the "updates"-field of a DiffPatient and returns corresponding
  // PatientProperty
  // (Note that careContactId is a key in "updates", so it is passed inside "u")
  // **/
  // def extractDiffPatientUpdate(u: Map[String, String]): api.PatientProperty = {
  //   var tempList = new ListBuffer[api.PatientProperty]()
  //   u.keys.foreach{ k =>
  //     val tmpp: api.PatientProperty = k match {
  //       case "ReasonForVisit" => api.Team(k("CareContactId"), "", k("ReasonForVisit")) // Ugly way to bypass the problem of not having all info available
  //       case "Location" => updateLocation(k("careContactId"), k("Location")) // Should this have timestamp
  //       case "Team" => api.Team(k("careContactId"), k("Team"), "") // Ugly way to bypass the problem of not having all info available
  //       case _ => api.Undefined() // ???
  //     }
  //     tempList = tempList ++ List(tmpp)
  //     //println(tempList+": TMPLIST")
  //     // tempList = tempList ++ List(api.PatientProperty = extractEvents(e))
  //   }
  //   val listigt: List[api.PatientProperty] = tempList.toList
  //   //println(listigt.filter(_ != (api.Undefined())).filter(_ != api.Attended(false,"","0000-01-24T00:00:00.000Z") )+" : LISTIGT")
  //   listigt.filter(_ != (api.Undefined()))
  // }


  def extractNewPatient(np: api.NewPatient): List[api.PatientProperty] = {
    var tempList = new ListBuffer[api.PatientProperty]()
    np.patientData.keys.foreach{ k =>
      val tmpp: api.PatientProperty = k match {
        case "Priority" => updatePriority(np.careContactId, np.patientData("Priority"), np.patientData("TimeOfTriage")) //println(np.patientData("CareContactId")+" : will have propety: "+np.patientData(k))
        case "TimeOfDoctor" => updateAttended(np.careContactId,  ( for ( e <- np.events; if (e("Title") == "Läkare") ) yield e("Value") ).mkString, np.patientData("TimeOfDoctor"))
        case "Location" => updateLocation(np.careContactId, np.patientData("Location")) // Should this have timestamp
        case "Team" => updateTeam(np.careContactId, np.patientData("Team"), np.patientData("ReasonForVisit"), np.patientData("Location"))
        case _ => api.Undefined(np.careContactId) // ???
      }
      tempList = tempList ++ List(tmpp)
      //println(tempList+": TMPLIST")
    }
    np.events.foreach{ e =>         // Checking the events-list
      val tmpp: api.PatientProperty = extractEvent(np.careContactId, e)
      tempList = tempList ++ List(tmpp)
    }

    val listigt: List[api.PatientProperty] = tempList.toList
    println(listigt.filter(_ != (api.Undefined(np.careContactId))).filter(_ != api.Attended(np.careContactId, false,"","0000-01-24T00:00:00.000Z") )+" : LISTIGT EXTRN")
    listigt.filter(_ != (api.Undefined(np.careContactId))).filter(_ != api.Attended(np.careContactId, false,"","0000-01-24T00:00:00.000Z"))
  }

  /*
  To test if laterTimestamp happened later than timestamp
  **/
  def happenedAfter(laterTimestamp: String, timestamp: String ): Boolean = {
    timestampToODT(laterTimestamp).isAfter(timestampToODT(timestamp))
  }

  /*
  Converts String of type "0000-01-24T00:00:00.000Z" into OffsetDateTime
  Used by happenedAfter.
  **/
  def timestampToODT(timestamp: String): OffsetDateTime = {
    OffsetDateTime.parse(timestamp, DateTimeFormatter.ISO_INSTANT)
  }

  /*
  Discerns priority and returns corresponding PriorityEvent-type
  **/
  def updatePriority(ccid: String, newPriority: String, timestamp: String): api.PriorityEvent = {
    newPriority match {
      case "Grön" => api.Green(ccid, timestamp)
      case "Gul" =>  api.Yellow(ccid, timestamp)
      case "Orange" => api.Orange(ccid, timestamp)
      case "Röd" => api.Red(ccid, timestamp)
      case _ => api.NotTriaged(ccid, timestamp)
    }
  }

  /*
  Takes attendantId and timestamp, returns an Attended-type.
  **/
  def updateAttended(ccid: String, attId: String, timestamp: String): api.Attended = {
    api.Attended(ccid, (timestamp != "0000-01-24T00:00:00.000Z"), attId, timestamp)
  }

  /*
  Cleans up Location-value and returns a RoomNr-type.
  **/
  def updateLocation(ccid: String, location: String): api.RoomNr = {
    api.RoomNr(ccid, decodeLocation(location))
  }

  /*
  Filters out a room nr or "ivr" from a location
  **/
  def decodeLocation(l: String): String = {
    if (l contains "ivr") "ivr"
    else l.replaceAll("[^0-9]","")
  }

  /*
  Discerns team and klinik, returns a Team-type.
  **/
  def updateTeam(ccid: String, klinik: String, reasonForVisit: String, location: String): api.Team = {
    api.Team(ccid, decodeTeam(reasonForVisit, location), decodeKlinik(klinik))
  }

  /*
  Discerns team from ReasonForVisit and Location-fields.
  Used by updateTeam().
  **/
  def decodeTeam(reasonForVisit: String, location: String): String = {
    (reasonForVisit, location.charAt(0)) match {
      case ("APK", location) => "Streamteam"
      case (reasonForVisit, 'B') => "Blå"
      case (reasonForVisit, 'G') => "Gul"
      case (reasonForVisit, 'P') => "Process"
      case _ => "Röd"
    }
  }

  /*
  Discerns klinik from Team-field.
  Used by updateTeam().
  **/
  def decodeKlinik(klinik: String): String = {
    klinik match {
      case "NAKKI" => "kirurgi"
      case "NAKME" => "medicin"
      case "NAKOR" => "ortopedi"
      case "NAKBA" | "NAKGY" | "NAKÖN" => "bgö"
    }
  }

  /*
  Takes a single event (e.g. from "Events", "newEvents", "removedEvents" fields)
  and returns corresponding PatientProperty
  **/
  def extractEvent(ccid: String, e: Map[String, String]): api.PatientProperty = {
    e("Category") match{
      case ("T"|"U"|"Q") => updateLatestEvent(ccid, e) // Not sure if all of these should return LatestEvent()
      // afaik T: title?, U: röntgen, Q: que
      case "P" =>  updatePriority(ccid, e("Value"), e("Start"))
      case "B" =>  api.Undefined(ccid) // Value -> "Omvårdnad"
      case _ => {
        println("Unexpected event Type in patientcardsservice: "+e)
        api.Undefined(ccid)}
      }
    }

    /*
    Takes a single event (e.g. from "Events", "newEvents", "removedEvents" fields)
    and returns corresponding LatestEvent with text to present on patient card, or Undefined
    **/
    def updateLatestEvent(ccid: String, e: Map[String, String]): api.PatientProperty = {
      e("Type") match {
        case "EJKÖLAPP" => api.LatestEvent(ccid, "Ej Kölapp", e("Start"))
        case "EXT MOTT" => api.LatestEvent(ccid, "Ext.mottagen", e("Start"))
        case "KLAR" => api.LatestEvent(ccid, "Klar", e("Start"))
        case "KÖLAPP" => api.LatestEvent(ccid, "Kölapp", e("Start"))
        case "LÄKARE" => api.LatestEvent(ccid, "Läkare", e("Start"))
        case "OMSKOORD" => api.LatestEvent(ccid, "Oms.koord", e("Start"))
        case "RTG-REMISS" => api.LatestEvent(ccid, "RTG-remiss", e("Start"))
        case "TRIAGE" => api.LatestEvent(ccid, "Triage", e("Start"))
        case "VPLKOORD" => api.LatestEvent(ccid, "Vpl.koord", e("Start"))
        case "VÄND" => api.LatestEvent(ccid, "Vänd", e("Start"))
        case "RÖNT/KLIN" => api.LatestEvent(ccid, "Röntgen", e("Start"))
        case _ => api.Undefined(ccid)
      }
    }


    def updateArrivalTime(ccid: String, ts: String): api.ArrivalTime = {
      api.ArrivalTime(ccid, ts)
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
