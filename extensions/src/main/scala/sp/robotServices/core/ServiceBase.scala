package sp.robotServices.core

import java.text.SimpleDateFormat

import akka.actor._
import akka.event.Logging
import com.codemettle.reactivemq.ReActiveMQMessages._
import com.codemettle.reactivemq._
import com.codemettle.reactivemq.model._
import org.json4s.{DefaultFormats, _}
import org.json4s.jackson.JsonMethods._
import sp.robotServices.launcher.LaunchRobotRuntimeService
import sp.system.SPActorSystem._

trait ServiceBase extends Actor {

  // Constants
  val customDateFormat = new DefaultFormats {
    override def dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
  }
  implicit val formats = customDateFormat ++ org.json4s.ext.JodaTimeSerializers.all // for json serialization
  val log = Logging(context.system, getClass.getName)

  // State
  var theBus: Option[ActorRef] = None

  // Methods
  override final def receive() = handleOtherMessages orElse handleBasicMessages

  def handleOtherMessages: PartialFunction[Any, Unit] = PartialFunction.empty

  def handleBasicMessages: PartialFunction[Any, Unit] = {
    case "connect" =>
      ReActiveMQExtension(context.system).manager ! GetConnection(s"nio://${settings.activeMQ}:${settings.activeMQPort}")
    case ConnectionEstablished(request, c) =>
      log.info("Connected: " + request)
      c ! ConsumeFromTopic(settings.activeMQTopic)
      theBus = Some(c)
    case ConnectionFailed(request, reason) =>
      log.error("Connection failed: " + reason)
    case mess @ AMQMessage(body, prop, headers) =>
      val json: JValue = parse(body.toString)
      handleAmqMessage(json)

  }

  def sendToBusWithTopic(topic: String, json: String) = {
    theBus.foreach{bus => bus ! SendMessage(Topic(topic), AMQMessage(json))}
  }

  def sendToBus(json: String) = {
    theBus.foreach{bus => bus ! SendMessage(Topic(s"${settings.activeMQTopic}"), AMQMessage(json))}
  }

  override def postStop() = {
    theBus.foreach(_ ! CloseConnection)
  }

  // Abstract methods
  def handleAmqMessage(json: JValue)
}
