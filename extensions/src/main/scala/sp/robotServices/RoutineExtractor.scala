package sp.robotServices

import akka.actor._
import org.json4s._
import org.json4s.native.Serialization.write
import sp.robotServices.core.Domain._
import sp.robotServices.core.Helpers._
import sp.robotServices.core.ServiceBase

/**
  * Created by Henrik on 2016-05-10.
  */

class RoutineExtractor extends ServiceBase {
  // Type aliases
  type RobotName = String
  type Id = String

  // Config file
  import sp.system.SPActorSystem._
  val waitRoutines = settings.routinesToIgnore // Config.config.getStringList("services.routineChange.routinesToIgnore")

  // Variables
  var activityIdMap: Map[RobotName, Map[String, Id]] = Map.empty
  var priorEventMap: Map[RobotName, PointerChangedEvent] = Map.empty
  val isStart: Boolean = true

  // Functions
  def handleAmqMessage(json: JValue) = {
    if (json.has("programPointerPosition") && !json.has("instruction")) {
      val event: PointerChangedEvent = json.extract[PointerChangedEvent]
      activityIdMap = handleActivityIdMap(activityIdMap, event)
      handleEvent(event)
    } else {
      // do nothing... OR log.info("Received message of unmanageable type property.")
    }
  }

  def handleActivityIdMap(map: Map[RobotName, Map[String, Id]], event: PointerChangedEvent):
  Map[RobotName, Map[String, Id]] = {
    var result = Map[RobotName, Map[String, Id]]()
    if (map.contains(event.robotId))
      result = map
    else
      result = map + (event.robotId -> Map[String, Id]("current" -> uuid))
    result
  }

  def handleEvent(event: PointerChangedEvent) = {
    if (priorEventMap.contains(event.robotId)) {
      val priorEvent = priorEventMap(event.robotId)
      val priorModule: String = priorEvent.programPointerPosition.position.module
      val currentModule: String = event.programPointerPosition.position.module
      val priorRoutine: String = priorEvent.programPointerPosition.position.routine
      val currentRoutine: String = event.programPointerPosition.position.routine
      if (!priorRoutine.equals(currentRoutine)) {
        activityIdMap = updateActivityIdMap(activityIdMap, event.robotId)
        val priorId = activityIdMap(event.robotId)("prior")
        val currentId = activityIdMap(event.robotId)("current")
        if (!isWaitingRoutine(priorRoutine)) {
          val routineStopEvent =
            ActivityEvent(priorId, !isStart, priorRoutine, event.robotId, event.programPointerPosition.time,
              "routines", event.workCellId)
          val json = write(routineStopEvent)
          // log.info("Previous routine: " + json)
          sendToBus(json)
        }
        if (!isWaitingRoutine(currentRoutine)) {
          val routineStartEvent =
            ActivityEvent(currentId, isStart, currentRoutine, event.robotId, event.programPointerPosition.time,
              "routines", event.workCellId)
          val json = write(routineStartEvent)
          // log.info("Current routine: " + json)
          sendToBus(json)
        }
      }
    }
    priorEventMap += (event.robotId -> event)
  }

  def updateActivityIdMap(map: Map[RobotName, Map[String, Id]], robotId: String): Map[RobotName, Map[String, Id]] = {
    var result = map
    val temp = result(robotId)("current")
    result += (robotId -> Map[String,Id]("current" -> uuid, "prior" -> temp))
    result
  }

  def isWaitingRoutine(routineName: String): Boolean = {
    var flag = false
    if (waitRoutines.contains(routineName))
      flag = true
    flag
  }

  def uuid: String = java.util.UUID.randomUUID.toString
}

object RoutineExtractor {
  def props = Props[RoutineExtractor]
}
