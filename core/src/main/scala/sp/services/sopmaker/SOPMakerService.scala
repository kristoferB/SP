package sp.services.sopmaker

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import scala.util._
import sp.domain._
import sp.system.messages._

import scala.concurrent.duration._

/**
 * Created by Kristofer on 2014-08-04.
 */
class SOPMakerService(modelHandler: ActorRef) extends Actor {
  private implicit val to = Timeout(1 seconds)

  import context.dispatcher

  def receive = {
    case Request(_, attr) => {
      val reply = sender
      extract(attr) match {
        case Some((model, opsID, base)) => {
          val modelInfoF = modelHandler ? GetModelInfo(model)
          val currentRelationsF = modelHandler ? GetResults(model, _.isInstanceOf[RelationResult])

          val resultFuture = for {
            modelInfoAnswer <- modelInfoF
            currentRelAnswer <- currentRelationsF
          } yield {
            List(modelInfoAnswer, currentRelAnswer) match {
              case ModelInfo(_, mVersion, _) :: SPIDs(relsIdAble) :: Nil => {
                val rels = relsIdAble map (_.asInstanceOf[RelationResult]) sortWith (_.modelVersion > _.modelVersion)
                if (rels.isEmpty)
                  reply ! SPError("Relations must be identified before SOP creation")

                else if (!{opsID.foldLeft(true)((res, id) => res && (rels.head.relationMap.enabledStates.map.contains(id)))})
                  reply ! SPError("Some operation id's are not in the relation map")
                else {
                  val sopmakers = context.actorOf(SOPMaker.props)

                  //TODO: Validate version on model when relation was created.
                  val relsMap = rels.head.relationMap
                  val makeMeASop = MakeMeASOP(opsID, relsMap, base)
                  val result = sopmakers ? makeMeASop

                  result onComplete{
                    case Success(res: List[_]) => {
                      val sops = res map(_.asInstanceOf[SOP])
                      reply ! SOPSpec(sops, "a SOP")
                    }
                    case Success(res) => println("WHAT IS THIS RELATION FINDER RETURNS: " + res)
                    case Failure(error) => reply ! SPError(error.getMessage)
                  }
                }
              }
              //TODO: This error handling should also be part of the general solution when extracting
              case error @ x :: xs => {
                val respond = error.foldLeft("- ")(_ + "\n- " + _.toString)
                reply ! respond
              }
            }
          }
          resultFuture.recover { case e: Exception => println("Resultfuture Fail: " + e.toString)}
        }


        case None => reply ! errorMessage(attr)
      }

    }
  }

  def extract(attr: SPAttributes) = {
    for {
      model <- attr.getAsString("model")
      ops <- attr.getAsList("operations") map( _.flatMap(_.asID))
    } yield {
      val base = attr.getAsID("base")
      (model, ops, base)
    }
  }

  def errorMessage(attr: SPAttributes) = {
    SPError("The request is missing parameters: \n" +
      s"model: ${attr.getAsString("model")}" + "\n" +
      s"ops: ${attr.getAsList("operations") map (_.flatMap(_.asID))}" )
  }
}


object SOPMakerService{
  def props(modelHandler: ActorRef) = Props(classOf[SOPMakerService], modelHandler)
}