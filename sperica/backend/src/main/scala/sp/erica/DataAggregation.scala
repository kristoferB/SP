package sp.erica

import com.github.nscala_time.time.Imports._
import sp.domain.Logic._
import sp.domain._
import sp.erica.{API_Data => api}

class DataAggregation {
  // Used for indexing to and from elasticsearch.
  // active patients are stored in ONGOING_PATIENT_INDEX/PATIENT_TYPE/CareContactId
  // removed patients are stored in FINISHED_PATIENT_INDEX/PATIENT_TYPE/CareContactId
  val PATIENT_TYPE = "patient_type"
  val ONGOING_PATIENT_INDEX= "on_going_patient_index"
  val FINISHED_PATIENT_INDEX = "finished_patient_index" // patients go here when they are removed


  def convertDiffToEricaEvents(updates: Map[String, SPValue], newEvents: List[api.ElvisEvent], removedEvents: List[api.ElvisEvent]): List[api.EricaEvent] = {
    val updAttr = SPAttributes.make(updates.filter(kv => kv._2 != SPValue("None")))
    val careContactId = updAttr.getAs[Int]("CareContactId").getOrElse(-1)
    val departmentComment = updAttr.getAs[String]("DepartmentComment")
    val location = updAttr.getAs[String]("Location")
    val reasonForVisit = updAttr.getAs[String]("ReasonForVisit")
    val team= updAttr.getAs[String]("Team")

    //var ericaEvents = List[api.EricaEvent]()

    val now = getNow.toString
    val defaultEvent = api.EricaEvent(
      CareContactId = careContactId,
      Category = "",
      End = "NA",
      Start = now,
      Title = "",
      Type = "",
      Value = "",
      VisitId = careContactId,
      TimeEvent = now
    )

    val departmentCommentEvent = departmentComment.map{x =>
      defaultEvent.copy(Category = "DepartmentCommentUpdate", Value = x)
    }

    val locationEvent = location.map{x =>
      defaultEvent.copy(Category = "LocationUpdate", Value = x)
    }

    val reasonForVisitEvent = reasonForVisit.map{x =>
      defaultEvent.copy(Category = "ReasonForVisitUpdate", Value = x)
    }

    val teamEvent = team.map{x =>
      defaultEvent.copy(Category = "TeamUpdate", Value = x)
    }

    val ericaEvents = List(departmentCommentEvent, locationEvent, reasonForVisitEvent, teamEvent).flatten

    // Handle new events
    val newEricaEvents = newEvents.map(elvisToErica)

    // Handle removed events
    val removedEricaEvents = removedEvents.map(elvisToEricaRemoved)
    newEricaEvents ::: removedEricaEvents ::: ericaEvents
  }




  def convertToEricaEvents(elvisPatient: api.ElvisPatient): List[api.EricaEvent] = {
    val now = getNow.toString
    val defaultEvent = api.EricaEvent(
      CareContactId = elvisPatient.CareContactId,
      Category = "",
      End = "NA",
      Start = now,
      Title = "",
      Type = "",
      Value = "",
      VisitId = elvisPatient.VisitId,
      TimeEvent = now
    )

    val locationUpdateEvent = extractParameter(elvisPatient.Location).map{x =>
      defaultEvent.copy(Category = "LocationUpdate", Value = x)
    }
    val reasonForVisitUpdateEvent = extractParameter(elvisPatient.ReasonForVisit).map{x =>
      defaultEvent.copy(Category = "ReasonForVisitUpdate", Value = x)
    }
    val teamUpdateEvent = extractParameter(elvisPatient.Team).map{x =>
      defaultEvent.copy(Category = "TeamUpdate", Value = x)
    }
    val departmentCommentUpdateEvent = extractParameter(elvisPatient.DepartmentComment).map{x =>
      defaultEvent.copy(Category = "DepartmentCommentUpdate", Value = x)
    }
    val visitRegistrationTimeUpdateEvent = extractParameter(elvisPatient.VisitRegistrationTime).map{x =>
      defaultEvent.copy(Category = "VisitRegistrationTimeUpdate", Value = x)
    }
    val xs = List(locationUpdateEvent, reasonForVisitUpdateEvent, teamUpdateEvent, departmentCommentUpdateEvent, visitRegistrationTimeUpdateEvent).flatten

    val newEricaEvents = elvisPatient.Events.map(elvisToErica)

    xs ++ newEricaEvents
  }

