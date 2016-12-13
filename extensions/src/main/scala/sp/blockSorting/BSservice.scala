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
import scala.collection.mutable.ListBuffer
import scala.math._

object BSservice extends SPService {

  val specification = SPAttributes(
    "service" -> SPAttributes(
      "group" -> "hide" // to organize in gui. maybe use "hide" to hide service in gui
    ),
    "command" -> KeyDefinition("String", List(), None),
    "Left" -> KeyDefinition("List[List[String]]", List(), None),
    "Right" -> KeyDefinition("List[List[String]]", List(), None),
    "Middle" -> KeyDefinition("List[List[Stting]]", List(), None)
  )
  val transformTuple = (   
    TransformValue("Left", _.getAs[List[List[String]]]("Left")),
    TransformValue("Right", _.getAs[List[List[String]]]("Right")),
    TransformValue("Middle", _.getAs[List[List[String]]]("Middle")),
    TransformValue("command", _.getAs[String]("command"))
    )

  val transformation = transformToList(transformTuple.productIterator.toList)
  def props(serviceHandler: ActorRef, eventHandler: ActorRef, operationController: String, RunnerService: String) = Props(classOf[BSservice], serviceHandler, eventHandler, operationController, RunnerService)

}
class BSservice(serviceHandler: ActorRef, eventHandler: ActorRef, operationController: String, RunnerService: String) extends Actor with ServiceSupport with TowerBuilder{
  var leftRaw : List[List[String]] = List()
  var rightRaw : List[List[String]] = List()
  var middleRaw : List[List[String]] = List()
  var startLeft = Array.fill[Byte](16)(0)
  var startRight = Array.fill[Byte](16)(0)
  var startMiddle = Array.fill[Byte](16)(0)
  var startRobotL : Byte = 0
  var startRobotR : Byte = 0
  var movesL : List[Move] = List()
  var movesR : List[Move] = List()
  var mC : Int = 0
  var varannan : Int = 1

  eventHandler ! SubscribeToSSE(self)
  
  
  def receive = {
    case r@Request(service, attr, ids, reqID) => {
      println("main")
   
      
      val replyTo = sender()
      implicit val rnr = RequestNReply(r, replyTo)
      
 
      
      println(transform(BSservice.transformTuple._4))
      if(transform(BSservice.transformTuple._4) == "hej"){//------------------------------------------update------------------------------------
       
      }else if(transform(BSservice.transformTuple._4) == "setup"){ //---------------------------------setup-----------------------------------
        
       //eventHandler ! Response(List(),SPAttributes("text" -> "hej"),"BS",reqID)
        
        leftRaw = transform(BSservice.transformTuple._1)
        rightRaw = transform(BSservice.transformTuple._2)
        middleRaw = transform(BSservice.transformTuple._3)
      
        val (left, right, middle) = GuiToOpt(leftRaw, rightRaw, middleRaw)
        startLeft = left
        startRight = right
        startMiddle = middle
      }else if(transform(BSservice.transformTuple._4) == "order"){ //---------------------------------order-----------------------------------
        leftRaw = transform(BSservice.transformTuple._1)
        rightRaw = transform(BSservice.transformTuple._2)
        middleRaw = transform(BSservice.transformTuple._3)
      
        val (left, right, middle) = GuiToOpt(leftRaw, rightRaw, middleRaw)
     
        
        serviceHandler ! Request(operationController,SPAttributes("command"->SPAttributes("commandType"->"stop")))
        //serviceHandler ! Request("RunnerService", SPAttributes("command"->"stop"))
       
        val desiredState = new BlockState(left, right, middle,0,0,0,null,null)
        
        println("start: " + startLeft(0) + " desired:" + left(0))
        
        val startState = new BlockState(startLeft, startRight, startMiddle,startRobotL,startRobotR,0,desiredState,ArrayBuffer[Move]())
        val movestemp = Astar.solver(startState)
        println(movestemp.size)
        movesL = movestemp(0).toList
        movesR = movestemp(1).toList
  
        eventHandler ! Response(List(),SPAttributes("movesL" -> movesL, "movesR" -> movesR),"BS",reqID)

        println(movesL.size)
        println(movesR.size )
  
        val paraSOP = movesToSOP(movesL, movesR, ids)
        val sopSpec = SOPSpec("tower", List(paraSOP._1))
        val upIds = sopSpec :: paraSOP._2 ++ ids
        val stations = Map("tower" -> sopSpec.id)
        
        mC = 0
        
        serviceHandler ! Request("OrderHandler", SPAttributes(
        "order" -> SPAttributes(
        "id" -> ID.newID,
        "name" -> "New_sequence",
        "stations" -> stations
          )
          ) ,upIds) 
      }  
      replyTo ! Response(List(), SPAttributes("tower" -> "hej"), rnr.req.service, rnr.req.reqID)  
    }
    
    //case error: SPError => { println("BSservice got an error: $error")}
    
    
    case Response(ids, attr, "toBS", id) =>{
      println("JAAAAAAAAAAAAAAAAAAAA")
      println("######### " + mC)
      varannan = varannan*(-1)
      if(varannan == 1){
        val (startLeftm,startRightm,startMiddlem,startRobotLm,startRobotRm) = updateStates(movesL, movesR ,mC,startLeft,startRight,startMiddle,startRobotL,startRobotR)
        
        eventHandler ! Response(List(),SPAttributes("left" -> startLeft, "right" -> startRight, "middle" -> startMiddle, "movesL" -> movesL(mC), "movesR" -> movesR(mC)),"BS",id)
        //moves.foreach{mm => println(mm)}
        //moves.slice(mC,moves.size).foreach{mm => println(mm)}
        
        startLeft = startLeftm
        startRight = startRightm
        startMiddle = startMiddlem
        startRobotL = startRobotLm  
        startRobotR = startRobotRm
        
        /**println("----------------------")
        print("left: ")
        startLeft.foreach{k => print(k + " ")}
        print("| right: ")
        startRight.foreach{k => print(k + " ")}
        print("| middle: ")
        startMiddle.foreach{k => print(k + " ") }
        print("| RobotL: ")
        print(startRobotL)
        print("| RobotR: ")
        print(startRobotR)*/
        mC += 1
      }
    }
  }
}



