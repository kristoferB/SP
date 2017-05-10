package elastic

import javax.swing.JList

import com.github.nscala_time.time.Imports._
import com.typesafe.config.ConfigFactory
import wabisabi._
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

  def handleMessage(message: String): List[api.EricaEvent] = {
    val json: JValue = parse(message)
    (json \ "isa").values.toString match {
      case "newLoad" | "new" => newPatient(json)
      case "diff" => diffPatient(json)
      case "removed" => removedPatient(json)
      case e => {
        println("Unexpected type of message received: " + e)
        List(api.EricaEvent(0, "", "", "", "", "", "", 0))
      }
    }
  }

  def newPatient(json: JValue): List[api.EricaEvent] = {
    val patientJson = initiatePatient(json \ "data" \ "patient")
    //val careContactId = (patientJson \ "CareContactId").values.toString
    val ericaEvents = newPatientDataToEricaEvents(patientJson)
    ericaEvents
    //val timestamp = (json \ "data" \ "timestamp").values.toString
    //patientData += ("timestamp" -> timestamp)
    //val events = extractNewPatientEvents(patientJson)
    //return api.NewPatient(careContactId, patientData, events)
  }

  def diffPatient(json: JValue): List[api.EricaEvent] = {
    //val careContactId = (json \ "data" \ "updates" \ "CareContactId").values.toString
    val ericaEvents = diffPatientDataToEricaEvents(json \ "data")
    ericaEvents
    //val newEvents = extractNewEvents(json \ "data")
    //val removedEvents = extractRemovedEvents(json \ "data")
    //return api.DiffPatient(careContactId, patientData, newEvents, removedEvents)
  }

  def removedPatient(json: JValue): List[api.EricaEvent] = {
    List(api.EricaEvent(
      (json \ "data" \ "patient" \ "CareContactId").values.toString.toInt,
      "RemovedPatient",
      "NA",
      (json \ "data" \ "timestamp").values.toString,
      "",
      "",
      "",
      (json \ "data" \ "patient" \ "CareContactId").values.toString.toInt
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

    var ericaEvents = new ListBuffer[api.EricaEvent]()

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
        careContactId.toInt
      )
      ericaEvents += departmentCommentEvent
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
        careContactId.toInt
      )
      ericaEvents += locationEvent
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
        careContactId.toInt
      )
      ericaEvents += reasonForVisitEvent
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
        careContactId.toInt
      )
      ericaEvents += teamEvent
    }

    // Handle new events
    var tmpListNewEvents = new ListBuffer[Map[String,String]]()
    val newEvents = castJValueToList[Map[String, JValue]](patient \ "newEvents")
    newEvents.foreach{ m =>
      var tmpMap = Map[String, String]()
      m.foreach{ kv => tmpMap += kv._1 -> kv._2.toString
      }
      tmpListNewEvents += tmpMap
    }

    var ericaNewEvents = new ListBuffer[api.EricaEvent]()
    tmpListNewEvents.toList.foreach{ e =>
      ericaNewEvents += api.EricaEvent(
        careContactId.toInt,
        e("Category"),
        e("End"),
        e("Start"),
        e("Title"),
        e("Type"),
        e("Value"),
        e("VisitId").toInt
      )
    }

    // Handle removed events
    var tmpListRemovedEvents = new ListBuffer[Map[String,String]]()
    val removedEvents = castJValueToList[Map[String, JValue]](patient \ "removedEvents")
    removedEvents.foreach{ m =>
      var tmpMap = Map[String, String]()
      m.foreach{ kv => tmpMap += kv._1 -> kv._2.toString }
      tmpListRemovedEvents += tmpMap
    }

    var ericaRemovedEvents = new ListBuffer[api.EricaEvent]()
    tmpListRemovedEvents.toList.foreach{ e =>
      ericaRemovedEvents += api.EricaEvent(
        careContactId.toInt,
        e("Category") + "-removed",
        e("End"),
        e("Start"),
        e("Title"),
        e("Type"),
        e("Value"),
        e("VisitId").toInt
      )
    }

    return ericaNewEvents.toList ::: ericaRemovedEvents.toList ::: ericaEvents.toList
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
      visitId.toInt
    )
    val locationEvent = api.EricaEvent(
      careContactId.toInt,
      "LocationUpdate",
      "NA",
      getNow.toString,
      "",
      "",
      location,
      visitId.toInt
    )
    val reasonForVisitEvent = api.EricaEvent(
      careContactId.toInt,
      "ReasonForVisitUpdate",
      "NA",
      getNow.toString,
      "",
      "",
      reasonForVisit,
      visitId.toInt
    )
    val teamEvent = api.EricaEvent(
      careContactId.toInt,
      "TeamUpdate",
      "NA",
      getNow.toString,
      "",
      "",
      team,
      visitId.toInt
    )
    val visitRegistrationTimeEvent = api.EricaEvent(
      careContactId.toInt,
      "VisitRegistrationTimeUpdate",
      "NA",
      getNow.toString,
      "",
      "",
      visitRegistrationTime,
      visitId.toInt
    )

    var tmpList = new ListBuffer[Map[String,String]]()
    val events = castJValueToList[Map[String, JValue]](patient \ "Events")
    events.foreach{ m =>
      var tmpMap = Map[String,String]()
      m.foreach{ kv => tmpMap += kv._1 -> kv._2.toString }
      tmpList += tmpMap
    }

    var ericaEventList = new ListBuffer[api.EricaEvent]()
    tmpList.toList.foreach{ e =>
      ericaEventList += api.EricaEvent(
        careContactId.toInt,
        e("Category"),
        e("End"),
        e("Start"),
        e("Title"),
        e("Type"),
        e("Value"),
        e("VisitId").toInt
      )
    }

    return ericaEventList.toList ::: List(departmentCommentEvent, locationEvent, reasonForVisitEvent, teamEvent, visitRegistrationTimeEvent)
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
