package sp.messages

import sp.domain._


/**
  * All messages among actors / services should be json-strings with headers and bodies
  * @param header Information about the message as key-value pairs
  * @param body The message encoded as SPAttributes
  */
case class SPMessage(header: SPAttributes, body: SPAttributes)


/**
  * A possible header to be used in SPMessage. All keys are optional and more will be included
  * @param reqID An id for the request used when replying to the sender
  * @param from The name of the sender
  * @param to The name of the service to receive
  */
case class SPHeader(reqID: Option[ID], from: Option[String], to: Option[String])


trait SPMessages
case class SPError(message: String, attributes: Option [SPAttributes] = None) extends SPMessages
case class SPACK(attributes: Option [SPAttributes]= None) extends SPMessages
case class SPOK(attributes: Option [SPAttributes]= None) extends SPMessages
case class SPDone(attributes: Option [SPAttributes]= None) extends SPMessages

case class StatusRequest(attributes: Option [SPAttributes]= None) extends SPMessages
case class StatusResponse(attributes: Option [SPAttributes]= None) extends SPMessages
case object StatusResponse{
  def apply(x: SPAttributes): StatusResponse = StatusResponse(Some(x))
}

