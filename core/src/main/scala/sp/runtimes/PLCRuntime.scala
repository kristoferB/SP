package sp.runtimes

import java.util.concurrent.TimeUnit
import akka.actor._
import akka.io.Tcp.{ConnectionClosed, Connected}
import akka.pattern.ask
import akka.util._
import sp.domain._
import sp.opc._
import sp.opc.simpleJsonMessToWeb._
import ServerSideEventsDirectives._
import sp.system.messages._
import spray.json._
import scala.concurrent.Await

class PLCRuntime(about: CreateRuntime) extends Actor {
  private implicit val to = Timeout(20, TimeUnit.SECONDS)
  import sp.system.SPActorSystem._

  case class OPC(name: String, hostname: String, port: Int, tagPrefix: String)

  var runtimeName: Option[String] = None
  var attachedModel: Option[ID] = None
  var attachedOPC: Option[OPC] = None

  var opcClient: Option[ActorRef] = None
  var opcHandler: Option[ActorRef] = None
  var sseChannels: List[ActorRef] = Nil
  var lastEventID: Int = 0

  def receive = {

    case SimpleMessage(_, attr) =>
      val reply = sender
      val action: Option[String] = attr.getAsString("action")

      action match {
        case Some("choosePLC") => choosePLC(attr, reply)
        case Some("connect") => connect(attr, reply)
        case Some("subscribe") => subscribe(attr, reply)
        case Some("getOpStates") =>
          opcHandler match {
            case Some(handler: ActorRef) => handler ? GetOpStates
            case None => SPError("Got a getOpStates request but has no OPCHandler to send it to.")
          }
        case Some("getVarStates") =>
          opcHandler match {
            case Some(handler: ActorRef) => handler ? GetVarStates
            case None => SPError("Got a getVarStates request request but has no OPCHandler to send it to.")
          }
        case None => reply ! SPError("Erroneous or missing property action: String")
        case _ => reply ! SPError("Could not match the supplied action: String")
      }
    case cr @ CreateRuntime(_, model, name, setting) =>
      // load things from the model here.
      // If needed return cr after load is complete
      println(cr)
      runtimeName = Some(name)
      model match {
        case x: ID =>
          attachedModel = Some(x)
          opcHandler = Some(context.actorOf(Props(new OPCHandler(self)), name + "OPCHandler"))
          sender ! cr
        case _ =>
          sender ! SPError("Missing or erroneous property model: ID")
          runtimeHandler ! StopRuntime(name)
      }
    case c @ Connected(host, local) => opcClient = Some(sender())
    case cc: ConnectionClosed => opcClient = None
    case ss: SubscribeToSSE => ss.channel :: sseChannels
    case GetRuntimes => sender ! about
    case ow @ (OPCWrite | OPCSubscribe) =>
      opcClient match {
        case Some(client: ActorRef) => client ! OPCWrite
        case None => SPError("You have to be connected to an OPC server in order to send write messages")
      }
    case ov @ (OPCValue | VarWrite | OpWrite) =>
      opcHandler match {
        case Some(handler: ActorRef) => handler ! ov
        case None => SPError("Got a message to OPCHandler but has no OPCHandler to send it to.")
      }
    case newState @ (OpState | VarState) => createEvent(newState)
    case _ => sender ! SPError("PLCRuntime: I don't understand this message.")
  }

  def createEvent(message: Any): Unit = {
    message match {
      case o: OpState => sendEvent(o.toJson.toString(), "newOpState")
      case v: VarState => sendEvent(v.toJson.toString(), "newVarState")
    }
  }

  def sendEvent(message: String, event: String): Unit = {
    for {
      channel <- sseChannels
    } yield {
      channel ! ServerSentEvent(message, event, lastEventID.toString)
    }
  }

  def subscribe(attr: SPAttributes, reply: ActorRef): Unit = {
    attachedModel match {
      case Some(m: ID) =>
        attachedOPC match {
          case Some(p: OPC) =>
            val opcDef: OpcDef = OPCDefinition.get(m, p.tagPrefix)
            opcHandler match {
              case Some(handler: ActorRef) => handler ! UpdateOpcSubscription(opcDef)
              case None => SPError("There is no OPCHandler actor attached to the runtime")
            }
          case None => reply ! SPError("You have to choose a PLC first")
        }
      case None => reply ! SPError("You have to choose a model first")
    }
  }

  def connect(attr: SPAttributes, reply: ActorRef): Unit = {
    attachedOPC match {
      case Some(p: OPC) =>
        import java.net.InetSocketAddress
        val remote: InetSocketAddress = new InetSocketAddress(p.hostname, p.port)
        runtimeName match {
          case Some(name) => context.actorOf(Props(new Client(remote, reply)), name + "opcClient")
          case None => reply ! SPError("The runtime has no have a name")
        }
      case None =>
        reply ! SPError("You have to choose a PLC first")
    }
  }

  def choosePLC(attr: SPAttributes, reply: ActorRef): Unit = {
    attachedModel match {
      case Some(m: ID) =>
        opcClient match {
          case None =>
            attr.getAsID("plcSpecID") match {
              case Some(plcSpecID: ID) =>
                Await.result(modelHandler ? GetIds(m, List(plcSpecID)), to.duration) match {
                  case Some(plcSpec: SPSpec) =>
                    val name: String = plcSpec.name
                    val ip: Option[String] = plcSpec.attributes.getAsString("ip")
                    val port: Option[Int] = plcSpec.attributes.getAsInt("port")
                    val tagPrefix: Option[String] = plcSpec.attributes.getAsString("tagPrefix")
                    (name, ip, port, tagPrefix) match {
                      case (n: String, Some(ip: String), Some(port: Int), Some(tagPrefix: String)) =>
                        attachedOPC = Some(OPC(name, ip, port, tagPrefix))
                        reply ! attachedOPC
                      case (_,None,_,_) => reply ! SPError("Erroneous or missing property ip: String")
                      case (_,_,None,_) => reply ! SPError("Erroneous or missing property port: Int")
                      case (_,_,_,None) => reply ! SPError("Erroneous or missing property tagPrefix: String")
                    }
                  case None => reply ! SPError("Failed to load the PLC specification")
                }
              case None => reply ! SPError("Erroneous or missing property plcSpecID: ID")
            }
          case Some(a: ActorRef) =>
            reply ! SPError("Please disconnect from the current PLC first")
        }
      case None => reply ! SPError("You have to choose a model first")
    }
  }


}

object PLCRuntime {
  def props(cr: CreateRuntime) = Props(classOf[SimulationRuntime], cr)
}