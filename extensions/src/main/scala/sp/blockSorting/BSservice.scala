package sp.blockSorting

import sp.psl._
import akka.actor._
import sp.domain.Logic._
import sp.domain._
import sp.system._
import sp.system.messages._


import scala.collection.mutable.MutableList
import astar.state.State
object BSservice extends SPService {

  val specification = SPAttributes(
    "service" -> SPAttributes(
      "group" -> "hide" // to organize in gui. maybe use "hide" to hide service in gui
    ),
    "getNext" -> KeyDefinition("Boolean", List(), None),
    "buildOrder" -> KeyDefinition("List[List[String]]", List(), None),
    "fixturePositions" -> KeyDefinition("Int", List(1,2), Some(0))
  )
  val transformTuple = (
    TransformValue("getNext", _.getAs[Boolean]("getNext")),
    TransformValue("buildOrder", _.getAs[List[List[String]]]("buildOrder")),
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
      
      val desiredStateRaw = transform(BSservice.transformTuple._2)
  
      val (leftPlate, rightPlate, middlePlate) = GuiToOpt(desiredStateRaw)
      
      val startState = new State(leftPlate,rightPlate,middlePlate,0,0,0,null,"")
      val desiredState = new State(leftPlate,rightPlate,middlePlate,0,0,0,startState,"")
      
      //val moves = Astar(desiredState)
      val moves = List[Move](new Move(true,true,true,1),new Move(false,false,false,1))
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

  def GuiToOpt(desiredStateRaw: List[List[String]]) = {
    val leftPlate = new Array[Byte](16)
    var rightPlate = new Array[Byte](16)
    val middlePlate = new Array[Byte](4)
    var j = 0
    desiredStateRaw(1).foreach{i =>
      leftPlate(j) = i.toByte 
      j += 1
    }
    j = 0
    desiredStateRaw(2).foreach{i =>
      rightPlate(j) = i.toByte 
      j += 1
    }
    j = 0
    desiredStateRaw(3).foreach{i =>
      middlePlate(j) = i.toByte 
      j += 1
    }
    
    (leftPlate, rightPlate, middlePlate)
  }

  def movesToSOP(moves: List[Move],   ids: List[IDAble]) = {
    val nameMap = ids.map(x => x.name -> x).toMap   
    val operations = movesToOperations(moves,nameMap)
    val sequence = Sequence(operations.map(o => Hierarchy(o.id)):_*)
    
    (sequence, operations)
  }
  
  def movesToOperations(moves: List[Move], nameMap: Map[String,IDAble]) = {
    val operations = for { m <- moves
    } yield {
      var name = "placeBlock"
      var position = 110
      var robot = "R5"
      if(m.ispicking == true){
        name = "pickBlock"
      }
      if(m.shared == true){
        position += m.position
        if(m.robot == true){
          robot = "R4"
        }
     } else if(m.robot == true){
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

  case class Move(robot: Boolean, ispicking: Boolean, shared: Boolean, position: Int)
  
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

