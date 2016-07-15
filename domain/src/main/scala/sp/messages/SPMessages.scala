package sp.messages

import sp.domain.SPAttributes


trait SPMessages

case class SPError(message: String, attributes: Option [SPAttributes] = None) extends SPMessages
case class SPACK(attributes: Option [SPAttributes]= None) extends SPMessages
case class SPOK(attributes: Option [SPAttributes]= None) extends SPMessages

case class StatusRequest(attributes: Option [SPAttributes]= None) extends SPMessages
case class StatusResponse(attributes: Option [SPAttributes]= None) extends SPMessages
case object StatusResponse{
  def apply(x: SPAttributes): StatusResponse = StatusResponse(Some(x))
}

