package sp.server

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.{ActorMaterializer, Materializer, SourceShape}
import akka.pattern.ask

import scala.concurrent.duration._
import akka.util.Timeout
import sp.system.PubActor

import scala.reflect.ClassTag
import akka.stream.scaladsl._
import akka.http.scaladsl.server.Directives
import akka.stream.scaladsl.Flow
import akka.cluster.pubsub._
import DistributedPubSubMediator._
import akka.http.scaladsl.model.ws.TextMessage.Strict

import scala.util._





object APITEST {
  sealed trait API
  case class Test1(p1: String, p2: String) extends API
  case class Test2(p1: Int, p2: Int) extends API
  case class Test3(p1: Double, p2: Tom) extends API

  sealed trait SUB
  case class Tom(str: String) extends SUB

  lazy val apiClasses: List[Class[_]] =   sp.macros.MacroMagic.values[API]
  lazy val apiJson: List[String] = sp.macros.MacroMagic.info[API, SUB]

}



object APIWebSocket {
  sealed trait API
  case class Subscribe(topic: String) extends API
  case class Unsubscribe(topic: String) extends API

  //case class filter(reqID: String) extends API // add more filters here in the future

  case class PublishMessage(mess: UPickleMessage, topic: String = "services") extends API

  // Move to general API in SP Domain
  case class SPACK(message: String) extends API
  case class SPERROR(message: String, attr: Option[upickle.Js.Value] = None) extends API

}


/**
 * Used by the SP launcher file
 * Created by Kristofer on 2014-06-19.
 */
object LaunchGUI  {//extends MySslConfiguration {

