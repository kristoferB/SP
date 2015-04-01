package sp.services

import akka.actor._
import sp.domain._
import sp.system.messages._
import sp.domain.logic._

/**
 * The service parse a string into a proposition and returns it.
 * Send a request including attributes:
 * "model" -> "the name of the model"
 * "parse" -> "the string to parse" e.g. r1.tool==kalle AND r2 = false
 * If we need better performance from multiple requests in the future,
 * we can have multiple actors in a round robin.
 **/
class PropositionParserActor extends Actor {
  def receive = {
    case Request(_, attr) => {
      extract(attr) match {

        case Some(res) => {
          PropositionParser.parseStr(res._2) match {
            case Left(failure) =>
              val errorMess = "[" + failure.next.pos + "] error: " + failure.msg + "\n\n" + failure.next.pos.longString
              sender ! SPErrorString(errorMess)
            case Right(prop) => {
              //TODO: fill in the ids
              sender ! prop
            }
          }
        }
        case None => sender ! errorMessage(attr)
      }
    }
  }

  def extract(attr: SPAttributes) = {
    for {
      model <- attr.getAsID("model")
      parse <- attr.getAsString("parse")
    } yield (model, parse)
  }

  def errorMessage(attr: SPAttributes) = {
    SPError("The request is missing parameters: \n" +
      s"model: ${attr.getAsID("model")}" + "\n" +
      s"parse: ${attr.getAsString("parse")}")
  }

}

object PropositionParserActor {
  def props = Props(classOf[PropositionParserActor])
}
