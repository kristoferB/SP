package spgui.communication

import java.util.UUID

import scala.concurrent.Future
import scala.concurrent.Promise
import scala.util.{Failure, Success, Try}
import org.scalajs._
import org.scalajs.dom.ext.Ajax
import org.scalajs.dom.raw.WebSocket

import sp.domain._
import Logic._

import spgui.communication.{APIWebSocket => socketAPI}


/**
  * Created by kristofer on 2017-01-04.
  */
object BackendCommunication {
  import rx._
  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()
  import scala.concurrent.ExecutionContext.Implicits.global

  // Since Comm is an object, to init the communication, we have to trigger it in a method
  // Each topic has its own websocket listener. Any one of them can be used for sending to any topic
  private var sockets: Map[String, WebSocketHandler] = Map()

  val id: UUID = UUID.randomUUID()

  def initCommunication(reInit: Boolean = false): Unit = {
    if (sockets.isEmpty  || reInit) {
      val answers = newWebsocket("answers")
      val spevents = newWebsocket("spevents")
      sockets ++= Map("answers" -> answers, "spevents"->spevents)
    }
  }

  def ask( mess: SPMessage, topic: String = "requests"): Future[SPMessage] = {
    val url = org.scalajs.dom.window.location.href + "api/ask/"+topic
    post(mess.toJson, url)
  }

  def publish(mess: SPMessage, topic: String = "services"): Future[String] = {
    val url = org.scalajs.dom.window.location.href + "api/publish/"+topic
    post(mess.toJson, url).map(x => "posted")
  }

  /**
    * Publish a message on a topic via websocket. Not sure it works very good. Use publish
    * @param topic The topic to publish on
    * @param mess The message to send
    * @return An option with a reactive variable to be used as observer. call trigger on it for side effects
    */
  def publishMessage(topic: String,  mess: SPMessage) = {
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

  def getMessageObserver(callBack: (SPMessage) => Unit, topic: String = "answers"): rx.Obs = {
    getMessageVar(topic).foreach(callBack)
  }
  def getWebSocketNotificationsCB(callBack: (SPMessage) => Unit, topic: String = "answers" ): rx.Obs = {
    getWebSocketNotifications(topic).foreach(callBack)
  }
  def getWebSocketErrorsCB(callBack: (String) => Unit, topic: String = "answers" ): rx.Obs = {
    getWebSocketErrors(topic).foreach(callBack)
  }
  def getWebSocketStatusCB(topic: String = "answers") = {
    getWebSocketStatus(topic)
  }

  def getMessageVar(topic: String = "answers"): rx.Var[SPMessage] = {
    subscribe(topic)
    sockets(topic).receivedMessage
  }
  def getWebSocketNotifications(topic: String = "answers" ): rx.Var[SPMessage] = {
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
  def getWebSocketStatusObserver(callBack: (Boolean) => Unit, topic: String = "answers"): rx.Obs = {
    getWebSocketStatus(topic).foreach(callBack)
  }








  private def post(x: String, url: String) = {
    val p = Promise[SPMessage]
    Ajax.post(url, x).onComplete{
      case Success(xhr) =>
        val api =  SPAttributes.fromJsonGetAs[socketAPI.API](xhr.responseText)
        val message =  SPMessage.fromJson(xhr.responseText)
        p.complete(message)
        api.map{case x => println("post got: " + x)}
      case Failure(e) =>
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
    val url = dom.document.location.host.replace("/#", "")
    s"$wsProtocol://${url}/socket/$topic/$id"

  }


}

case class WebSocketHandler(uri: String) {
  import rx._
  import rx.async._
  import rx.async.Platform._
  import scala.concurrent.duration._
  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()

  def publishMessage(topic: String,  mess: SPMessage) = {
    val toSend = socketAPI.PublishMessage(mess, topic)
    sendMessage(toJson(toSend))
    receivedMessage
  }

  def unsubscribe(topic: String) = {
    ws.close()
  }

  def sendMessage(mess: String) = {
    if (wsOpen.now) {
      ws.send(mess)
    } else errors() = "The websocket is not Open"
  }


  var c = 0

  val mess = Var("")
  val receivedMessage: Var[SPMessage] = Var(SPMessage(SPAttributes(), SPAttributes()))
  val notification: Var[SPMessage] = Var(SPMessage(SPAttributes(), SPAttributes()))
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
      if (e.data.toString != "keep-alive")
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

  // need to have separete functions for json magic due to macro problems
  def fJ(str: String) = SPMessage.fromJson(str)
  val convWebsocketStringToSPMessage = Rx{
    val str = mess()
    //println("websocket got a message: "+ str)
    fJ(str)
    }




  def getAsSPE(p: SPAttributes) = p.getAs[APISP.SPError]()
  def getAsSPAPI(p: SPAttributes) = p.getAs[APISP]()

  val separateGeneralMessages = Rx {
    val a = convWebsocketStringToSPMessage()
    a.map{
      case x @ SPMessage(h, b) if getAsSPE(b).nonEmpty =>
        errors() = getAsSPE(b).get.message
      case x @ SPMessage(h, b) if getAsSPAPI(b).nonEmpty =>
        notification() = x
        receivedMessage() = x
      case x =>
        println(s"$uri - $c"); c += 1
        receivedMessage() = x
    }
    a.failed.foreach(t =>
      errors() = "Didn't get an SPMessage: " + t.getMessage
    )

  }

  errors.triggerLater {
    //println(s"An error: ${errors.now}")
  }
  wsOpen.triggerLater {
    println(s"Websocket is: ${wsOpen.now}")
  }
//   //some printlns for testing
//  mess.trigger(println("GOT A MESSAGE ON WEBSOCKET: " + mess.now))
//  socketAPI.trigger(println("Converted Message: " + socketAPI.toTry))
  notification.triggerLater {
    println(s"A NOTIFICATION: ${notification.now}")
  }

}


