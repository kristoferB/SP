package sp.blockSorting

import sp.psl._
import akka.actor._
import sp.domain.Logic._
import sp.domain._
import sp.system._
import sp.system.messages._
import scala.collection.mutable.MutableList
import sp.blockSorting.astar._
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
  def props(serviceHandler: ActorRef, eventHandler: ActorRef, operationController: String, BSrunner: String) = Props(classOf[BSservice], serviceHandler, eventHandler, operationController, BSrunner)

}
class BSservice(serviceHandler: ActorRef, eventHandler: ActorRef, operationController: String, BSrunner: String) extends Actor with ServiceSupport with TowerBuilder{
  var layoutRaw: List[List[List[String]]] = List()
  var layoutCurrent = Array.fill[Byte](5,16)(0)
  var layoutStart = Array.fill[Byte](5,16)(0)
  var movesL : List[Move] = List()
  var movesR : List[Move] = List()
  var moves : List[Move] = List()
  var mC : Int = 0
  var vC : Int = 1
  var doNext : Boolean = false

  eventHandler ! SubscribeToSSE(self)
  
  def receive = {
    case r@Request(service, attr, ids, reqID) => {

      val replyTo = sender()
      implicit val rnr = RequestNReply(r, replyTo)
      
      layoutRaw = List(transform(BSservice.transformTuple._1), transform(BSservice.transformTuple._2),transform(BSservice.transformTuple._3))
      
      val layoutCurrent = GuiToOpt(layoutRaw)

      if(transform(BSservice.transformTuple._4) == "manuell step"){
        if(mC <= movesL.size - 1){
          layoutStart = updateStates(movesL, movesR ,mC,layoutStart)
          mC += 1
          eventHandler ! Response(List(),SPAttributes("left" -> layoutStart(0), "right" -> layoutStart(1), "middle" -> layoutStart(2), "robotL" -> layoutStart(3)(0), "robotR" -> layoutStart(4)(0),"moves" -> moves),"BSupdate",reqID)
        }    
      }else if(transform(BSservice.transformTuple._4) == "setup"){
        layoutStart = layoutCurrent
      }else if(transform(BSservice.transformTuple._4) == "order"){
        mC = 0
        
        val desiredState = new BlockState(layoutCurrent(0), layoutCurrent(1), layoutCurrent(2),0,0,0,null,null)
        val startState = new BlockState(layoutStart(0), layoutStart(1), layoutStart(2), layoutStart(3)(0), layoutStart(4)(0),0,desiredState,ArrayBuffer[Move]())
           
        val movestemp = Astar.solver(startState)
        movesL = makeEmptyMoves(movestemp(0).toList,true)
        movesR = makeEmptyMoves(movestemp(1).toList,false)
        moves = movesL ::: movesR
         
        eventHandler ! Response(List(),SPAttributes("left" -> layoutStart(0), "right" -> layoutStart(1), "middle" -> layoutStart(2), "robotL" -> layoutStart(3), "robotR" -> layoutStart(4),"moves" -> moves),"BSInit",reqID)

        val paraSOP = movesToSOP(movesL, movesR, ids)
        val sopSpec = SOPSpec("tower", List(paraSOP._1))
        val upIds = sopSpec :: paraSOP._2 ++ ids
        val stations = Map("tower" -> sopSpec.id)
        
        serviceHandler ! Request("BSorderHandler", SPAttributes("order" -> SPAttributes("id" -> ID.newID,"name" -> "New_sequence","stations" -> stations)) ,upIds) 
      
      }  
      replyTo ! Response(List(), SPAttributes("tower" -> "hej"), rnr.req.service, rnr.req.reqID)  
    }
    
    //case error: SPError => { println("BSservice got an error: $error")}
      
    
    case Response(ids, attr, "toBS", id) =>{
      vC = vC*(-1)
      if(vC == 1){
        layoutStart = updateStates(movesL, movesR ,mC, layoutStart)
        mC += 1
        eventHandler ! Response(List(),SPAttributes("left" -> layoutStart(0), "right" -> layoutStart(1), "middle" -> layoutStart(2), "robotL" -> layoutStart(3)(0), "robotR" -> layoutStart(4)(0),"moves" -> moves),"BSupdate",id)
      }
    }
  }
}



