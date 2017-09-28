package sp.labkit

import akka.actor._
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import java.util.UUID

import scala.concurrent.duration._
import akka.persistence._
import sp.domain._
import sp.domain.Logic._
import org.joda.time._

import scala.util.{Failure, Success, Try}
import com.github.nscala_time.time.Imports._

import sp.devicehandler._

import sp.devicehandler.{APIVirtualDevice => vdapi}
import sp.labkit.{APILabkit => api}

class OPMakerLabKit extends PersistentActor
    with ActorLogging
    with OPMakerLogic
    with TrackProducts
    with sp.service.ServiceCommunicationSupport
    with sp.service.MessageBussSupport {
  override def persistenceId = "rawPLC"
  import context.dispatcher

  subscribe(vdapi.topicRequest)

  //private var state: Map[String, SPValue] = Map()
  private var currentOps: Map[String, api.OP] = Map()

  // set up driver
  val cp = "|var|CODESYS Control for Raspberry Pi SL.Application.Process_IOs."
  def v(name: String) = Thing(name, SPAttributes("drivername" -> (cp+name)))
  val vars = allPLCVars.map(v)

  val driverID = UUID.randomUUID()
  def sm(vars: List[Thing]): List[vdapi.OneToOneMapper] = vars.flatMap { v =>
    v.attributes.getAs[String]("drivername").map(dn => vdapi.OneToOneMapper(v.id, driverID, dn))
  }
  val setup = SPAttributes("url" -> "opc.tcp://192.168.0.10:4840", "identifiers" -> sm(vars).map(_.driverIdentifier))
  val driver = vdapi.Driver("codesys", driverID, "OPCUA", setup)
  publish(vdapi.topicRequest, SPMessage.makeJson[SPHeader, vdapi.SetUpDeviceDriver](SPHeader(from = "hej"), vdapi.SetUpDeviceDriver(driver)))
  val idmap = vars.map(v=>v.id -> v).toMap

  // setup resource
  val labkit = vdapi.Resource("labkit", UUID.randomUUID(), vars.map(_.id).toSet, sm(vars), SPAttributes())
  val resources = List(labkit)

  resources.foreach { res =>
    val body = APIVirtualDevice.SetUpResource(res)
    publish(APIVirtualDevice.topicRequest, SPMessage.makeJson[SPHeader, APIVirtualDevice.SetUpResource](SPHeader(from = "hej"), body))
  }


  def receiveCommand = {
    case mess @ _ if {println(s"OPMaker got: $mess from $sender"); false} => Unit

    case x: String =>
      persist(x)(fixTheOps)


  }

  def fixTheOps(mess: String) = {
      val msg = SPMessage.fromJson(mess)

      for {
        m <- msg
        h <- m.getHeaderAs[SPHeader]
        b <- m.getBodyAs[vdapi.Response]
      } yield {
        b match {
          case vdapi.StateEvent(resourceName, rid, state, diff) if rid == labkit.id => {
            val time = org.joda.time.DateTime.now // todo: time is lost is this version
            val stateOnName = state.flatMap { case (id,v) =>
              idmap.get(id).map(t => (t.name -> v)) }.toMap
            val updOps = makeMeOps(stateOnName, time, currentOps).map(updPositionsAndOps)
            println("NEW OPS")
            updOps.foreach(println(_))
            updOps.foreach{ x =>
              val header = SPHeader(from = "OPMakerLabkit")
              val body = x
              publish(api.topicResponse, SPMessage.makeJson(header, body))
            }


            if(updOps.nonEmpty) {
              val h = SPHeader(from = "OPMakerLabkit")
              val b = api.Positions(positions, postime.toString)
              publish(api.topicResponse, SPMessage.makeJson(h, b))
            }

            currentOps = (currentOps ++ updOps.map(x => x.start.name -> x)).filter{case (k,v) => v.end.isEmpty }
          }
          case x => println("Nope, no Raw mess parsing")
        }
      }
  }

  val baseTimeNow = org.joda.time.DateTime.now
  var baseTimeThen: Long = -1
  var lastms: Long = 0
  def receiveRecover = {
    case x: String =>
      //      fixTheOps(x)

      // todo
      // val attr = SPValue.fromJson(x)
      // val rawMess = attr.flatMap(_.to[RawMess])
      // if(rawMess.nonEmpty && rawMess.get.state.nonEmpty && rawMess.get.time.nonEmpty) {
      //   val ms = new DateTime(rawMess.get.time).getMillis()
      //   if(baseTimeThen == -1) baseTimeThen = ms // init replay time
      //   val msOfExecution = ms - baseTimeThen
      //   val sleep = msOfExecution - lastms
      //   Thread.sleep(sleep)
      //   lastms = msOfExecution
      //   val rawFixedTime = RawMess(rawMess.get.state, baseTimeNow.plusMillis(msOfExecution.intValue()).toString)
      //   fixTheOps(SPValue(rawFixedTime).toJson.toString)
      // }
    case RecoveryCompleted =>
      println("recover done")
    case x => println("hej: "+x)
  }



}

