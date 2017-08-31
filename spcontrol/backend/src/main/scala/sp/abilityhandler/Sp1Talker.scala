package sp.abilityhandler

import java.util.UUID

import sp.domain._
import sp.domain.Logic._
import akka.actor._


object Sp1Talker {
  def props = Props(classOf[Sp1Talker])
}

// This actor will keep track of the abilities and parse all messages from the VD
class Sp1Talker extends Actor {
  import context.dispatcher
  import akka.cluster.pubsub.DistributedPubSubMediator.{Publish, Put, Subscribe}
  val mediator = akka.cluster.pubsub.DistributedPubSub(context.system).mediator

  mediator ! Subscribe("sp1services", self)
  mediator ! Subscribe("answers", self)
  mediator ! Subscribe("spevents", self)

  def receive = {
    case x: SPAttributes =>
      println("got request: " + x)
      val from = x.getAs[String]("from").getOrElse("")
      val cmd = x.getAs[String]("command").getOrElse("")

      if(cmd == "GetAbs") {
        val h = SPHeader(from = from, to = AbilityHandler.attributes.service)
        val b = APIAbilityHandler.GetAbilities
        mediator ! Publish("services", SPMessage.makeJson(h, b))
      }

      if(cmd == "StartAb") {
        x.getAs[ID]("id").map{abid =>
          val h = SPHeader(from = from, to = AbilityHandler.attributes.service)
          val b = APIAbilityHandler.StartAbility(abid)
          mediator ! Publish("services", SPMessage.makeJson(h, b))
        }
      }

      if(cmd == "StartSOP") {
        val ops = x.getAs[List[Operation]]("ops").getOrElse(List()).toSet
        val abmap = x.getAs[List[(ID,ID)]]("abmap").getOrElse(List()).toMap
        //val initstate = x.getAs[List[(ID,SPValue)]]("initstate").getOrElse(List()).toMap
        val initstate = ops.map(o=>o.id -> SPValue("i")).toMap
        val name = "Hej"
        val id = ID.newID

        val h = SPHeader(from = from, to = sp.runners.APIOperationRunner.service)
        val b = sp.runners.APIOperationRunner.Runners(List(sp.runners.APIOperationRunner.Setup(name, id, ops, abmap, initstate)))
        println("Starting sop!")
        mediator ! Publish("services", SPMessage.makeJson(h, b))
      }

    case x: String =>
      val mess = SPMessage.fromJson(x)
      for {
        m <- mess
        h <- m.getHeaderAs[SPHeader]
        b <- m.getBodyAs[APIAbilityHandler.Response]
      } yield {
        println(b)
        b match {
          case APIAbilityHandler.Abs(a) =>
            println("got abilities!!. sending on")
            val reply = SPAttributes("from" -> "AbilityHandler", "abilities" -> a)
            mediator ! Publish("sp1answers", reply)
          case APIAbilityHandler.AbilityStarted(id) =>
            println("got abilty started!!. sending on")
            val reply = SPAttributes("from" -> "AbilityHandler", "started" -> id)
            mediator ! Publish("sp1answers", reply)
          case APIAbilityHandler.AbilityCompleted(id, result) =>
            println("got abilty completed!!. sending on")
            val reply = SPAttributes("from" -> "AbilityHandler", "finished" -> id, "result" -> result)
            mediator ! Publish("sp1answers", reply)
          case _ =>
        }
      }

    case _ =>
  }

}