trait TowerBuilder extends TowerOperationTypes {

  def makeEmptyMoves(movesNull: List[Move], usingLeftRobot: Boolean) = {
    val moves = new ListBuffer[Move]()
    for(i <- 0 to movesNull.size - 1  ){
      if(movesNull(i) != null){ moves += movesNull(i) }
      else{
        moves += new Move(usingLeftRobot,false,false,999,0)
      }
    }
    moves.toList
  }
  
  def updateStates(movesL: List[Move], movesR : List[Move], mC: Int, layoutStart: Array[Array[Byte]]) = {
    var layoutTemp = layoutStart
    if(movesL(mC).position != 999){ 
      if(movesL(mC).isPicking == true){
        layoutTemp(3)(0) = movesL(mC).color
        if(movesL(mC).usingMiddle == true){
          layoutTemp(2)(movesL(mC).position) = 0
        }else { 
          layoutTemp(0)(movesL(mC).position) = 0
        }
      }else {
        layoutTemp(3)(0) = 0
       if(movesL(mC).usingMiddle == true){
          layoutTemp(2)(movesL(mC).position) = movesL(mC).color
        } else { 
          layoutTemp(0)(movesL(mC).position) = movesL(mC).color
        }  
      }
    }
    if(movesR(mC).position != 999){ 
      if(movesR(mC).isPicking == true){
        layoutTemp(4)(0) = movesR(mC).color
        if(movesR(mC).usingMiddle == true){
          layoutTemp(2)(movesR(mC).position) = 0
        }else { 
          layoutTemp(1)(movesR(mC).position) = 0
        } 
      }else {
        layoutTemp(4)(0) =  0
        if(movesR(mC).usingMiddle == true){
          layoutTemp(2)(movesR(mC).position) = movesR(mC).color
        } else { 
          layoutTemp(1)(movesR(mC).position) = movesR(mC).color
        }  
      }
    }
    layoutTemp
  }
  
  def GuiToOpt(layout: List[List[List[String]]]) = {
    val leftPlate = layout(0)(0).map(x => x.toByte) ::: layout(0)(1).map(x => x.toByte) ::: layout(0)(2).map(x => x.toByte) ::: layout(0)(3).map(x => x.toByte)
    val rightPlate = layout(1)(0).map(x => x.toByte) ::: layout(1)(1).map(x => x.toByte) ::: layout(1)(2).map(x => x.toByte) ::: layout(1)(3).map(x => x.toByte)
    val middlePlate = layout(2)(0).map(x => x.toByte)

    List(leftPlate.toArray ,rightPlate.toArray ,middlePlate.toArray,List(0.toByte).toArray,List(0.toByte).toArray).toArray
  }
  
  def movesToSOP(movesL: List[Move], movesR: List[Move], ids: List[IDAble]) = {

    val nameMap = ids.map(x => x.name -> x).toMap

    val oL = movesToOperations(movesL, nameMap)
    val oR = movesToOperations(movesR, nameMap)
    val allOps = oL ++ oR
    var brickSeq = new ListBuffer[SOP]
    for(i <- 0 to oL.size-1){
      val seqL = Sequence(List(oL(i)).map(o => Hierarchy(o.id)):_*)
      val seqR = Sequence(List(oR(i)).map(o => Hierarchy(o.id)):_*)
      brickSeq += Parallel(seqL,seqR)
    }
    (Sequence(brickSeq:_*),allOps)
  }

  
  def movesToOperations(moves: List[Move], nameMap: Map[String,IDAble]) = {
    val operations = for { m <- moves
    } yield {
      var operation = makeOperation("R5", "toHome", nameMap)
      var name = "placeBlock"
      var position = 0
      var robot = "R5"
      if(m.position == 999){
        if(m.usingLeftRobot == true){
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
