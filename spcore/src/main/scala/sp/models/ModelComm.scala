package sp.models

import sp.domain._
import Logic._
import sp.domain.SchemaLogic._

import scala.util.Try



object ModelMakerInfo {
  case class ModelMakerRequest(request: APIModelMaker.Request)
  case class ModelMakerResponse(response: APIModelMaker.Response)

  val req: com.sksamuel.avro4s.SchemaFor[ModelMakerRequest] = com.sksamuel.avro4s.SchemaFor[ModelMakerRequest]
  val resp: com.sksamuel.avro4s.SchemaFor[ModelMakerResponse] = com.sksamuel.avro4s.SchemaFor[ModelMakerResponse]

  val apischema = makeMeASchema(
    req(),
    resp()
  )

  val attributes: APISP.StatusResponse = APISP.StatusResponse(
    service = APIModelMaker.service,
    tags = List("model", "modelmaker"),
    api = apischema,
    version = 1,
    attributes = SPAttributes.empty
  )
}

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
//  def extractRequest(mess: Try[SPMessage], name: String,  id: ID): Option[(SPHeader, api.Request)] = for {
//    m <- mess.toOption
//    h <- m.getHeaderAs[SPHeader].toOption if h.to == id.toString || h.to == name
//    b <- m.getBodyAs[api.Request].toOption
//  } yield (h, b)

  def extractAPISP(mess: Option[SPMessage]): Option[(SPHeader, APISP)] = for {
    m <- mess
    h <- m.getHeaderAs[SPHeader]
    b <- m.getBodyAs[APISP] if b == APISP.StatusRequest
  } yield (h, b)

}