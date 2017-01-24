package communication

import java.util.UUID

import communication.APIWebSocket.PublishMessage

import scala.concurrent.Future
import scala.concurrent.Promise
import fr.hmil.roshttp.HttpRequest
import fr.hmil.roshttp.body.{JSONBody, PlainTextBody}
import monix.execution.Scheduler.Implicits.global

import scala.util.{Failure, Success, Try}
import fr.hmil.roshttp.response.SimpleHttpResponse
import org.scalajs._
import org.scalajs.dom.ext.Ajax
import org.scalajs.dom.raw.WebSocket

import scala.reflect.ClassTag

case class UPickleMessage(header: upickle.Js.Value, body: upickle.Js.Value)
object APIWebSocket {
  sealed trait API
  case class Subscribe(topic: String) extends API
  case class Unsubscribe(topic: String) extends API

  case class PublishMessage(mess: UPickleMessage, topic: String = "services") extends API

  // Move to general API in SP Domain
  case class SPACK(message: String) extends API
  case class SPERROR(message: String, attr: Option[upickle.Js.Value] = None) extends API
}


/**
  * Created by kristofer on 2017-01-04.
  */
object Comm {
  import rx._
  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()

  // Since Comm is an object, to init the communication, we have to trigger it in a method
  private var ws: Option[WebSocketHandler] = None


  def initCommunication(reInit: Boolean = false): Unit = {
    if (ws.isEmpty  || reInit) {
      ws = newWebsocket
    }
  }

  /**
    * Publish a message on a topic via websocket
    * @param topic The topic to publish on
    * @param mess The meassage to send
    * @return An option with a reactive variable to be used as observer. call trigger on it for side effects
    */
  def publishMessage(topic: String,  mess: UPickleMessage) = {
    initCommunication()
    ws.map(_.publishMessage(topic, mess))
  }

  def subscribe(topic: String) = {
    initCommunication()
    ws.foreach(_.subscribe(topic))
  }

  def unsubscribe(topic: String) = {
    initCommunication()
    ws.foreach(_.unsubscribe(topic))
  }

  def getMessageObserver(callBack: (UPickleMessage) => Unit ): rx.Obs = {
    initCommunication()
    ws.get.receivedMessage.foreach(callBack)
  }
  def getWebSocketNotifications(callBack: (String) => Unit ): rx.Obs = {
    initCommunication()
    ws.get.notification.foreach(callBack)
  }
  def getWebSocketErrors(callBack: (String) => Unit ): rx.Obs = {
    initCommunication()
    ws.get.errors.foreach(callBack)
  }
  def getWebSocketStatus = {
    initCommunication()
    ws.get.wsOpen
  }



  def ask(
        mess: UPickleMessage,
        topic: String = "request"
        ): Future[UPickleMessage] = {

    val url = org.scalajs.dom.window.location.href + "/api/"+topic
    val p = Promise[UPickleMessage]

    Ajax.post(url, APIParser.write(mess)).onSuccess{ case xhr =>
      val res = Try{APIParser.read[UPickleMessage](xhr.responseText)}
        p.complete(res)
    }


    p.future

  }


  private def newWebsocket = {
    Some(WebSocketHandler(getWebsocketUri))
  }

  def getWebsocketUri: String = {
    val wsProtocol = if (dom.document.location.protocol == "https:") "wss" else "ws"
    s"$wsProtocol://${dom.document.location.host}/socket"
  }

}

case class WebSocketHandler(uri: String) {
  import rx._
  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()

  /**
    * Publish a message on a topic via websocket
    * @param topic The topic to publish on
    * @param mess The meassage to send
    * @return An reactive variable to be used as obserer. call trigger on it for side effect
    */
  def publishMessage(topic: String,  mess: UPickleMessage) = {
    val toSend = APIWebSocket.PublishMessage(mess, topic)
    sendMessage(APIParser.write(toSend))
    receivedMessage
  }

  def subscribe(topic: String) = {
    val toSend = APIWebSocket.Subscribe(topic)
    sendMessage(APIParser.write(toSend))
  }
  def unsubscribe(topic: String) = {
    val toSend = APIWebSocket.Unsubscribe(topic)
    sendMessage(APIParser.write(toSend))
  }

  def sendMessage(mess: String) = {
    if (wsOpen.now) {
      ws.send(mess)
    } else notification() = "The websocket is not Open"
  }



  val mess = Var("")
  val receivedMessage: Var[UPickleMessage] = Var(UPickleMessage(upickle.Js.Null, upickle.Js.Null))
  val notification = Var("")
  val errors = Var("")
  val wsOpen = Var(false)

  private var ws: WebSocket = makeWS()

  def makeWS(): WebSocket = {
    val newWs = new WebSocket(uri)

    newWs.onopen = { (e: dom.Event) =>
      wsOpen() = true
    }
    newWs.onmessage = (e: dom.MessageEvent) => {
      mess() = e.data.toString
    }
    newWs.onclose = { (e: dom.Event) =>
      wsOpen() = false
      ws = makeWS() // reconnecting websocket if idle. Maybe better to send keep alive messages?
    }
    newWs.onerror = { (e: dom.ErrorEvent) =>
      errors() = e.message
    }
    newWs
  }


  def conv(str: String) = {
    Try{APIParser.read[APIWebSocket.API](str)} match {
      case x: Success[APIWebSocket.API] => x.value
      case x: Failure[_] => APIWebSocket.SPERROR(x.exception.getMessage)
    }
  }
  val api = Rx{conv(mess())} // need to have seperate conv due to macro problems


  val filter = Rx {
      api() match {
        case m: APIWebSocket.PublishMessage => receivedMessage() = m.mess
        case m: APIWebSocket.SPACK => notification() = m.message
        case m: APIWebSocket.SPERROR => errors() = m.message // handle error better
        case x => errors() = "Shouldn't get this on websocket: " + x.toString
      }
  }

  errors.triggerLater {
    println(s"An error: ${errors.now}")
  }
  wsOpen.triggerLater {
    println(s"Websocket is: ${wsOpen.now}")
  }
//   //some printlns for testing
//  mess.trigger(println("GOT A MESSAGE ON WEBSOCKET: " + mess.now))
//  api.trigger(println("Converted Message: " + api.toTry))
//  notification.trigger {
//    println(s"A NOTIFICATION: ${notification.now}")
//  }
//  receivedMessage.trigger {
//    println(s"A Message: ${receivedMessage.now}")
//  }
}

import upickle._
object APIParser extends upickle.AttributeTagged {
  override val tagName = "isa"

  override def annotate[V: ClassTag](rw: Reader[V], n: String) = Reader[V]{
    case Js.Obj(x@_*) if x.contains((tagName, Js.Str(n.split('.').takeRight(2).mkString(".")))) =>
      rw.read(Js.Obj(x.filter(_._1 != tagName):_*))
  }

  override def annotate[V: ClassTag](rw: Writer[V], n: String) = Writer[V]{ case x: V =>
    val filter = n.split('.').takeRight(2).mkString(".")
    Js.Obj((tagName, Js.Str(filter)) +: rw.write(x).asInstanceOf[Js.Obj].value:_*)
  }

}