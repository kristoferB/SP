package sp.labkit

import akka.actor._
import akka.cluster.pubsub.DistributedPubSubMediator.Publish

import scala.concurrent.duration._
import akka.persistence._
import sp.domain._
import sp.domain.Logic._
import sp.messages._
import org.joda.time._

import scala.util.{Failure, Success, Try}
import com.github.nscala_time.time.Imports._


object APIOPMaker {
  sealed trait API
  case class OPEvent(name: String, time: DateTime, id: String, resource: String, product: Option[String]) extends API
  case class OP(start: OPEvent, end: Option[OPEvent], attributes: SPAttributes = SPAttributes()) extends API
  case class Positions(positions: Map[String,String]) extends API
}

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
  private var currentOps: Map[String, APIOPMaker.OP] = Map()


  def receiveCommand = {
    case mess @ _ if {println(s"OPMaker got: $mess from $sender"); false} => Unit

    case x: String =>
      val attr = SPValue.fromJson(x)
      val rawMess = attr.flatMap(_.to[RawMess])

      if (rawMess.isEmpty) println("Nope, no Raw mess parsing")

      val updState = rawMess.map { mess =>
        // persistAsync(x){mess => } // for playing back later



        val updOps = makeMeOps(mess.state, new DateTime(mess.time), currentOps).map(updPositionsAndOps)
        println("NEW OPS")
        updOps.foreach(println(_))
        updOps.foreach(mediator ! Publish("ops", _))

        if(updOps.nonEmpty) {
          mediator ! Publish("pos", APIOPMaker.Positions(positions))
        }

        currentOps = (currentOps ++ updOps.map(x => x.start.name -> x)).filter{case (k,v) => v.end.isEmpty }
        println("ALL OPS")
        currentOps.foreach(println(_))

      }


  }






  def receiveRecover = {
    case x: String =>
      println("recover states")
    case RecoveryCompleted =>


  }



}

object OPMakerLabKit {
  def props() = Props(classOf[ProductAggregator])
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

