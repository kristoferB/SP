package sp.example {

  import sp.domain._
  import Logic._


  // this object is used for converting to and from json.
  object APIExampleService {

    sealed trait Request

    sealed trait Response

    val service = "ExampleService"

    // The messages that this service can send and receive is defined as case classes
    // Messages you can send to me
    /**
      * Adds a new pie to the memory with an id
      *
      * @param id an UUID identifying the pie
      */
    case class StartTheTicker(id: ID) extends Request

    /**
      * removes the pie with the id
      *
      * @param id an UUID identifying the pie
      */
    case class StopTheTicker(id: ID) extends Request

    /**
      * Changes the pie to the given map
      *
      * @param id  an UUID identifying the pie
      * @param map A map representing a pie
      */
    case class SetTheTicker(id: ID, map: Map[String, Int]) extends Request

    case object GetTheTickers extends Request

    case object ResetAllTickers extends Request


    // Messages that I will send as answer
    case class TickerEvent(map: Map[String, Int], id: ID) extends Response

    case class TheTickers(ids: List[ID]) extends Response


    // The below is an example how to create the json formaters
    object Formats {

      import play.api.libs.json._

      implicit val fStartTheTicker: JSFormat[StartTheTicker] = Json.format[StartTheTicker]
      implicit val fStopTheTicker: JSFormat[StopTheTicker] = Json.format[StopTheTicker]
      implicit val fSetTheTicker: JSFormat[SetTheTicker] = Json.format[SetTheTicker]
      implicit val fGetTheTickers: JSFormat[GetTheTickers.type] = deriveCaseObject[GetTheTickers.type]
      implicit val fResetAllTickers: JSFormat[ResetAllTickers.type] = deriveCaseObject[ResetAllTickers.type]
      implicit val fTickerEvent: JSFormat[TickerEvent] = Json.format[TickerEvent]
      implicit val fTheTickers: JSFormat[TheTickers] = Json.format[TheTickers]
      def fExampleServiceRequest: JSFormat[Request] = Json.format[Request]
      def fExampleServiceResponse: JSFormat[Response] = Json.format[Response]
    }

    object Request {
      implicit lazy val fExampleServiceRequest: JSFormat[Request] = Formats.fExampleServiceRequest
    }

    object Response {
      implicit lazy val fExampleServiceResponse: JSFormat[Response] = Formats.fExampleServiceResponse
    }

  }


}