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

class PatientsToElastic {
  implicit val formats = org.json4s.DefaultFormats ++ org.json4s.ext.JodaTimeSerializers.all // json4s needs this for something

  // Used for indexing to and from elasticsearch.
  // active patients are stored in ONGOING_PATIENT_INDEX/PATIENT_TYPE/CareContactId
  // removed patients are stored in FINISHED_PATIENT_INDEX/PATIENT_TYPE/CareContactId
  val PATIENT_TYPE = "patient_type"
  val ONGOING_PATIENT_INDEX= "on_going_patient_index"
  val FINISHED_PATIENT_INDEX = "finished_patient_index" // patients go here when they are removed

  // load configs from resources/application.conf
  val config = ConfigFactory.load()
  val ip = config.getString("elastic.ip")
  val port = config.getString("elastic.port")

  //println("PatientsToElastic instance attempting to connect to elasticsearch on address: " + ip + ":" + port )
  val client = new Client(s"http://$ip:$port") // creates a wabisabi client for communication with elasticsearch

  /** Handles the initial parsing of incoming messages */
  def messageReceived(message: String) {
    //println("TIME: " + getNow)
    //println("MESS: " + messageU)
    // figure out what sort of message we just received
    val json: JValue = parse(message) // this jsons the String.
    (json \ "isa").values.toString match {
      case "newLoad" | "new" => postEntirePatientToElastic(json)
      case "diff" => diffPatient(json)
      case "removed" => removePatient(json)
      case _ => println("WARNING: Searcher received an unrecognized message format. json: " + json)
    }

  }

  /** Instantiates a patient and sends it to ONGOING_PATIENT_INDEX
  * @param data patient to send to elastic
  */
  def postEntirePatientToElastic(data: JValue): Unit = {
    val patient = initiatePatient(data \ "data" \ "patient")
    addPatient(patient, ONGOING_PATIENT_INDEX) // index the new patient
  }

  /** This reformats a patient from the Transfomrationservice way of doing things to the OnGoingPatients way of doing things.
  * This essentially means adding a bunch of fields.
  */
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

  /** Applies a diff to an OnGoingPatient.
  * The incoming diff is parsed and the CareContactId of the diff is extracted. the relevant patient is fetched from
  * the database using the CareContactId. The next iteration of the patient is generated from the diff and the old
  * patient. The new patient is then indexed into the database, overwriting the old version
  */
  def diffPatient(data: JValue) {
    val diff = data \ "data"
    // extract CareContactId and fetch patient from elasticsearch
    val careContactId: String = ( diff \ "updates" \ "CareContactId" ).values.toString
    val patient: JValue = getPatientFromElastic(ONGOING_PATIENT_INDEX, careContactId)

    // add and remove Events from event array.
    val oldEvents:      List[Map[String, JValue]] = castJValueToList[Map[String, JValue]](patient \ "Events")
    val newEvents:      List[Map[String, JValue]] = castJValueToList[Map[String, JValue]](diff \ "newEvents")
    val removedEvents:  List[Map[String, JValue]] = castJValueToList[Map[String, JValue]](diff \ "removedEvents")

    // Construct Event map
    val actualNewEvents:List[Map[String, JValue]] = newEvents.filter(e => !oldEvents.contains(e))       // filter out Events from newEvents (do not add an Event that has already happened)
    val actualOldEvents:List[Map[String, JValue]] = oldEvents.filter(e => !removedEvents.contains(e))   // filter out removedEvents from Events
    val events:List[Map[String, JValue]] = actualOldEvents ++ actualNewEvents

    val updates:List[ElvisUpdateEvent] = createNewUpdateList(patient, diff \ "updates") // create List of ElvisUpdateEvents

    val fieldData = patient merge (diff \ "updates") // create field Data

    // rebuild the patient and send to elastic
    val newPatient = elvisPatientFactory(fieldData, events, updates)
    addPatient(newPatient, ONGOING_PATIENT_INDEX)


  }

  /** Casts a jValue to a List[A] without crashing on empty lists */
  def castJValueToList[A](list:JValue): List[A] = {
    list match {
      case JNothing => List[A]()
      case _ => list.asInstanceOf[JArray].values.asInstanceOf[List[A]]
    }
  }