trait TowerBuilder extends TowerOperationTypes {

  def updateStates(movesL: List[Move],movesR : List[Move], mC: Int, startLeft: Array[Byte], startRight : Array[Byte], startMiddle : Array[Byte], startRobotL: Byte, startRobotR : Byte) = {
    var sL = startLeft
    var sR = startRight
    var sM = startMiddle
    var srL = startRobotL
    var srR = startRobotR

    if(movesL(mC) != null){ 
      if(movesL(mC).isPicking == true){
        if(movesL(mC).usingMiddle == true){
          srL = movesL(mC).color
          sM(movesL(mC).position) = 0
        }else { 
          srL = movesL(mC).color
          sL(movesL(mC).position) = 0
        }
      }else {
       if(movesL(mC).usingMiddle == true){
          sM(movesL(mC).position) = startRobotL
          srL = 0
        } else { 
          srL = 0
          sL(movesL(mC).position) = movesL(mC).color
        }  
      }
    }
    if(movesR(mC) != null){ 
      if(movesR(mC).isPicking == true){
        if(movesR(mC).usingMiddle == true){
          srR =  movesR(mC).color
          sM(movesR(mC).position) = 0
        }else { 
          srR = movesR(mC).color
          sR(movesR(mC).position) = 0
        }
      }else {
       if(movesR(mC).usingMiddle == true){
          sM(movesR(mC).position) = startRobotR
          srR = 0
        } else { 
          srR =  0
          sR(movesR(mC).position) = movesR(mC).color
        }  
      }
    }
    (sL, sR, sM, srL, srR)
  }
  
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
  
  def movesToSOP(movesL: List[Move], movesR: List[Move], ids: List[IDAble]) = {

    val nameMap = ids.map(x => x.name -> x).toMap

    val oL = movesToOperations(movesL, nameMap, true)
    val oR = movesToOperations(movesR, nameMap, false)
    val allOps = oL ++ oR
    println(oL.size)
    var brickSeq = new ListBuffer[SOP]
    for(i <- 0 to oL.size-1){
      println(i)
      val seqL = Sequence(List(oL(i)).map(o => Hierarchy(o.id)):_*)
      val seqR = Sequence(List(oR(i)).map(o => Hierarchy(o.id)):_*)
      brickSeq += Parallel(seqL,seqR)
    }
    //val seqL = Sequence(oL.map(o => Hierarchy(o.id)):_*)
    //val seqR = Sequence(oR.map(o => Hierarchy(o.id)):_*)
    //val buildSOP: SOP = if (seqL.isEmpty) seqR else if (seqR.isEmpty) seqL else Parallel(seqL, seqR)
    //var buildSOP: List[SOP]
    //val brickSeq: List[SOP] = List(buildSOP)  

    (Sequence(brickSeq:_*),allOps)
  }

  
  def movesToOperations(moves: List[Move], nameMap: Map[String,IDAble], isR4: Boolean) = {
    //println(moves(0).toString)
    var Rmoves = new ListBuffer[Move]()
    moves.foreach{a => 
      if(a != null)
        {Rmoves += a}
      else{Rmoves += new Move(false,false,false,999,0)}
    }
    val operations = for { m <- Rmoves.toList 
    } yield {
      var operation = makeOperation("R5", "toHome", nameMap)
      var name = "placeBlock"
      var position = 0
      var robot = "R5"
      if(m.position == 999){
        if(isR4 == true){
          operation = makeOperation("R4","toHome",nameMap)
        }
      }else{
        if(m.isPicking == true){
          name = "pickBlock"
          if(m.usingMiddle == true){
            position += -89 + m.position
            if(m.usingLeftRobot == true){
              robot = "R4"
            }
          }else if(m.usingLeftRobot == true){
            robot = "R4"
            if(m.position <= 7){
              position += 11 + m.position
            }else {        
              position += 13 + m.position
            }
          }else {  
            if(m.position <= 7){
              position += 31 + m.position
            }else { 
              position += 33 + m.position
            }
          }
        }else{
          if(m.usingMiddle == true){
            position += m.position + 11
            if(m.usingLeftRobot == true){
              robot = "R4"
            }
          }else if(m.usingLeftRobot == true){
            robot = "R4"
            if(m.position <= 7){
              position += 111 + m.position
            }else {        
              position += 113 + m.position
            }
          } else {  
            if(m.position <= 7){
              position += 131 + m.position
            }else {
              position += 133 + m.position
            }
          }
        }
        operation = makeOperationWithParameter(robot,name,"pos",position,nameMap)
      }  
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
