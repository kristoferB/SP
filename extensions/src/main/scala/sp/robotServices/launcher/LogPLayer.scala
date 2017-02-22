package sp.robotServices.launcher

import sp.system._
import akka.actor._
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import com.codemettle.reactivemq.ReActiveMQExtension
import com.codemettle.reactivemq.ReActiveMQMessages._
import com.codemettle.reactivemq.model.{AMQMessage, Topic}
import sp.system.messages._
import sp.domain._
import sp.robotServices.core.Helpers._
import sp.domain.Logic._
import sp.system.SPActorSystem._
import sp.robotServices.core._
import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization
import org.json4s.native.Serialization._
import sp.robotServices.core.Domain.{ModulesReadEvent, WorkCell}

import scala.io.Source

/**
  * Created by ashfaqf on 1/15/17.
  */
case class LogPlayerSetup(filePath: String, command: String, freq: Int)

object LogPlayer extends SPService {
  implicit val formats = DefaultFormats
  val specification = SPAttributes(
    "service" -> SPAttributes(
      "description" -> "Replays logs from a given log file at a given frequency in millis"
    ),
    "setup" -> SPAttributes(
      "filePath" -> KeyDefinition("String",List(),Some("/home/ashfaqf/Projects/Lisa files/from_volvo/logs/log-13_12_35")),
      "command" -> KeyDefinition("String", List("LoadFile", "PlayLog","ListEvents", "playEvent","setupBus", "sendRobotModules", "printOps", "printRels"), Some("setupBus")),
      "freq" -> KeyDefinition("Int",List(),Some(10))
      // "AMQBusIP" -> KeyDefinition("String", List(), Some(settings.activeMQ)),
      // "AMQBusUsername" -> KeyDefinition("String",List(),Some("admin")),
      // "AMQBusPass" -> KeyDefinition("String",List(),Some("admin")),
      // "AMQBusTopic" -> KeyDefinition("String",List(),Some(settings.activeMQTopic))
    )
  )

  val transformTuple = (
    TransformValue("setup", _.getAs[LogPlayerSetup]("setup"))
    )

  val transformation = transformToList(transformTuple.productIterator.toList)

  // important to incl ServiceLauncher if you want unique actors per request
  def props = Props(classOf[LogPlayer])

  // Alla får även "core" -> ServiceHandlerAttributes

  //  case class ServiceHandlerAttributes(model: Option[ID],
  //                                      responseToModel: Boolean,
  //                                      onlyResponse: Boolean,
  //                                      includeIDAbles: List[ID])

}
class LogPlayer extends Actor with ServiceSupport{
  implicit val formats = DefaultFormats
  var theBus: Option[ActorRef] = None
  var jsonFile: JValue = JNothing
  var wcellfile: List[JValue] = List.empty
  var rcdProgs: List[JValue] = List.empty
  var evts: List[JValue] = List.empty
  var fileLoaded= false
  var wcellmap: Map[String,WorkCell] =Map.empty
  var robotIdToModues: Map[String,ModulesReadEvent] = Map.empty
  var playLog :Boolean = true
  def receive = {

    case r@Request(service, attr, ids, reqID) =>
      val replyTo = sender()
      implicit val rnr = RequestNReply(r, replyTo)
      val setup = transform(LogPlayer.transformTuple)

    
      setup.command match {
        case "setupBus" =>

          ReActiveMQExtension(context.system).manager ! GetConnection(s"nio://${settings.activeMQ}:${settings.activeMQPort}")

        case "LoadFile" =>
         jsonFile = loadFile(setup.filePath)
          if(jsonFile != JNothing) {
            evts = jsonFile.children.filterNot(_.has("readValue")).filterNot(_.has("workCells"))
            rcdProgs = jsonFile.children.filter(_.has("readValue"))
            robotModules(rcdProgs)
            wcellfile = jsonFile.children.filterNot(_.has("readValue")).filter(_.has("workCells"))
            fileLoaded = true
            wcellfile.foreach{cell =>
              val wcell = (cell \ "workCells").extract[List[WorkCell]]
              wcell.foreach { w =>
                if (!wcellmap.contains(w.id))
                  wcellmap += (w.id -> w)
              }
            }
          }

        case "sendRobotModules" =>
          rcdProgs.foreach{x =>
            sendToBusWithTopic(settings.activeMQTopic,x.toJson)
          Thread.sleep(setup.freq)}

        case "printOps" =>   import akka.cluster.pubsub._
          val mediator = DistributedPubSub(context.system).mediator
          mediator ! Publish("robotServices", "printOps")

        case "printRels" =>   import akka.cluster.pubsub._
          val mediator = DistributedPubSub(context.system).mediator
          mediator ! Publish("robotServices", "printRels")


        case "playEvent" =>
          playEvent()

        case "PlayLog" =>
          while(!evts.isEmpty && playLog) {
            playEvent()
            Thread.sleep(setup.freq)
          }
          println("Done playing log")

        case "ListEvents" =>
          if (fileLoaded) {
            println("Printing first 5 events")
            evts.slice(0,if(evts.length>5)5 else evts.length).foreach(x => println(x))

          }
          else
            println("file not loaded")
      }



      // include this if you want to send progress messages. Send attributes to it during calculations
      val progress = context.actorOf(progressHandler)

      replyTo ! Response(List(), SPAttributes("info" -> "Player started"), service, reqID)

    case (r: Response, reply: ActorRef) =>
      reply ! r
    case ConnectionEstablished(request, c) => {
      println("connected:" + request)
      c ! ConsumeFromTopic(settings.activeMQRequestTopic)
      theBus = Some(c)
    }
    case ConnectionFailed(request, reason) => {
      println("failed:" + reason)
    }
    case mess@AMQMessage(body, prop, headers) => {
      val json: JValue = parse(body.toString)
      //println("got"+ json)
      implicit val formats = Serialization.formats(NoTypeHints)
      if(json.has("event")) {
        println("got event" + (json \ "event"))
        //println((json \ "event").extract[String] == "workCellListOpened" )
        if((json \ "event").extract[String] == "newRobotEncountered") {
         // println("Sending robotModule")
          sendToBusWithTopic(settings.activeMQTopic, write(robotIdToModues((json \ "robotId").extract[String])))
        }
        if ((json \ "event").extract[String] == "newWorkCellEncountered"  || (json \ "event").extract[String] == "workCellListOpened" ) {
         // println("Sending wcell")
          sendToBusWithTopic(settings.activeMQTopic, write("workCells" -> wcellmap.values.toList))
        }
      }

    }
    case x =>
      sender() ! SPError("What do you whant me to do? " + x)
  }

  def sendToBusWithTopic(topic: String, json: String) = {
    theBus.foreach{bus => bus ! SendMessage(Topic(topic), AMQMessage(json))}
  }

  def robotModules(json:List[JValue]){
    val modules = json.map(_.extract[ModulesReadEvent])
    modules.foreach(x => robotIdToModues += (x.robotId -> x))
    
  }
  def loadFile(path: String): JValue ={
    println(s"loading ${path}")
    import java.nio.file.{Paths, Files}
    if (!Files.exists(Paths.get(path))) {
      sender() ! SPError("File does not exist")
      JNothing
    }
    else {
      val source: String = Source.fromFile(path).getLines.mkString
      val parsedFile = parse(source)
      println("Finished parsing file")

      parsedFile
    }
  }

  def playEvent(): Unit ={
    sendToBusWithTopic(settings.activeMQTopic,evts.head.toJson)
    evts = evts.tail

  }
}
