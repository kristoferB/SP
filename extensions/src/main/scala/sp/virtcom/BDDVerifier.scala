package sp.virtcom

import akka.actor._
import sp.system._
import sp.system.messages._
import sp.domain._
import sp.domain.Logic._
import scala.concurrent.Future
import akka.util._
import akka.pattern.ask
import scala.concurrent._
import scala.concurrent.duration._
import org.json4s.JString

import scala.util._


case class BDDArg(bdd: Map[String, Int] => Option[Boolean])

object BDDVerifier extends SPService {
  val specification = SPAttributes()
  val transformTuple = (
    TransformValue("command", _.getAs[SPAttributes]("command"))
  )
  val transformation = transformToList(transformTuple.productIterator.toList)

  def props(modelHandler: ActorRef, eventHandler: ActorRef) = Props(classOf[BDDVerifier], modelHandler, eventHandler)
}



class BDDVerifier(modelHandler: ActorRef, eventHandler: ActorRef) extends Actor with ServiceSupport {
  implicit val timeout = Timeout(100.seconds)
  val serviceID = ID.newID
  var bdds: Map[String, Map[String, Int] => Option[Boolean]] = Map()

  import context.dispatcher

  def receive = {
    case r@Request(service, attr, ids, reqID) => {
      val replyTo = sender()
      implicit val rnr = RequestNReply(r, replyTo)
      val command = transform(BDDVerifier.transformTuple)
      for {
        c <- r.attributes.getAs[ServiceHandlerAttributes]("core")
        m <- c.model
      } yield {

      }

      replyTo ! Response(List(), SPAttributes("silent"->true), service, serviceID)
    }

    case x: BDDArg => bdds += "test" -> x.bdd

    case _ => sender ! SPError("Ill formed request")
  }
}
