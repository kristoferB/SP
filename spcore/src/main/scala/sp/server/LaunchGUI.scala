package sp.server

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.{ActorMaterializer, Materializer, SourceShape}

import scala.concurrent.duration._
import akka.util.Timeout

import scala.reflect.ClassTag
import akka.stream.scaladsl._
import akka.http.scaladsl.server.{Directives, MethodRejection, SchemeRejection}
import akka.stream.scaladsl.Flow
import akka.cluster.pubsub._
import DistributedPubSubMediator._
import akka.http.scaladsl.model.ws.TextMessage.Strict
import sp.messages.APIParser

import scala.util._








case class UPickleMessage(header: upickle.Js.Value, body: upickle.Js.Value)

sealed trait APIWebSocket
object APIWebSocket {

  //case class filter(reqID: String) extends API // add more filters here in the future

  case class PublishMessage(mess: UPickleMessage, topic: String = "services") extends APIWebSocket

  // Move to general API in SP Domain
  case class SPACK(message: String) extends APIWebSocket
  case class SPError(message: String, attr: Option[upickle.Js.Value] = None) extends APIWebSocket

}


/**
 * Used by the SP launcher file
 * Created by Kristofer on 2014-06-19.
 */
class LaunchGUI(system: ActorSystem)  {
  implicit val timeout = Timeout(5 seconds)
  import scala.concurrent.Future
  import akka.pattern.ask
  implicit val actorSystem = system
  implicit val materializer = ActorMaterializer()
  val mediator = DistributedPubSub(system).mediator

  def launch = {
      val webFolder: String = system.settings.config getString "sp.webFolder"
      val devFolder: String = system.settings.config getString "sp.devFolder"
      val buildFolder: String = system.settings.config getString "sp.buildFolder"
      val devMode: Boolean = system.settings.config getBoolean "sp.devMode"
      val interface = system.settings.config getString "sp.interface"
    val port = system.settings.config getInt "sp.port"
    val srcFolder: String = if(devMode)
      devFolder else buildFolder



    //import upickle.default._

    def api =
        path("socket" / Segment){ topic =>
          get{
              val h = new WebsocketHandler(mediator, topic)
               handleWebSocketMessages(h.webSocketHandler)
          }
      } ~
      pathPrefix("api") {
        get {
          pathEndOrSingleSlash {
            complete("THE SP API")
          }
        } ~
          postMessage("ask", true) ~
          postMessage("publish", false)
      }



    val route =
      api ~
      pathEndOrSingleSlash {
        getFromFile(srcFolder + "/index.html")
      } ~
        getFromDirectory(srcFolder) ~
        getFromDirectory(webFolder) ~
        getFromFile(srcFolder + "/index.html")
        getFromFile(webFolder + "/index.html")


    val bindingFuture = Http().bindAndHandle(route, interface, port)

    println(s"Server started ${system.name}, $interface:$port")

    bindingFuture

//  scala.io.StdIn.readLine("Press ENTER to exit application.\n") match {
//    case x => system.terminate()
//  }



  }



  def postMessage(url: String, shouldAsk: Boolean) = {
    path(url / Segments) { cmd =>
      if (cmd.isEmpty)
        reject(SchemeRejection("no topic"))

      println("tjo ho in post message: "+ url +" - "+cmd)

      val topic = cmd.head
      val service = cmd.tail.headOption.getOrElse("")

      post { entity(as[String]){data =>
        val mess = fixMess(data, topic, service)
        if (shouldAsk){
          val answer = mediator.ask(Publish(topic, mess)).mapTo[String]
          completeOrRecoverWith(answer){extr =>
            complete(APIParser.write(APIWebSocket.SPError("No service answered the request")))
          }
        } else {
          mediator ! Publish(topic, mess)
          complete(APIParser.write(APIWebSocket.SPACK("Message sent")))
        }
      }}
    }
  }


  def fixMess(mess: String, topic: String, service: String) = {
    val uP = Try{APIParser.read[UPickleMessage](mess)}

    val toSend = uP.map{ m =>
      val updH = if (service.nonEmpty) {
        val h = Try{m.header.obj.toList :+ ("service" -> upickle.Js.Str(service))}.map(x => upickle.Js.Obj(x:_*))
        h.getOrElse(m.header)
      } else m.header
      APIParser.write(m.copy(header = updH))
    }
    toSend.getOrElse(mess)
  }

}





