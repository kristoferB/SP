package sp.models

import sp.domain._
import Logic._
import sp.domain.SchemaLogic._

import scala.util.Try



object ModelInfo {
  case class ModelRequest(request: APIModel.Request)
  case class ModelResponse(response: APIModel.Response)

  val req: com.sksamuel.avro4s.SchemaFor[ModelRequest] = com.sksamuel.avro4s.SchemaFor[ModelRequest]
  val resp: com.sksamuel.avro4s.SchemaFor[ModelResponse] = com.sksamuel.avro4s.SchemaFor[ModelResponse]

  val apischema = makeMeASchema(
    req(),
    resp()
  )

  val attributes: APISP.StatusResponse = APISP.StatusResponse(
    service = APIModel.service,
    tags = List("model"),
    api = apischema,
    version = 1,
    attributes = SPAttributes.empty
  )
}

import sp.models.{APIModel => api}


object ModelsComm {
  def extractRequest(mess: Try[SPMessage], name: String,  id: ID): Option[(SPHeader, api.Request)] = for {
    m <- mess.toOption
    h <- m.getHeaderAs[SPHeader].toOption if h.to == id.toString || h.to == name
    b <- m.getBodyAs[api.Request].toOption
  } yield (h, b)

  def extractAPISP(mess: Try[SPMessage]): Option[(SPHeader, APISP)] = for {
    m <- mess.toOption
    h <- m.getHeaderAs[SPHeader].toOption
    b <- m.getBodyAs[APISP].toOption if b == APISP.StatusRequest
  } yield (h, b)


  def makeMess(h: SPHeader, b: api.Response) = SPMessage.makeJson[SPHeader, api.Response](h, b)
  def makeMess(h: SPHeader, b: APISP) = SPMessage.makeJson[SPHeader, APISP](h, b)
}