package sp.service

import sp.domain._
import Logic._
import sp.domain.SchemaLogic._





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
    topicRequest = APIServiceHandler.topicRequest,
    topicResponse = APIServiceHandler.topicResponse,
    attributes = SPAttributes.empty
  )
}