  def launch = {
    implicit val system = sp.system.SPActorSystem.system
    implicit val materializer = ActorMaterializer()

    val widgets = system.actorOf(PubActor.props("widgets"))

    val interface = system.settings.config getString "sp.interface"
    val port = system.settings.config getInt "sp.port"
    val webFolder: String = sp.system.SPActorSystem.settings.webFolder
    val srcFolder: String = if(sp.system.SPActorSystem.settings.devMode)
      sp.system.SPActorSystem.settings.devFolder else sp.system.SPActorSystem.settings.buildFolder


    val mediator = DistributedPubSub(system).mediator

    //import upickle.default._

    def api =
      pathPrefix("test"){
        post{
          entity(as[String]){t =>
            val res = APIParser.read[APITEST.API](t)
            complete("JA, det funderar: "+ t)
          }
        } ~
        get {
          val t = APITEST.Test1("hej", "då")
          complete(HttpResponse(entity = HttpEntity(ContentType(MediaTypes.`application/json`), APIParser.write(t))))
        }
      } ~
      pathPrefix("operation"){
        get {
          import sp.domain._
          import sp.domain.Logic._
          val t = Operation("hej", List(), SPAttributes("test"->APITEST.Test1("hej", "kalle")))
          val json = SPValue(t).toJson
          complete(HttpResponse(entity = HttpEntity(ContentType(MediaTypes.`application/json`), json)))
        }
      } ~
        pathPrefix("socket"){
          get{
              val h = new WebsocketHandler(mediator, "answers")
               handleWebSocketMessages(h.webSocketHandler)

          }
      } ~
      pathPrefix("api") {
        get {
          pathEndOrSingleSlash {
            complete("yes")
          } ~
            path("widget" / Remaining) { file =>
              implicit val timeout: Timeout = 2.seconds
              println("ho ho widget: " + file)

              //getFromFile(s"./gui/sp-example-widget/$file")

              val res = (widgets ? file).mapTo[java.io.File]
              //complete(res)


              import akka.http.scaladsl.model.HttpEntity
              import akka.http.scaladsl.model.MediaTypes.`application/javascript`
              onSuccess(res) { r =>
                getFromFile(r)
              }


            }
        }
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
}





// test websockets

import akka.actor._
import akka.stream._
import akka.stream.scaladsl._
import akka.http.scaladsl.model.ws.{ Message, TextMessage }


// https://markatta.com/codemonkey/blog/2016/04/18/chat-with-akka-http-websockets/
// https://github.com/jrudolph/akka-http-scala-js-websocket-chat

case class UPickleMessage(header: upickle.Js.Value, body: upickle.Js.Value)

class WebsocketHandler(mediator: ActorRef, initialTopic: String = "services") {
  var topics: Set[String] = Set()
  var messFilter: UPickleMessage => Boolean = (x: UPickleMessage) => true
  var myRef: Option[ActorRef] = None

  case class MessageAndAck(messToBus: Option[Any], reply: APIWebSocket.API)

  lazy val webSocketHandler: Flow[Message, Message, NotUsed]  = Flow.fromGraph(GraphDSL.create() { implicit b: GraphDSL.Builder[NotUsed] =>
    import GraphDSL.Implicits._

    val in = b.add(transformMessages)
    val out = b.add(sendToWebSocket)

    val parseResultBC = b.add(Broadcast[Try[APIWebSocket.API]](2))
    val messageBC = b.add(Broadcast[MessageAndAck](2))
    val merge = b.add(Merge[APIWebSocket.API](3))

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
      mediator ! Subscribe(initialTopic, ref)
    }

  val sendToBus: Sink[Any, NotUsed] = Sink.actorRef[Any](mediator, "Killing me")
  val sendToWebSocket: Flow[String, Strict, NotUsed] =  Flow[String].map(str => TextMessage(str))


  val transformMessages: Flow[Message, Try[APIWebSocket.API], NotUsed] = Flow[Message]
    .collect{ case TextMessage.Strict(text) => text}
    .map{str => Try{APIParser.read[APIWebSocket.API](str)}}

  val matchWebSocketMessages: Flow[Try[APIWebSocket.API], MessageAndAck, NotUsed] = Flow[Try[APIWebSocket.API]]
      .collect{case x: Success[APIWebSocket.API] => x.value}
      .collect{
        case APIWebSocket.Subscribe(topic) if ! topics.contains(topic) =>
          topics = topics + topic
          MessageAndAck(myRef.map(Subscribe(topic, _)), APIWebSocket.SPACK(s"Subscribing to topic $topic"))
        case APIWebSocket.Unsubscribe(topic) if topics.contains(topic) =>
          topics = topics - topic
          MessageAndAck(myRef.map(Unsubscribe(topic, _)), APIWebSocket.SPACK(s"Unsubscribing from topic $topic"))
        case APIWebSocket.PublishMessage(mess, topic) =>
          topics = topics - topic
          MessageAndAck(Some(Publish(topic, APIParser.write(mess))), APIWebSocket.SPACK(s"Message sent to topic $topic"))
      }

  val prepareToSend: Flow[MessageAndAck, Any, NotUsed] = Flow[MessageAndAck]
    .collect{case x: MessageAndAck if x.messToBus.isDefined => x.messToBus.get}

  val prepareToSendACK: Flow[MessageAndAck, APIWebSocket.API, NotUsed] = Flow[MessageAndAck]
    .collect{
      case x: MessageAndAck if x.messToBus.isDefined => x.reply
      case x: MessageAndAck if x.messToBus.isEmpty => APIWebSocket.SPERROR("Something wrong with the akka stream")
    }

  val parsingError: Flow[Try[APIWebSocket.API], APIWebSocket.SPERROR, NotUsed] = Flow[Try[APIWebSocket.API]]
    .collect{case x: Failure[APIWebSocket.API] => APIWebSocket.SPERROR(x.exception.getMessage)}

  val parseMessagesFromBus: Flow[Any, APIWebSocket.API, NotUsed] = Flow[Any]
    .collect{case str: String => str}
    .map(str => Try{upickle.default.read[UPickleMessage](str)})
    .collect{case x: Success[UPickleMessage] => x.value}
    .filter(messFilter)
    .map(mess => APIWebSocket.PublishMessage(mess, "FROMBUS"))

  val convertAPIToString = Flow[APIWebSocket.API]
    .map(x => APIParser.write(x))

  val printFlow = Flow[Any].filter(x => {println(s"WE GOT FROM THE BUS: $x"); true})


}




// move to shaerd folder between jvm and js
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