  def makeMeOps(state: Map[String, SPValue], time: DateTime, currentOps: Map[String, APIOPMaker.OP]) = {
    opCounter = opCounter + 1
    val ops = List(
      getOrMakeANew(feedCylinder,feeder, time, currentOps, checkValue(state, List(feeder_exec -> true, newCylinder_var -> true))),

      getOrMakeANew(fromFeedToP1,pnp1,time, currentOps, checkValue(state, List(pnp1_mode -> 2, pnp1from_var -> 3, pnp1to_var -> 1))),
      getOrMakeANew(fromFeedToC,pnp1, time, currentOps, checkValue(state, List(pnp1_mode -> 2, pnp1from_var -> 3, pnp1to_var -> 5))),
      getOrMakeANew(fromP1ToC,pnp1, time, currentOps, checkValue(state, List(pnp1_mode -> 2, pnp1from_var -> 1, pnp1to_var -> 5))),

      getOrMakeANew(transport,conv, time, currentOps, checkValue(state, List(convMove_var -> true))),

      getOrMakeANew(to3,pnp2, time, currentOps, checkValue(state, List(pnp2_mode -> 2, pnp2to3_var -> true))),
      getOrMakeANew(to4,pnp2, time, currentOps, checkValue(state, List(pnp2_mode -> 2, pnp2to4_var -> true))),

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
                    currentOps: Map[String, APIOPMaker.OP],
                    hasStarted: Boolean
                   ) = {


    //println("OP "+name + ": "+hasStarted)
    val currOP = currentOps.get(name).orElse{
      if (hasStarted){
        Some(APIOPMaker.OP(APIOPMaker.OPEvent(name, time, name+opCounter, resource, None), None))
      } else None
    }
    currOP.map{op =>
      if (!hasStarted) { // it has completed now
      val duration = op.start.time to time toDurationMillis
        val end = APIOPMaker.OPEvent(op.start.name, time, op.start.id, op.start.resource, op.start.product)
        op.copy(end = Some(end), attributes =  op.attributes + ("duration"->duration))
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

  case class OPMove(from: String, to: String, via:String = "")

  val opMovements = Map(
    feedCylinder  -> OPMove("", inLoader),
    fromFeedToP1  -> OPMove(inLoader, inP1, inPnp1),
    fromFeedToC   -> OPMove(inLoader, inConvIn, inPnp1),
    fromP1ToC     -> OPMove(inP1, inConvIn, inPnp1),
    transport     -> OPMove(inConvIn, inConvOut),
    to3           -> OPMove(inConvOut, inP3, inPnp2),
    to4           -> OPMove(inConvOut, inP4, inPnp2),
    p3move        -> OPMove(inP3, ""),
    p4move        -> OPMove(inP4, "")
  )

  def updPositionsAndOps(op: APIOPMaker.OP) = {
    val res = opMovements.get(op.start.name).map { move =>
      val from = positions(move.from)

      if (move.from.isEmpty) {
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
        if (op.end.isEmpty ){
          updProdStart(op, from)
        } else {
          updPos(move.from, move.to)
          updProdEnd(op, from)
        }

      } else if (op.end.isEmpty ) {
        if (move.via.nonEmpty) updPos(move.from, move.via)
        updProdStart(op, from)
      } else if (op.end.nonEmpty) {
        if (move.via.nonEmpty) updPos(move.via, move.to)
        else updPos(move.from, move.to)
        updProdEnd(op, positions(move.to))

      } else op
    }
    res.getOrElse(op)
  }

  def updPos(moveFrom: String, moveTo: String) = {
    val from = positions(moveFrom)
    val to = positions(moveTo) // Should be empty!
    if (to.nonEmpty) println("WE HAVE OVERWRITTEN A POSITION")
    positions = positions + (moveTo -> from) + (moveFrom -> "") + ("" -> "")
  }

  def updProdStart(op: APIOPMaker.OP, prod: String) = op.copy(start = op.start.copy(product = Some(prod)))
  def updProdEnd(op: APIOPMaker.OP, prod: String) = op.copy(end = op.end.map(_.copy(product = Some(prod))))





}








// Will be in the domain later


import upickle._
import scala.reflect.ClassTag
import org.json4s.JsonAST
object APIParser extends upickle.AttributeTagged {
  override val tagName = "isa"

  import sp.domain.Logic._

  override def annotate[V: ClassTag](rw: Reader[V], n: String) = Reader[V]{
    case x: Js.Obj if n == "org.json4s.JsonAST.JObject" =>
      val res = x.value.map(kv => kv._1 -> fromUpickle(kv._2))
      SPAttributes(res:_*).asInstanceOf[V]
    case Js.Obj(x@_*) if x.contains((tagName, Js.Str(n.split('.').takeRight(2).mkString(".")))) =>
      rw.read(Js.Obj(x.filter(_._1 != tagName):_*))
  }

  override def annotate[V: ClassTag](rw: Writer[V], n: String) = Writer[V]{
    case x: SPValue =>
      toUpickle(x)
    case x: V =>
      val filter = n.split('.').takeRight(2).mkString(".")
      Js.Obj((tagName, Js.Str(filter)) +: rw.write(x).asInstanceOf[Js.Obj].value:_*)
  }


  def toUpickle(value: SPValue): upickle.Js.Value = value match {
    case x: JsonAST.JBool => upickle.default.writeJs(x.values)
    case x: JsonAST.JDecimal => upickle.default.writeJs(x.values)
    case x: JsonAST.JDouble => upickle.default.writeJs(x.values)
    case x: JsonAST.JInt => upickle.default.writeJs(x.values)
    case x: JsonAST.JLong => upickle.default.writeJs(x.values)
    case x: JsonAST.JString => upickle.default.writeJs(x.values)
    case x: JsonAST.JObject =>
      val res = x.obj.map(kv => kv._1 -> toUpickle(kv._2))
      upickle.Js.Obj(res:_*)
    case x: JsonAST.JArray => upickle.Js.Arr(x.arr.map(toUpickle):_*)
    case x => upickle.Js.Null
  }
  def fromUpickle(value: upickle.Js.Value): SPValue = {
    val json = upickle.json.write(value)
    SPValue.fromJson(json).getOrElse(SPValue("ERROR_UPICKLE"))
  }



}
