package sp.ros

import akka.actor._
import com.codemettle.reactivemq.ReActiveMQMessages._
import com.codemettle.reactivemq._
import com.codemettle.reactivemq.model._
import sp.domain.Logic._
import sp.domain._
import sp.system._
import sp.system.messages._


object DESExplorer extends SPService {
  val specification = SPAttributes(
    "service" -> SPAttributes(
      "group" -> "runtimes",
      "description" -> "A runtime service for emats."
    ),
    "active mq host" -> KeyDefinition("String", List("localhost", "10.0.0.16"), Some("10.0.0.16")),
    "active mq port" -> KeyDefinition("String", List("61616"), Some("61616"))
  )

  val transformTuple = (
    TransformValue("host", _.getAs[String]("active mq host")),
    TransformValue("port", _.getAs[String]("active mq port"))
    )
  val transformation = transformToList(transformTuple.productIterator.toList)

  def props = ServiceLauncher.props(Props(classOf[DESExplorer]))
}


class DESExplorer extends Actor with ServiceSupport {

  var activemq_connection: ActorRef = null //null if !connected


  def receive = {
    case r@Request(service, attr, ids, reqID) =>
      val replyTo = sender()
      implicit val rnr = RequestNReply(r, replyTo)

      val host = transform(DESExplorer.transformTuple._1)
      val port = transform(DESExplorer.transformTuple._2)
      val core = r.attributes.getAs[ServiceHandlerAttributes]("core").get

//      println(s"calc got: $r")

      //      val res = sign match {
      //        case "+" => a + b
      //        case "-" => a - b
      //      }
      //      replyTo ! Response(List(), SPAttributes("result" -> res), rnr.req.service, rnr.req.reqID)

      ReActiveMQExtension(context.system).manager ! GetConnection(s"tcp://$host:$port")

    //Active MQ stuff
    case ConnectionEstablished(request, c) =>
      activemq_connection = c
      //setup queues to subscribe
      activemq_connection ! ConsumeFromQueue("completedOperation")

    //      println("connected:" + request)
    //      val mess = SPAttributes(
    //        "a string from sp" -> "hello world",
    //        "an integer from sp" -> 43
    //      )
    //      println("sending message: " + AMQMessage(mess.toJson).toString)
    //      c ! SendMessage(Queue("ROS-IN"), AMQMessage(mess.toJson))

    case mess@AMQMessage(body, prop, headers) =>
      println(s"got stomp message:\nbody: $body\nprop: $prop\n headers: $headers")

  }


}
