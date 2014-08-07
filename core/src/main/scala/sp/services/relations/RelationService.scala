package sp.services.relations

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import sp.domain._
import sp.system.messages._

import scala.concurrent.duration._

/**
 * Created by Kristofer on 2014-08-04.
 */
class RelationService extends Actor {
  import sp.system.SPActorSystem._
  private implicit val to = Timeout(1 seconds)

  def receive = {
    case Request(_, attr) => {
      extract(attr) map { res =>
        val model = res._1
        val opIDs = res._2

        val ops = modelHandler ? GetIds(opIDs, model)
        // get conds from specs

      }
    }
  }

  def extract(attr: SPAttributes) = {
    for {
      model <- attr.getAsString("model")
      ops <- attr.getAsList("operations") map( _.flatMap(_.asID))
      //labels <- attr.getAsList("labels")
      // initial state
      // goal proposition
    } yield (model, ops)
  }
}


object RelationService{
  def props = Props(classOf[RelationService])
}