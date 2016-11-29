package sp.blockSorting

import sp.psl._
import akka.actor._
import sp.domain.Logic._
import sp.domain._
import sp.system._
import sp.system.messages._
import scala.collection.mutable.MutableList
import astar._
import scala.collection.mutable.ArrayBuffer

object BSservice extends SPService {

  val specification = SPAttributes(
    "service" -> SPAttributes(
      "group" -> "hide" // to organize in gui. maybe use "hide" to hide service in gui
    ),
    "getNext" -> KeyDefinition("Boolean", List(), None),
    "Left" -> KeyDefinition("List[List[String]]", List(), None),
    "Right" -> KeyDefinition("List[List[String]]", List(), None),
    "Middle" -> KeyDefinition("List[List[Stting]]", List(), None),
    "fixturePositions" -> KeyDefinition("Int", List(1,2), Some(0))
  )
  val transformTuple = (
    TransformValue("getNext", _.getAs[Boolean]("getNext")),
    TransformValue("Left", _.getAs[List[List[String]]]("Left")),
    TransformValue("Right", _.getAs[List[List[String]]]("Right")),
    TransformValue("Middle", _.getAs[List[List[String]]]("Middle")),
    TransformValue("fixturePositions", _.getAs[Int]("fixturePositions"))
    )

  val transformation = transformToList(transformTuple.productIterator.toList)
  def props(serviceHandler: ActorRef) = Props(classOf[BSservice], serviceHandler)

}
class BSservice(sh: ActorRef) extends Actor with ServiceSupport with TowerBuilder{
  var fixturePosition = 2

  def receive = {
    case r@Request(service, attr, ids, reqID) => {
      val replyTo = sender()
      implicit val rnr = RequestNReply(r, replyTo)
      
      val leftRaw = transform(BSservice.transformTuple._2)
      val rightRaw = transform(BSservice.transformTuple._3)
      val middleRaw = transform(BSservice.transformTuple._4)
      
      println(leftRaw)
      println(rightRaw)
      println(middleRaw)
  
      val desiredRaw = GuiToOpt(leftRaw,rightRaw,middleRaw)
      
      val desiredState = new BlockState(desiredRaw._1,desiredRaw._2,desiredRaw._3,0,0,0,null,null)
      val startState = new BlockState(desiredRaw._1,desiredRaw._2,desiredRaw._3,0,0,0,desiredState,ArrayBuffer[Move]())
      
      val moves = Astar.solver(startState)
      val paraSOP = movesToSOP(moves, ids)
      val sopSpec = SOPSpec("Sequence", List(paraSOP._1))
      val upIds = sopSpec :: paraSOP._2 ++ ids   
      
      sh! Request("OrderHandler", SPAttributes(
        "order" -> SPAttributes(
          "id" -> ID.newID,
          "name" -> "hej",
          "stations" -> "R4_R5"
        )
      ) ,upIds   )
      
   
      
      replyTo ! Response(List(), SPAttributes("sequence" -> sopSpec), rnr.req.service, rnr.req.reqID)
    }

    case error: SPError => println("Operator Service got an error: $error")
  }
}



trait TowerBuilder extends TowerOperationTypes {

  def GuiToOpt(left: List[List[String]], right: List[List[String]], middle: List[List[String]]) = {
    val leftPlate = new Array[Byte](16)
    var rightPlate = new Array[Byte](16)
    val middlePlate = new Array[Byte](4)
    var j = 0
    left.foreach{i =>
      i.foreach{k =>
        leftPlate(j) = k.toByte 
        j += 1
      }
    }
    j = 0
    right.foreach{i =>
      i.foreach{k =>
        rightPlate(j) = k.toByte 
        j += 1
      }
    }
    j = 0
    middle.foreach{i =>
      i.foreach{k =>
        middlePlate(j) = k.toByte 
        j += 1
      }
    }
    
    (leftPlate, rightPlate, middlePlate)
  }

  def movesToSOP(moves: Array[Move],  ids: List[IDAble]) = {
    val nameMap = ids.map(x => x.name -> x).toMap   
    val operations = movesToOperations(moves,nameMap)
    val sequence = Sequence(operations.map(o => Hierarchy(o.id)):_*)
    
    (sequence, operations.toList)
  }
  
  def movesToOperations(moves: Array[Move], nameMap: Map[String,IDAble]) = {
    val operations = for { m <- moves
    } yield {
      var name = "placeBlock"
      var position = 110
      var robot = "R5"
      if(m.isPicking == true){
        name = "pickBlock"
      }
      if(m.usingMiddle == true){
        position += m.position
        if(m.usingLeftRobot == true){
          robot = "R4"
        }
     } else if(m.usingLeftRobot == true){
        robot = "R4"
        if(m.position <= 8){
          position = 120 + m.position
        } else {        
          position = 130 + m.position
        }
      } else {  
        if(m.position <= 8){
          position = 100 + m.position
        } else {
          position = 110 + m.position
        }
      }
      val operation = makeOperationWithParameter(robot,name,"pos",position,nameMap)
      List(operation)
    }
    operations.flatten
  }
  
 // def operationsName(moves: List[Move]) moves.map(_.pos).mkString("_")
}

trait TowerOperationTypes {
  def makeOperationWithParameter(resource: String, ability: String, parameter: String, value: SPValue, nameMap: Map[String, IDAble]) = {
    val ab = nameMap(s"$resource.$ability")
    val p = nameMap(s"$resource.$ability.$parameter")
    val valueJson = if (value.isInstanceOf[SPAttributes]) "" else "_"+value.toJson
    Operation(s"O_$ability${resource}$valueJson", List(), attributes = SPAttributes("ability" -> AbilityStructure(ab.id, List(AbilityParameter(p.id, value)))))
  }

  def makeOperation(resource: String, ability: String, nameMap: Map[String, IDAble]) = {
    val ab = nameMap(s"$resource.$ability")
    Operation(s"O_$ability${resource}", List(), attributes = SPAttributes("ability" -> AbilityStructure(ab.id, List())))
  }


}

