package sp.opc

import java.util.UUID

import sp.domain._
import akka.actor._
import sp.domain.Logic._

import akka.actor.{ Actor, ActorRef, Props }
import akka.io.{ IO, Tcp }
import akka.util.ByteString
import java.net.InetSocketAddress

import scala.concurrent.duration.{FiniteDuration, SECONDS}

//case class ReadOpcTag(variable: String, value: Any)
case class WriteValue(varValueMap: Map[String, SPValue])

case class OPCHost(h: String)
case class OPCPort(p: Int)
case class OPCSubscribe(variables: List[String]) {
  def ++(o: OPCSubscribe) = OPCSubscribe(variables ++ o.variables)
}
case class OPCWrite(variableMap: Map[String, SPValue])
case class OPCValue(address: String, value: SPValue)
case class OPCError(e: List[String])

object Client {
  def props(remote: InetSocketAddress, replies: ActorRef) =
    Props(classOf[Client], remote, replies)
}

class Client(remote: InetSocketAddress, listener: ActorRef) extends Actor with akka.actor.ActorLogging {

  println("Creating a TCPCLient " + this)

  import Tcp._
  import context.system

  var connection: ActorRef = null
  var connected = false
  val clientID = java.util.UUID.randomUUID().toString
  val regMessage = ByteString("{\"messType\":\"REG\",\"id\":\"" + clientID + "\"}" + "\n")

  IO(Tcp) ! Connect(remoteAddress = remote, timeout = Some(FiniteDuration(3, SECONDS)))

  def receive = {
    case cf @ CommandFailed(_: Connect) =>
      listener forward cf
      context stop self

    case c @ Connected(host, local) =>
      listener ! c
      connection = sender()
      connection ! Register(self)
      connection ! Write(regMessage)
      connected = true

      println("OPCClient connected")
      context become {
        case data: ByteString =>
          connection ! Write(data)
        case "YourState" =>
          println("TCPClient got mess: " + "YourState")
          sender ! (if (connected) "Connected" else "Disconnected")
        case OPCSubscribe(vars) =>
          if (connected) {
            //println("TCPClient got mess: " + "OPCSubscribe" + vars)
            sendSubMessage(vars)
          }
        case OPCWrite(m) =>
          if (connected) {
            println("TCPClient got mess: " + "OPCWrite" + m)
            sendWriteMessage(m map (kv => kv._1 -> kv._2))
          }
        case CommandFailed(w: Write) =>
          // O/S buffer was full
          listener ! "write failed"
        case Received(bytes: ByteString) =>
          val mess: String = bytes.utf8String
          //println("Got mess from OPCServer: " + mess)
          mess.split("\n") foreach { mess =>
            try {
              val asl = SPAttributes.fromJson(mess)
              val mJ = asl.flatMap(_.getAs[JsonM]("")).get
              if (mJ.messType == "VALUE") {
                val values = mJ.mess map (kv => OPCValue(kv._1, kv._2))
                values foreach (listener ! _)
              } else if (mJ.messType == "ERROR") {
                val em = mJ.mess.map(kv => kv._1 + ": " + kv._2)
                listener ! OPCError(em.toList)
              }
            } catch {
              case e: Exception => println("Error converting received message " + mess)
            }
          }
        case "close" =>
          println("Disconnecting ")
          connection ! Close
        case cc: ConnectionClosed =>
          connected = false
          listener ! cc
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
    if (connected) {
      self ! ByteString(org.json4s.native.Serialization.write(m) + "\n")
    }
  }

}

case class JsonM(messType: String, id: String, mess: Map[String, SPValue])


object OPCTalker {
  val id = ID(UUID.fromString("c894234d-67ac-4ee0-8cf9-51d75143f234"))

  def test(system: ActorSystem) = {

    val host = OPCHost("192.168.56.101") //172.16.205.18
    val port = OPCPort(7070)
    val remote: InetSocketAddress = new InetSocketAddress(host.h, port.p)
    val recActor = system.actorOf(Props(new Actor {
      def receive = {
        case (variable: String, value: Any) => {
          println(variable + " " + value)
        }
        case l: List[_] => {
          println(l)
        }
      }
    }), name = "recAct")

    val client = system.actorOf(Props(new Client(remote, recActor)), "opcClient")

    val matrikonSW = OPCSubscribe(List("Square Waves.Boolean"))
    val matrikonBB = OPCSubscribe(List("Bucket Brigade.Boolean"))
    //val writeBB = (b: JsBoolean) => OPCWrite(Map("Bucket Brigade.Boolean" -> b))

    client ! matrikonSW ++ matrikonBB

  }
}