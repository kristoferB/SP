package sp.messages

import sp.domain._
import sp.domain.LogicNoImplicit._
import scala.util._
import org.json4s._



/**
  * Created by kristofer on 2016-07-15.
  *
  * Maybe change to protobuf at a later stage
  * Also fix apiFormats using macros in the future
  *
  */
trait SPCommunicationAPI {
  type MessageType
  type SUBType
  lazy val apiClasses: List[Class[_]] = List()
  lazy val apiJson: List[String] = List()


  lazy val spFormats = new JsonFormats{}
  lazy val formats = new JsonFormats {
    override val typeHints = ShortTypeHints(spFormats.typeHints.hints ++ apiClasses)
    override val companions = apiClasses.map(c => c -> this.getClass)
  }


  def write[T <: AnyRef](x: T)(implicit formats : org.json4s.Formats, mf : scala.reflect.Manifest[T]) = {
    org.json4s.native.Serialization.write[T](x)
  }


  /**
    * A helper method for parsing and matching messages.
    * @param x The message to parse
    * @param pf The partial function matching the MessageType API created for the service
    * @param pfSPMessages The partial function matching the standard SPMessages
    * @param orElse A method to return parsing errors to the caller (usually {reply ! _})
    * @param formats Implicit formats that need to be inscope, use implicit val in your code
    *                e.g. implicit val formats = ModelCommandAPI.formats
    * @param mf implicit that need to be inscopse, imported as above
    * @param mfSPM implicit that need to be inscopse, imported as above
    * @return boolean, false if nothing was done with the message, true otherwise
    */
  def readPF(x: String)(pf: PartialFunction[MessageType, Unit])(pfSPMessages: PartialFunction[SPMessages, Unit] = PartialFunction.empty)(orElse: String => Unit = (x: String) => Unit)(implicit formats : org.json4s.Formats, mf : scala.reflect.Manifest[MessageType], mfSPM : scala.reflect.Manifest[SPMessages]) = {
    val spMess = Try(org.json4s.native.Serialization.read[SPMessages](x)) match {
      case Failure(thr) => false
      case Success(mess) =>
        if (pfSPMessages.isDefinedAt(mess)) pfSPMessages(mess)
        true
    }

    spMess || {
      Try(org.json4s.native.Serialization.read[MessageType](x)) match {
        case Failure(thr) =>
          orElse(write(SPError("Couldn't parse message", Some(SPAttributes("parseError" -> thr.getMessage)))))
          true // return true here since it actually did something
        case Success(mess) =>
          if (pf.isDefinedAt(mess)) pf(mess)
          pf.isDefinedAt(mess)
      }
    }



  }
  def readSPMessagePF(x: String)(pf: PartialFunction[SPMessages, Unit])(orElse: String => Unit)(implicit formats : org.json4s.Formats, mf : scala.reflect.Manifest[SPMessages]) = {
    Try(org.json4s.native.Serialization.read[SPMessages](x)) match {
      case Failure(thr) => orElse(write(SPError("Couldn't parse message", Some(SPAttributes("parseError"->thr.getMessage)))))
      case Success(mess) => pf(mess)
    }
  }
  def read(x: String)(implicit formats : org.json4s.Formats, mf : scala.reflect.Manifest[MessageType]) = Try(org.json4s.native.Serialization.read[MessageType](x))
  def readSPMessage(x: String)(implicit formats : org.json4s.Formats, mf : scala.reflect.Manifest[SPMessages]) = Try(org.json4s.native.Serialization.read[SPMessages](x))
  def readSPAttributes(x: String)(implicit formats : org.json4s.Formats, mf : scala.reflect.Manifest[SPAttributes]) = Try(org.json4s.native.Serialization.read[SPAttributes](x))


}





