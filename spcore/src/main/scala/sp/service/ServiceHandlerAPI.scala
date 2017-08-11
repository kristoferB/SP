package sp.service


import sp.domain._
import Logic._
import scala.util.Try





object APIServiceHandler {
  sealed trait Request
  sealed trait Response
  val service = "ServiceHandler"

  case object GetServices extends Request
  case class Services(xs: List[APISP.StatusResponse]) extends Response
  case class NewService(x: APISP.StatusResponse) extends Response
  case class RemovedService(x: APISP.StatusResponse) extends Response


  object Request {
    implicit lazy val fServiceHandlerRequest: JSFormat[Request] = deriveFormatISA[Request]
  }
  object Response {
    implicit lazy val fServiceHandlerResponse: JSFormat[Response] = deriveFormatISA[Response]
  }
}

object ServiceHandlerInfo {
  case class ServiceHandlerRequest(request: APIServiceHandler.Request)
  case class ServiceHandlerResponse(response: APIServiceHandler.Response)

  val req: com.sksamuel.avro4s.SchemaFor[ServiceHandlerRequest] = com.sksamuel.avro4s.SchemaFor[ServiceHandlerRequest]
  val resp: com.sksamuel.avro4s.SchemaFor[ServiceHandlerResponse] = com.sksamuel.avro4s.SchemaFor[ServiceHandlerResponse]

  val apischema = makeMeASchema(
    req(),
    resp()
  )


  val attributes: APISP.StatusResponse = APISP.StatusResponse(
    service = APIServiceHandler.service,
    instanceID = Some(ID.newID),
    instanceName = "",
    tags = List("service", "core"),
    api = apischema,
    version = 1,
    attributes = SPAttributes.empty
  )
}




object ServiceHandlerComm {
  def extractRequest(mess: Try[SPMessage]) = for {
    m <- mess
    h <- m.getHeaderAs[SPHeader] if h.to == "ServiceHandler"
    b <- m.getBodyAs[APIServiceHandler.Request]
  } yield (h, b)

  def extractAPISP(mess: Try[SPMessage]) = for {
    m <- mess
    h <- m.getHeaderAs[SPHeader]
    b <- m.getBodyAs[APISP]
  } yield (h, b)


  def makeMess(h: SPHeader, b: APIServiceHandler.Response) = SPMessage.makeJson(h, b)
  def makeMess(h: SPHeader, b: APISP) = SPMessage.makeJson(h, b)
}
