package spgui.communication

import java.util.UUID


import scala.concurrent.Future
import scala.concurrent.Promise

import scala.util.{Failure, Success, Try}
import org.scalajs._
import org.scalajs.dom.ext.Ajax
import org.scalajs.dom.raw.WebSocket
import scala.concurrent.ExecutionContext.Implicits.global
import scala.reflect.ClassTag








/**
  * Created by kristofer on 2017-01-04.
  */
object BackendCommunication {
  import rx._
  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()

  // Since Comm is an object, to init the communication, we have to trigger it in a method
  // Each topic has its own websocket listener. Any one of them can be used for sending to any topic
  private var sockets: Map[String, WebSocketHandler] = Map()

  def initCommunication(reInit: Boolean = false): Unit = {
    if (sockets.isEmpty  || reInit) {
      val answers = newWebsocket("answers")
      val spevents = newWebsocket("spevents")
      sockets ++= Map("answers" -> answers, "spevents"->spevents)
    }
  }

  /**
    * Publish a message on a topic via websocket
    * @param topic The topic to publish on
    * @param mess The message to send
    * @return An option with a reactive variable to be used as observer. call trigger on it for side effects
    */
  def publishMessage(topic: String,  mess: UPickleMessage) = {
    initCommunication()
    ws.publishMessage(topic, mess)
  }

  def subscribe(topic: String) = {
    initCommunication()
    if (!sockets.contains(topic))
      sockets += topic -> newWebsocket(topic)
  }

  def unsubscribe(topic: String) = {
    if (sockets.contains(topic)){
      val s = sockets(topic)
      s.unsubscribe(topic)
      sockets -= topic
    }
  }

  def getMessageObserver(callBack: (UPickleMessage) => Unit, topic: String = "answers"): rx.Obs = {
    getMessageVar(topic).foreach(callBack)
  }
  def getWebSocketNotificationsCB(callBack: (String) => Unit, topic: String = "answers" ): rx.Obs = {
    getWebSocketNotifications(topic).foreach(callBack)
  }
  def getWebSocketErrorsCB(callBack: (String) => Unit, topic: String = "answers" ): rx.Obs = {
    getWebSocketErrors(topic).foreach(callBack)
  }
  def getWebSocketStatusCB(topic: String = "answers") = {
    getWebSocketStatus(topic)
  }

  def getMessageVar(topic: String = "answers"): rx.Var[UPickleMessage] = {
    subscribe(topic)
    sockets(topic).receivedMessage
  }
  def getWebSocketNotifications(topic: String = "answers" ): rx.Var[String] = {
    subscribe(topic)
    sockets(topic).notification
  }
  def getWebSocketErrors(topic: String = "answers" ): rx.Var[String] = {
    subscribe(topic)
    sockets(topic).errors
  }
  def getWebSocketStatus(topic: String = "answers"): rx.Var[Boolean] = {
    subscribe(topic)
    sockets(topic).wsOpen
  }





  def ask( mess: UPickleMessage, topic: String = "requests"): Future[UPickleMessage] = {
    val url = org.scalajs.dom.window.location.href + "api/ask/"+topic
    post(APIParser.write(mess), url)
  }

  def publish(mess: UPickleMessage, topic: String = "services"): Future[String] = {
    val url = org.scalajs.dom.window.location.href + "api/publish/"+topic
    post(APIParser.write(mess), url).map(x => "posted")
  }

  private def post(x: String, url: String) = {
    val p = Promise[UPickleMessage]
    Ajax.post(url, x).onSuccess{ case xhr =>
      val socketAPI =  Try{APIParser.read[APIWebSocket](xhr.responseText)}
      val message =  Try{APIParser.read[UPickleMessage](xhr.responseText)}
      p.complete(message)
      socketAPI.map{case x => println("post got: " + x)}
      socketAPI.recover{case x => println("post got (parse error): " + x.getMessage)}
    }
    p.future
  }


  private def ws = {
    initCommunication(false)
    sockets.head._2
  }

  private def newWebsocket(topic: String) = {
    WebSocketHandler(getWebsocketUri(topic))
  }

  def getWebsocketUri(topic: String): String = {
    val wsProtocol = if (dom.document.location.protocol == "https:") "wss" else "ws"
    s"$wsProtocol://${dom.document.location.host}/socket/$topic"

  }


}

case class WebSocketHandler(uri: String) {
  import rx._
  import rx.async._
  import rx.async.Platform._
  import scala.concurrent.duration._
  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()

  def publishMessage(topic: String,  mess: UPickleMessage) = {
    val toSend = APIWebSocket.PublishMessage(mess, topic)
    sendMessage(APIParser.write(toSend))
    receivedMessage
  }

  def unsubscribe(topic: String) = {
    ws.close()
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
  val retry = Var(false)
  val delayRetry = retry.delay(5 seconds)
  val triggerReconnect = Rx{if (delayRetry()) {
    makeWS()
    retry() = false
  }}

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
      retry() = true
    }
    newWs.onerror = { (e: dom.ErrorEvent) =>
      errors() = e.message
    }
    newWs
  }


  def conv(str: String) = {
    Try{APIParser.read[APIWebSocket](str)} match {
      case x: Success[APIWebSocket] => x.value
      case x: Failure[_] => APIWebSocket.SPError(x.exception.getMessage)
    }
  }
  val api = Rx{conv(mess())} // need to have seperate conv due to macro problems


  val filter = Rx {
      api() match {
        case m: APIWebSocket.PublishMessage => receivedMessage() = m.mess
        case m: APIWebSocket.SPACK => notification() = "spack"
        case m: APIWebSocket.SPError => errors() = m.message // handle error better
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


