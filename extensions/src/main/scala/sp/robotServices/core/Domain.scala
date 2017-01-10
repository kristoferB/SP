package sp.robotServices.core

import com.github.nscala_time.time.Imports._
import org.joda.time.ReadableDuration

/**
  * Created by daniel on 2016-06-01.
  */

object Domain {

  // Tip Dressing
  case class TipDressWarningEvent(robotId: String,
                                  workCellId: String,
                                  address: RobotDataAddress,
                                  cutterWarning: Boolean)

  case class TipDressEvent(robotId: String,
                           workCellId: String,
                           address: RobotDataAddress,
                           tipDressData: TipDressData)

  case class RobotDataAddress(domain: String,
                              kind: String,
                              path: List[String])

  case class TipDressData(tipDressWear: Float,
                          time: DateTime)

  // Activities
  case class Activity(id: String,
                      from: DateTime,
                      name: String,
                      to: DateTime,
                      `type`: String)

  case class ActivityEvent(activityId: String,
                           isStart: Boolean,
                           name: String,
                           robotId: String,
                           time: DateTime,
                           `type`: String,
                           workCellId: String)

  case class IncomingCycleEvent(address: SignalAddress,
                                newSignalState: NewSignalState,
                                robotId: String,
                                time: DateTime,
                                workCellId: String)

  case class OutgoingCycleEvent(cycleId: String,
                                isStart: Boolean,
                                time: DateTime,
                                workCellId: String)

  // Get work cells from endpoint
  case class WorkCell(id: String,
                      description: String,
                      robots: List[Robot])

  case class Robot(id: String,
                   name: String)

  // Cycle Fold, Store and Search
  case class WorkCellCycle(workCellId: String,
                           id: String,
                           from: DateTime,
                           to: DateTime,
                           activities: Map[String, Map[String, List[Activity]]])

  case class WorkCellActivity(workCellId: String,
                              cycleId: String,
                              cycleStart: DateTime,
                              cycleEnd: DateTime,
                              cycleTime: ReadableDuration,
                              resource: String,
                              activityId: String,
                              activityStart: DateTime,
                              activityEnd: DateTime,
                              name: String,
                              `type`: String)

  case class RobotCycleSearchQuery(cycleId: Option[String],
                                   timeSpan: Option[TimeSpan],
                                   workCellId: String)

  case class TimeSpan(from: DateTime,
                      to: DateTime)

  case class RobotCyclesResponse(workCellId: String,
                                 error: Option[String],
                                 foundCycles: Option[List[WorkCellCycle]])

  // Robot Endpoint
  case class RapidAddress(domain: String,
                          kind: String,
                          path: List[String])

  case class SignalAddress(domain: String,
                           signal: String)

  // IO Signals
  case class NewSignalState(value: Float,
                            simulated: Boolean,
                            quality: Map[String, Int])

  // Program Pointer
  case class PointerChangedEvent(robotId: String,
                                 workCellId: String,
                                 address: RapidAddress,
                                 programPointerPosition: PointerPosition)

  case class PointerPosition(position: Position,
                             task: String,
                             time: DateTime)

  case class Position(module: String,
                      routine: String,
                      range: Range)

  case class Range(begin: Location,
                   end: Location)

  case class Location(column: Int,
                      row: Int)

  // RAPID Modules
  case class ModulesReadEvent(robotId: String,
                              workCellId: String,
                              address: RapidAddress,
                              readValue: List[TaskWithModules])

  case class TaskWithModules(name: String,
                             modules: List[Module])

  case class Module(name: String,
                    file: List[String])


  // Instruction Fill
  case class PointerWithInstruction(robotId: String,
                                    workCellId: String,
                                    address: RapidAddress,
                                    instruction: String,
                                    programPointerPosition: PointerPosition)

  // Is Waiting Fill
  case class PointerWithIsWaiting(robotId: String,
                                  workCellId: String,
                                  address: RapidAddress,
                                  instruction: String,
                                  isWaiting: Boolean,
                                  programPointerPosition: PointerPosition)

}