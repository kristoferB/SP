package sp.runtimes

import java.util.concurrent.TimeUnit
import akka.actor._
import akka.io.Tcp.{Connect, CommandFailed, ConnectionClosed, Connected}
import akka.pattern.ask
import akka.util._
import sp.domain._
import sp.opc._
import ServerSideEventsDirectives._
import sp.system.messages._
import spray.http.StatusCodes
import spray.json._
import simpleJsonMessToWeb._
import DefaultJsonProtocol._
import scala.concurrent.{Future, Await}
import simpleJsonMessToWeb._
import scala.concurrent.ExecutionContext.Implicits.global

class PLCRuntime(about: CreateRuntime) extends Actor {
  private implicit val to = Timeout(2, TimeUnit.SECONDS)
  import sp.system.SPActorSystem._

  case class OPC(name: String, hostname: String, port: Int, tagPrefix: String, idsToTags: Map[Option[ID], Option[String]])

  var runtimeName: Option[String] = None
  var attachedModel: Option[ID] = None
  var attachedOPCSpec: Option[ID] = None

  var opcClient: Option[ActorRef] = None
  var opcHandler: Option[ActorRef] = None
  var sseChannels: List[ActorRef] = Nil
  var lastEventID: Int = 0

  def receive = {

    case SimpleMessage(_, attr) =>
      val reply = sender
      val action: Option[String] = attr.getAsString("action")

      action match {
        case Some("chooseOPC") => chooseOPC(attr, reply)
        case Some("connect") => connect(reply)
        case Some("disconnect") => disconnect(attr, reply)
        case Some("subscribe") => subscribe(reply)
        case Some("opWrite") => writeOp(attr, reply)
        case Some("varWrite") => writeVar(attr, reply)
        case Some("getRuntimeState") => reply ! getRuntimeState
        case None => reply ! SPError("Erroneous or missing property action: String")
        case _ => reply ! SPError("Could not match the supplied action: String")
      }
    case cr @ CreateRuntime(_, model, name, setting) =>
      // load things from the model here.
      // If needed return cr after load is complete
      println(cr)
      println("A creation message has reached the PLC runtime.")
      runtimeName = Some(name)
      model match {
        case x: ID =>
          attachedModel = Some(x)
          opcHandler = Some(context.actorOf(Props(new OPCHandler(self)), name + "OPCHandler"))
          println("Model and OPC Handler attached.")
          sender ! cr
        case _ =>
          println("The model is not an ID")
          sender ! SPError("Missing or erroneous property model: ID")
          runtimeHandler ! StopRuntime(name)
      }
    case c @ Connected(host, local) =>
      opcClient = Some(sender())
      val data = ConnectionInfo(host.getHostName, host.getPort)
      sendEvent(data.toJson.toString(), "Connected")
      println("Connection established")
      subscribe()
    case cf @ CommandFailed(c: Connect) =>
      val data = ConnectionInfo(c.remoteAddress.getHostName, c.remoteAddress.getPort)
      sendEvent(data.toJson.toString(), "CommandFailed")
      println("Connection to " + c.remoteAddress.getHostName + ":" + c.remoteAddress.getPort + " failed")
    case cc: ConnectionClosed =>
      opcClient = None
      sendEvent("", "ConnectionClosed")
      println("Connection closed")
    case ss: SubscribeToSSE =>
      sseChannels = ss.channel :: sseChannels
      println("Added a new SSE channel")
    case GetRuntimes => sender ! about
    case ow @ (OPCWrite(_) | OPCSubscribe(_)) =>
      opcClient match {
        case Some(client: ActorRef) =>
          ow match {
            case os: OPCSubscribe => sendEvent(getRuntimeState.toJson.toString(), "Subscribed")
            case _ =>
          }
          client ! ow
        case None => SPError("You have to be connected to an OPC server in order to send write messages")
      }
    case ov @ (OPCValue(_, _) | VarWrite(_, _) | OpWrite(_, _, _)) =>
      opcHandler match {
        case Some(handler: ActorRef) => handler ! ov
        case None => SPError("Got a message to OPCHandler but has no OPCHandler to send it to.")
      }
    case os: OpState => sendEvent(os.toJson.toString(), "NewOpState")
    case vs: VarState => sendEvent(vs.toJson.toString(), "NewVarState")
    case oe: OPCError => sendEvent(oe.e.head, "Error")
    case e: SPErrorString => sendEvent(e.error, "Error")
    case StatusCodes.OK =>
    case e @ _ =>
      println("PLCRuntime: I couldn't match the following message:")
      println(e)
  }

