package elastic

import javax.swing.JList

import com.github.nscala_time.time.Imports._
import com.typesafe.config.ConfigFactory
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization._
import org.json4s._
import org.json4s.JsonDSL._
import scala.concurrent.ExecutionContext.Implicits.global // Client.get gets sad without this
import scala.collection.mutable.ListBuffer

import sp.gPubSub.{API_Data => api}

class DataAggregation {
  implicit val formats = org.json4s.DefaultFormats ++ org.json4s.ext.JodaTimeSerializers.all // json4s needs this for something

  // Used for indexing to and from elasticsearch.
  // active patients are stored in ONGOING_PATIENT_INDEX/PATIENT_TYPE/CareContactId
  // removed patients are stored in FINISHED_PATIENT_INDEX/PATIENT_TYPE/CareContactId
  val PATIENT_TYPE = "patient_type"
  val ONGOING_PATIENT_INDEX= "on_going_patient_index"
  val FINISHED_PATIENT_INDEX = "finished_patient_index" // patients go here when they are removed

  // load configs from resources/application.conf
  //val config = ConfigFactory.load()
  //val ip = config.getString("elastic.ip")
  //val port = config.getString("elastic.port")

  //println("PatientsToElastic instance attempting to connect to elasticsearch on address: " + ip + ":" + port )
  //val client = new Client(s"http://$ip:$port") // creates a wabisabi client for communication with elasticsearch
/*
  def handleMessage(message: String): List[api.EricaEvent] = {
    val json: JValue = parse(message)
    (json \ "isa").values.toString match {
      case "newLoad" | "new" => newPatient(json)
      case "diff" => diffPatient(json)
      case "removed" => removedPatient(json)
      case e => {
        println("Unexpected type of message received: " + e)
        List(api.EricaEvent(0, "", "", "", "", "", "", 0, ""))
      }
    }
  }*/

 def handleMessage(message: String): List[api.EricaEvent] = {
   val json: JValue = parse(message)
   // get the "diff", "removed" or "new". json4s provides no straight forward way to do this.
   val messageType: String = json.extract[Map[String, JValue]].foldLeft("nothingyet"){
     (str, keyAndValue) =>
       if(str == "nothingyet") keyAndValue._1
       else throw new Exception("more than one top level key in json, not supposed to happen")
   }

   messageType match {
     case "new" => newPatient(json \ "new")
     case "newLoad" => newPatient(json \ "newLoad")
     case "diff" => diffPatient(json \ "diff")
     case "removed" => removedPatient(json \ "removed")
     case e => throw new Exception("Unexpected type of message received: " + e)
   }
 }

  //def convertDiffToEricaEvents(updates: Map[String, JValue], newEvents: List[api.ElvisEvent], removedEvents: List[api.ElvisEvent]): List[api.EricaEvent] = {
  //  var departmentComment = ""
  //  var location = ""
  //  var reasonForVisit = ""
  //  var team = ""

  //  val careContactIdJson = updates.get("CareContactId")//(patient \ "updates" \ "CareContactId")
  //  val careContactId = careContactIdJson.values.toString
  //  val departmentCommentJson = updates.get("DepartmentComment")
  //  val locationJson = updates.get("Location")
  //  val reasonForVisitJson = updates.get("ReasonForVisit")
  //  val teamJson = updates.get("Team")

  //  var ericaEvents = List[api.EricaEvent]()