  /** Assembles a patient as a JSON object.
  * @param fieldData data from the text fields in Elvis and also Priority, TimeToDoctor and TimeToTriage
  * @param events list of historical ElvisEvents
  * @param updates list fo historical ElvisUpdateEvents
  * @return
  */
  def elvisPatientFactory(fieldData: JValue, events: List[Map[String, JValue]], updates: List[ElvisUpdateEvent]): JValue = {
    val careContactRegistrationTime = DateTime.parse((fieldData \ "CareContactRegistrationTime").values.toString)


    val timeOfDoctor = getTimeOfEvent("Läkare", events, careContactRegistrationTime)
    val timeOfTriage = getTimeOfEvent("Triage", events, careContactRegistrationTime)
    val timeOfFinished = getTimeOfEvent("Klar", events, careContactRegistrationTime)

    val timeToDoctor = timeDifference(careContactRegistrationTime,  timeOfDoctor)
    val timeToTriage = timeDifference(careContactRegistrationTime,  timeOfTriage)
    val timeToFinished = timeDifference(careContactRegistrationTime,  timeOfFinished)
    val prio = getPriority(events)

    fieldData merge
    (
      ("Priority" -> prio) ~
      ("TimeToDoctor" -> timeToDoctor) ~
      ("TimeToTriage" -> timeToTriage) ~
      ("TimeToFinished" -> timeToFinished) ~
      ("TimeOfDoctor" -> timeOfDoctor.toString()) ~
      ("TimeOfTriage" -> timeOfTriage.toString()) ~
      ("TimeOfFinished" -> timeOfFinished.toString()) ~
      ("Events" -> parse(write(events))) ~
      ("Updates" ->  parse(write(updates)))
    )
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
  private def getPriority(events: List[Map[String, JValue]]): String ={
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
  /** This horrible mess generates for a patient a new list of ElvisUpdateEvent, given a Map of updates.
  * @param patient the patient to apply the updates to. The old list of ElvisUpdateEvent is retrieved from this patient
  * @param newUpdates a map of the new updates
  * @return new ElvisUpdateEvent list
  */
  def createNewUpdateList(patient: JValue, newUpdates: JValue ): List[ElvisUpdateEvent]= {
    val relevantKeys = List("DepartmentComment", "Location", "ReasonForVisit", "Team") // the four keys which interest us
    var updatedVariables: List[ElvisUpdateEvent] = castJValueToList[ElvisUpdateEvent](patient \ "updates")  // make a mutable list and fill it with the current list of updates
    relevantKeys.foreach( k => if(newUpdates \ k != JNothing){ // foreach relevant key, check if newUpdates has an associated value
      //println("update recorded: " +k+ "->" +(newUpdates \ k).values)
      updatedVariables ++= List(
        read[ElvisUpdateEvent](write(Map( // ElvisUpdateEvent made from String made from Map
          "Timestamp"-> (newUpdates\"timestamp").values, // please note timestamp vs Timestamp
          "ModifiedField" -> k,                          // the key for the field that was changed...
          "ModifiedTo" -> (newUpdates\k).values,         // ...and the value it was changed to
          "CareContactId" -> patient\"CareContactId",
          "ModifiedFrom" -> patient\k
        )))).filter(k => !updatedVariables.contains(k))  // filter out any update that has already been recorded. This filter is called on a list that contains one element.
      })
      updatedVariables
    }

    /** if update is a JValue, return update.asInstanceOf[A]. Otherwise (if update is None), old is returned. */
    def updateOrElse[A](update: Any, old: A): A = {
      update match {
        case Some(a:JValue) =>  update.asInstanceOf[Option[JValue]].get.values.asInstanceOf[A]
        case _ =>  old
      }
    }

    /** Deletes a patient from ONGOING_PATIENT_INDEX and adds it to FINISHED_PATIENT_INDEX
    * @param data the patient to be removed (as a JSON)
    */
    def removePatient(data: JValue) {
      val careContactId = (data \ "data" \ "patient"\ "CareContactId").values.toString
      val patient = getPatientFromElastic(ONGOING_PATIENT_INDEX, careContactId)
      val visitRegistrationTime = DateTime.parse((patient \ "CareContactRegistrationTime").values.toString)

      val now = getNow

      val newPatient = patient merge parse(write
        (
          ("RemovedTime" -> getNow.toString) ~
          ("TotalTime" -> timeDifference(visitRegistrationTime, getNow))
        )
      )

      // delete patient from ongoing and send to finished patients
      deletePatient(patient, ONGOING_PATIENT_INDEX)
      addPatient(newPatient, FINISHED_PATIENT_INDEX)
    }

    /** Attempts to delete a patient from elasticsearch under /targetIndex/PATIENT_TYPE/patient.CareContactId
    * @param patient the patient to be removed, in the ElvisPatientPlus format
    * @param targetIndex should ONLY be one of the values ONGOING_PATIENT_INDEX or FINISHED_PATIENT_INDEX
    */
    def deletePatient(patient : JValue, targetIndex: String): Unit = {
      val careContactId:String = (patient \ "CareContactId").values.toString
      client.delete(
        index = targetIndex,
        `type` = PATIENT_TYPE,
        id = careContactId
      )
    }

    /** Adds a patient to elasticsearch under /targetIndex/PATIENT_TYPE/patient.CareContactId
    * Note that this overwrites anything previously on that path
    * @param patient the patient to be added, in the ElvisPatientPlus format
    * @param targetIndex should ONLY be one of the values ONGOING_PATIENT_INDEX or FINISHED_PATIENT_INDEX
    */
    def addPatient(patient : JValue, targetIndex: String): Unit = {
      val careContactId:String = (patient \"CareContactId").values.toString
      client.index(
        index = targetIndex,
        `type` = PATIENT_TYPE,
        id = Some(careContactId),
        data = write(patient),
        refresh = true
      )
    }

    /** Fetches from elastic the patient under /index/PATIENT_TYPE/careContactId */
    def getPatientFromElastic(index: String, careContactId: String): JValue ={
      val oldPatientQuery = client.get(index, PATIENT_TYPE, careContactId).map(_.getResponseBody) //fetch patient from database
      while (!oldPatientQuery.isCompleted) {} // patiently wait for response from the database. //TODO at some point add timeout. It could get stuck here forever (but probably not). Update: it has not happened for 60 days
      val oldPatient:JValue = parse(oldPatientQuery.value.get.get) // unpack the string and cast to json-map
      //println("Retrieved patient: " +oldPatient \ "_source")
      return oldPatient \ "_source" // The good stuff is located in _source.
    }

    def getNow:DateTime = {
      DateTime.now(DateTimeZone.forID("Europe/Stockholm"))
    }
  }