  def writeOp(attr: SPAttributes, reply: ActorRef): Unit = {
    val id: Option[ID] = attr.getAsID("id")
    val start: Option[Boolean] = attr.getAsBool("start")
    val reset: Option[Boolean] = attr.getAsBool("reset")
    (id, start, reset) match {
      case (Some(i: ID), Some(s: Boolean), Some(r: Boolean)) =>
        opcHandler match {
          case Some(handler: ActorRef) => handler ! OpWrite(i.toString(), JsBoolean(s), JsBoolean(r))
          case None => reply ! SPError("There is no OPCHandler actor attached to the runtime")
        }
        reply ! StatusCodes.OK
      case _ => reply ! SPError("Wrong or missing ID, start or reset property for action writeOp.")
    }
  }

  def writeVar(attr: SPAttributes, reply: ActorRef): Unit = {
    val id: Option[ID] = attr.getAsID("id")
    val intValue: Option[Int] = attr.getAsInt("value")
    val boolValue: Option[Boolean] = attr.getAsBool("value")
    opcHandler match {
      case Some(handler: ActorRef) =>
        (id, intValue, boolValue) match {
          case (Some(i: ID), Some(iv: Int), _) =>
            handler ! VarWrite(i.toString(), JsNumber(iv))
            reply ! StatusCodes.OK
          case (Some(i: ID), _, Some(bv: Boolean)) =>
            handler ! VarWrite(i.toString(), JsBoolean(bv))
            reply ! StatusCodes.OK
          case _ => reply ! SPError("Wrong or missing ID or value property for action writeVar.")
        }
      case None => reply ! SPError("There is no OPCHandler actor attached to the runtime")
    }

  }

  def sendEvent(data: String, eventName: String): Unit = {
    for {
      channel <- sseChannels
    } yield {
      channel ! ServerSentEvent(data, eventName, lastEventID.toString)
    }
  }

  def getRuntimeState: RuntimeState = {
    val opcSpecID: String = attachedOPCSpec match {
      case Some(id: ID) => id.toString()
      case None => ""
    }
    val connected: Boolean = opcClient match {
      case Some(a: ActorRef) => true
      case None => false
    }
    val (opStates: Map[String, OpState], varStates: Map[String, VarState]) = opcHandler match {
      case Some(handler: ActorRef) =>
        val os: Map[String, OpState] = Await.result(handler ? GetOpStates, to.duration).asInstanceOf[Map[String, OpState]]
        val vs: Map[String, VarState] = Await.result(handler ? GetVarStates, to.duration).asInstanceOf[Map[String, VarState]]
        (os, vs)
      case None =>
        SPError("Got a getOpStates request but has no OPCHandler to send it to.")
        (Map[String, OpState](), Map[String, VarState]())
    }
    RuntimeState(opcSpecID, connected, opStates.values.toList, varStates.values.toList)
  }