  //  if (departmentCommentJson != JNothing && departmentCommentJson.values.toString != "None") {
  //    departmentComment = departmentCommentJson.values.toString
  //    val departmentCommentEvent = api.EricaEvent(
  //      careContactId.toInt,
  //      "DepartmentCommentUpdate",
  //      "NA",
  //      getNow.toString,
  //      "",
  //      "",
  //      departmentComment,
  //      careContactId.toInt,
  //      getNow.toString
  //    )
  //    ericaEvents = departmentCommentEvent :: ericaEvents
  //  }
  //  if (locationJson != JNothing && locationJson.values.toString != "None") {
  //    location = locationJson.values.toString
  //    val locationEvent = api.EricaEvent(
  //      careContactId.toInt,
  //      "LocationUpdate",
  //      "NA",
  //      getNow.toString,
  //      "",
  //      "",
  //      location,
  //      careContactId.toInt,
  //      getNow.toString
  //    )
  //    ericaEvents = locationEvent :: ericaEvents
  //  }
  //  if (reasonForVisitJson != JNothing && reasonForVisitJson.values.toString != "None") {
  //    reasonForVisit = reasonForVisitJson.values.toString
  //    val reasonForVisitEvent = api.EricaEvent(
  //      careContactId.toInt,
  //      "ReasonForVisitUpdate",
  //      "NA",
  //      getNow.toString,
  //      "",
  //      "",
  //      reasonForVisit,
  //      careContactId.toInt,
  //      getNow.toString
  //    )
  //    ericaEvents = reasonForVisitEvent :: ericaEvents
  //  }
  //  if (teamJson != JNothing && teamJson.values.toString != "None") {
  //    team = teamJson.values.toString
  //    val teamEvent = api.EricaEvent(
  //      careContactId.toInt,
  //      "TeamUpdate",
  //      "NA",
  //      getNow.toString,
  //      "",
  //      "",
  //      team,
  //      careContactId.toInt,
  //      getNow.toString
  //    )
  //    ericaEvents = teamEvent :: ericaEvents
  //  }

  //  // Handle new events
  //  val newEricaEvents = newEvents.foldLeft(List.empty[api.EricaEvent]){ (list, e) =>
  //    api.EricaEvent(
  //      e.CareEventId.toInt,
  //      e.Category,
  //      e.End.toString,
  //      e.Start.toString,
  //      e.Title,
  //      e.Type,
  //      e.Value,
  //      e.VisitId.toInt,
  //      getNow.toString
  //    ) :: list
  //  }

  //  // Handle removed events
  //  val removedEricaEvents = removedEvents.foldLeft(List.empty[api.EricaEvent]){ (list, e) =>
  //    api.EricaEvent(
  //      e.CareEventId.toInt,
  //      e.Category + "-removed",
  //      e.End.toString,
  //      e.Start.toString,
  //      e.Title,
  //      e.Type,
  //      e.Value,
  //      e.VisitId.toInt,
  //      getNow.toString
  //    ) :: list
  //  }
  //  return newEricaEvents ::: removedEricaEvents ::: ericaEvents
  //}

