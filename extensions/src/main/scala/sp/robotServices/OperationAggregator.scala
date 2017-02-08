package sp.robotServices

import java.text.SimpleDateFormat

import akka.actor._
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import akka.persistence._
import org.joda.time.{DateTime, Duration}
import org.json4s.DefaultFormats
import sp.domain._
import sp.robotServices.core.Domain._




case class Resources(robots: Set[String])
case class OpInstance(RobotCylNo: Int, start: Option[DateTime], stop: Option[DateTime], duration: Option[Duration], opId: String, opType: String)
//opInstances: Map[activityId, opInstance]
case class Op(name:String, opInstances: Map[String,OpInstance], robotId: String)


class OperationAggregator extends PersistentActor with ActorLogging{
  override def persistenceId = "robotServices"
  val customDateFormat = new DefaultFormats {
    override def dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
  }
  implicit val formats = customDateFormat ++ org.json4s.ext.JodaTimeSerializers.all
  import context.dispatcher

  import akka.cluster.pubsub._
  import DistributedPubSubMediator.{ Put, Subscribe }
  val mediator = DistributedPubSub(context.system).mediator
  mediator ! Subscribe("robotServices", self)
  mediator ! Put(self)

  private var opMap : Map[String, Op] = Map()
  private var resources: Resources = Resources(Set.empty)


  def receiveCommand = {
    case mess @ _ if{/*println(s"OpAgg got: $mess from $sender");*/ false} => Unit

    case "printOps" => opMap.values.foreach(println(_))
    case x: String =>
      persist(x)(updOps)
  }

  def updOps(mess: String ) = {
    val event = SPValue.fromJson(mess).get.extract[ActivityEventWithRobotCycle]
    opMap +=  updOp(event)
    resources = Resources(updResources(event.robotId,resources.robots))
    opMap = calcDurations
   // sendToFrontEnd(event)
  }


  def calcDurations ={
    def calcDur(start:Option[DateTime],stop:Option[DateTime]): Option[Duration] =
    {
      if (start.isDefined && stop.isDefined){
        Some(Duration.millis(stop.get.getMillis - start.get.getMillis))
      }
      else None
    }
    opMap.map{
      case (opName,op) => opName -> op.copy(opInstances = op.opInstances.map{
        case (activityid,instance) => activityid -> instance.copy(duration = calcDur(instance.start,instance.stop))
      })
    }
   // opMap.values.map(ops => ops.opInstances.map(instance => instance._2.copy(duration = calcDur(instance._2.start,instance._2.stop))))
  }
  def updResources(robotiId: String, knownRobots: Set[String]) ={
    if(!knownRobots.contains(robotiId))
      knownRobots + robotiId
    else
      knownRobots
  }


  def updOp(event: ActivityEventWithRobotCycle) ={
    def createNewInstance = {
      OpInstance(event.cycleId, start = if (event.isStart) Some(event.time) else None, stop = if (!event.isStart) Some(event.time) else None, duration = None,opId = event.activityId, opType = event.`type`)
    }

    def updOpInstance(opInst: Map[String,OpInstance])  ={
        if(!opInst.contains(event.activityId)){
          createNewInstance
        }
        else {
          if(event.isStart)
            opInst(event.activityId).copy(start = Some(event.time))
          else
            opInst(event.activityId).copy(stop = Some(event.time))
        }
    }
    def createNewOp ={
      val opInst = createNewInstance
      Op(event.name, Map(event.activityId -> opInst), event.robotId )
    }


    if(!opMap.contains(event.name)){
      event.name -> createNewOp
    }
    else {
      val opInst = opMap(event.name).opInstances
      val updInstance = opInst + (event.activityId -> updOpInstance(opInst))
      event.name -> opMap(event.name).copy(opInstances = updInstance)
    }

  }

  override def receiveRecover = {
    case x: String => updOps(x)
    case RecoveryCompleted =>
      println("recoveryDone")
    case x => println("Hej" +x)
  }

}

object OperationAggregator {
  def props() = Props(classOf[OperationAggregator])
}

