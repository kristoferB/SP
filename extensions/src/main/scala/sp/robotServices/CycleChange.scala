package sp.robotServices

import akka.actor._
import org.json4s._
import org.json4s.native.Serialization.write
import sp.robotServices.core.Domain._
import sp.robotServices.core.Helpers._
import sp.robotServices.core.{Config, ServiceBase}

/**
  * Created by Henrik on 2016-06-02.
  */

class CycleChange extends ServiceBase {
  // Type aliases
  type Id = String
  type Instruction = String
  type RobotId = String
  type WorkCellId = String

  // Config values
  import sp.system.SPActorSystem._
  val homePosSignals = settings.homePosSignals //Config.config.getStringList("services.cycleChange.homePosSignals")

  // State
  var getWorkCellsFlag: Boolean = true
  var cycleIdMap: Map[WorkCellId, Id] = Map.empty
  var workCellMap: Map[WorkCellId, List[RobotId]] = Map.empty
  var workCellStopFlagMap: Map[WorkCellId, Boolean] = Map.empty
  var robotStartFlagMap: Map[WorkCellId, Map[RobotId, Boolean]] = Map.empty

  // Functions
  def handleAmqMessage(json: JValue) = {
    if (json.has("newSignalState") && homePosSignals.contains((json \ "address" \ "signal").extract[String]) &&  getWorkCellsFlag) {
      val event: IncomingCycleEvent = json.extract[IncomingCycleEvent]
      if (!workCellMap.contains(event.workCellId))
        requestWorkCells()
      else
      convert(event)
    } else if (json.has("workCells") && !getWorkCellsFlag) {
      val workCells: List[WorkCell] = (json \ "workCells").extract[List[WorkCell]]
      workCells.foreach{workCell =>
        workCellMap += (workCell.id -> workCell.robots.map(r => r.id))
        cycleIdMap = handleCycleIdMap(cycleIdMap, workCell.id)
        workCellStopFlagMap += (workCell.id -> true)
      }
      initializeRobotStartFlagMap()
      getWorkCellsFlag = !getWorkCellsFlag
    } else {
      // do nothing... OR log.info("Received message of unmanageable type property.")
    }
  }

  def handleCycleIdMap(map: Map[WorkCellId, Id], workCellId: WorkCellId): Map[WorkCellId, Id] = {
    var result = Map[WorkCellId, Id]()
    if (map.contains(workCellId))
      result = map
    else
      result = map + (workCellId -> uuid)
    result
  }

  def initializeRobotStartFlagMap() = {
    workCellMap.foreach{element =>
      var robotMap: Map[RobotId, Boolean] = Map.empty
      element._2.foreach{robotId =>
        robotMap += (robotId -> false)
      }
      robotStartFlagMap += (element._1 -> robotMap)
    }
  }

  def convert(event: IncomingCycleEvent) = {
    val isStartOrStop: Option[Boolean] = evaluateIsStart(event)
    if (isStartOrStop.isDefined) {
      val isStart = isStartOrStop.get
      if (isStart)
        cycleIdMap += (event.workCellId -> uuid)
      val cycleId = cycleIdMap(event.workCellId)
      val outgoingCycleEvent = OutgoingCycleEvent(cycleId, isStart, event.time, event.workCellId)
      val json = write(outgoingCycleEvent)
      log.info("From cycleChange: " + json)
      sendToBus(json)
    }
  }

  def evaluateIsStart(event: IncomingCycleEvent): Option[Boolean] = {
    var result: Option[Boolean] = None
    var robotMap = robotStartFlagMap(event.workCellId)
    robotMap += (event.robotId -> (event.newSignalState.value > 0))
    robotStartFlagMap += (event.workCellId -> robotMap)
    val flagList: List[Boolean] = robotStartFlagMap(event.workCellId).flatMap(element => List(element._2)).toList
    if (flagList.forall(_ == true)) {
      workCellStopFlagMap += (event.workCellId -> true)
      result = Some(false)
    }
    else if (flagList.count(_ == false) == 1 && workCellStopFlagMap(event.workCellId)) {
      workCellStopFlagMap += (event.workCellId -> false)
      result = Some(true)
    }
    result
  }

  def requestWorkCells() = {
    import org.json4s.JsonDSL._
    getWorkCellsFlag = !getWorkCellsFlag
    val json = ("event" -> "newWorkCellEncountered") ~ ("service" -> "cycleChange")
    sendToBus(write(json))
  }

  def uuid: String = java.util.UUID.randomUUID.toString
}

object CycleChange {
  def props = Props[CycleChange]
}