  //def convertToEricaEvents(elvisPatient: api.ElvisPatient): List[api.EricaEvent] = {
  //  var ericaEventsBuffer = new ListBuffer[api.EricaEvent]()
  //  if (elvisPatient.Location != "None") {
  //    ericaEventsBuffer += api.EricaEvent(
  //      elvisPatient.CareContactId,
  //      "LocationUpdate",
  //      "NA",
  //      getNow.toString,
  //      "",
  //      "",
  //      elvisPatient.Location,
  //      elvisPatient.VisitId,
  //      getNow.toString
  //    )
  //  }
  //  if (elvisPatient.ReasonForVisit != "None") {
  //    ericaEventsBuffer += api.EricaEvent(
  //      elvisPatient.CareContactId,
  //      "ReasonForVisitUpdate",
  //      "NA",
  //      getNow.toString,
  //      "",
  //      "",
  //      elvisPatient.ReasonForVisit,
  //      elvisPatient.VisitId,
  //      getNow.toString
  //    )
  //  }
  //  if (elvisPatient.Team != "None") {
  //    ericaEventsBuffer += api.EricaEvent(
  //      elvisPatient.CareContactId,
  //      "TeamUpdate",
  //      "NA",
  //      getNow.toString,
  //      "",
  //      "",
  //      elvisPatient.Team,
  //      elvisPatient.VisitId,
  //      getNow.toString
  //    )
  //  }
  //  if (elvisPatient.DepartmentComment != "None") {
  //    ericaEventsBuffer += api.EricaEvent(
  //      elvisPatient.CareContactId,
  //      "DepartmentCommentUpdate",
  //      "NA",
  //      getNow.toString,
  //      "",
  //      "",
  //      elvisPatient.DepartmentComment,
  //      elvisPatient.VisitId,
  //      getNow.toString
  //    )
  //  }
  //  if (elvisPatient.VisitRegistrationTime != "None") {
  //    ericaEventsBuffer += api.EricaEvent(
  //      elvisPatient.CareContactId,
  //      "VisitRegistrationTimeUpdate",
  //      "NA",
  //      getNow.toString,
  //      "",
  //      "",
  //      elvisPatient.VisitRegistrationTime,
  //      elvisPatient.VisitId,
  //      getNow.toString
  //    )
  //  }
  //  elvisPatient.Events.foreach{ e =>
  //    ericaEventsBuffer += api.EricaEvent(
  //      e.CareEventId.toInt,
  //      e.Category,
  //      e.End,
  //      e.Start,
  //      e.Title,
  //      e.Type,
  //      e.Value,
  //      e.VisitId,
  //      getNow.toString
  //    )
  //  }
  //  return ericaEventsBuffer.toList
  //}

  def newPatient(json: JValue): List[api.EricaEvent] = {
    val patientJson = initiatePatient(json \ "patient")
    val ericaEvents = newPatientDataToEricaEvents(patientJson)
    ericaEvents
  }

  def diffPatient(json: JValue): List[api.EricaEvent] = {
    val ericaEvents = diffPatientDataToEricaEvents(json)
    ericaEvents
  }

  def removedPatient(json: JValue): List[api.EricaEvent] = {
    List(api.EricaEvent(
      (json \ "patient" \ "CareContactId").values.toString.toInt,
      "RemovedPatient",
      "NA",
      (json \ "timestamp").values.toString,
      "",
      "",
      "",
      (json \ "patient" \ "CareContactId").values.toString.toInt,
      getNow.toString
    ))
  }

  def initiatePatient(patient: JValue): JValue = {
    val events: List[Map[String, JValue]] = castJValueToList[Map[String, JValue]](patient \ "Events")
    val careContactRegistrationTime = DateTime.parse((patient \ "CareContactRegistrationTime").values.toString)

    val timeOfDoctor = getTimeOfEvent("Läkare", events, careContactRegistrationTime)
    val timeOfTriage = getTimeOfEvent("Triage", events, careContactRegistrationTime)
    val timeOfFinished = getTimeOfEvent("Klar", events, careContactRegistrationTime)

    val timeToDoctor = timeDifference(careContactRegistrationTime,  timeOfDoctor)
    val timeToTriage = timeDifference(careContactRegistrationTime,  timeOfTriage)
    val timeToFinished = timeDifference(careContactRegistrationTime,  timeOfFinished)
    val prio = getPriority(events)
    patient merge
    (
      ("Priority" -> prio) ~
      ("TimeToDoctor" -> timeToDoctor) ~
      ("TimeToTriage" -> timeToTriage) ~
      ("TimeToFinished" -> timeToFinished) ~
      ("TimeOfDoctor" -> timeOfDoctor.toString()) ~
      ("TimeOfTriage" -> timeOfTriage.toString()) ~
      ("TimeOfFinished" -> timeOfFinished.toString())
    )
  }

