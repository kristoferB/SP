package sp.robotServices

import akka.actor._
import com.codemettle.reactivemq.ReActiveMQMessages._
import com.codemettle.reactivemq._
import com.github.nscala_time.time.Imports._
import org.json4s._
import org.json4s.native.Serialization.write
import sp.robotServices.core.Domain._
import sp.robotServices.core.Helpers._
import sp.robotServices.core.ServiceBase
import wabisabi._


import org.json4s.jackson.JsonMethods._


import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.util.{Failure, Success}

/**
  * Created by Henrik on 2016-04-15.
  */

class CycleAggregator extends ServiceBase {
  // Type aliases
  type RobotId = String
  type WorkCellId = String
  type ActivityType = String
  type ActivityEvents = List[ActivityEvent]
  type Activities = List[Activity]

  // Elasticsearch
  var elasticClient: Option[Client] = None
  final val index_robotCycle = "robot-cycle-store"
  final val index_robotActivities = "robot-activities"

  val indexes = List(index_robotCycle,index_robotActivities)

  // Maps
  var cycleEventsMap: Map[RobotId, Map[ActivityType, ActivityEvents]] = Map.empty
  var earlyOrLateEventsMap: Map[RobotId, Map[ActivityType, ActivityEvents]] = Map.empty
  var flagMap: Map[WorkCellId, Boolean] = Map.empty
  var workCellMap: Map[WorkCellId, List[RobotId]] = Map.empty
  var workCellStartTimeMap: Map[WorkCellId, DateTime] = Map.empty

  override def handleOtherMessages = {
    case "connect" =>
      import sp.system.SPActorSystem._
      val elasticIP = settings.elasticSearchIP //Config.config.getString("elastic.ip")
      val elasticPort = settings.elasticSearchPort //Config.config.getString("elastic.port")
      ReActiveMQExtension(context.system).manager ! GetConnection(s"nio://${settings.activeMQ}:${settings.activeMQPort}")
      elasticClient = Some(new Client(s"http://$elasticIP:$elasticPort"))
      elasticClient.foreach(client => indexes.foreach(index => client.createIndex(index)))
  }

  def handleAmqMessage(json: JValue): Unit = {
    if (json.has("isStart") && json.has("cycleId")) {
      val event: OutgoingCycleEvent = json.extract[OutgoingCycleEvent]
      if (event.isStart) {
        flagMap += (event.workCellId -> true)
        workCellStartTimeMap += (event.workCellId -> event.time)
        cycleEventsMap = resetCycleEventsMap(cycleEventsMap, event)
        handleEarlyEvents(event)
      } else {
        flagMap += (event.workCellId -> false)
        storeCycle(event)
      }
    } else if (json.has("isStart") && json.has("activityId")) {
      val event: ActivityEvent = json.extract[ActivityEvent]
      updateWorkCellMap(event)
      updateFlagMap(event.workCellId)
      if (flagMap(event.workCellId))
        cycleEventsMap = addToEventMap(cycleEventsMap, event)
      else {
        earlyOrLateEventsMap = addToEventMap(earlyOrLateEventsMap, event)
      }
    } else if (json.has("timeSpan")) {
      val event: RobotCycleSearchQuery = json.extract[RobotCycleSearchQuery]
      retrieveFromES(event)
    } else {
      // do nothing... OR log.info("Received message of unmanageable type property.")
    }
  }

  override def postStop() = {
    theBus.foreach(_ ! CloseConnection)
    Client.shutdown()
  }

  def resetCycleEventsMap(map: Map[RobotId, Map[ActivityType, ActivityEvents]], event: OutgoingCycleEvent):
  Map[RobotId, Map[ActivityType, ActivityEvents]] = {
    var result = map
    if(workCellMap.contains(event.workCellId)) {
      workCellMap(event.workCellId).foreach { robotId: RobotId =>
        if (result.contains(robotId))
          result += (robotId -> Map.empty[ActivityType, ActivityEvents])
      }
    }
    result
  }

  def updateWorkCellMap(event: ActivityEvent) = {
    if (workCellMap.contains(event.workCellId)) {
      val workCell = workCellMap(event.workCellId)
      if (workCell.contains(event.robotId))
        workCellMap = workCellMap
      else {
        val newRobotList = workCellMap(event.workCellId) :+ event.robotId
        workCellMap = workCellMap + (event.workCellId -> newRobotList)
      }
    } else
      workCellMap = workCellMap + (event.workCellId -> List[RobotId](event.robotId))
  }

