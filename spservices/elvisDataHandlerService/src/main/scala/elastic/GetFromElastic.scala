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

class GetFromElastic {
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

  //println("GetFromElastic instance attempting to connect to elasticsearch on address: " + ip + ":" + port )
  val client = new Client(s"http://$ip:$port") // creates a wabisabi client for communication with elasticsearch

  def searchES(index: String, query: String): JValue = {
    val resQuery = client.search(index = index, query = query).map(_.getResponseBody)
    while (!resQuery.isCompleted) {} // patiently wait for response from the database. //TODO at some point add timeout. It could get stuck here forever (but probably not). Update: it has not happened for 60 days
    val res:JValue = parse(resQuery.value.get.get) // unpack the string and cast to json-map
    return res
  }

  /**
  Queries the database for all patients in the given team
  :param team: the team to search for
  :return: all patients currently under the given team, returned as a list
  */
  def getPatientsOfTeam(team: String): List[JValue] = {
    val results = searchES("on_going_patient_index", "{\"query\": {\"match\": {\"Team\":\"" + team + "\"}}}")
    val extractedResults = results \ "hits" \ "hits"
    var patients  = new ListBuffer[JValue]()
    for {
      JObject(obj) <- extractedResults
      JField("CareContactId", JInt(careContactId)) <- obj } {
        patients += careContactId }
    return patients.toList.distinct // return list withouth duplicates
  }

  /**
  :return: list containg all patients currently at the emergency room.
  */
  def getAllPatients(): List[JObject] = {
    val results = searchES("on_going_patient_index", "{\"query\": {\"match_all\": {}}}")
    val extractedResults = results \ "hits" \ "hits"
    var patients = new ListBuffer[JObject]()
    for {
      JObject(obj) <- extractedResults } {
        patients += obj }
    return patients.toList.distinct // return list withouth duplicates
  }

  /**
  :return: list containg all patients currently at the emergency room.
  */
  def getAllFinishedPatients(): List[JValue] = {
    val results = searchES("finished_patient_index", "{\"query\": {\"match_all\": {}}}")
    val extractedResults = results \ "hits" \ "hits"
    var patients = new ListBuffer[JValue]()
    for {
      JObject(obj) <- extractedResults
      JField("CareContactId", JInt(careContactId)) <- obj } {
        patients += careContactId }
    return patients.toList.distinct // return list withouth duplicates
  }

  // UNDONE
  def loadValue(fields: String): List[JValue] = {
    val results = searchES("on_going_patient_index", "{\"stored_fields\":\"" + fields + "\", \"query\": {\"match_all\": {}}}")
    val extractedResults = results \ "hits" \ "hits"
    var tmp = new ListBuffer[JValue]()
    for {
      JArray(objList) <- extractedResults
      JObject(obj) <- objList
      JField("_id", JString(id)) <- obj } {
        tmp += id
      }
    return tmp.toList.distinct
  }


  /** Fetches from elastic the patient under /index/PATIENT_TYPE/careContactId */
  def getPatientFromElastic(index: String, careContactId: String): JValue = {
    val oldPatientQuery = client.get(index, PATIENT_TYPE, careContactId).map(_.getResponseBody) //fetch patient from database
    while (!oldPatientQuery.isCompleted) {} // patiently wait for response from the database. //TODO at some point add timeout. It could get stuck here forever (but probably not). Update: it has not happened for 60 days
    val oldPatient:JValue = parse(oldPatientQuery.value.get.get) // unpack the string and cast to json-map
    //println("Retrieved patient: " +oldPatient \ "_source")
    return oldPatient \ "_source" // The good stuff is located in _source.
  }


  def getArrivingPatients(startTime: Long, endTime: Long, intervalMinutes: Int): String = {
    val str: String = "{\"query\":{\"match_all\":{}},\"filter\":{\"range\":{\"VisitRegistrationTime\":{\"gte\":" + startTime + ",\"lte\":" + endTime + ",\"format\":\"epoch_millis\"}}}}"
    println(str)
    val resQuery = client.search(index = "*", query = str).map(_.getResponseBody)
    println(resQuery)
    while (!resQuery.isCompleted) {} // patiently wait for response from the database. //TODO at some point add timeout. It could get stuck here forever (but probably not). Update: it has not happened for 60 days
    val results:JValue = parse(resQuery.value.get.get) // unpack the string and cast to json-map
    val extractedResults = results \ "hits" \ "hits"
    println(extractedResults)
    return "hej"
  }




}