  def diffPatientDataToEricaEvents(patient: JValue): List[api.EricaEvent] = {
    var departmentComment = ""
    var location = ""
    var reasonForVisit = ""
    var team = ""

    val careContactIdJson = (patient \ "updates" \ "CareContactId")
    val careContactId = careContactIdJson.values.toString
    val departmentCommentJson = (patient \ "updates" \ "DepartmentComment")
    val locationJson = (patient \ "updates" \ "Location")
    val reasonForVisitJson = (patient \ "updates" \ "ReasonForVisit")
    val teamJson = (patient \ "updates" \ "Team")

    var ericaEvents = List[api.EricaEvent]()

    if (departmentCommentJson != JNothing) {
      departmentComment = departmentCommentJson.values.toString
      val departmentCommentEvent = api.EricaEvent(
        careContactId.toInt,
        "DepartmentCommentUpdate",
        "NA",
        getNow.toString,
        "",
        "",
        departmentComment,
        careContactId.toInt,
        getNow.toString
      )
      departmentCommentEvent :: ericaEvents
    }
    if (locationJson != JNothing) {
      location = locationJson.values.toString
      val locationEvent = api.EricaEvent(
        careContactId.toInt,
        "LocationUpdate",
        "NA",
        getNow.toString,
        "",
        "",
        location,
        careContactId.toInt,
        getNow.toString
      )
      locationEvent :: ericaEvents
    }
    if (reasonForVisitJson != JNothing) {
      reasonForVisit = reasonForVisitJson.values.toString
      val reasonForVisitEvent = api.EricaEvent(
        careContactId.toInt,
        "ReasonForVisitUpdate",
        "NA",
        getNow.toString,
        "",
        "",
        reasonForVisit,
        careContactId.toInt,
        getNow.toString
      )
      reasonForVisitEvent :: ericaEvents
    }
    if (teamJson != JNothing) {
      team = teamJson.values.toString
      val teamEvent = api.EricaEvent(
        careContactId.toInt,
        "TeamUpdate",
        "NA",
        getNow.toString,
        "",
        "",
        team,
        careContactId.toInt,
        getNow.toString
      )
      teamEvent :: ericaEvents
    }

    // Handle new events
    val newEvents = castJValueToList[Map[String, JValue]](patient \ "newEvents").foldLeft(List.empty[api.EricaEvent]){ (list, e) =>
      api.EricaEvent(
        careContactId.toInt,
        e("Category").toString,
        e("End").toString,
        e("Start").toString,
        e("Title").toString,
        e("Type").toString,
        e("Value").toString,
        e("VisitId").toString.toInt,
        getNow.toString
      ) :: list
    }

    // Handle removed events
    val removedEvents = castJValueToList[Map[String, JValue]](patient \ "removedEvents").foldLeft(List.empty[api.EricaEvent]){ (list, e) =>
      api.EricaEvent(
        careContactId.toInt,
        e("Category").toString + "-removed",
        e("End").toString,
        e("Start").toString,
        e("Title").toString,
        e("Type").toString,
        e("Value").toString,
        e("VisitId").toString.toInt,
        getNow.toString
      ) :: list
    }

    return newEvents ::: removedEvents ::: ericaEvents
  }

