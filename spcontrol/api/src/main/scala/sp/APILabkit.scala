package sp.labkit {

  import sp.domain._
  import sp.domain.Logic._

  object APILabkit {
    val service = "Labkit"
    val topicRequest = "labkitRequests"
    val topicResponse = "labkitResponse"

    sealed trait Request
    sealed trait Response
    sealed trait API

    case class OPEvent(name: String, time: String, id: String, resource: String, product: Option[String]) extends API
    case class OP(start: OPEvent, end: Option[OPEvent], attributes: SPAttributes = SPAttributes()) extends API
    case class Positions(positions: Map[String,String], time: String) extends API

    case class OperationStarted(name: String, resource: String, product: String, operationType: String, time: String) extends Response
    case class OperationFinished(name: String, resource: String, product: String, operationType: String, time: String) extends Response
    case class ResourcePies(data: Map[String, Map[String, Int]]) extends Response
    case class ProductPies(data: List[(String, List[(String, Int)])]) extends Response
    case class ProdStat(name: String, leadtime: Int, processingTime: Int, waitingTime: Int, noOfOperations: Int, noOfPositions: Int) extends Response
    case class ProductStats(data: List[ProdStat]) extends Response

    object Formats {
      import play.api.libs.json._
      implicit lazy val fOPEvent: JSFormat[OPEvent] = Json.format[OPEvent]
      implicit lazy val fOP: JSFormat[OP] = Json.format[OP]
      implicit lazy val fPositions: JSFormat[Positions] = Json.format[Positions]
      implicit lazy val fOperationStarted: JSFormat[OperationStarted] = Json.format[OperationStarted]
      implicit lazy val fOperationFinished: JSFormat[OperationFinished] = Json.format[OperationFinished]
      implicit lazy val fResourcePies: JSFormat[ResourcePies] = Json.format[ResourcePies]
      implicit lazy val fProductPies = Json.format[ProductPies]
      implicit lazy val fProdStat = Json.format[ProdStat]
      implicit lazy val fProductStats = Json.format[ProductStats]

      def fLabkitRequest: JSFormat[Request] = Json.format[Request]
      def fLabkitResponse: JSFormat[Response] = Json.format[Response]
      def fLabkitAPI: JSFormat[API] = Json.format[API]
    }

    object Request {
      implicit lazy val fLabkitRequest: JSFormat[Request] = Formats.fLabkitRequest
    }

    object Response {
      implicit lazy val fLabkitResponse: JSFormat[Response] = Formats.fLabkitResponse
    }

    object API {
      implicit lazy val fLabkitAPI: JSFormat[API] = Formats.fLabkitAPI
    }

  }


}
