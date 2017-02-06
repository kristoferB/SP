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

object BDDVerifier extends SPService {
  val specification = SPAttributes()
  val transformTuple = (
    TransformValue("command", _.getAs[SPAttributes]("command"))
  )
  val transformation = transformToList(transformTuple.productIterator.toList)

  def props(modelHandler: ActorRef) = Props(classOf[BDDVerifier], modelHandler)
}

class BDDVerifier(modelHandler: ActorRef) extends Actor with ServiceSupport {
  implicit val timeout = Timeout(100.seconds)
  val serviceID = ID.newID
  var bdds: Map[String, Map[String, Int] => Option[Boolean]] = Map()

  import context.dispatcher

  def receive = {
    case r@Request(service, attr, ids, reqID) => {
      val replyTo = sender()
      println("bdd verifier got request:");
      println(attr)
      implicit val rnr = RequestNReply(r, replyTo)
      val command = transform(BDDVerifier.transformTuple)
      for {
        c <- r.attributes.getAs[ServiceHandlerAttributes]("core")
        m <- c.model
      } yield {
        println("bdd verifier got request: " + command);
        val bddName = command.getAs[String]("bdd").getOrElse("")
        val f = bdds.get(bddName).getOrElse((_ => None): Map[String,Int] => Option[Boolean])
        val partialState = command.getAs[Map[String,Int]]("partialState").getOrElse(Map())
        println("State: " + partialState)
        val resp = f(partialState) match {
          case Some(result) => Response(List(), SPAttributes("silent"->true, "result"->result), service, serviceID)
          case None => Response(List(), SPAttributes("silent"->true, "invalid"->true), service, serviceID)
        }
        replyTo ! resp
      }

    }

    case RegisterBDD(name, bdd, _) =>
      println("BDD " + name + " registered")
      bdds += name -> bdd

      // test code
      // List(0,1,2,3).foreach { n =>
      //   bdd(Map("vChamber_status"->n,"vRobot_pos"->2)) match {
      //     case Some(result) => println("Chamber state: " + n + ", can weld? " + result)
      //     case None => println("No BDD exists :(")
      //   }
      // }

    case _ => sender ! SPError("Ill formed request")
  }
}
