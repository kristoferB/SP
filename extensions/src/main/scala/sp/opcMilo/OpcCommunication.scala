package sp.opcMilo

// SP
import sp.domain._
import org.json4s.JsonAST.{JValue,JBool,JInt,JDouble,JString}
import org.json4s.DefaultFormats

// Milo
import org.eclipse.milo.opcua.stack.core.Identifiers
import org.eclipse.milo.opcua.stack.core.BuiltinDataType
import org.eclipse.milo.opcua.sdk.client.api.nodes.Node
import org.eclipse.milo.opcua.sdk.client.api.UaClient
import org.eclipse.milo.opcua.sdk.client.OpcUaClient
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfig
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfigBuilder
import org.eclipse.milo.opcua.stack.client.UaTcpStackClient
import org.eclipse.milo.opcua.stack.core.Stack
import org.eclipse.milo.opcua.stack.core.AttributeId
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy
import org.eclipse.milo.opcua.stack.core.types.builtin._
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned._
import org.eclipse.milo.opcua.stack.core.types.enumerated._
import org.eclipse.milo.opcua.stack.core.types.structured._
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned._
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem
import org.eclipse.milo.opcua.sdk.client.nodes.UaVariableNode
import org.eclipse.milo.opcua.stack.core.Stack

// Java support
import scala.collection.JavaConverters._
import java.util.function.BiConsumer
import java.util.concurrent.atomic.AtomicLong

// Scala
import akka.actor._

case class StateUpdate(activeState: Map[String, SPValue])

object MiloOPCUAClient {
  // call this at the end
  def destroy() = {
    Stack.releaseSharedResources();
  }
}

// simple client, beware it is blocking everywhere... maybe we dont care
// also there is no error handling
class MiloOPCUAClient {
  var client: UaClient = null
  var clientHandles: AtomicLong = new AtomicLong(1L);
  var availableNodes: Map[String, UaVariableNode] = Map()
  var activeState: Map[String, SPValue] = Map()
  var currentTime: org.joda.time.DateTime = org.joda.time.DateTime.now

  def disconnect() = {
    if(client != null) {
      client.disconnect().get();
      client = null
    }
  }

  def isConnected = client != null

  def connect(url: String = "opc.tcp://localhost:12686"): Boolean = {
    try {
      val configBuilder: OpcUaClientConfigBuilder = new OpcUaClientConfigBuilder();
      val endpoints = UaTcpStackClient.getEndpoints(url).get().toList
      val endpoint: EndpointDescription = endpoints.filter(e => e.getEndpointUrl().equals(url)) match {
        case xs :: _ => xs
        case Nil => throw new Exception("no desired endpoints returned")
      }
      configBuilder.setEndpoint(endpoint);
      configBuilder.setApplicationName(LocalizedText.english("SP OPC UA client"));
      configBuilder.build();
      client = new OpcUaClient(configBuilder.build()).connect().get()
      // periodically ask for the server time just to keep session alive
      setupServerTimeSubsciption()
      populateNodes(client, Identifiers.RootFolder)
      true
    }
    catch {
      case e: Exception =>
        println("OPCUA - " + e.getMessage())
        client = null;
        false
    }
  }

  // for now we only support Variable nodes with String identifiers
  // and identifiers need to be unique
  def populateNodes(client: UaClient, browseRoot: NodeId): Unit = {
    def nodes: List[Node] = client.getAddressSpace().browse(browseRoot).get().asScala.toList
    nodes.map{x =>
      val nodeid = x.getNodeId.get()
      if(x.getNodeClass().get() == NodeClass.Variable && nodeid.getType() == IdType.String) {
        val identifier = nodeid.getIdentifier().toString
        if(!availableNodes.exists(_._1 == identifier))
          availableNodes += (identifier -> x.asInstanceOf[UaVariableNode])
        else
          println(s"OPCUA - Node ${identifier} already exists, skipping!")
      }
      populateNodes(client, nodeid)
    }
  }

  def getCurrentTime: org.joda.time.DateTime = currentTime

  def setupServerTimeSubsciption(): Unit = {
    val subscription = client.getSubscriptionManager.createSubscription(100).get()
    val node  = client.getAddressSpace().createVariableNode(Identifiers.Server_ServerStatus_CurrentTime);

    val n = node.getNodeId().get()
    def readValueId = new ReadValueId(n, AttributeId.Value.uid(), null, QualifiedName.NULL_VALUE)
    def parameters = new MonitoringParameters(uint(clientHandles.getAndIncrement()), 100, null, uint(10), true)
    val requests = List(new MonitoredItemCreateRequest(readValueId, MonitoringMode.Reporting, parameters))
    def onItemCreated = new BiConsumer[UaMonitoredItem, Integer] {
      def accept(item:UaMonitoredItem, id: Integer): Unit = {
        item.setValueConsumer(onSubscription)
      }
    }
    def onSubscription = new BiConsumer[UaMonitoredItem, DataValue] {
      def accept(item:UaMonitoredItem, dataValue: DataValue): Unit = {
        val epoch = dataValue.getValue().getValue().asInstanceOf[DateTime].getJavaTime()
        currentTime = new org.joda.time.DateTime(epoch)
      }
    }
    subscription.createMonitoredItems(TimestampsToReturn.Both, requests.asJava, onItemCreated)
  }

