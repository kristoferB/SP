package sp.robotServices

import akka.actor.Props
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import com.codemettle.reactivemq.ReActiveMQExtension
import com.codemettle.reactivemq.ReActiveMQMessages.GetConnection
import com.github.nscala_time.time.Imports.DateTime
import org.json4s._
import sp.robotServices.core.Domain._
import sp.robotServices.core.Helpers._
import sp.robotServices.core._
import org.json4s.native.Serialization.write
import sp.system.SPActorSystem.settings
import wabisabi.Client

import com.github.tototoshi.csv._

/**
  * Created by ashfaqf on 1/16/17.
  */
class SaveToES extends ServiceBase{

  type RobotId = String
  type WorkCellId = String
  type ActivityType = String
  type ActivityEvents = List[ActivityEvent]
  type Activities = List[Activity]

    import context.dispatcher

  import akka.cluster.pubsub._
  val mediator = DistributedPubSub(context.system).mediator


  var elasticClient: Option[Client] = None
  final val index_activity = "robot-activity"
  final val index_activityEvents = "robot-activity-events"
  final val index_cycleEvents = "robot-cycle-events"

  val csvFile = CSVWriter.open("testFiles/events.csv")

  val indexes = List(index_activity,index_activityEvents,index_cycleEvents)

  var robotActivityMap : Map[RobotId,String] = Map.empty
  var robotIdToCurrentPos : Map[RobotId,Boolean] = Map.empty
  var robotIdToCurrentRobotCycle : Map[RobotId,String] = Map.empty

  override def handleOtherMessages = {
    case "connect" =>
      import sp.system.SPActorSystem._
      val elasticIP = settings.elasticSearchIP //Config.config.getString("elastic.ip")
      val elasticPort = settings.elasticSearchPort //Config.config.getString("elastic.port")
      ReActiveMQExtension(context.system).manager ! GetConnection(s"nio://${settings.activeMQ}:${settings.activeMQPort}")
      elasticClient = Some(new Client(s"http://$elasticIP:$elasticPort"))
      elasticClient.foreach(client => indexes.foreach(index => client.createIndex(index)))
  }

  def allRobotsAtHome():Boolean ={
    robotIdToCurrentPos.values.foreach(print(_))
    robotIdToCurrentPos.values.forall(_ == true)
  }
  def updateActivityMap(evt:ActivityEvent):Unit ={
    robotActivityMap += (evt.robotId -> evt.name)
    
  }
  def allRobotsRunMain():Boolean = {
    robotActivityMap.values.foreach(print(_))
    robotActivityMap.values.forall(_.contains("main"))

  }
  val homePosSignals = settings.homePosSignals
  var cycleId = uuid
  override def handleAmqMessage(json: JValue): Unit = {
    if (json.has("activityId")) {
      val event: ActivityEvent = json.extract[ActivityEvent]

      updateActivityMap(event)

      if (event.isStart && allRobotsRunMain() && allRobotsAtHome()/*robotIdToCurrentPos.getOrElse(event.robotId,false)*/ ){
      //robotIdToCurrentRobotCycle += (event.robotId -> uuid) // (robotIdToCurrentRobotCycle.getOrElse(event.robotId,-1) + 1) )
      cycleId = uuid
      }
      def robCylId = robotIdToCurrentRobotCycle.contains(event.robotId) match {
        case true => robotIdToCurrentRobotCycle(event.robotId)
        case false => robotIdToCurrentRobotCycle += (event.robotId -> uuid)
          robotIdToCurrentRobotCycle(event.robotId)

      }
      val activityToSend =write(ActivityEventWithRobotCycle(event.activityId, cycleId/*robCylId*//*robotIdToCurrentRobotCycle(event.robotId)*/,event.isStart,event.name,event.robotId,event.time,event.`type`,event.workCellId))
      sendToES(activityToSend,uuid,index_activity)
      mediator ! Publish("robotServices", activityToSend)
      writeToCSV(List(event.activityId,cycleId,event.isStart,event.name,event.robotId,event.time,event.`type`,event.workCellId,robotIdToCurrentPos(event.robotId)))
 
    }
    if (json.has("newSignalState") && homePosSignals.contains((json \ "address" \ "signal").extract[String])){
      val event = json.extract[IncomingCycleEvent]
      robotIdToCurrentPos += (event.robotId -> (event.newSignalState.value > 0))
      sendToES(write(event),uuid,index_cycleEvents)
    }

  }
  override def postStop()={
    csvFile.close()
    super.postStop()
  }
  /*
  def foldActivities(event : ActivityEvent): Unit ={
    if (!robotActivityMap.contains(event.robotId) ){
      robotActivityMap += (event.robotId -> event)

    }
    else if(robotActivityMap(event.robotId).activityId != event.activityId){
      val startEvent = robotActivityMap(event.robotId)
      Activity(uuid,startEvent.time,startEvent.name,event.time,startEvent.`type`)
    }
  }
   */
  def uuid: String = java.util.UUID.randomUUID.toString
  def writeToCSV(row:List[Any]):Unit={
    csvFile.writeRow(row)
  }
  def sendToES(json: String, cycleId: String, index: String): Unit = {
    elasticClient.foreach{client => client.index(
      index = index, `type` = "cycles", id = Some(cycleId),
      data = json, refresh = true
    )}
  }
}


object SaveToES {
  def props = Props[SaveToES]
}