  def updateFlagMap(workCellName: WorkCellId) = {
    if (flagMap.contains(workCellName))
      flagMap = flagMap
    else
      flagMap = flagMap + (workCellName -> false)
  }

  def addToEventMap(map: Map[RobotId, Map[ActivityType, ActivityEvents]], event: ActivityEvent) = {
    var typeToEvents: Map[ActivityType, ActivityEvents] = if (map.contains(event.robotId)) map(event.robotId) else Map.empty
    var events: ActivityEvents = if (typeToEvents.contains(event.`type`)) typeToEvents(event.`type`) else List.empty
    events = events :+ event
    typeToEvents = typeToEvents + (event.`type` -> events)
    map + (event.robotId -> typeToEvents)
  }

  def handleEarlyEvents(startEvent: OutgoingCycleEvent) = {
    if(workCellMap.contains(startEvent.workCellId)) {
      workCellMap(startEvent.workCellId).foreach { robotId: RobotId =>
        if (earlyOrLateEventsMap.contains(robotId)) {
          earlyOrLateEventsMap(robotId).foreach { case (activityType, activityEvents) =>
            activityEvents.foreach { activityEvent =>
              if (activityEvent.time.isAfter(startEvent.time))
                cycleEventsMap = addToEventMap(cycleEventsMap, activityEvent)
            }
          }
          earlyOrLateEventsMap = earlyOrLateEventsMap + (robotId -> Map.empty)
        }
      }
    }
  }

  def storeCycle(cycleStop: OutgoingCycleEvent) = {
    if(workCellStartTimeMap.contains(cycleStop.workCellId) && workCellMap.contains(cycleStop.workCellId)) {
      val cycleStartTime = workCellStartTimeMap(cycleStop.workCellId)
      val workCellRobotIds = workCellMap(cycleStop.workCellId)
      var workCellEvents = cycleEventsMap.filterKeys(workCellRobotIds.contains(_))
      cycleEventsMap = cycleEventsMap.filterKeys(!workCellRobotIds.contains(_))

      // waits, asynchronously, for pointer changes which may arrive after cycle stop even though they should not
      val asyncWait: Future[Unit] = Future { Thread.sleep(5000) }
      asyncWait onSuccess {
        case _ =>
          workCellRobotIds.foreach { robotId =>
            if (earlyOrLateEventsMap.contains(robotId)) {
              earlyOrLateEventsMap(robotId).foreach { case (eventType, events) =>
                events.foreach { event =>
                  if (event.time.isBefore(cycleStop.time))
                    workCellEvents = addToEventMap(workCellEvents, event)
                }
              }
              earlyOrLateEventsMap += (robotId -> Map.empty)
            }
          }

          val activities = foldToActivities(workCellEvents, cycleStartTime, cycleStop.time)

          if (allRobotsInCycle(cycleStop.workCellId, activities)) {
            val cycleId = uuid
            val workCellCycle = WorkCellCycle(cycleStop.workCellId, cycleId, cycleStartTime, cycleStop.time, activities)
            log.info("CycleAggregator sent to ES: " + workCellCycle)
            val json = write(workCellCycle)
            sendToES(json, cycleId, index_robotCycle)
            flattenWorkCellCycle(workCellCycle)
          }
      }
    }
  }

  def foldToActivities(workCellEvents: Map[RobotId, Map[ActivityType, ActivityEvents]], cycleStartTime: DateTime,
                       cycleStopTime: DateTime): Map[RobotId, Map[ActivityType, Activities]] = {
    workCellEvents.map { case (robotId, robotEventTypes) =>
      (robotId, robotEventTypes.map { case (eventType, events) =>
        val uniqueActivityIds = events.map(e => e.activityId).toSet
        val activities = uniqueActivityIds.flatMap { id =>
          val eventsWithId = events.filter(_.activityId == id)
          val startEvents = eventsWithId.filter(_.isStart == true)
          val stopEvents = eventsWithId.filter(_.isStart == false)
          if (startEvents.length == 1 && stopEvents.length == 1)
            Some(Activity(id, startEvents.head.time, startEvents.head.name, stopEvents.head.time, eventType))
          else if (startEvents.length == 1 && stopEvents.isEmpty)
            Some(Activity(id, startEvents.head.time, startEvents.head.name, cycleStopTime, eventType))
          else if (startEvents.isEmpty && stopEvents.length == 1)
            Some(Activity(id, cycleStartTime, stopEvents.head.name, stopEvents.head.time, eventType))
          else
            None
        }.toList
        (eventType, activities)
      })
    }

  }

