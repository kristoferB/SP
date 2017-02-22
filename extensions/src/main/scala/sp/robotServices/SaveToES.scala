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

  val indexes = List(index_activity,index_activityEvents,index_cycleEvents)

  var robotActivityMap : Map[RobotId,ActivityEvent] = Map.empty
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

  val homePosSignals = settings.homePosSignals
  override def handleAmqMessage(json: JValue): Unit = {
    if (json.has("activityId")) {
      val event: ActivityEvent = json.extract[ActivityEvent]
    if (event.isStart && event.name.contains("main") && robotIdToCurrentPos.getOrElse(event.robotId,false)){
        robotIdToCurrentRobotCycle += (event.robotId -> uuid) // (robotIdToCurrentRobotCycle.getOrElse(event.robotId,-1) + 1) )
      }
      def robCylId = robotIdToCurrentRobotCycle.contains(event.robotId) match {
        case true => robotIdToCurrentRobotCycle(event.robotId)
        case false => robotIdToCurrentRobotCycle += (event.robotId -> uuid)
          robotIdToCurrentRobotCycle(event.robotId)

      }
      val activityToSend =write(ActivityEventWithRobotCycle(event.activityId, robCylId/*robotIdToCurrentRobotCycle(event.robotId)*/,event.isStart,event.name,event.robotId,event.time,event.`type`,event.workCellId))
      sendToES(activityToSend,uuid,index_activity)
      mediator ! Publish("robotServices", activityToSend)
 
    }
    if (json.has("newSignalState") && homePosSignals.contains((json \ "address" \ "signal").extract[String])){
      val event = json.extract[IncomingCycleEvent]
      robotIdToCurrentPos += (event.robotId -> (event.newSignalState.value > 0))
      sendToES(write(event),uuid,index_cycleEvents)
    }

  }

  def foldActivities(event : ActivityEvent): Unit ={
    if (!robotActivityMap.contains(event.robotId) ){
      robotActivityMap += (event.robotId -> event)

    }
    else if(robotActivityMap(event.robotId).activityId != event.activityId){
      val startEvent = robotActivityMap(event.robotId)
      Activity(uuid,startEvent.time,startEvent.name,event.time,startEvent.`type`)
    }
  }
  def uuid: String = java.util.UUID.randomUUID.toString
  def sendToES(json: String, cycleId: String, index: String): Unit = {
    //println("Writing to ES" + cycleId + " " + index )
    elasticClient.foreach{client => client.index(
      index = index, `type` = "cycles", id = Some(cycleId),
      data = json, refresh = true
    )}
  }
}


object SaveToES {
  def props = Props[SaveToES]
}
