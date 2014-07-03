package sp.system

import akka.actor.{Actor, ActorRef, Props}
import sp.system.messages._

/**
 * Created by Kristofer on 2014-06-18.
 */
class TestApp extends App {


  // temp test
  import SPActorSystem._

  val testA = system.actorOf(Props(classOf[TestA], modelHandler), "testA")

  private def readFromLineToActor() = {
    def waitEOF(): Unit = Console.readLine() match {
      case "exit" => system.shutdown()
      case s: String => testA ! s; waitEOF()
      case _ => waitEOF()
    }
    waitEOF()
  }

  readFromLineToActor()


}



class TestA(mh: ActorRef) extends Actor {
    import sp.domain._
    var o1: IDAble = Operation("o1")
    val model = CreateModel("test")
    def receive = {
      case "createM" => mh ! model
      case "newop" => mh ! UpdateIDs("test",  List(UpdateID.addNew(Operation("random"))))
      case "updateOp" => mh ! UpdateIDs("test",  List(UpdateID.addNew(o1)))
      case "op1" => mh ! GetIds(List(o1.id), "test")
      case "ops" => mh ! GetOperations("test")
      case "things" => mh ! GetThings("test")
      case "diff" => mh ! GetDiff("test", 5)
      case "getIDs" =>
      case x @ SPIDs(ids) => {
        println(x)
        if (ids.exists(_.id == o1.id)) for{o <- ids.find(_.id == o1.id)} yield(o1 = o)
      }
      case x @ ModelDiff(ids, model, prevV, v, t) => println(x)
      case x: Any => println(x)
    }
  }
