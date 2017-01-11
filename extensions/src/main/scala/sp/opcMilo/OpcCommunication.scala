package sp.opcMilo

/**
 * Created by ashfaqf on 11/23/16.
 */

import java.util
import java.util.concurrent.ExecutionException
import java.util.function.BiConsumer

import akka.actor.{ Actor, ActorRef, Props }
import akka.actor._
import com.codemettle.reactivemq._
import com.codemettle.reactivemq.ReActiveMQMessages._
import com.codemettle.reactivemq.model._
import org.json4s.JsonAST.JInt
import sp.domain.logic.IDAbleLogic
import scala.concurrent._
import sp.system.messages._
import sp.system._
import sp.domain._
import sp.domain.Logic._
import org.eclipse.milo.opcua.sdk.client.api.nodes.Node;

import scala.util.Random;

object OpcCommunication extends SPService {
  val specification = SPAttributes(
    "service" -> SPAttributes(
      "group" -> "communication" // to organize in gui. maybe use "hide" to hide service in gui
      ),
    "setup" -> SPAttributes(
      "AMQbusIP" -> KeyDefinition("String", List(), Some("0.0.0.0")),
      "publishTopic" -> KeyDefinition("String", List(), Some("commands")),
      "subscribeTopic" -> KeyDefinition("String", List(), Some("response")),
      "OPCServerIP" -> KeyDefinition("String", List(), Some("0.0.0.0")),
      "OPCServerPort" -> KeyDefinition("String", List(), Some("4840"))),
    "command" -> SPAttributes(
      "commandType" -> KeyDefinition("String", List("connect", "disconnect", "listenToChanges", "request","subscribe", "unsubscribe", "request", "writeToPlc"), Some("connect")),
      "parameters" -> KeyDefinition("Option[SPAttributes]", List(), None)))
  val transformTuple = (
    TransformValue("setup", _.getAs[BusSetup]("setup")),
    TransformValue("command", _.getAs[SPAttributes]("command")))
  val transformation = transformToList(transformTuple.productIterator.toList)

  def props(eventHandler: ActorRef) = Props(classOf[OpcCommunication], eventHandler)
}
case class BusSetup(AMQbusIP:String,publishTopic:String, subscribeTopic: String, OPCServerIP:String, OPCServerPort :String)
class OpcCommunication(eventHandler: ActorRef) extends Actor with ServiceSupport {

  import context.dispatcher

  val nodesToSubscribe: List[String] = List("ixGripping", "ixArmAtY0A", "ixArmAtY0B")

  val serviceID = ID.newID
  var theBus: Option[ActorRef] = None
  var setup: Option[BusSetup] = None
  var serviceName: Option[String] = None
  var activeNode : Map[String, Node] = Map()
  var nodeNameToValue : Map[String,Any] = Map()
  var state: State = State(Map())

  def receive = {
    case r @ Request(service, attr, ids, reqID) => {
      // Always include the following lines. Are used by the helper functions
      val replyTo = sender()
      implicit val rnr = RequestNReply(r, replyTo)

      val s = transform(OpcCommunication.transformTuple._1)
      val commands = transform(OpcCommunication.transformTuple._2)
      val core = r.attributes.getAs[ServiceHandlerAttributes]("core").get
      serviceName = Some(service)

      println("Commands: " + commands.getAs[String]("commandType"))

      commands.getAs[String]("commandType").get match {
        case "connect" =>
          setupBus(s, rnr)
          connectToPlc(s)

        case "disconnect" =>
          disconnect();
        case "subscribe" =>
          //          if (theBus.isEmpty)
          //            replyTo ! SPError("The bus must be connected before subscription. Current status: "+connectedAttribute())
          //          else
          subscribe(nodesToSubscribe)
        case "unsubscribe" =>
          if (theBus.isEmpty)
            replyTo ! SPError("The bus must be connected before unsubscription. Current status: " + connectedAttribute())
          else
            unsubscribe()
        case "writeToPlc"=>
          writeToPLC(commands.getAs[SPAttributes]("parameters").get)
        case "request" => 
          request()
        case _ =>
      }
      replyTo ! Response(List(), connectedAttribute() merge SPAttributes("silent" -> true), service, serviceID)

    }
    case ConnectionEstablished(request, c) => {
      println("connected:" + request)
      setup.foreach { s =>
        c ! ConsumeFromTopic(s.subscribeTopic)
        theBus = Some(c)
        eventHandler ! Progress(SPAttributes("theBus" -> "Connected"), serviceName.get, serviceID)
      }
    }
    case ConnectionFailed(request, reason) => {
      println("failed:" + reason)
    }
    case mess @ AMQMessage(body, prop, headers) => {
      val resp = SPAttributes.fromJson(body.toString)

      resp.get.getAs[String]("response").get match {
        case "Exception"=> 
          println("Exception on OPC end" + resp.get)
          eventHandler ! Response(List(),resp.get,serviceName.get, serviceID)
        case "writeToPlc" => 
          val statusMap = resp.get.getAs[SPAttributes]("status").get
          println("Written Objects status",statusMap)
          eventHandler ! Response(List(), statusMap, serviceName.get, serviceID)
        case "request" =>
           activeNode = resp.get.getAs[SPAttributes]("activeNodes").asInstanceOf[Map[String,Node]]
           nodeNameToValue = resp.get.getAs[SPAttributes]("subscribedValues").asInstanceOf[Map[String,Any]]
          
           eventHandler ! Response(List(), SPAttributes("currentVals" -> nodeNameToValue), serviceName.get, serviceID)
          
          
      }
      eventHandler ! Response(List(), SPAttributes(), serviceName.get, serviceID)

    }
    case ConnectionInterrupted(ca, x) => {
      println("connection closed")
      setup = None
    }
    case x => {
      // println("PLC control got message "+x)
      //sender() ! SPError("What do you want me to do? "+ x)
    }

  }
  
