package sp.robotCycleAnalysis

import com.github.nscala_time.time.Imports._
import sp.domain.{ID, SPAttributes}

/**
  * Created by daniel on 2016-06-03.
  */
object fakes {
  implicit val formats = org.json4s.DefaultFormats ++ org.json4s.ext.JodaTimeSerializers.all // for json serialization
  var latestActivityId: Option[ID] = None

  def activityEvent = {
    val activityId =
      if (latestActivityId.isDefined) {
        val id = latestActivityId.get
        latestActivityId = None
        id
      }
      else {
        val id = ID.newID
        latestActivityId = Some(id)
        id
      }

    SPAttributes(
      "activityId" -> activityId.toString,
      "isStart" -> latestActivityId.isDefined,
      "name" -> "Moving",
      "robotId" -> "10.200.39.150",
      "time" -> DateTime.now,
      "type" -> "routines",
      "workCellId" -> "1741000"
    )

  }

  var latestCycleId: Option[ID] = None

  def cycleEvent = {
    val cycleId =
      if (latestCycleId.isDefined) {
        val id = latestCycleId.get
        latestCycleId = None
        id
      }
      else {
        val id = ID.newID
        latestCycleId = Some(id)
        id
      }

    SPAttributes(
      "cycleId" -> cycleId.toString,
      "isStart" -> latestCycleId.isDefined,
      "robotId" -> "10.200.39.150",
      "time" -> DateTime.now,
      "workCellId" -> "1741000"
    )
  }

  def foundCycles = SPAttributes(
    "foundCycles" -> List(
      SPAttributes(
        "activities" -> SPAttributes(
          "10.200.39.100" -> SPAttributes(),
          "10.200.39.150" -> SPAttributes(
            "routines" -> List(
              SPAttributes(
                "id" -> ID.newID.toString,
                "from" -> DateTime.now.minusHours(6).plusSeconds(10),
                "name" -> "PickUpRearLeft",
                "to" -> DateTime.now.minusHours(6).plusSeconds(20),
                "type" -> "routines"
              ),
              SPAttributes(
                "id" -> ID.newID.toString,
                "from" -> DateTime.now.minusHours(6).plusSeconds(25),
                "name" -> "MoveToBody",
                "to" -> DateTime.now.minusHours(6).plusSeconds(40),
                "type" -> "routines"
              )
            )
          )
        ),
        "from" -> DateTime.now.minusHours(6),
        "id" -> ID.newID.toString,
        "to"  -> DateTime.now.minusHours(6).plusMinutes(1).plusSeconds(45),
        "workCellId" -> "1741000"
      )
    ),
    "workCellId" -> "1741000"
  )

  val workCells = SPAttributes(
    "workCells" -> List(
      SPAttributes(
        "id" -> "1741000",
        "description" -> "Mount rear doors",
        "robots" -> List(
          SPAttributes(
            "id" -> "10.200.39.100",
            "name" -> "R1"
          ),
          SPAttributes(
            "id" -> "10.200.39.150",
            "name" -> "R2"
          )
        )
      )
    )
  )

}
