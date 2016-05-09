package sp.psl

import sp.control.DBValue
import sp.control.BusAddress
import akka.actor._
import com.codemettle.reactivemq._
import com.codemettle.reactivemq.ReActiveMQMessages._
import com.codemettle.reactivemq.model._
import org.json4s.JsonAST.JBool
import org.json4s.JsonAST.JArray
import sp.domain.logic.IDAbleLogic
import sp.system.messages._
import sp.system._
import sp.domain._
import sp.domain.Logic._

import scala.util.Try


object OperatorInstructions extends SPService {
  val specification = SPAttributes(
    "service" -> SPAttributes(
      "group"-> "psl" // to organize in gui. maybe use "hide" to hide service in gui
    ),
    "setup" -> SPAttributes(
      "busIP" -> KeyDefinition("String", List(), Some("129.16.26.22")),
      "publishTopic" -> KeyDefinition("String", List(), Some("response")),
      "subscribeTopic" -> KeyDefinition("String", List(), Some("commands"))
    ),
    "command" -> KeyDefinition("String", List("connect", "disconnect", "done"), Some("connect")),
    "doneID" -> KeyDefinition("ID", List(), Some(SPValue(ID.newID)))
  )
  val transformTuple  = (
    TransformValue("setup", _.getAs[BusSetup]("setup")),
    TransformValue("command", _.getAs[String]("command")),
    TransformValue("doneID", _.getAs[ID]("doneID"))
  )
  val transformation = transformToList(transformTuple.productIterator.toList)
  def props(eventHandler: ActorRef) = Props(classOf[OperatorInstructions], eventHandler)
}

case class BusSetup(busIP: String, publishTopic: String, subscribeTopic: String)
case class OperatorInstructionData(id: ID, colors: List[String], textMessage: String)


// Add constructor parameters if you need access to modelHandler and ServiceHandler etc
class OperatorInstructions(eventHandler: ActorRef) extends Actor with ServiceSupport {
  import context.dispatcher
  val serviceID = ID.newID
  var theBus: Option[ActorRef] = None
  var setup: Option[BusSetup] = None
  var serviceName: Option[String] = None
  var subscriptions: Map[ID, DBValue] = Map()
  var run: Boolean = false // start
  var mode: Int = 0  // notReady, ready, executing, finished

  val addressPrefix = "operatorInstructions"

  def receive = {
    case r @ Request(service, attr, ids, reqID) => {
      // Always include the following lines. Are used by the helper functions
      val replyTo = sender()
      implicit val rnr = RequestNReply(r, replyTo)

      val s = transform(OperatorInstructions.transformTuple._1)
      val command= transform(OperatorInstructions.transformTuple._2)
      val core = r.attributes.getAs[ServiceHandlerAttributes]("core").get

      println("Command: " + command)

      command match {
        case "connect" =>
          setupBus(s, rnr)
        case "disconnect" =>
          disconnect()
        case "done" =>
          val id = transform(OperatorInstructions.transformTuple._3)
          sendDone(id)
        case _ =>
          sender ! SPError("Ill formed request");
      }
      replyTo ! Response(List(), connectedAttribute(), service, serviceID)
    }
    case ConnectionEstablished(request, c) => {
      println("OperatorInstructions connected to the bus:"+request)
      setup.foreach{ s=>
        c ! ConsumeFromTopic(s.subscribeTopic)
        theBus = Some(c)
        eventHandler ! Response(List(), connectedAttribute(), serviceName.get, serviceID)
        // ready!
        mode = 1
        sendState
      }
    }
    case ConnectionFailed(request, reason) => {
      println("failed:"+reason)
    }
    case mess @ AMQMessage(body, prop, headers) => {
      println(s"new instruction on the bus!!!!!!!")
      val resp = SPAttributes.fromJson(body.toString).getOrElse(SPAttributes())

      resp.getAs[String]("command") match {
        case Some("subscribe") =>
          println("OperatorInstructions: got subscribe command -- " + resp)
          resp.getAs[List[DBValue]]("dbs").foreach { oi =>
            oi.map { dbv =>
              println("dbv: " + dbv);
              dbv.address.to[BusAddress] match {
                case Some(a) => { println("subscribing..."); subscriptions += (dbv.id -> dbv) }
                case _ => // skip these
              }
            }
            // reply with updated state
            sendState
          }
        case Some("write") =>
          println("OperatorInstructions: got write command -- " + resp)
          // set variables... (only run exist. hack it)
          val newRun = for {
            db <- resp.getAs[List[DBValue]]("dbs").getOrElse(List())
            r <- db.address.to[BusAddress] if r.name == addressPrefix + ".run"
            v <- db.value.to[Boolean]
          } yield {
            v
          }

          val newP = for {
            db <- resp.getAs[List[DBValue]]("dbs").getOrElse(List())
            r <- db.address.to[BusAddress] if r.name == addressPrefix + ".brickPositions"
            v <- db.value.to[List[Brick]]
          } yield {
            v
          }

          if(newRun.nonEmpty && newP.nonEmpty) {
            // set run
            run = newRun.foldLeft(true)(_&&_)
            if(run) mode = 2 // run flag set, executing until we are done
            else mode = 1 // run flag reset, ready to start again
            sendState // send new state

            // send instructions to ui (parameter)
            val p = newP.flatten

            println("new run: " + run + " new parameters: " + p);
            eventHandler ! Response(List(), SPAttributes("operatorInstructions"->p, "silent"->true), serviceName.get, serviceID)
          }
        case x@_ =>
          println("Unexpected " + x + " from " + body.toString)
      }
    }

    case ConnectionInterrupted(ca, x) => {
      println("connection closed")
      setup = None
    }
    case x => {
      println("no match for message: "+x)
    }
  }

  def sendState = {
    val dbs = subscriptions.map { case (k,v) =>
      val value = v.address.to[BusAddress] match {
        case Some(addr) if addr.name == addressPrefix + ".run" => SPValue(run)
        case Some(addr) if addr.name == addressPrefix + ".mode" => SPValue(mode)
        case s@_ => {
          println("subscription does not exist: " + s)
          SPValue(false)
        }
      }
      SPAttributes("id" -> k, "value" -> value)
    }.toList
    val mess = SPAttributes("dbs" -> dbs)
    sendMessage(mess)
  }

  def setupBus(s: BusSetup, rnr: RequestNReply) = {
      setup = Some(s)
      serviceName = Some(rnr.req.service)
      println(s"connecting to $s")
      ReActiveMQExtension(context.system).manager ! GetConnection(s"nio://${s.busIP}:61616")
  }

  def connectedAttribute() = {
    if (setup.isEmpty)
      SPAttributes("theBus"->"Not connected") merge SPAttributes("silent"->true)
    else if (theBus.isEmpty)
      SPAttributes("theBus"->"Connecting") merge SPAttributes("silent"->true)
    else
      SPAttributes("theBus"->"Connected") merge SPAttributes("silent"->true)
  }

  def disconnect() = {
    println("disconnecting from the bus")
    theBus.foreach(_ ! CloseConnection)
    this.setup = None
    this.theBus = None
  }

  def sendMessage(mess: SPAttributes) = {
    for {
      bus <- theBus
      s <- setup
    } yield {
      println(s"sending: ${mess.toJson}")
      bus ! SendMessage(Topic(s.publishTopic), AMQMessage(mess.toJson))
    }
  }

  def sendDone(id: ID) = {
    mode = 3 // completed
    sendState
  }

  override def postStop() = {
    println("post stop, disconnect...")
    disconnect()
  }
}
