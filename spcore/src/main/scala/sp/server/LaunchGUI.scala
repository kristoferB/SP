package sp.server

import akka.{NotUsed, Done}
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.{ActorMaterializer, Materializer, SourceShape}

import scala.concurrent.duration._
import scala.concurrent._
import akka.util.Timeout

import scala.reflect.ClassTag
import akka.stream.scaladsl._
import akka.http.scaladsl.server.{Directives, MethodRejection, SchemeRejection}
import akka.stream.scaladsl.Flow
import akka.cluster.pubsub._
import DistributedPubSubMediator._
import akka.http.scaladsl.model.ws.TextMessage.Strict

import scala.util._


import sp.domain._
import sp.domain.Logic._

object APIWebSocket {
  sealed trait API
  case class PublishMessage(mess: SPMessage, topic: String = "services") extends API
  case class FilterHeader(keyValues: Map[String, Set[SPValue]]) extends API
  case class FilterBody(keyValues: Map[String, Set[SPValue]]) extends API
  // removing filters with keys in the set keys. If it is empty, all keys are removed
  case class ClearFilters(keys: Set[String] = Set()) extends API

  object API {
    implicit val apiFormat = deriveFormatISA[API]
  }

}
import sp.server.{APIWebSocket => api}

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
  implicit val dispatcher = system.dispatcher
  val mediator = DistributedPubSub(system).mediator
  val log = org.slf4j.LoggerFactory.getLogger(getClass.getName)

  def launch = {
      val webFolder: String = system.settings.config getString "sp.webFolder"
      val devFolder: String = system.settings.config getString "sp.devFolder"
      val buildFolder: String = system.settings.config getString "sp.buildFolder"
      val devMode: Boolean = system.settings.config getBoolean "sp.devMode"
      val interface = system.settings.config getString "sp.interface"
    val port = system.settings.config getInt "sp.port"
    val srcFolder: String = if(devMode)
      devFolder else buildFolder




    def api =
        path("socket" / Segment / JavaUUID){ (topic, id) =>
          get{
            extractUpgradeToWebSocket{ upg =>
              val h = new WebsocketHandler(mediator, topic, id)
              complete(upg.handleMessagesWithSinkSource(h.fromFrontEnd, h.toFrontEnd))
            }
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



    import java.io.InputStream
    import java.security.{ SecureRandom, KeyStore }
    import javax.net.ssl.{ SSLContext, TrustManagerFactory, KeyManagerFactory }

    import akka.actor.ActorSystem
    import akka.http.scaladsl.server.{ Route, Directives }
    import akka.http.scaladsl.{ ConnectionContext, HttpsConnectionContext, Http }
    import akka.stream.ActorMaterializer
    import com.typesafe.sslconfig.akka.AkkaSSLConfig


    val serverContext: ConnectionContext = {
      val password = "abcdef".toCharArray
      val context = SSLContext.getInstance("TLS")
      val ks = KeyStore.getInstance("PKCS12")
      ks.load(getClass.getClassLoader.getResourceAsStream("keys/server.p12"), password)
      val keyManagerFactory = KeyManagerFactory.getInstance("SunX509")
      keyManagerFactory.init(ks, password)
      context.init(keyManagerFactory.getKeyManagers, null, new SecureRandom)
      // start up the web server
      ConnectionContext.https(context)
    }


    //Http().setDefaultServerHttpContext(serverContext)
    val bindingFuture = Http().bindAndHandle(route, interface, port) //, connectionContext = serverContext)

    log.info(s"Server started. System: ${system.name}, Adress: $interface:$port")

    bindingFuture

//  scala.io.StdIn.readLine("Press ENTER to exit application.\n") match {
//    case x => system.terminate()
//  }



  }



  def postMessage(url: String, shouldAsk: Boolean) = {
    path(url / Segments) { cmd =>
      if (cmd.isEmpty)
        reject(SchemeRejection("no topic"))

      log.debug("postMessage event: "+ url +" - "+cmd)

      val topic = cmd.head
      val service = cmd.tail.headOption.getOrElse("")

      post { entity(as[String]){data =>
        val (mess, h) = fixMess(data, topic, service)
        if (shouldAsk){
          val answer = mediator.ask(Publish(topic, mess)).mapTo[String]
          completeOrRecoverWith(answer){ extr =>
            val re = SPMessage.make(h, APISP.SPError("No service answered the request"))
            complete(re.toJson)
          }
        } else {
          mediator ! Publish(topic, mess)
          val re = SPMessage.make(h, APISP.SPACK()) // SPAttributes("result" ->"Message sent")
          complete(re.toJson)
        }
      }}
    }
  }


  def fixMess(mess: String, topic: String, service: String) = {
    val uP = SPMessage.fromJson(mess)

    val toSend = uP.map{ m =>
      val updH = if (service.nonEmpty) {
        m.header + ("service" -> SPValue(service))
      } else m.header
      (m.copy(header = updH).toJson, updH)
    }
    toSend.getOrElse(mess, SPAttributes.empty)
  }

}





// test websockets

import akka.actor._
import akka.stream._
import akka.stream.scaladsl._
import akka.http.scaladsl.model.ws.{ Message, TextMessage }


// https://markatta.com/codemonkey/blog/2016/04/18/chat-with-akka-http-websockets/
// https://github.com/jrudolph/akka-http-scala-js-websocket-chat





class WebsocketHandler(mediator: ActorRef, topic: String, clientID: java.util.UUID) {
  val log = org.slf4j.LoggerFactory.getLogger(getClass.getName)
  case class Filters(h: Map[String, Set[SPValue]], b: Map[String, Set[SPValue]])
  var filter = Filters(Map(), Map())

  lazy val fromFrontEnd: Sink[Message, NotUsed]  = Sink.fromGraph(GraphDSL.create() { implicit b: GraphDSL.Builder[NotUsed] =>
    import GraphDSL.Implicits._
    val toAPI = b.add(fromWebSocketToAPI)
    val split = b.add(Broadcast[Try[APIWebSocket.API]](2))
    val sendToBus: Sink[Publish, NotUsed] = Sink.actorRef[Publish](mediator, "Killing me")

    toAPI ~> split ~> toBePublished ~> sendToBus
    split ~> updFilters

    SinkShape(toAPI.in)
  })

  val fromWebSocketToAPI: Flow[Message, Try[APIWebSocket.API], NotUsed] = Flow[Message]
    .collect{ case TextMessage.Strict(text) => log.debug(s"Websocket got: $text"); text}
    .map{str =>
      SPAttributes.fromJson(str).flatMap(_.to[APIWebSocket.API])
    }

  val toBePublished: Flow[Try[APIWebSocket.API], Publish, NotUsed] = Flow[Try[APIWebSocket.API]]
    .collect{case x: Success[APIWebSocket.API] => x.value}
    .collect{
      case APIWebSocket.PublishMessage(mess, t) =>
        Publish(t, mess.toJson)
    }


  val updFilters: Sink[Try[APIWebSocket.API], Future[Done]] = Sink.foreach[Try[APIWebSocket.API]] { x =>
    x.toOption.collect{
      case APIWebSocket.FilterBody(f) =>
        val updM = f
        filter = filter.copy(b = filter.b ++ updM)
      case APIWebSocket.FilterHeader(f) =>
        val updM = f
        filter = filter.copy(h = filter.h ++ updM)
      case APIWebSocket.ClearFilters(keys) =>
        if (keys.isEmpty)
          filter = Filters(Map(), Map())
        else {
          val toRemove = (kv: (String, Set[SPValue])) => !keys.contains(kv._1)
          val newHF = filter.h.filter(toRemove)
          val newBF = filter.b.filter(toRemove)
          filter = Filters(newHF, newBF)
        }
    }
  }




  lazy val toFrontEnd: Source[Message, NotUsed]  = Source.fromGraph(GraphDSL.create() { implicit b: GraphDSL.Builder[NotUsed] =>
    import GraphDSL.Implicits._
    val in = b.add(receiveFromBus)
    val out = b.add(injectKeepAlive)

    in ~> parseMessagesFromBus ~> convertToWebSockMess ~> out

    SourceShape(out.out)
  })


  val receiveFromBus: Source[Any, Unit] = Source.actorRef[Any](1000, OverflowStrategy.dropNew)
    .mapMaterializedValue { ref =>
      mediator ! Subscribe(topic, ref)
      mediator ! Subscribe(clientID.toString, ref)
    }

  val parseMessagesFromBus: Flow[Any, String, NotUsed] = Flow[Any]
    //.map{x => println(s"ws got before conv: $x"); x}
    .collect{case str: String => str}
    .map(str => SPMessage.fromJson(str))
    .collect{case x: Some[SPMessage] => x.value}
    .filter(messFilter)
    .map(mess => mess.toJson)
    //.map{x => println(s"ws will forward: $x"); x}


  def messFilter(mess: SPMessage) = {
    (filter.b.isEmpty && filter.h.isEmpty) ||
      (filterPickle(mess.header, filter.h) && filterPickle(mess.body, filter.b))
  }

  def filterPickle(p: SPAttributes, f: Map[String, Set[SPValue]]) = {
    Try{
      f.forall(x => !p.keys.contains(x._1) || p.keys.contains(x._1) && x._2.contains(p.value(x._1)))
    }.getOrElse(true)
  }

  val convertToWebSockMess: Flow[String, Strict, NotUsed] =  Flow[String].map(str => TextMessage(str))

  val injectKeepAlive: Flow[Strict, Strict, NotUsed] =
    Flow[Strict].keepAlive(2.second, () => TextMessage("keep-alive"))


//  lazy val webSocketHandler: Flow[Message, Message, NotUsed]  = Flow.fromGraph(GraphDSL.create() { implicit b: GraphDSL.Builder[NotUsed] =>
//    import GraphDSL.Implicits._
//
//    val in = b.add(transformMessages)
//    val out = b.add(sendToWebSocket)
//
//    val parseResultBC = b.add(Broadcast[Try[api.APIWebSocket2]](2))
//    val messageBC = b.add(Broadcast[MessageAndAck](2))
//    val merge = b.add(Merge[APIWebSocket2](3))
//
//    val sendReceive = Flow.fromSinkAndSource(sendToBus, receiveFromBus)
//
//
//
//    in ~> parseResultBC ~> matchWebSocketMessages ~> messageBC ~> prepareToSend ~> sendReceive ~> parseMessagesFromBus ~> merge
//    parseResultBC ~> parsingError ~> merge
//    messageBC ~> prepareToSendACK ~> merge
//    merge ~> convertAPIToString ~> out
//
//    FlowShape(in.in, out.out)
//  })
//
//
//  val receiveFromBus: Source[Any, Unit] = Source.actorRef[Any](1000, OverflowStrategy.fail)
//    .mapMaterializedValue { ref =>
//      myRef = Some(ref)
//      mediator ! Subscribe(topic, ref)
//    }
//
//
//    val sendToBus: Sink[Any, NotUsed] = Sink.actorRef[Any](mediator, "Killing me")
//    val sendToWebSocket: Flow[String, Strict, NotUsed] =  Flow[String].map(str => TextMessage(str))
//
//
//  val transformMessages: Flow[Message, Try[APIWebSocket2], NotUsed] = Flow[Message]
//    .collect{ case TextMessage.Strict(text) => println(s"Websocket got: $text"); text}
//    .map{str => Try{read[APIWebSocket2](str)}}
//
//  val matchWebSocketMessages: Flow[Try[APIWebSocket2], MessageAndAck, NotUsed] = Flow[Try[APIWebSocket2]]
//      .collect{case x: Success[APIWebSocket2] => x.value}
//      .collect{
//        case PublishMessage(mess, t) =>
//          MessageAndAck(Some(Publish(t, toJson(mess))), APIWebSocket.SPACK(s"Message sent to topic $t"))
//      }
//
//  val prepareToSend: Flow[MessageAndAck, Any, NotUsed] = Flow[MessageAndAck]
//    .collect{case x: MessageAndAck if x.messToBus.isDefined => x.messToBus.get}
//
//  val prepareToSendACK: Flow[MessageAndAck, APIWebSocket, NotUsed] = Flow[MessageAndAck]
//    .collect{
//      case x: MessageAndAck if x.messToBus.isDefined => x.reply
//      case x: MessageAndAck if x.messToBus.isEmpty => APIWebSocket.SPError("Something wrong with the akka stream")
//    }
//
//  val parsingError: Flow[Try[APIWebSocket], APIWebSocket.SPError, NotUsed] = Flow[Try[APIWebSocket]]
//    .collect{case x: Failure[APIWebSocket] => APIWebSocket.SPError(x.exception.getMessage)}
//
//  val parseMessagesFromBus: Flow[Any, APIWebSocket, NotUsed] = Flow[Any]
//    .collect{case str: String => str}
//    .map(str => Try{upickle.default.read[UPickleMessage](str)})
//    .collect{case x: Success[UPickleMessage] => x.value}
//    .filter(messFilter)
//    .map(mess => APIWebSocket.PublishMessage(mess, "FROMBUS"))
//
//  val convertAPIToString = Flow[APIWebSocket]
//    .map(x => toJson(x))
//
//  val printFlow = Flow[Any].filter(x => {println(s"WE GOT FROM THE BUS: $x"); true})


}
