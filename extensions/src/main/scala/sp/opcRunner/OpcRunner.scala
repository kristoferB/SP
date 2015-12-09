package sp.opcrunner

import akka.io.Tcp
import akka.io.IO
import akka.util.ByteString
import java.net.InetSocketAddress
import akka.actor._
import sp.domain.logic.{ActionParser, PropositionParser}
import sp.system._
import sp.system.messages._
import sp.domain._
import sp.domain.Logic._
import scala.concurrent.Future
import akka.util._
import akka.pattern.ask
import scala.concurrent._
import scala.concurrent.duration._
import akka.camel._

case class OPCSubscribe(variables: List[String])
case class OPCWrite(variableMap: Map[String, SPValue])
case class JsonM(messType: String, id: String, mess: Map[String, SPValue])
case class OPCValue(address: String, value: SPValue)
case class OPCError(e: List[String])

class OpcDAClientConnection(master : ActorRef) extends Actor {
  import context.system
  import context.dispatcher

  val remote = new InetSocketAddress("localhost", 7070)
  val clientID = java.util.UUID.randomUUID().toString
  val regMessage = ByteString("{\"messType\":\"REG\",\"id\":\"" + clientID + "\"}" + "\n")

  import Tcp._
  IO(Tcp) ! Connect(remoteAddress = remote, timeout = Some(FiniteDuration(3, SECONDS)))

  def receive = {
    case cf @ CommandFailed(_: Connect) =>
      println("Command failed, stopping")
      context stop self

    case c @ Connected(host, local) =>
      val connection = sender()
      connection ! Register(self)
      connection ! Write(regMessage)

      println("OPCClient connected")

      master ! "Connected"

      context become {
        case data: ByteString =>
          connection ! Write(data)
        case OPCSubscribe(vars) =>
          println("TCPClient got mess: " + "OPCSubscribe" + vars)
          sendSubMessage(vars)
        case OPCWrite(m) =>
          println("TCPClient got mess: " + "OPCWrite" + m)
          sendWriteMessage(m map (kv => kv._1 -> kv._2))
        case CommandFailed(w: Write) =>
          // O/S buffer was full
          println("write failed")
        case Received(bytes: ByteString) =>
          val mess: String = bytes.utf8String
          println("Got mess from OPCServer: " + mess)

          mess.split("\n") foreach { mess =>
            try {
              val asl = SPAttributes.fromJson(mess)
              val mJ = asl.flatMap(_.getAs[JsonM]("")).get
              if (mJ.messType == "VALUE") {
                val values = (mJ.mess - "DATE") map (kv => OPCValue(kv._1, kv._2))
                values foreach (master ! _ )
              } else if (mJ.messType == "ERROR") {
                val em = mJ.mess.map(kv => kv._1 + ": " + kv._2)
                master ! OPCError(em.toList)
              }
            } catch {
              case e: Exception => println("Error converting received message " + mess)
            }
          }
        case "close" =>
          println("Disconnecting ")
          connection ! Close
        case cc: ConnectionClosed =>
          println("OPCClient disconnected")
          context stop self
      }
  }

  private def sendRegMessage() = sendM(JsonM("REG", clientID, Map()))
  private def sendSubMessage(variable: String) = sendM(JsonM("SUBSCRIBE", clientID, Map(variable -> (""))))
  private def sendSubMessage(variable: List[String]) = sendM(JsonM("SUBSCRIBE", clientID, (for (v <- variable) yield v -> org.json4s.JString("")).toMap))
  private def sendWriteMessage(variable: String, value: SPValue) = sendM(JsonM("WRITE", clientID, Map(variable -> value)))
  private def sendWriteMessage(variableMap: Map[String, SPValue]) = sendM(JsonM("WRITE", clientID, variableMap))
  private def sendM(m: JsonM) = {
    self ! ByteString(org.json4s.native.Serialization.write(m) + "\n")
  }
}

case class OPCSetup(command: String, ops: List[ID])

object OpcRunner extends SPService {
  val specification = SPAttributes(
    "service" -> SPAttributes(
      "group" -> "External",
      "description" -> "Start PS operations via OPC"
    ),
    "setup" -> SPAttributes(
      "command" -> KeyDefinition("String", List("start_op"), Some("start_op")),
      "ops" -> KeyDefinition("List[ID]", List(), Some(SPValue(List())))
    )
  )

  val transformTuple = (
    TransformValue("setup", _.getAs[OPCSetup]("setup"))
  )
  val transformation = transformToList(transformTuple.productIterator.toList)

  def props = ServiceLauncher.props(Props(classOf[OpcRunner]))
}

class OpcRunner extends Actor with ServiceSupport {
  implicit val timeout = Timeout(100 seconds)

  import context.dispatcher

  val client = context.actorOf(Props(classOf[OpcDAClientConnection], self))

  def receive = {
    case r@Request(service, attr, ids, reqID) => {
      val replyTo = sender()
      implicit val rnr = RequestNReply(r, replyTo)
      val progress = context.actorOf(progressHandler)

      progress ! SPAttributes("progress" -> "starting opcrunner")

      var done = false
      val setup = transform(OpcRunner.transformTuple)
      val core = r.attributes.getAs[ServiceHandlerAttributes]("core").get

      val ops = ids.filter(_.isInstanceOf[Operation]).map(_.asInstanceOf[Operation]).filter(op => setup.ops.contains(op.id))
      val sigs = ops.flatMap(o =>
        for {
          start <- o.attributes.getAs[String]("starting_signal")
          end <- o.attributes.getAs[String]("ending_signal")
        } yield {
          (start,end)
        })

      if(sigs.isEmpty) {
        println(ids)
        println(ops)
        println(sigs)
        sender ! SPError("Operation doesn't exist or signals not properly defined");
      } else {
        val sig = sigs.head

        import org.json4s.JsonAST.{JValue,JBool}

        context become {
          case "Connected" =>
            setup.command match {
              case "start_op" =>
                client ! OPCSubscribe(List(sig._1,sig._2))
                client ! OPCWrite(Map ( sig._1 -> true ))
              case _ => throw new Exception("No such command!")
            }
          case OPCValue(sig._2, JBool(true)) =>
            client ! OPCWrite(Map ( sig._1 -> false ))
            done = true
          case OPCValue(sig._2, JBool(false)) =>
            if(done) self ! "Done"
          case "Done" =>
            replyTo ! Response(List(), SPAttributes("opcrunner" -> "start_op"), rnr.req.service, rnr.req.reqID)
            terminate(progress)
            context unbecome
        }
      }
    }
    case _ => sender ! SPError("Ill formed request");
  }

  def terminate(progress: ActorRef): Unit = {
    self ! PoisonPill
    progress ! PoisonPill
  }
}
