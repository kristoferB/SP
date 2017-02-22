package sp.robotServices

import java.text.SimpleDateFormat

import akka.actor.Actor.Receive
import akka.actor._
import akka.cluster.pubsub.DistributedPubSubMediator._
import akka.persistence._
import org.joda.time.{DateTime, Duration}
import org.json4s.DefaultFormats
import sp.domain._
import sp.labkit.{ OperationFinished, OperationStarted }
import sp.robotServices.core.Domain._

/**
  * Created by ashfaqf on 2/16/17.
  */

trait opState
case object init extends opState
case object executing extends opState
case object finished extends opState
trait relations
case object sequence extends relations
case object parallel extends relations
case object hierarchy extends relations


class RelationFinder extends Actor{
  val customDateFormat = new DefaultFormats {
    override def dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
  }
  implicit val formats = customDateFormat ++ org.json4s.ext.JodaTimeSerializers.all

  var opStatus: Map[String, opState] = Map.empty
  var relMap: Map[String, Map[relations, Int]] = Map.empty
  var opCnt: Map[String, Int] = Map.empty

  import akka.cluster.pubsub._
  import DistributedPubSubMediator.{ Put, Subscribe }
  val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe("robotServices", self)
  mediator ! Put(self)
  println("relationFinder")

  def receive = {
    case mess @ _ if {println(s"RelationFinder MESSAGE: $mess from $sender"); false} => Unit

    case "printRels" => // relMap.foreach { case(name, map) => map match{ //
    //   case r => if(r.contains(parallel)) println(name)                 //
    // } }//

      println(relMap)
    case x: String =>
      val currentOp = SPValue.fromJson(x).get.extract[ActivityEventWithRobotCycle]
      updateStatus(currentOp)
      updateRelMap(currentOp)
      opCounter(currentOp)

  }

  def opCounter(op:ActivityEventWithRobotCycle): Unit = {
    val cnt = opCnt.getOrElse(op.name,0)
    if(op.isStart){
      val tmp = cnt + 1
      opCnt += (op.name -> tmp)
      mediator ! Publish("frontend", OperationStarted(tmp.toString,op.name,"", op.`type`,op.time.toString))
      }else
         mediator ! Publish("frontend", OperationFinished(cnt.toString,op.name,"", op.`type`,op.time.toString))
  }

  def updateRelMap(op: ActivityEventWithRobotCycle): Unit = {
    def getRelTo(annanOp:String):Option[relations]={
      opStatus(annanOp) match {
        //case `init` => println("we wont be here")
        //  sequence
        case `executing` => Some(parallel)
        case `finished` => if(op.isStart) Some(sequence) else None
        }
    }
    opStatus.keys.foreach{unqOp =>
      val rel = getRelTo(unqOp)
      rel match {
        case Some(r) =>if (!relMap.contains(op.name+unqOp) )
        relMap+= (op.name+unqOp -> Map(r -> 1))
      else{
        val updRelCtr = r -> (relMap(op.name+unqOp).getOrElse(r,0) + 1)
        val updateRel = relMap(op.name+unqOp) + updRelCtr
        relMap += (op.name+unqOp -> updateRel)
      }
        case None => 

      }
      
    }
  }
  def updateStatus(op: ActivityEventWithRobotCycle): Unit ={
    def getState : opState ={
      if (op.isStart)
        executing
      else
        finished
    }
    opStatus += (op.name -> getState)
  }
}

object RelationFinder {
  def props() = Props(classOf[RelationFinder])
}