  def newPatientDataToEricaEvents(patient: JValue): List[api.EricaEvent] = {
    val careContactId = (patient \ "CareContactId").values.toString
    val careContactRegistrationTime = (patient \ "CareContactRegistrationTime").values.toString // Currently not used
    val departmentComment = (patient \ "DepartmentComment").values.toString
    val location = (patient \ "Location").values.toString
    val patientId = (patient \ "PatientId").values.toString // Currently not used
    val reasonForVisit = (patient \ "ReasonForVisit").values.toString
    val team = (patient \ "Team").values.toString
    val visitId = (patient \ "VisitId").values.toString
    val visitRegistrationTime = (patient \ "VisitRegistrationTime").values.toString

    // Transform DepartmentComment, Location, ReasonForVisit and Team to EricaEvent

    val departmentCommentEvent = api.EricaEvent(
      careContactId.toInt,
      "DepartmentCommentUpdate",
      "NA",
      getNow.toString,
      "",
      "",
      departmentComment,
      visitId.toInt,
      getNow.toString
    )
    val locationEvent = api.EricaEvent(
      careContactId.toInt,
      "LocationUpdate",
      "NA",
      getNow.toString,
      "",
      "",
      location,
      visitId.toInt,
      getNow.toString
    )
    val reasonForVisitEvent = api.EricaEvent(
      careContactId.toInt,
      "ReasonForVisitUpdate",
      "NA",
      getNow.toString,
      "",
      "",
      reasonForVisit,
      visitId.toInt,
      getNow.toString
    )
    val teamEvent = api.EricaEvent(
      careContactId.toInt,
      "TeamUpdate",
      "NA",
      getNow.toString,
      "",
      "",
      team,
      visitId.toInt,
      getNow.toString
    )
    val visitRegistrationTimeEvent = api.EricaEvent(
      careContactId.toInt,
      "VisitRegistrationTimeUpdate",
      "NA",
      getNow.toString,
      "",
      "",
      visitRegistrationTime,
      visitId.toInt,
      getNow.toString
    )

    val ericaEvents = castJValueToList[Map[String, JValue]](patient \ "Events").foldLeft(List.empty[api.EricaEvent]){ (list, e) =>
      api.EricaEvent(
        careContactId.toInt,
        e("Category").toString,
        e("End").toString,
        e("Start").toString,
        e("Title").toString,
        e("Type").toString,
        e("Value").toString,
        e("VisitId").toString.toInt,
        getNow.toString
      ) :: list
    }

    return ericaEvents ::: List(departmentCommentEvent, locationEvent, reasonForVisitEvent, teamEvent, visitRegistrationTimeEvent)
  }

  /** Casts a jValue to a List[A] without crashing on empty lists */
  def castJValueToList[A](list:JValue): List[A] = {
    list match {
      case JNothing => List[A]()
      case _ => list.asInstanceOf[JArray].values.asInstanceOf[List[A]]
    }
  }

  /** Calculates the time between "CareContactRegistrationTime" and the first instance of and Event with a given title
  * @param eventTitle the title of the event. examples are "Läkare" and "Triage".
  * @param events the list of events
  * @param careContactRegistrationTime the time to compare the event to. This is usually "CareContactRegistrationTime"
  * @return the time between visitRegistrationTime and the event, in milliseconds.
  */
  private def getTimeOfEvent(eventTitle: String, events: List[Map[String, JValue]], careContactRegistrationTime: DateTime): DateTime ={
    events.foreach(e =>
      if(e.get("Title").get.toString == eventTitle) { return {
        DateTime.parse(e.get("Start").get.asInstanceOf[String])
      }}
    )
    DateTime.parse("0000-01-24T00:00:00Z")
  }

  /** calculates the elapsed time between two DateTimes and returns it in milliseconds. If
  * the time is negative it returns 0. If one of the time points is None, it returns -1. */
  private def timeDifference(fromTime: DateTime, toTime: DateTime): Long ={
    try{
      (fromTime to toTime).toDurationMillis
    }catch{
      case e:IllegalAccessException  => 0 // return 0 if time is negative
      case e:Exception => -1 // usually illegal formatting
    }
  }

  /** returns the priority event with the latest timestamp */
  val prios = List("Blå","Grön","Gul","Orange","Röd")

  private def getPriority(events: List[Map[String, JValue]]): String = {
    var timestamp = DateTime.parse("0000-01-24T00:00:00Z")
    var prio:String = ""
    events.foreach( e =>
      if(prios.contains(e.get("Title").get.asInstanceOf[String])){
        if (DateTime.parse(e.get("Start").get.asInstanceOf[String]).compareTo(timestamp) > 0) {
          timestamp = DateTime.parse(e.get("Start").get.asInstanceOf[String])
          prio = e.get("Title").get.toString
        }
      }
    )
    prio
  }

    def getNow: DateTime = {
      DateTime.now(DateTimeZone.forID("Europe/Stockholm"))
    }
  }
