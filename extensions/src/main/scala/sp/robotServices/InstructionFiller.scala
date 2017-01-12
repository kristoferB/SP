package sp.robotServices

import java.util.UUID

import akka.actor.Props
import com.github.nscala_time.time.Imports._
import org.json4s._
import org.json4s.native.Serialization._
import core.Domain.{ActivityEvent, PointerWithInstruction, PointerWithIsWaiting, _}
import core.Helpers._
import sp.robotServices.core.ServiceBase
import sp.system.SPActorSystem._


/**
  * Created by ashfaqf on 1/2/17.
  *
  * Moving, merging and refactoring functions/classes by Daniel and Henrik from robot-services into SP
  */
class InstructionFiller extends ServiceBase{
  // Type aliases
  type RobotId = String
  type TaskName = String
  type ModuleName = String
  type Instruction = String
  type ActivityId = UUID
  type WaitInstruction = String

  // Maps
  var robotMap: Map[RobotId, Map[TaskName, Map[ModuleName, Module]]] = Map.empty
  var taskMap: Map[TaskName, Map[ModuleName, Module]] = Map.empty
  var moduleMap: Map[ModuleName, Module] = Map.empty
  var timerMap: Map[RobotId, DateTime] = Map.empty

  // State
  var isWaiting: Map[RobotId, Option[(ActivityId, WaitInstruction)]] = Map.empty

  // Functions
  def handleAmqMessage(json: JValue) = {
    if (json.has("readValue")) {
      val event: ModulesReadEvent = json.extract[ModulesReadEvent]
      event.readValue.foreach(task => {
        task.modules.foreach(module => moduleMap += (module.name -> module))
        taskMap += (task.name -> moduleMap)
        moduleMap = Map.empty[ModuleName, Module]
      })
      robotMap += (event.robotId -> taskMap)
      taskMap = Map.empty[TaskName, Map[ModuleName, Module]]
    } else if (json.has("programPointerPosition") && !json.has("instruction")) {
      val event: PointerChangedEvent = json.extract[PointerChangedEvent]
      fill(event)
    } else {
      // do nothing... OR log.info("Received message of unmanageable type property.")
    }

    //is waiting
    //waitChange
  }

  //InstructionFiller
  def fill(event: PointerChangedEvent) = {
    val eventPPPos = event.programPointerPosition
    if (robotMap.contains(event.robotId)) {
      if (robotMap(event.robotId).contains(eventPPPos.task)) {
        if (robotMap(event.robotId)(eventPPPos.task).contains(eventPPPos.position.module)) {
          val module: Module = robotMap(event.robotId)(eventPPPos.task)(eventPPPos.position.module)
          val range: Range = eventPPPos.position.range
          val instruction: Instruction = module.file(range.begin.row - 1).
            slice(range.begin.column - 1, range.end.column + 1)
          val filledEvent: PointerWithInstruction =
            PointerWithInstruction(event.robotId, event.workCellId, event.address, instruction, eventPPPos)

          //To write to ES
          val json = write(filledEvent)
          log.info("From instruction filler: " + json)
          sendToBus(json)


          fillWithIsWaiting(filledEvent)
        } else
          log.info(s"The system ${event.robotId} does not contain the module called" +
            s"${eventPPPos.position.module}")
      } else
        log.info(s"The system ${event.robotId} does not contain the task called" +
          s"${eventPPPos.task}")
    } else {
      if (timerMap.contains(event.robotId)) {
        if ((timerMap(event.robotId) to DateTime.now).millis < 60000) {
          timerMap += (event.robotId -> DateTime.now)
          requestModules(event.robotId)
        }
      } else {
        timerMap += (event.robotId -> DateTime.now)
        requestModules(event.robotId)
      }
    }
  }

  //is waiting
  def fillWithIsWaiting(event: PointerWithInstruction) = {
    //fill for isWaiting
    val instruction: Instruction = event.instruction
    var isWaiting: Boolean = false
    if (instruction.startsWith("Wait") || instruction.startsWith("ExecEngine"))
      isWaiting = true
    val filledEvent = PointerWithIsWaiting(event.robotId, event.workCellId, event.address, instruction, isWaiting,
      event.programPointerPosition)
   // val json: String = write(filledEvent)
    //log.info("From isWaiting: " + json)
    //sendToBus(json)

    checkIfWaitChange(filledEvent)
  }


  //wait change
  def checkIfWaitChange(event: PointerWithIsWaiting) = {
    //val event: PointerWithIsWaiting = json.extract[PointerWithIsWaiting]

    if (!isWaiting.contains(event.robotId)) {
      isWaiting += (event.robotId -> None)
    }

    if (isWaiting(event.robotId).isDefined != event.isWaiting) {
      val (activityId, waitInstruction): (ActivityId, WaitInstruction) = if (event.isWaiting) {
        val id = UUID.randomUUID()
        isWaiting += (event.robotId -> Some((id, event.instruction)))
        (id, event.instruction)
      } else {
        val (id, instruction) = isWaiting(event.robotId).get
        isWaiting += (event.robotId -> None)
        (id, instruction)
      }
      val activityEvent = ActivityEvent(activityId.toString, event.isWaiting, waitInstruction, event.robotId,
        event.programPointerPosition.time, "wait", event.workCellId)
      log.info("From waitChange: " + activityEvent)
      sendToBus(write(activityEvent))
    }

  }


  def requestModules(robotId: RobotId) = {
    import org.json4s.JsonDSL._
    val json = ("event" -> "newRobotEncountered") ~ ("robotId" -> robotId) ~ ("service" -> "instructionFiller")
    sendToBusWithTopic(settings.activeMQRequestTopic, write(json))
  }
}

object InstructionFiller {
  def props = Props[InstructionFiller]
}