  def flattenWorkCellCycle(cycle: WorkCellCycle): Unit ={
    val cycleActivities = cycle.activities.flatMap {
      activity =>
        activity._2.map(activityList => activityList._2.map(act => WorkCellActivity(cycle.workCellId,
          cycle.id, cycle.from, cycle.to,ReadableDurationOrdering(cycle.to - cycle.from) ,activity._1, act.id, act.from, act.to, act.name, act.`type`)))
    }

    sendToES(write(cycleActivities), cycle.id, index_robotActivities)
  }

  def allRobotsInCycle(workCellId: WorkCellId, activities: Map[RobotId, _]): Boolean = {
    val robotsInCycle = activities.keySet
    val robotsInWorkCell = workCellMap(workCellId).toSet
    if (robotsInWorkCell.diff(robotsInCycle).isEmpty)
      true
    else
      false
  }

  def sendToES(json: String, cycleId: String, index: String): Unit = {
    elasticClient.foreach{client => client.index(
      index = index_robotCycle, `type` = "cycles", id = Some(cycleId),
      data = json, refresh = true
    )}
  }

  def retrieveFromES(event: RobotCycleSearchQuery) = {
    var jsonQuery: Option[String] = None
    if (event.cycleId.isDefined) {
      jsonQuery = Some("{ \"size\" : 20, \"query\": { \"match\" : { \"entryId\" : \"" +
        s"${event.cycleId.get}" +
        "\" } } }")
    } else if (event.timeSpan.isDefined) {
      jsonQuery = Some("{ \"size\" : 20, \"query\": { \"bool\" :{ \"must\" : [ { \"term\" : { \"workCellId\" : \"" +
        s"${event.workCellId}" +
        "\" } },{ \"range\" : { \"from\" : { \"gte\" : \"" +
        s"${event.timeSpan.get.from}" +
        "\" } } }, { \"range\" : { \"to\" : { \"lte\" : \"" +
        s"${event.timeSpan.get.to}" +
        "\" } } } ] } } }")
    }
    if (jsonQuery.isDefined) {
      elasticClient.foreach{client =>
        val searchResponse: Future[String] =
          client.search(index = index_robotCycle, query = jsonQuery.get).map(_.getResponseBody)
        searchResponse onComplete {
          case Failure(e) => log.error("An error has occurred while retrieving cycles from elastic: " + e.getMessage)
          case Success(cycles) =>
            val json = parse(cycles)
            if (json.has("error")) {
              val errorMessage = (json \ "error" \ "type").extract[String]
              log.info(errorMessage)
            } else {
              val hits: Int = (json \ "hits" \ "total").extract[Int]
              if (hits == 1) {
                val extractedCycles = (json \ "hits" \ "hits" \ "_source").extract[WorkCellCycle]
                val robotCyclesResponse =
                  RobotCyclesResponse(s"${event.workCellId}", None, Some(List[WorkCellCycle](extractedCycles)))
                val jsonResponse = write(robotCyclesResponse)
                sendToBus(jsonResponse)
              } else {
                val extractedCycles = (json \ "hits" \ "hits" \ "_source").extract[List[WorkCellCycle]]
                val robotCyclesResponse = RobotCyclesResponse(s"${event.workCellId}", None, Some(extractedCycles))
                val jsonResponse = write(robotCyclesResponse)
                sendToBus(jsonResponse)
              }
            }
        }
      }
    } else {
      val errorMessage = "Id or time span has not been provided. Either is required."
      val emptyResponse = RobotCyclesResponse(s"${event.workCellId}", Some(errorMessage), None)
      val jsonResponse = write(Map[String, RobotCyclesResponse]("cycleSearchResult" -> emptyResponse))
      sendToBus(jsonResponse)
    }
  }

  def uuid: String = java.util.UUID.randomUUID.toString
}

object CycleAggregator {
  def props = Props[CycleAggregator]
}