package sp.virtcom.modeledCases

import sp.domain.SPAttributes
import sp.domain.Logic._
import sp.virtcom.CollectorModel

/**
 * Type based model of a cell at Volvo cars
 * Product type A
 * Product type B
 * One robot
 * Stationary weld with tip dress
 */
case class VolvoWeldConveyerCase() extends CollectorModel {

//  v("vOp1",Seq("i","e","f"),"i","f")
////  v("vOp2",Seq("i","e","f"),"i","f")
////  v("vR",Seq("i","w"),"i","i")
//  op("op1",c("vOp1","i","e","f"))
////  op("op1",c("vR","i","w","i"))
////  op("op2",c("vOp2","i","e","f"))
////  x("order","vOp1!=i & vOp2!=f")

  //Resources/Machines/Variables
  v(name = "vIn_car", domain = Seq("empty", "productA", "productB"), init = "empty", marked = "empty")

  v(name = "vRobot_car", domain = Seq("empty", "productA", "productAWelded", "productB", "productBWelded"), init = "empty", marked = "empty")
  v(name = "vRobot_pos", domain = Seq("atHome", "atIn", "atWeld", "atConveyers"), init = "atHome", marked = "atHome")

  v(name = "vOperator", domain = Seq("idle", "addingProductA", "addingProductB"), init = "idle", marked = "idle")

  v(name = "vTipDresser", domain = Seq("idle", "tipdressing"), init = "idle", marked = "idle")

    conveyerX("A")
    conveyerX("B")

  //Product A
    productX("A")

  //Product B
    productX("B")

  //Tip dressing
  op("tipDress", c("vTipDresser", "idle", "tipdressing", "idle"))

  //Robot movements
  val staticRobotPoses = Map("atHome" -> Set("atIn"),
    "atIn" -> Set("atWeld"),
    "atWeld" -> Set("atConveyers"),
    "atConveyers" -> Set("atHome"))
  createMoveOperations(robotName = "Robot", staticRobotPoses = staticRobotPoses)

  //Macros-----------------------------
  def conveyerX(X: String) = {
    v(name = s"vOutConveyer$X", domain = Seq("idle", "starting", "running", "stopping"), init = "idle", marked = "idle")
    v(name = s"vOutConveyer${X}_car", domain = Seq("empty", "partlyOccupied", "occupied"), init = "empty", marked = "empty")
    op(s"startOutConveyer$X", c(s"vOutConveyer$X", "idle", "starting", "running"))
    op(s"stopOutConveyer$X", c(s"vOutConveyer$X", "running", "stopping", "idle"))
  }

  def productX(X: String) = {
    op(s"addProduct$X", c("vIn_car", "empty", s"product$X"))
    op(s"addProduct$X", c("vOperator", "idle", s"addingProduct$X", "idle"))

    x("inZone", s"vOperator == addingProduct$X & vRobot_pos == atInGripping$X")

    op(s"gripProduct$X", c("vIn_car", s"product$X", "empty"))
    op(s"gripProduct$X", c("vRobot_car", "empty", s"product$X"))
    op(s"gripProduct$X", c("vRobot_pos", "atIn", s"atInGripping$X", "atIn"))
    v(name = "vRobot_pos", domain = s"atInGripping$X")

    op(s"weldProduct$X", c("vRobot_pos", "atWeld", s"atWeldWelding$X", "atWeld"))
    op(s"weldProduct$X", c("vRobot_car", s"product$X", s"product${X}Welded"))
    v(name = "vRobot_pos", domain = s"atWeldWelding$X")

    x("weldZone", s"vRobot_pos == atWeldWelding$X & vTipDresser == tipdressing")

    op(s"releaseProduct$X", c(s"vOutConveyer${X}_car", "empty", "occupied"))
    op(s"releaseProduct$X", c("vRobot_car", s"product${X}Welded", "empty"))
    op(s"releaseProduct$X", c("vRobot_pos", "atConveyers", s"atConveyersReleasingProduct$X", "atConveyers"))
    v(name = "vRobot_pos", domain = s"atConveyersReleasingProduct$X")

    op(s"removeProduct$X", c(s"vOutConveyer${X}_car", "occupied", "partlyOccupied", "empty"))
    op(s"removeProduct$X", conditions = SPAttributes("preGuard" -> Set(s"vOutConveyer$X==running"), "postGuard" -> Set(s"vOutConveyer$X==running")))
  }
}
