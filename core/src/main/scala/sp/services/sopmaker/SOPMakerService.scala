package sp.services.sopmaker

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import scala.util._
import sp.domain._
import sp.domain.Logic._
import sp.system.messages._

import scala.concurrent.duration._

/**
 * Created by Kristofer on 2014-08-04.
 */
class SOPMakerService(modelHandler: ActorRef) extends Actor {
  private implicit val to = Timeout(20 seconds)

  import context.dispatcher

  val sopmakers = context.actorOf(SOPMaker.props)

  def receive = {
    case Request(_, attr, _) => {
      val reply = sender
      extract(attr) match {
        case Some((model, opsID, relations, base)) => {
          val modelInfoF = modelHandler ? GetModelInfo(model)
          val currentRelationsF = modelHandler ? GetIds(model, List(relations))

          val resultFuture = for {
            modelInfoAnswer <- modelInfoF
            currentRelAnswer <- currentRelationsF
          } yield {
            List(modelInfoAnswer, currentRelAnswer) match {
              case ModelInfo(_, _, mVersion, _, _) :: SPIDs(relsIdAble) :: Nil => {
                val rels = relsIdAble map (_.asInstanceOf[RelationResult]) sortWith (_.modelVersion > _.modelVersion)
                if (rels.isEmpty)
                  reply ! SPError("Relations must be specificed before SOP creation")
                else if (rels.head.relationMap == None)
                  reply ! SPError("No Relations in the latest relationmap, fix the logic")

                else {
                  //TODO: Validate version on model when relation was created.
                  val relsMap = rels.head.relationMap.get


                  if (!{opsID.foldLeft(true)((res, id) => res && (relsMap.enabledStates.map.contains(id)))})
                    reply ! SPError("Some operation id's are not in the relation map")
                  else {
                    val makeMeASop = MakeMeASOP(opsID, relsMap, base)
                    val result = sopmakers ? makeMeASop

                    result onComplete{
                      case Success(res: List[_]) => {
                        val sops = res map(_.asInstanceOf[SOP])
                        println("WE HAVE A SOP: "+ sops)
                        reply ! SOPSpec("a SOP", sops)
                      }
                      case Success(res) => println("WHAT IS THIS RELATION FINDER RETURNS: " + res)
                      case Failure(error) => reply ! SPError(error.getMessage)
                    }
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
      model <- attr.getAs[ID]("model")
      ops <- attr.getAs[List[ID]]("operations")
      relations <- attr.getAs[ID]("relations")
    } yield {
      val base = attr.getAs[ID]("base")
      (model, ops, relations, base)
    }
  }

  def errorMessage(attr: SPAttributes) = {
    SPError("The request is missing parameters: \n" +
      s"model: ${attr.getAs[ID]("model")}" + "\n" +
      s"relations: ${attr.getAs[ID]("relations")}" + "\n" +
      s"ops: ${attr.getAs[List[ID]]("operations")}" )
  }
}


object SOPMakerService{
  def props(modelHandler: ActorRef) = Props(classOf[SOPMakerService], modelHandler)
}