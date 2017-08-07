package sp.example

import sp.domain._
import Logic._


// this object is used for converting to and from json.
object APIExampleService {
  sealed trait Request
  sealed trait Response

  // The messages that this service can send and receive is defined as case classes
  // Messages you can send to me
  /**
    * Adds a new pie to the memory with an id
    * @param id an UUID identifying the pie
    */
  case class StartTheTicker(id: ID) extends APIExampleService.Request

  /**
    * removes the pie with the id
    * @param id an UUID identifying the pie
    */
  case class StopTheTicker(id: ID) extends APIExampleService.Request

  /**
    * Changes the pie to the given map
    * @param id  an UUID identifying the pie
    * @param map A map representing a pie
    */
  case class SetTheTicker(id: ID, map: Map[String, Int]) extends APIExampleService.Request
  case object GetTheTickers extends APIExampleService.Request
  case object ResetAllTickers extends APIExampleService.Request


  // Messages that I will send as answer
  case class TickerEvent(map: Map[String, Int], id: ID) extends APIExampleService.Response
  case class TheTickers(ids: List[ID]) extends APIExampleService.Response

  object Request {
    implicit lazy val fExampleServiceRequest: JSFormat[Request] = deriveFormatISA[Request]
  }
  object Response {
    implicit lazy val fExampleServiceResponse: JSFormat[Response] = deriveFormatISA[Response]
  }

}



object ExampleServiceInfo {
  case class ExampleServiceSchema(request: APIExampleService.Request, response: APIExampleService.Response)
  val s: com.sksamuel.avro4s.SchemaFor[ExampleServiceSchema] = com.sksamuel.avro4s.SchemaFor[ExampleServiceSchema]

  val attributes: APISP.StatusResponse = APISP.StatusResponse(
    service = "ExampleServiceSchema",
    instanceID = Some(ID.newID),
    instanceName = "",
    tags = List("example"),
    api = SPAttributes.fromJson(s().toString).get,
    version = 1,
    attributes = SPAttributes.empty
  )
}