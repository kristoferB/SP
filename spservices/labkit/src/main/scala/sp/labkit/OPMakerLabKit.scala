package sp.labkit

import akka.actor._
import akka.cluster.pubsub.DistributedPubSubMediator.Publish

import scala.concurrent.duration._
import akka.persistence._
import sp.domain._
import sp.domain.Logic._
import org.threeten.bp._

import scala.util.{Failure, Success, Try}


object APIOPMaker {
  sealed trait API
  object API {
    implicit lazy val readResp2 = deriveReadISA[APIOPMaker_OPEvent]
    implicit lazy val writeResp2 = deriveWriteISA[APIOPMaker_OPEvent]
    implicit lazy val readResp = deriveReadISA[API]
    implicit lazy val writeResp = deriveWriteISA[API]
  }
}

  case class APIOPMaker_OPEvent(name: String, time: ZonedDateTime, id: String, resource: String, product: Option[String]) extends APIOPMaker.API
  case class APIOPMaker_OP(start: APIOPMaker_OPEvent, end: Option[APIOPMaker_OPEvent], attributes: SPAttributes = SPAttributes()) extends APIOPMaker.API
  case class APIOPMaker_Positions(positions: Map[String,String], time: ZonedDateTime) extends APIOPMaker.API

case class RawMess(state: Map[String, SPValue], time: String)


class OPMakerLabKit extends PersistentActor with ActorLogging with OPMakerLogic with TrackProducts {
  override def persistenceId = "rawPLC"
  import context.dispatcher

  import akka.cluster.pubsub._
  import DistributedPubSubMediator.{ Put, Subscribe }
  val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe("raw", self)
  mediator ! Put(self)

  //private var state: Map[String, SPValue] = Map()
  private var currentOps: Map[String, APIOPMaker_OP] = Map()


  implicit lazy val rawMessF: JSFormat[RawMess] = deriveFormatSimple[RawMess]

  def receiveCommand = {
    case mess @ _ if {println(s"OPMaker got: $mess from $sender"); false} => Unit

    case x: String =>
      persist(x)(fixTheOps)


  }

  def fixTheOps(mess: String) = {
    val attr = SPValue.fromJson(mess)
    val rawMess = attr.flatMap(_.to[RawMess])

    if (rawMess.isFailure) println("Nope, no Raw mess parsing")

    val updState = rawMess.map { mess =>


      val time = Try{ZonedDateTime.parse(mess.time)}.getOrElse(ZonedDateTime.now)
      val updOps = makeMeOps(mess.state, time, currentOps).map(updPositionsAndOps)
      println("NEW OPS")
      updOps.foreach(println(_))
      updOps.foreach(mediator ! Publish("ops", _))

      if(updOps.nonEmpty) {
        mediator ! Publish("pos", APIOPMaker_Positions(positions, postime))
      }

      currentOps = (currentOps ++ updOps.map(x => x.start.name -> x)).filter{case (k,v) => v.end.isEmpty }
      //println("ALL OPS")
      //currentOps.foreach(println(_))

    }
  }







  val baseTimeNow = org.joda.time.DateTime.now
  var baseTimeThen: Long = -1
  var lastms: Long = 0
  var testRec: List[String] = List()
  def receiveRecover = {
    case x: String =>
      testRec = x :: testRec
//      fixTheOps(x)
//      val attr = SPValue.fromJson(x)
//      val rawMess = attr.flatMap(_.to[RawMess])
//      if(rawMess.nonEmpty && rawMess.get.state.nonEmpty && rawMess.get.time.nonEmpty) {
//        val ms = new DateTime(rawMess.get.time).getMillis()
//        if(baseTimeThen == -1) baseTimeThen = ms // init replay time
//        val msOfExecution = ms - baseTimeThen
//        val sleep = msOfExecution - lastms
//        Thread.sleep(sleep)
//        lastms = msOfExecution
//        val rawFixedTime = RawMess(rawMess.get.state, baseTimeNow.plusMillis(msOfExecution.intValue()).toString)
//        fixTheOps(SPValue(rawFixedTime).toJson.toString)
    // }

    case RecoveryCompleted =>
      println("*********************************")
      println("*********************************")
      println("*********************************")
      testRec = testRec.reverse
      val s: Map[String, SPValue] = extrState(testRec.head)
      println("hhh")


      case class TimeStampedValue(value: SPValue, time: SPValue)
      val t = s("time")
      var aggr:  Map[String, List[TimeStampedValue]] = s.map(kv => kv._1 -> List(TimeStampedValue(kv._2, t)))
     
      
      // println(SPAttributes(s.toList).toJson)
      testRec.foldLeft(s){(a, b) => {
        val next = extrState(b)
        val f = next.filter{
          case (key, v) =>
               a.contains(key) && a(key) != v || key == "time"
        }

          //println(SPAttributes(f.toList).toJson)
        val upd = f.map(kv => kv._1 -> (aggr(kv._1) :+ TimeStampedValue(kv._2, f("time"))))
        aggr = aggr ++ upd

        a ++ f
      }



      }
      aggr.map(println)

      println("*********************************")
      println("*********************************")
      println("*********************************")


      println("recover done")
    case x => println("hej: "+x)
  }

  def extrState(x: String): Map[String, SPValue] = {
    val t = SPAttributes.fromJsonGet(x, "time").getOrElse(SPValue(""))
    SPAttributes.fromJsonGetAs[Map[String, SPValue]](x, "state").map(_ + ("time" -> t)).getOrElse(Map())
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
}

trait OPMakerLogic extends NamesAndValues{
  private var opCounter = 0

  def makeMeOps(state: Map[String, SPValue], time: ZonedDateTime, currentOps: Map[String, APIOPMaker_OP]) = {
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
                    time: ZonedDateTime,
                    currentOps: Map[String, APIOPMaker_OP],
                    hasStarted: Boolean
                   ) = {


    //println("OP "+name + ": "+hasStarted)
    val currOP = currentOps.get(name).orElse{
      if (hasStarted){
        Some(APIOPMaker_OP(APIOPMaker_OPEvent(name, time, name+opCounter, resource, None), None))
      } else None
    }
    currOP.map{op =>
      if (!hasStarted) { // it has completed now
      val duration = org.threeten.bp.Duration.between(op.start.time, time).toMillis  //op.start.time to time toDurationMillis
        val end = APIOPMaker_OPEvent(op.start.name, time, op.start.id, op.start.resource, op.start.product)
        op.copy(end = Some(end), attributes =  op.attributes + ("duration"->SPValue(duration)))
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

  var postime = ZonedDateTime.now
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

  def updPositionsAndOps(op: APIOPMaker_OP) = {

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

  def updProdStart(op: APIOPMaker_OP, prod: String) = op.copy(start = op.start.copy(product = Some(prod)))
  def updProdEnd(op: APIOPMaker_OP, prod: String) = op.copy(end = op.end.map(_.copy(product = Some(prod))))

  def lastTime(op: APIOPMaker_OP) = {
    op.end.getOrElse(op.start).time
  }



}