  def subscribeToNodes(identifiers: List[String], reciever: ActorRef, samplingInterval: Double = 100.0): Unit = {
    val subscription = client.getSubscriptionManager.createSubscription(samplingInterval).get()

    val filtered = identifiers.filter(availableNodes.contains(_))
    identifiers.filterNot(availableNodes.contains(_)).foreach { s => println("OPCUA - key does not exist! skipping: " + s) }

    val requests = filtered.map { i =>
      val n = availableNodes(i).getNodeId().get()
      def readValueId = new ReadValueId(n, AttributeId.Value.uid(), null, QualifiedName.NULL_VALUE)
      def parameters = new MonitoringParameters(uint(clientHandles.getAndIncrement()), samplingInterval, null, uint(10), true)
      new MonitoredItemCreateRequest(readValueId, MonitoringMode.Reporting, parameters)
    }

    def onItemCreated = new BiConsumer[UaMonitoredItem, Integer] {
      def accept(item:UaMonitoredItem, id: Integer): Unit = {
        item.setValueConsumer(onSubscription)
      }
    }

    def onSubscription = new BiConsumer[UaMonitoredItem, DataValue] {
      def accept(item:UaMonitoredItem, dataValue: DataValue): Unit = {
        val nodeid = item.getReadValueId.getNodeId.getIdentifier().toString
        val spval = fromDataValue(dataValue)
        println("OPCUA - " + nodeid + " got " + spval)
        activeState += (nodeid -> spval)
        println(activeState)
        reciever ! StateUpdate(activeState)
      }
    }
    subscription.createMonitoredItems(TimestampsToReturn.Both, requests.asJava, onItemCreated)
  }

  def fromDataValue(dv: DataValue): SPValue = {
    val v = dv.getValue().getValue()
    val typeid = dv.getValue().getDataType().get()
    val c = BuiltinDataType.getBackingClass(typeid)
    c match {
      case q if q == classOf[java.lang.Integer] => JInt(v.asInstanceOf[Int])
      case q if q == classOf[UByte] => JInt(v.asInstanceOf[UByte].intValue())
      case q if q == classOf[java.lang.Short] => JInt(v.asInstanceOf[java.lang.Short].intValue())
      case q if q == classOf[java.lang.Long] => JInt(v.asInstanceOf[java.lang.Long].intValue())
      case q if q == classOf[String] => JString(v.asInstanceOf[String])
      case q if q == classOf[java.lang.Boolean] => JBool(v.asInstanceOf[Boolean])
      case q if q == classOf[java.lang.Double] => JDouble(v.asInstanceOf[java.lang.Double].doubleValue())
      case _ => println(s"need to add type: ${c}"); JString("fail")
    }
  }

  def toDataValue(spVal: SPValue, targetType: NodeId): DataValue = {
    implicit val formats = DefaultFormats
    val c = BuiltinDataType.getBackingClass(targetType)
    c match {
      case q if q == classOf[java.lang.Integer] => new DataValue(new Variant(spVal.extract[Int]))
      case q if q == classOf[UByte] => new DataValue(new Variant(ubyte(spVal.extract[Int])))
      case q if q == classOf[java.lang.Short] => new DataValue(new Variant(spVal.extract[Short]))
      case q if q == classOf[java.lang.Long] => new DataValue(new Variant(spVal.extract[Long]))
      case q if q == classOf[String] => new DataValue(new Variant(spVal.extract[String]))
      case q if q == classOf[java.lang.Boolean] => new DataValue(new Variant(spVal.extract[Boolean]))
      case q if q == classOf[java.lang.Double] => new DataValue(new Variant(spVal.extract[Double]))
      case _ => println(s"need to add type: ${c}"); new DataValue(new Variant(false))
    }
  }

  def write(nodeIdentifier: String, spVal: SPValue): Unit = {
    availableNodes.get(nodeIdentifier) match {
      case Some(n) =>
        val typeid = n.getDataType().get()
        val dv = toDataValue(spVal, typeid)
        println("trying to write: " + dv)
        if(client.writeValue(n.getNodeId().get(), dv).get().isGood()) {
          println("OPCUA - value written")
        }
        else {
          println(s"OPCUA - Failed to write to node ${nodeIdentifier} - probably wrong datatype, should be: " + typeid)
        }
      case None => println(s"OPCUA No such node ${nodeIdentifier}")
    }
  }

  def getAvailableNodes(): Map[String, String] = {
    availableNodes.map { case (i,n) =>
      val t = n.getDataType().get()
      val c = BuiltinDataType.getBackingClass(t)
      (i,c.getSimpleName)
    }.toMap
  }
}