  def request(){
    val mess = SPAttributes(
        "command" -> "request")
        sendMessage(mess)
  }

  def writeToPLC(attrs: SPAttributes){
    val mess = SPAttributes(
        "command" -> "writeToPlc",
        "NodeValues" -> attrs)
        sendMessage(mess)
  }
  def connectToPlc(s: BusSetup) {
    val mess = SPAttributes(
      "command" -> "connect",
      "url" -> s"opc.tcp://${s.OPCServerIP}:${s.OPCServerPort}")
    sendMessage(mess)
  }
  def subscribe(nodes: List[String]) {
    val mess = SPAttributes(
      "command" -> "subscribe",
      "nodes" -> nodes)
    sendMessage(mess)
  }

  def unsubscribe() {
    val mess = SPAttributes(
      "command" -> "unsubscribe")
    sendMessage(mess)
  }

  def setupBus(s: BusSetup, rnr: RequestNReply) = {
    setup = Some(s)
    serviceName = Some(rnr.req.service)
    println(s"connecting: $s")
    ReActiveMQExtension(context.system).manager ! GetConnection(s"nio://${s.AMQbusIP}:61616")
  }

  def disconnect() = {
    println("OPCComm Disconnecting")
    theBus.foreach(_ ! CloseConnection)
    //theBus.foreach(_ ! PoisonPill)
    this.setup = None
    this.theBus = None
  }

  def connectedAttribute() = {
    if (setup.isEmpty)
      SPAttributes("theBus" -> "Not connected")
    else if (theBus.isEmpty)
      SPAttributes("theBus" -> "Connecting")
    else
      SPAttributes("theBus" -> "Connected")
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
}

/**
  *
  * Not using this, easier to use AMQ interface and run the opc client serperately
  *
  *
  *
  *
class OpcCommunication {
  var client: UaClient

  var request: util.List[MonitoredItemCreateRequest]
  var activeNodes: Map[String, Node]
  var nodeIdToString: Map[NodeId, String]

  def connect(url: String = "opc.tcp://localhost:4048") = {
    val configBuilder: OpcUaClientConfigBuilder = new OpcUaClientConfigBuilder();
    val endpoints = UaTcpStackClient.getEndpoints(url).get().toList
    val endpoint: EndpointDescription = endpoints.filter(e => e.getEndpointUrl().equals(url)) match {
      case xs :: _ => xs
      case Nil => throw new Exception("no desired endpoints returned")
    }
    configBuilder.setEndpoint(endpoint);
    configBuilder.setApplicationName(LocalizedText.english("Codesys Lab opc-ua client"));
    configBuilder.build();
    client = new OpcUaClient(configBuilder.build()).connect().get()

  }

  def populateNodes(indent: String, client: OpcUaClient, browseRoot: NodeId): Map[String, Node] = {

    def nodes: List[Node] = client.getAddressSpace().browse(browseRoot).get().toList
    nodes match {
      case x :: xs => activeNodes + (x.getBrowseName.get().getName -> x.getNodeId.get())
        populateNodes(indent + " ", client, x.getNodeId.get())
      case Nil =>
    }
    println(activeNodes mkString)
    activeNodes


  }

  def subscribeToNodes(nodes: List[String]): Unit = {

    val subscription = client.getSubscriptionManager.createSubscription(100).get()
    nodes map { x =>
      var y = activeNodes getOrElse(x, "NotFound") match {
        case x: Node => x.getNodeId.get()
      }
      def readValueId = new ReadValueId(y, AttributeId.Value.uid(), null, QualifiedName.NULL_VALUE)
      def parameters = new MonitoringParameters(uint(Random.nextInt()), 100, null, uint(10), true)
      request.add(new MonitoredItemCreateRequest(readValueId, MonitoringMode.Reporting, parameters))


    }
    val onItemCreated: BiConsumer[UaMonitoredItem, Integer] = (item: UaMonitoredItem, id: Integer) => item.setValueConsumer(onSubscription)
    subscription.createMonitoredItems(TimestampsToReturn.Both, request, onSubscription)

  }

  def onSubscription = (uaMonitoredItem: UaMonitoredItem, dataValue: DataValue) => {
    println(uaMonitoredItem.getReadValueId.getNodeId.toString + dataValue.getValue.toString)
  }
}

*/