  def extractParameter(x: String): Option[String] = x match {
    case "None" => None
    case _ => Some(x)
  }

  def elvisToEricaEvent(e: api.ElvisEvent)(postfix: String = ""): api.EricaEvent = {
    api.EricaEvent(
      e.CareEventId,
      e.Category + postfix,
      e.End.toString,
      e.Start.toString,
      e.Title,
      e.Type,
      e.Value,
      e.VisitId,
      getNow.toString
    )
  }
  def elvisToErica(e: api.ElvisEvent) = elvisToEricaEvent(e)("")
  def elvisToEricaRemoved(e: api.ElvisEvent) = elvisToEricaEvent(e)("-removed")





//  def newPatient(json: SPValue): List[api.EricaEvent] = {
//    val patientJson = initiatePatient(json \ "data" \ "patient")
//    val ericaEvents = newPatientDataToEricaEvents(patientJson)
//    ericaEvents
//  }
//
//  def diffPatient(json: JValue): List[api.EricaEvent] = {
//    val ericaEvents = diffPatientDataToEricaEvents(json \ "data")
//    ericaEvents
//  }
//
//  def removedPatient(json: JValue): List[api.EricaEvent] = {
//    List(api.EricaEvent(
//      (json \ "data" \ "patient" \ "CareContactId").values.toString.toInt,
//      "RemovedPatient",
//      "NA",
//      (json \ "data" \ "timestamp").values.toString,
//      "",
//      "",
//      "",
//      (json \ "data" \ "patient" \ "CareContactId").values.toString.toInt,
//      getNow.toString
//    ))
//  }
//
//  def initiatePatient(patient: SPAttributes): SPAttributes = {
//    val events: List[Map[String, JValue]] = castJValueToList[Map[String, JValue]](patient \ "Events")
//    val careContactRegistrationTime = DateTime.parse((patient \ "CareContactRegistrationTime").values.toString)
//
//    val timeOfDoctor = getTimeOfEvent("Läkare", events, careContactRegistrationTime)
//    val timeOfTriage = getTimeOfEvent("Triage", events, careContactRegistrationTime)
//    val timeOfFinished = getTimeOfEvent("Klar", events, careContactRegistrationTime)
//
//    val timeToDoctor = timeDifference(careContactRegistrationTime,  timeOfDoctor)
//    val timeToTriage = timeDifference(careContactRegistrationTime,  timeOfTriage)
//    val timeToFinished = timeDifference(careContactRegistrationTime,  timeOfFinished)
//    val prio = getPriority(events)
//    patient merge
//    (
//      ("Priority" -> prio) ~
//      ("TimeToDoctor" -> timeToDoctor) ~
//      ("TimeToTriage" -> timeToTriage) ~
//      ("TimeToFinished" -> timeToFinished) ~
//      ("TimeOfDoctor" -> timeOfDoctor.toString()) ~
//      ("TimeOfTriage" -> timeOfTriage.toString()) ~
//      ("TimeOfFinished" -> timeOfFinished.toString())
//    )
//  }
//
//  def diffPatientDataToEricaEvents(patient: JValue): List[api.EricaEvent] = {
//    var departmentComment = ""
//    var location = ""
//    var reasonForVisit = ""
//    var team = ""
//
//    val careContactIdJson = (patient \ "updates" \ "CareContactId")
//    val careContactId = careContactIdJson.values.toString
//    val departmentCommentJson = (patient \ "updates" \ "DepartmentComment")
//    val locationJson = (patient \ "updates" \ "Location")
//    val reasonForVisitJson = (patient \ "updates" \ "ReasonForVisit")
//    val teamJson = (patient \ "updates" \ "Team")
//
//    var ericaEvents = List[api.EricaEvent]()
//
//    if (departmentCommentJson != JNothing) {
//      departmentComment = departmentCommentJson.values.toString
//      val departmentCommentEvent = api.EricaEvent(
//        careContactId.toInt,
//        "DepartmentCommentUpdate",
//        "NA",
//        getNow.toString,
//        "",
//        "",
//        departmentComment,
//        careContactId.toInt,
//        getNow.toString
//      )
//      departmentCommentEvent :: ericaEvents
//    }
//    if (locationJson != JNothing) {
//      location = locationJson.values.toString
//      val locationEvent = api.EricaEvent(
//        careContactId.toInt,
//        "LocationUpdate",
//        "NA",
//        getNow.toString,
//        "",
//        "",
//        location,
//        careContactId.toInt,
//        getNow.toString
//      )
//      locationEvent :: ericaEvents
//    }
//    if (reasonForVisitJson != JNothing) {
//      reasonForVisit = reasonForVisitJson.values.toString
//      val reasonForVisitEvent = api.EricaEvent(
//        careContactId.toInt,
//        "ReasonForVisitUpdate",
//        "NA",
//        getNow.toString,
//        "",
//        "",
//        reasonForVisit,
//        careContactId.toInt,
//        getNow.toString
//      )
//      reasonForVisitEvent :: ericaEvents
//    }
//    if (teamJson != JNothing) {
//      team = teamJson.values.toString
//      val teamEvent = api.EricaEvent(
//        careContactId.toInt,
//        "TeamUpdate",
//        "NA",
//        getNow.toString,
//        "",
//        "",
//        team,
//        careContactId.toInt,
//        getNow.toString
//      )
//      teamEvent :: ericaEvents
//    }
//
//    // Handle new events
//    val newEvents = castJValueToList[Map[String, JValue]](patient \ "newEvents").foldLeft(List.empty[api.EricaEvent]){ (list, e) =>
//      api.EricaEvent(
//        careContactId.toInt,
//        e.get("Category").toString,
//        e.get("End").toString,
//        e.get("Start").toString,
//        e.get("Title").toString,
//        e.get("Type").toString,
//        e.get("Value").toString,
//        e.get("VisitId").toString.toInt,
//        getNow.toString
//      ) :: list
//    }
//
//    // Handle removed events
//    val removedEvents = castJValueToList[Map[String, JValue]](patient \ "removedEvents").foldLeft(List.empty[api.EricaEvent]){ (list, e) =>
//      api.EricaEvent(
//        careContactId.toInt,
//        e.get("Category").toString + "-removed",
//        e.get("End").toString,
//        e.get("Start").toString,
//        e.get("Title").toString,
//        e.get("Type").toString,
//        e.get("Value").toString,
//        e.get("VisitId").toString.toInt,
//        getNow.toString
//      ) :: list
//    }
//
//    return newEvents ::: removedEvents ::: ericaEvents
//  }
//
//  def newPatientDataToEricaEvents(patient: JValue): List[api.EricaEvent] = {
//    val careContactId = (patient \ "CareContactId").values.toString
//    val careContactRegistrationTime = (patient \ "CareContactRegistrationTime").values.toString // Currently not used
//    val departmentComment = (patient \ "DepartmentComment").values.toString
//    val location = (patient \ "Location").values.toString
//    val patientId = (patient \ "PatientId").values.toString // Currently not used
//    val reasonForVisit = (patient \ "ReasonForVisit").values.toString
//    val team = (patient \ "Team").values.toString
//    val visitId = (patient \ "VisitId").values.toString
//    val visitRegistrationTime = (patient \ "VisitRegistrationTime").values.toString
//
//    // Transform DepartmentComment, Location, ReasonForVisit and Team to EricaEvent
//
//    val departmentCommentEvent = api.EricaEvent(
//      careContactId.toInt,
//      "DepartmentCommentUpdate",
//      "NA",
//      getNow.toString,
//      "",
//      "",
//      departmentComment,
//      visitId.toInt,
//      getNow.toString
//    )
//    val locationEvent = api.EricaEvent(
//      careContactId.toInt,
//      "LocationUpdate",
//      "NA",
//      getNow.toString,
//      "",
//      "",
//      location,
//      visitId.toInt,
//      getNow.toString
//    )
//    val reasonForVisitEvent = api.EricaEvent(
//      careContactId.toInt,
//      "ReasonForVisitUpdate",
//      "NA",
//      getNow.toString,
//      "",
//      "",
//      reasonForVisit,
//      visitId.toInt,
//      getNow.toString
//    )
//    val teamEvent = api.EricaEvent(
//      careContactId.toInt,
//      "TeamUpdate",
//      "NA",
//      getNow.toString,
//      "",
//      "",
//      team,
//      visitId.toInt,
//      getNow.toString
//    )
//    val visitRegistrationTimeEvent = api.EricaEvent(
//      careContactId.toInt,
//      "VisitRegistrationTimeUpdate",
//      "NA",
//      getNow.toString,
//      "",
//      "",
//      visitRegistrationTime,
//      visitId.toInt,
//      getNow.toString
//    )
//
//    val ericaEvents = castJValueToList[Map[String, JValue]](patient \ "Events").foldLeft(List.empty[api.EricaEvent]){ (list, e) =>
//      api.EricaEvent(
//        careContactId.toInt,
//        e.get("Category").toString,
//        e.get("End").toString,
//        e.get("Start").toString,
//        e.get("Title").toString,
//        e.get("Type").toString,
//        e.get("Value").toString,
//        e.get("VisitId").toString.toInt,
//        getNow.toString
//      ) :: list
//    }
//
//    return ericaEvents ::: List(departmentCommentEvent, locationEvent, reasonForVisitEvent, teamEvent, visitRegistrationTimeEvent)
//  }
//
//  /** Casts a jValue to a List[A] without crashing on empty lists */
//  def castJValueToList[A](list:JValue): List[A] = {
//    list match {
//      case JNothing => List[A]()
//      case _ => list.asInstanceOf[JArray].values.asInstanceOf[List[A]]
//    }
//  }
//
//  /** Calculates the time between "CareContactRegistrationTime" and the first instance of and Event with a given title
//  * @param eventTitle the title of the event. examples are "Läkare" and "Triage".
//  * @param events the list of events
//  * @param careContactRegistrationTime the time to compare the event to. This is usually "CareContactRegistrationTime"
//  * @return the time between visitRegistrationTime and the event, in milliseconds.
//  */
//  private def getTimeOfEvent(eventTitle: String, events: List[Map[String, JValue]], careContactRegistrationTime: DateTime): DateTime ={
//    events.foreach(e =>
//      if(e.get("Title").get.toString == eventTitle) { return {
//        DateTime.parse(e.get("Start").get.asInstanceOf[String])
//      }}
//    )
//    DateTime.parse("0000-01-24T00:00:00Z")
//  }
//
//  /** calculates the elapsed time between two DateTimes and returns it in milliseconds. If
//  * the time is negative it returns 0. If one of the time points is None, it returns -1. */
//  private def timeDifference(fromTime: DateTime, toTime: DateTime): Long ={
//    try{
//      (fromTime to toTime).toDurationMillis
//    }catch{
//      case e:IllegalAccessException  => 0 // return 0 if time is negative
//      case e:Exception => -1 // usually illegal formatting
//    }
//  }
//
//  /** returns the priority event with the latest timestamp */
//  val prios = List("Blå","Grön","Gul","Orange","Röd")
//
//  private def getPriority(events: List[Map[String, JValue]]): String = {
//    var timestamp = DateTime.parse("0000-01-24T00:00:00Z")
//    var prio:String = ""
//    events.foreach( e =>
//      if(prios.contains(e.get("Title").get.asInstanceOf[String])){
//        if (DateTime.parse(e.get("Start").get.asInstanceOf[String]).compareTo(timestamp) > 0) {
//          timestamp = DateTime.parse(e.get("Start").get.asInstanceOf[String])
//          prio = e.get("Title").get.toString
//        }
//      }
//    )
//    prio
//  }

    def getNow: DateTime = {
      DateTime.now(DateTimeZone.forID("Europe/Stockholm"))
    }
  }