object OPMakerLabKit {
  def props() = Props(classOf[OPMakerLabKit])
}


trait NamesAndValues {
  // resources
  val feeder          = "feeder"
  val pnp1            = "pnp1"
  val p1            = "p1"
  val conv            = "conv"
  val pnp2            = "pnp2"
  val p3            = "p3"
  val p4            = "p4"

  // operations
  val feedCylinder  = "feedCylinder"
  val fromFeedToP1              = "fromFeedToP1"
  val fromFeedToC             = "fromFeedToC"
  val fromP1ToC             = "fromP1ToC"
  val p1move              = "p1move"
  val p1Process             = "p1Process"
  val transport             = "transport"
  val to3             = "to3"
  val to4             = "to4"
  val p3move              = "p3move"
  val p3Process             = "p3Process"
  val p4move              = "p4move"
  val p4Process             = "p4Process"

  // Positions
  val inLoader         = "inLoader"
  val inPnp1           = "inPnp1"
  val inP1             = "inP1"
  val inConvIn         = "inConvIn"
  val inConvOut        = "inConvOut"
  val inPnp2           = "inPnp2"
  val inP3             = "atP3"
  val inP4             = "atP4"

  // PLC variables in state
  val  feeder_exec     = "feeder_exec"
  val  newCylinder_var = "newCylinder_var"
  val  pnp1_mode       = "pnp1_mode"
  val  pnp1from_var    = "pnp1from_var"
  val  pnp1to_var      = "pnp1to_var"
  val  p1_mode         = "p1_mode"
  val  p1Transport_var = "p1Transport_var"
  val  p1Process_var   = "p1Process_var"
  val  convFree_var    = "convFree_var"
  val  convMove_var    = "convMove_var"
  val  convAtOut_var   = "convAtOut_var"
  val  pnp2_mode       = "pnp2_mode"
  val  pnp2to3_var     = "pnp2to3_var"
  val  pnp2to4_var     = "pnp2to4_var"
  val  p3_mode         = "p3_mode"
  val  p3Process_var   = "p3Process_var"
  val  p4_mode         = "p4_mode"
  val  p4Process_var   = "p4Process_var"
  val  allPLCVars = List(feeder_exec, newCylinder_var, pnp1_mode, pnp1from_var, pnp1to_var,
    p1_mode, p1Transport_var, p1Process_var, convFree_var, convMove_var, convAtOut_var,
    pnp2_mode, pnp2to3_var, pnp2to4_var, p3_mode, p3Process_var, p4_mode, p4Process_var)
}

trait OPMakerLogic extends NamesAndValues{
  private var opCounter = 0

  def makeMeOps(state: Map[String, SPValue], time: DateTime, currentOps: Map[String, api.OP]) = {
    opCounter = opCounter + 1
    val ops = List(
      getOrMakeANew(feedCylinder,feeder, time, currentOps, checkValue(state, List(feeder_exec -> true, newCylinder_var -> true))),

      getOrMakeANew(fromFeedToP1,pnp1,time, currentOps, checkValue(state, List(pnp1_mode -> 2, pnp1from_var -> 3, pnp1to_var -> 5))),
      getOrMakeANew(fromFeedToC,pnp1, time, currentOps, checkValue(state, List(pnp1_mode -> 2, pnp1from_var -> 3, pnp1to_var -> 1))),
      getOrMakeANew(fromP1ToC,pnp1, time, currentOps, checkValue(state, List(pnp1_mode -> 2, pnp1from_var -> 5, pnp1to_var -> 1))),

      getOrMakeANew(transport,conv, time, currentOps, checkValue(state, List(convMove_var -> true))),

      getOrMakeANew(to3,pnp2, time, currentOps, checkValue(state, List(pnp2_mode -> 2, pnp2to3_var -> true, pnp2to4_var -> false))),
      getOrMakeANew(to4,pnp2, time, currentOps, checkValue(state, List(pnp2_mode -> 2, pnp2to4_var -> true, pnp2to3_var -> false))),

      getOrMakeANew(p1move,p1, time, currentOps, checkValue(state, List(p1Transport_var -> true))),
      getOrMakeANew(p1Process,p1, time, currentOps, checkValue(state, List(p1Process_var -> true))),

      getOrMakeANew(p3move,p3, time, currentOps, checkValue(state, List(p3_mode -> 2))),
      getOrMakeANew(p3Process,p3, time, currentOps, checkValue(state, List(p3Process_var -> true))),

      getOrMakeANew(p4move,p4, time, currentOps, checkValue(state, List(p4_mode -> 2))),
      getOrMakeANew(p4Process,p4, time, currentOps, checkValue(state, List(p4Process_var -> true)))
    )

    val temp = ops.collect{
      case Some(o) if !currentOps.contains(o.start.name) || currentOps(o.start.name) != o => o
    }



    temp

    // send out operations
    // remove if completed
    // also track the products in this actor.


  }

