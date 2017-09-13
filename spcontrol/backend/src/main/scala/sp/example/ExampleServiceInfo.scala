package sp.example

import sp.domain._


object ExampleServiceInfo {
    import sp.domain.SchemaLogic._
    case class ExampleServiceRequest(request: APIExampleService.Request)
    case class ExampleServiceResponse(response: APIExampleService.Response)

    lazy val req: com.sksamuel.avro4s.SchemaFor[ExampleServiceRequest] = com.sksamuel.avro4s.SchemaFor[ExampleServiceRequest]
    lazy val resp: com.sksamuel.avro4s.SchemaFor[ExampleServiceResponse] = com.sksamuel.avro4s.SchemaFor[ExampleServiceResponse]

    val apischema = makeMeASchema(
      req(),
      resp()
    )

    val attributes: APISP.StatusResponse = APISP.StatusResponse(
      service = APIExampleService.service,
      instanceID = Some(ID.newID),
      instanceName = "",
      tags = List("example"),
      api = apischema,
      version = 1,
      attributes = SPAttributes.empty
    )
  }
