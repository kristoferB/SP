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

  var ws: Option[WebSocketHandler] = None

  /**
    * Publish a message on a topic via websocket
    * @param topic The topic to publish on
    * @param mess The meassage to send
    * @return An option with a reactive variable to be used as observer. call trigger on it for side effects
    */
  def publishMessage(topic: String,  mess: UPickleMessage) = {
    if (ws.isEmpty) {
      ws = Some(WebSocketHandler(getWebsocketUri))

    }
    ws.map(_.publishMessage(topic, mess))
  }







  def sendRequest(
        mess: upickle.Js.Value,
        header: upickle.Js.Obj = upickle.Js.Obj("empty"-> upickle.Js.Str("header"))
        ): Future[String] = {

    val url = org.scalajs.dom.window.location.href
    val request = HttpRequest(url).withPath("/request")
    val p = Promise[String]

    // upd header

    import fr.hmil.roshttp.body.Implicits._
    val json = fr.hmil.roshttp.body.JSONBody.JSONObject(
      "header"->upickle.default.write(header),
      "body" -> upickle.default.write(mess)
    )

    request.post(json).onComplete {
      case x: Success[SimpleHttpResponse] =>
        //check for errors here later
        p.complete(Try{x.value.body})
      case x: Failure[SimpleHttpResponse] =>
        p.failure(x.exception)

    }

    p.future

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
    if (isWebsocketOpen) {
      val toSend = APIWebSocket.PublishMessage(mess, topic)
      ws.send(APIParser.write(toSend))
    } else notification() = "The websocket is still Not Open"
    receivedMessage
  }
  def isWebsocketOpen = wsIsOpen



  val mess = Var("")
  val receivedMessage: Var[UPickleMessage] = Var(UPickleMessage(upickle.Js.Null, upickle.Js.Null))
  val notification = Var("")
  val errors = Var("")

  private val ws: WebSocket = new WebSocket(uri)
  private var wsIsOpen = false
  ws.onopen = { (e: dom.Event) =>
    wsIsOpen = true
  }
  ws.onmessage = (e: dom.MessageEvent) => {
    mess() = e.data.toString
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

//  mess.trigger(println("GOT A MESSAGE ON WEBSOCKET: " + mess.now))
//  api.trigger(println("Converted Message: " + api.toTry))
  notification.trigger {
    println(s"A NOTIFICATION: ${notification.now}")
  }
  receivedMessage.trigger {
    println(s"A Message: ${receivedMessage.now}")
  }
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