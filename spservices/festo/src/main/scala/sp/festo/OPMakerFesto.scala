package sp.festo

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
  case class Positions(positions: Map[String,String], time: DateTime) extends API




}

case class RawMess(state: Map[String, SPValue], time: String)

class OPMakerFesto extends PersistentActor with ActorLogging with OPMakerLogic with TrackProducts {
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
      persist(x)(fixTheOps)


  }

  var state: Map[String, SPValue] = Map()

  def fixTheOps(mess: String) = {
    val attr = SPValue.fromJson(mess)
    val rawMess = attr.flatMap(_.to[RawMess])

    if (rawMess.isEmpty) println("Nope, no Raw mess parsing")

    val updState = rawMess.map { mess =>


      state = state ++ mess.state
      val time = Try{new DateTime(mess.time)}.getOrElse(org.joda.time.DateTime.now)
      val updOps = makeMeOps(state, time, currentOps).map(updPositionsAndOps)
      println("NEW OPS")
      updOps.foreach(println(_))
      updOps.foreach(mediator ! Publish("ops", _))

      if(updOps.nonEmpty) {
        mediator ! Publish("pos", APIOPMaker.Positions(positions, postime))
      }

      currentOps = (currentOps ++ updOps.map(x => x.start.name -> x)).filter{case (k,v) => v.end.isEmpty }
      //println("ALL OPS")
      //currentOps.foreach(println(_))

    }
  }







  val baseTimeNow = org.joda.time.DateTime.now
  var baseTimeThen: Long = -1
  var lastms: Long = 0
  def receiveRecover = {
    case x: String =>
//      fixTheOps(x)
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

object OPMakerFesto {
  def props() = Props(classOf[OPMakerFesto])
}


trait NamesAndValues {
  // resources
  val b6 = "b6"
  val b14 = "b14"

  // operations
  val b6atStopRelease = "b6atStopRelease"
  val b14atStopRelease = "b14atStopRelease"

  // Positions
  val b6atStop         = "b6atStop"
  val b14atStop         = "b14atStop"

  // PLC variables in state
  val  b6xBG21 = "b6.Transport.xBG21"
  val  b6spSt1RFID = "b6.SPMapping.spSt1RFID"
  val  b14xBG21 = "b14.Transport.xBG21"
  val  b14spSt1RFID = "b14.SPMapping.spSt1RFID"
}

trait OPMakerLogic extends NamesAndValues{
  private var opCounter = 0

  def makeMeOps(state: Map[String, SPValue], time: DateTime, currentOps: Map[String, APIOPMaker.OP]) = {
    opCounter = opCounter + 1
    val ops = List(
      getOrMakeANew(b6atStopRelease,b6, time, currentOps, checkValue(state, List(b6xBG21 -> true)), state.get(b6spSt1RFID)),
      getOrMakeANew(b14atStopRelease,b14, time, currentOps, checkValue(state, List(b14xBG21 -> true)), state.get(b6spSt1RFID))
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
                    hasStarted: Boolean,
                    id: Option[SPValue]
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
        val start = op.start.copy(product = id.map(_.toJson))
        val end = APIOPMaker.OPEvent(start.name, time, start.id, start.resource, start.product)
        op.copy(start = start, end = Some(end), attributes =  op.attributes + ("duration"->duration))
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






  def makeTransportOps(name: String,
                       resource: String,
                       time: DateTime,
                       currentOps: Map[String, APIOPMaker.OP],
                       state: Map[String, SPValue],
                       startTrigger: Boolean,
                       idTagKey: String,
                       endKeys: String
                      ) = {
    val currOP = currentOps.get(name).orElse{
      if (startTrigger){
        val id = state.get(idTagKey)
        Some(APIOPMaker.OP(APIOPMaker.OPEvent(name, time, name+opCounter, resource, id.map(_.toJson)), None))
      } else None
    }
    currOP.map { op =>
      val id = op.start.product.flatMap(SPValue.fromJson(_))

    }
  }




  def getOrMakeANewStartStop(name: String,
                             resource: String,
                             time: DateTime,
                             currentOps: Map[String, APIOPMaker.OP],
                             start: Boolean,
                             stop: Boolean
                            ) = {


    //println("OP "+name + ": "+hasStarted)
    val currOP = currentOps.get(name).orElse{
      if (start){
        Some(APIOPMaker.OP(APIOPMaker.OPEvent(name, time, name+opCounter, resource, None), None))
      } else None
    }
    currOP.map{op =>
      if (stop) { // it has completed now
      val duration = op.start.time to time toDurationMillis
        val end = APIOPMaker.OPEvent(op.start.name, time, op.start.id, op.start.resource, op.start.product)
        op.copy(end = Some(end), attributes =  op.attributes + ("duration"->duration))
      } else op
    }
  }



  var prevState: Map[String, SPValue] = Map()
  def triggerFlank(state: Map[String, SPValue], variables: List[String]): Boolean = {
    val res = variables.forall( key => Try{state(key) != prevState(key)}.getOrElse(true))
    prevState = state
    res
  }

  def anyKeyHasValue(state: Map[String, SPValue], keys: List[String], value: SPValue): Boolean = {
    keys.exists(k => state.get(k).contains(value))
  }

  def valueIsInSet(state: Map[String, SPValue], key: String, values: Set[SPValue]): Boolean = {
    state.get(key).exists(values.contains)
  }



}



trait TrackProducts extends NamesAndValues {
  var prodID = 0

  var postime = org.joda.time.DateTime.now
  var positions: Map[String, String] = Map()

  case class OPMove(from: String, to: String)


  val opMovements: Map[String, OPMove] = Map()

  def updPositionsAndOps(op: APIOPMaker.OP) = {

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

  def updProdStart(op: APIOPMaker.OP, prod: String) = op.copy(start = op.start.copy(product = Some(prod)))
  def updProdEnd(op: APIOPMaker.OP, prod: String) = op.copy(end = op.end.map(_.copy(product = Some(prod))))

  def lastTime(op: APIOPMaker.OP) = {
    op.end.getOrElse(op.start).time
  }



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
    case x: Js.Str if n == "org.joda.time.DateTime" =>
      new DateTime(x.value).asInstanceOf[V]
    case Js.Obj(x@_*) if x.contains((tagName, Js.Str(n.split('.').takeRight(2).mkString(".")))) =>
      rw.read(Js.Obj(x.filter(_._1 != tagName):_*))
  }

  override def annotate[V: ClassTag](rw: Writer[V], n: String) = Writer[V]{
    case x: SPValue =>
      toUpickle(x)
    case x: org.joda.time.DateTime =>
      upickle.Js.Str(x.toString())
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
