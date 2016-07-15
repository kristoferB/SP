package sp.messages

import sp.domain.SPAttributes


trait SPMessages

case class SPError(message: String, attributes: Option [SPAttributes] = None) extends SPMessages
case class SPACK(attributes: Option [SPAttributes]= None) extends SPMessages
case class SPOK(attributes: Option [SPAttributes]= None) extends SPMessages

case class StatusRequest(attributes: Option [SPAttributes]= None) extends SPMessages
case class Status(attributes: Option [SPAttributes]= None) extends SPMessages
case object Status{
  def apply(x: SPAttributes): Status = Status(Some(x))
}

