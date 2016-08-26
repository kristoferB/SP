package sp.system

import akka.actor.Actor.Receive
import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import akka.cluster.pubsub._
import org.json4s.ShortTypeHints

import scala.concurrent.duration._
import sp.system.messages._
import sp.domain.LogicNoImplicit._

import scala.util.Try

/**
  * Created by kristofer on 2016-08-25.
  *
  * Translates messages in the old structure to the json version. Used during migration
  *
  */
class JsonAPI extends Actor {
  import DistributedPubSubMediator.{ Put, Subscribe, Publish }
  val mediator = DistributedPubSub(context.system).mediator

  // Add all topics here
  mediator ! Subscribe("modelHandler", self)

  def receive = {
    case x: String =>
  }

}

// Temporary solution until we have moved to the new model under sp.model
object ModelAPI {
  lazy val spFormats = new JsonFormats{}
  lazy val apiClasses: List[Class[_]] = sp.macros.MacroMagic.values[ModelAPI]
  lazy val formats = new JsonFormats {
    override val typeHints = ShortTypeHints(spFormats.typeHints.hints ++ apiClasses)
  }

  def write[T <: AnyRef](x: T)(implicit formats : org.json4s.Formats, mf : scala.reflect.Manifest[T]) = {
    org.json4s.native.Serialization.write[T](x)
  }
  def read(x: String)(implicit formats : org.json4s.Formats, mf : scala.reflect.Manifest[ModelAPI]) = Try(org.json4s.native.Serialization.read[ModelAPI](x))


}

class ModelHandlerJson(mh: ActorRef) extends Actor {
  implicit val timeout = Timeout(1 seconds)
  import context.dispatcher
  implicit val f = ModelAPI.formats

  def receive = {
    case x: String =>
      val r = sender()
      val conv = ModelAPI.read(x)
      conv.foreach(mess =>
        (mh ? mess).map{
          case res: ModelAPI => r ! ModelAPI.write(res)
          case res => r ! res
        }
      )
      println(s"ModelHandler json got string: $x and converted it to "+conv)
    case x: ModelAPI =>
      println("modelHandlerJson got: "+x)

      import DistributedPubSubMediator.{ Put, Subscribe, Publish }
      val mediator = DistributedPubSub(context.system).mediator
      mediator ! Publish("modelResponse", ModelAPI.write(x))

  }

}

object ModelHandlerJson {
  def props(mh: ActorRef) = Props(classOf[ModelHandlerJson], mh)
}