  def getOrMakeANew(name: String,
                    resource: String,
                    time: DateTime,
                    currentOps: Map[String, api.OP],
                    hasStarted: Boolean
                   ) = {


    //println("OP "+name + ": "+hasStarted)
    val currOP = currentOps.get(name).orElse{
      if (hasStarted){
        Some(api.OP(api.OPEvent(name, time.toString, name+opCounter, resource, None), None))
      } else None
    }
    currOP.map{op =>
      if (!hasStarted) { // it has completed now
        val startTime = new DateTime(op.start.time)
      val duration = startTime to time toDurationMillis
        val end = api.OPEvent(op.start.name, time.toString, op.start.id, op.start.resource, op.start.product)
        op.copy(end = Some(end), attributes =  op.attributes + ("duration"->duration.toInt))
      } else op
    }
  }

  def checkValue(state: Map[String, SPValue], values: List[(String, SPValue)]) = {
    values.foldLeft(true){(a, b) =>
//      println("Checking state ******")
//      println(s"${b._1} = ${b._2}, state: ${state.get(b._1)}")
//      println(a && state.contains(b._1) && state(b._1) == b._2 )

      a && state.contains(b._1) && state(b._1) == b._2
    }
  }
}



trait TrackProducts extends NamesAndValues {
  var prodID = 0

  var postime = org.joda.time.DateTime.now
  var positions: Map[String, String] = Map(
    inLoader -> "",
    inP1     -> "",
    inPnp1     -> "",
    inConvIn -> "",
    inConvOut-> "",
    inPnp2   -> "",
    inP3     -> "",
    inP4     -> "",
    "" -> ""
  )

  case class OPMove(from: String, to: String)


  val opMovements = Map(
    feedCylinder  -> OPMove("", inLoader),
    fromFeedToP1  -> OPMove(inLoader, inP1),
    fromFeedToC   -> OPMove(inLoader, inConvIn),
    fromP1ToC     -> OPMove(inP1, inConvIn),
    p1move        -> OPMove(inP1, inP1),
    p1Process     -> OPMove(inP1, inP1),
    transport     -> OPMove(inConvIn, inConvOut),
    to3           -> OPMove(inConvOut, inP3),
    to4           -> OPMove(inConvOut, inP4),
    p3move        -> OPMove(inP3, ""),
    p3Process        -> OPMove(inP3, inP3),
    p4move        -> OPMove(inP4, ""),
    p4Process        -> OPMove(inP4, inP4)
  )

  def updPositionsAndOps(op: api.OP) = {

    val move = opMovements(op.start.name)
    val from = positions(move.from)
    val to = positions(move.to)

    postime = lastTime(op)



    val res = if (move.from.isEmpty) {
      // Source op for cylinders
      if (op.start.product.isEmpty) {
        prodID = prodID + 1
        val cylId = "cyl" + prodID
        updProdStart(op, cylId)
      } else {
        val cylId = op.start.product.get
        if (op.end.nonEmpty) {
          positions = positions + (move.to -> cylId)
          updProdEnd(op, cylId)
        } else op
      }


    } else if (move.to.isEmpty) {
      // sink op
      if (op.end.isEmpty) {
        updProdStart(op, from)
      } else {
        updPos("", move.from, move.to)
        updProdEnd(op, from)
      }

    } else if (op.end.isEmpty && op.start.product.isEmpty && from.nonEmpty) {
      val prodID = from
      positions = positions + (move.to -> prodID)
      updProdStart(op, prodID)

    } else if (op.end.nonEmpty && op.start.product.nonEmpty) {
      val prodID = op.start.product.get
      if (move.from != move.to) positions = positions + (move.from -> "")
      updProdEnd(op, prodID)

    } else op

//    println(s"UPDPOS: ${op.start.name} - move:${move} - ($from, $to) - pos: (${positions(move.from)}, ${positions(move.to)})")
//    println(s"UPDPOS_OP: $res")

    res

  }

  def updPos(id: String, moveFrom: String, moveTo: String) = {
    val from = positions(moveFrom)
    val to = positions(moveTo)

    positions = positions + (moveFrom -> "") + (moveTo -> id) + ("" -> "")
  }

  def updProdStart(op: api.OP, prod: String) = op.copy(start = op.start.copy(product = Some(prod)))
  def updProdEnd(op: api.OP, prod: String) = op.copy(end = op.end.map(_.copy(product = Some(prod))))

  def lastTime(op: api.OP) = {
    new DateTime(op.end.getOrElse(op.start).time)
  }



}
