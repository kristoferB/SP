package sp.robotServices.launcher





import akka.actor.{ActorSystem, Props}
import sp.robotServices._
import sp.system.{SPService, ServiceSupport}
import sp.system.messages.{KeyDefinition, TransformValue}
import akka.actor._
import sp.system.messages._
import sp.domain._
import sp.domain.Logic._

import scala.collection.mutable.ListBuffer



/**
  * Created by Henrik on 2016-05-02.
  */



case class LaunchRobotRuntimeSetup(command:String) //, AMQBusIP: String, AMQBusUsername: String, AMQBusPass:String, AMQBusTopic:String)

object LaunchRobotRuntimeService extends SPService {

  val specification = SPAttributes(
    "service" -> SPAttributes(
      "description" -> "starts the robot runtime for processing and aggregation of data"
    ),
    "setup" -> SPAttributes(
      "command" -> KeyDefinition("String",List("StartRuntime","StopRuntime"),Some("StartRuntime"))
     // "AMQBusIP" -> KeyDefinition("String", List(), Some(settings.activeMQ)),
     // "AMQBusUsername" -> KeyDefinition("String",List(),Some("admin")),
     // "AMQBusPass" -> KeyDefinition("String",List(),Some("admin")),
     // "AMQBusTopic" -> KeyDefinition("String",List(),Some(settings.activeMQTopic))
    )
  )

  val transformTuple = (
    TransformValue("setup", _.getAs[LaunchRobotRuntimeSetup]("setup"))
    )

  val transformation = transformToList(transformTuple.productIterator.toList)

  // important to incl ServiceLauncher if you want unique actors per request
  def props = Props(classOf[LaunchRobotRuntimeService])

  // Alla får även "core" -> ServiceHandlerAttributes

  //  case class ServiceHandlerAttributes(model: Option[ID],
  //                                      responseToModel: Boolean,
  //                                      onlyResponse: Boolean,
  //                                      includeIDAbles: List[ID])

}



  class LaunchRobotRuntimeService extends Actor with ServiceSupport{

  println("Launch robots......")
  var actors = List.empty[ActorRef]
  actors = startServices()

  def receive = {
    case r@Request(service, attr, ids, reqID) =>


      println("Recieved request")
      // Always include the following lines. Are used by the helper functions
      val replyTo = sender()
      implicit val rnr = RequestNReply(r, replyTo)
      val progress = context.actorOf(progressHandler)


      val setup = transform(LaunchRobotRuntimeService.transformTuple)

      setup.command match {
        case "StartRuntime" => println("starting  robot Services")
          actors = startServices()
          println("actors started:")
          actors.foreach(a => println(a))
        case "StopRuntime" => println("Stopping  robot Services")
          actors.foreach(a => println(a))
          stopServices(actors)
      }



      replyTo ! Response(List(), SPAttributes("info"->"Runtime Started"), service, reqID)


    case (r: Response, reply: ActorRef) =>
      reply ! r
    case x =>
      sender() ! SPError("What do you whant me to do? " + x)
      //self ! PoisonPill
  }

  def stopServices(actors: List[ActorRef]) = {

    actors.foreach(a=> a ! PoisonPill)


  }

    override def postStop(): Unit = {
      stopServices(actors)
      super.postStop()
    }
  def startServices() = {
    val actors  = new ListBuffer[ActorRef]


    val relationFinder= context.actorOf(RelationFinder.props, "RelationFinder")

    val operationAggregator = context.actorOf(OperationAggregator.props, "operationAggregator")

    actors.append(fillWithInstructionActor)
    fillWithInstructionActor ! "connect"

    val routineChangeActor = context.actorOf(RoutineExtractor.props,"routineChange")
    routineChangeActor ! "connect"
    actors.append(routineChangeActor)

    val cycleEventActor = context.actorOf(CycleChange.props,"cycleChange")
    cycleEventActor ! "connect"
    actors.append(cycleEventActor)

    //val cycleAggregatorActor = context.actorOf(CycleAggregator.props,"cycleAggregator")
    //cycleAggregatorActor ! "connect"
    //actors.append(cycleAggregatorActor)

    val saveToEs = context.actorOf(SaveToES.props,"saveToEs")
    saveToEs ! "connect"
    actors.append(saveToEs)

    actors.toList


    // log.info("Started")

    /*// Remove comment to test the system using the provided tester actor
    val testerActor = system.actorOf(robotServices.launcher.testMessageSender.props)
    testerActor ! "connect"*/
  }

  def sendResp(r: Response, progress: ActorRef)(implicit rnr: RequestNReply) = {
    rnr.reply ! r
    progress ! PoisonPill
    self ! PoisonPill
  }
}



/*
object launcher extends SPService{

  implicit var system: ActorSystem = ActorSystem()
  var log: LoggingAdapter = Logging(system, "launcher")

  def main(args: Array[String]): Unit = {
    startServices()
    scala.io.StdIn.readLine("Press any key to exit.\n")
    stop(Array.empty)
  }

  def startServices() = {
    log.info("Starting")
    log.info("Base dir: " + Config.jarDir)
    log.info("External config file: " + Config.extConfFile)
    log.info("extConfFile.exists && !extConfFile.isDirectory: " + (Config.extConfFile.exists && !Config.extConfFile.isDirectory))

    implicit val executor = system.dispatcher

    val fillWithInstructionActor = system.actorOf(InstructionFiller.props)
    fillWithInstructionActor ! "connect"

    val routineChangeActor = system.actorOf(RoutineExtractor.props)
    routineChangeActor ! "connect"

    val cycleEventActor = system.actorOf(CycleChange.props)
    cycleEventActor ! "connect"

    val cycleAggregatorActor = system.actorOf(CycleAggregator.props)
    cycleAggregatorActor ! "connect"

    log.info("Started")

    /*// Remove comment to test the system using the provided tester actor
    val testerActor = system.actorOf(robotServices.launcher.testMessageSender.props)
    testerActor ! "connect"*/
  }

  var stopped = false

  def start(args: Array[String]): Unit = {
    startServices()
    while(!stopped)
      Thread.sleep(1000)
  }

  def stop(args: Array[String]): Unit = {
    log.info("Stopping the services. This may take a while.")
    stopped = true
    system.terminate()
  }

  override val specification: SPAttributes = _
  override val transformation: Any = _
}

*/