// test websockets

import akka.actor._
import akka.stream._
import akka.stream.scaladsl._
import akka.http.scaladsl.model.ws.{ Message, TextMessage }


// https://markatta.com/codemonkey/blog/2016/04/18/chat-with-akka-http-websockets/
// https://github.com/jrudolph/akka-http-scala-js-websocket-chat


class WebsocketHandler(mediator: ActorRef, topic: String = "answers") {
  var messFilter: UPickleMessage => Boolean = (x: UPickleMessage) => true
  var myRef: Option[ActorRef] = None

  case class MessageAndAck(messToBus: Option[Any], reply: APIWebSocket)

  lazy val webSocketHandler: Flow[Message, Message, NotUsed]  = Flow.fromGraph(GraphDSL.create() { implicit b: GraphDSL.Builder[NotUsed] =>
    import GraphDSL.Implicits._

    val in = b.add(transformMessages)
    val out = b.add(sendToWebSocket)

    val parseResultBC = b.add(Broadcast[Try[APIWebSocket]](2))
    val messageBC = b.add(Broadcast[MessageAndAck](2))
    val merge = b.add(Merge[APIWebSocket](3))

    val sendReceive = Flow.fromSinkAndSource(sendToBus, receiveFromBus)



    in ~> parseResultBC ~> matchWebSocketMessages ~> messageBC ~> prepareToSend ~> sendReceive ~> parseMessagesFromBus ~> merge
    parseResultBC ~> parsingError ~> merge
    messageBC ~> prepareToSendACK ~> merge
    merge ~> convertAPIToString ~> out

    FlowShape(in.in, out.out)
  })


  val receiveFromBus: Source[Any, Unit] = Source.actorRef[Any](5, OverflowStrategy.fail)
    .mapMaterializedValue { ref =>
      myRef = Some(ref)
      mediator ! Subscribe(topic, ref)
    }


  val sendToBus: Sink[Any, NotUsed] = Sink.actorRef[Any](mediator, "Killing me")
  val sendToWebSocket: Flow[String, Strict, NotUsed] =  Flow[String].map(str => TextMessage(str))


  val transformMessages: Flow[Message, Try[APIWebSocket], NotUsed] = Flow[Message]
    .collect{ case TextMessage.Strict(text) => println(s"Websocket got: $text"); text}
    .map{str => Try{sp.messages.APIParser.read[APIWebSocket](str)}}

  val matchWebSocketMessages: Flow[Try[APIWebSocket], MessageAndAck, NotUsed] = Flow[Try[APIWebSocket]]
      .collect{case x: Success[APIWebSocket] => x.value}
      .collect{
        case APIWebSocket.PublishMessage(mess, t) =>
          MessageAndAck(Some(Publish(t, sp.messages.APIParser.write(mess))), APIWebSocket.SPACK(s"Message sent to topic $t"))
      }

  val prepareToSend: Flow[MessageAndAck, Any, NotUsed] = Flow[MessageAndAck]
    .collect{case x: MessageAndAck if x.messToBus.isDefined => x.messToBus.get}

  val prepareToSendACK: Flow[MessageAndAck, APIWebSocket, NotUsed] = Flow[MessageAndAck]
    .collect{
      case x: MessageAndAck if x.messToBus.isDefined => x.reply
      case x: MessageAndAck if x.messToBus.isEmpty => APIWebSocket.SPError("Something wrong with the akka stream")
    }

  val parsingError: Flow[Try[APIWebSocket], APIWebSocket.SPError, NotUsed] = Flow[Try[APIWebSocket]]
    .collect{case x: Failure[APIWebSocket] => APIWebSocket.SPError(x.exception.getMessage)}

  val parseMessagesFromBus: Flow[Any, APIWebSocket, NotUsed] = Flow[Any]
    .collect{case str: String => str}
    .map(str => Try{upickle.default.read[UPickleMessage](str)})
    .collect{case x: Success[UPickleMessage] => x.value}
    .filter(messFilter)
    .map(mess => APIWebSocket.PublishMessage(mess, "FROMBUS"))

  val convertAPIToString = Flow[APIWebSocket]
    .map(x => sp.messages.APIParser.write(x))

  val printFlow = Flow[Any].filter(x => {println(s"WE GOT FROM THE BUS: $x"); true})


}