  def subscribe(reply: ActorRef = self): Unit = {
    attachedModel match {
      case Some(model: ID) =>
        attachedOPCSpec match {
          case Some(opcSpecID: ID) =>
            Await.result(modelHandler ? GetIds(model, List(opcSpecID)), to.duration) match {
              case SPIDs(List(opcSpec: SPSpec)) =>
                val tagPrefix: Option[String] = opcSpec.attributes.getAsString("tagPrefix")
                val idsToTagsRaw: Option[Map[String, SPAttributeValue]] = opcSpec.attributes.getAsMap("idsToTags")
                idsToTagsRaw match {
                  case Some(m: Map[String, SPAttributeValue]) =>
                    val idsToTags: Map[Option[ID], Option[String]] = m map {case (id, tag) => (ID.makeID(id), tag.asString)}
                    tagPrefix match {
                      case Some(tagPrefix: String) =>
                        val opcDef: OpcDef = OPCDefinition.get(model, tagPrefix, idsToTags, reply)
                        opcHandler match {
                          case Some(handler: ActorRef) =>
                            opcClient match {
                              case Some(client: ActorRef) =>
                                handler ! UpdateOpcSubscription(opcDef)
                                reply ! StatusCodes.OK
                              case None => reply ! SPError("Subscription error: The runtime first has to be connected to a OPC Server")
                            }
                          case None => reply ! SPError("Subscription error: There is no OPCHandler actor attached to the runtime")
                        }
                      case None => reply ! SPError("Subscription error: Erroneous or missing property tagPrefix: String")
                    }
                  case _ => reply ! SPError("Subscription error: idsToTags consists of other types than [String, StringPrimitive]")
                }
              case _ => reply ! SPError("Subscription error: Failed to load the PLC specification")
            }
          case None => reply ! SPError("Subscription error: You have to choose an OPC first")
        }
      case None => reply ! SPError("Subscription error: You have to choose a model first")
    }
  }

  def connect(reply: ActorRef = self): Unit = {
    attachedModel match {
      case Some(model: ID) =>
        attachedOPCSpec match {
          case Some(opcSpecID: ID) =>
            Await.result(modelHandler ? GetIds(model, List(opcSpecID)), to.duration) match {
              case SPIDs(List(opcSpec: SPSpec)) =>
                val ip: Option[String] = opcSpec.attributes.getAsString("ip")
                val port: Option[Int] = opcSpec.attributes.getAsInt("port")
                (ip, port) match {
                  case (Some(ip: String), Some(port: Int)) =>
                    import java.net.InetSocketAddress
                    val remote: InetSocketAddress = new InetSocketAddress(ip, port)
                    runtimeName match {
                      case Some(name) =>
                        opcClient match {
                          case Some(_: ActorRef) => reply ! SPError("Connected already")
                          case _ =>
                            context.actorOf(Props(new Client(remote, self)), name + "opcClient")
                            reply ! StatusCodes.OK
                            sendEvent("","Connecting")
                        }
                      case None => reply ! SPError("The runtime has to have a name")
                    }
                  case (None,_) => reply ! SPError("Erroneous or missing property ip: String")
                  case (_,None) => reply ! SPError("Erroneous or missing property port: Int")
                }
              case _ => reply ! SPError("Failed to load the PLC specification")
            }
          case None => reply ! SPError("You have to choose a PLC first")
        }
      case None => reply ! SPError("You have to choose a model first")
    }
  }

  def disconnect(attr: SPAttributes, reply: ActorRef): Unit = {
    opcClient match {
      case Some(client: ActorRef) =>
        client ! "close"
        reply ! StatusCodes.OK
        sendEvent("","Disconnecting")
      case _ => reply ! SPError("There is no OPC to disconnect from.")
    }
  }

  def chooseOPC(attr: SPAttributes, reply: ActorRef): Unit = {
    attachedModel match {
      case Some(m: ID) =>
        opcClient match {
          case None =>
            attr.getAsID("opcSpecID") match {
              case Some(opcSpecID: ID) =>
                attachedOPCSpec = Some(opcSpecID)
                reply ! StatusCodes.OK
                sendEvent(opcSpecID.toString(),"OPCChosen")
                connect()
              case None => reply ! SPError("Erroneous or missing property plcSpecID: ID")
            }
          case Some(a: ActorRef) =>
            reply ! SPError("Please disconnect from the current OPC first")
        }
      case None => reply ! SPError("You have to choose a model first")
    }
  }

}

object PLCRuntime {
  def props(cr: CreateRuntime) = Props(classOf[PLCRuntime], cr)
}