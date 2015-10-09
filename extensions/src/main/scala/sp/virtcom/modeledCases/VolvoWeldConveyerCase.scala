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
//case class VolvoWeldConveyerCase() extends ExplicitPrePost with SimOps
case class VolvoWeldConveyerCase(modelName: String = "Volvo Weld Conveyer") extends ImplicitOnlyCarriersAndResources with SimOps with SlowGripWhenOperatorPresent

sealed trait ImplicitOnlyCarriersAndResources extends CollectorModel {
  //Resources/Machines/Variables
  v(name = "vRobot_pos", idleValue = "atHome")
  v(name = "vOperator", idleValue = "idle")
  v(name = "vTipDresser", idleValue = "idle")

  //Conveyers
  conveyerX("A")
  conveyerX("B")

  //Product A
  productX("A")

  //Product B
  productX("B")

  //Tip dressing
  op("tipDress", SPAttributes(aResourceTrans("vTipDresser", "idle", "tipdressing", "idle")))
  x("weldZoneA", operations = "tipDress")
  x("weldZoneB", operations = "tipDress")

  //Robot movements
  lazy val staticRobotPoses = SPAttributes("atHome" -> Set(SPAttributes("to" -> "atIn0", "simop" -> "1,134")),
    "atIn1" -> Set(SPAttributes("to" -> "atWeld0", "simop" -> "1,178")),
    "atWeld1" -> Set(SPAttributes("to" -> "atConveyers0", "simop" -> "1,235")),
    "atConveyers1" -> Set(SPAttributes("to" -> "atHome", "simop" -> "1,90")))
  robotMovements("v","Robot","_pos", staticRobotPoses)

  //Macros-----------------------------
  def conveyerX(X: String) = {
    v(name = s"vOutConveyer$X", idleValue = "running")
  }

  def productX(X: String) = {
    op(s"addProduct$X", SPAttributes(aCarrierTrans("vIn_car", atComplete = s"product$X"), aResourceTrans("vOperator", "idle", s"addingProduct$X", "idle")))

    op(s"gripProduct$X", SPAttributes(aResourceTrans("vRobot_pos", "atIn0", s"atInGripping$X", "atIn1")))
    op(s"gripProduct$X", SPAttributes(aCarrierTrans("vIn_car", atStart = s"product$X")))
    op(s"gripProduct$X", SPAttributes(aCarrierTrans("vRobot_car", atComplete = s"product$X")))

    op(s"weldProduct$X", SPAttributes(aResourceTrans("vRobot_pos", "atWeld0", s"atWeldWelding$X", "atWeld1")))
    op(s"weldProduct$X", SPAttributes(aCarrierTrans("vRobot_car", s"product$X", s"partlyProduct${X}Welded", s"product${X}Welded")))

    x(s"weldZone$X", operations = s"weldProduct$X")

    op(s"releaseProduct$X", SPAttributes(aCarrierTrans(s"vOutConveyer${X}_car", atComplete = s"product$X"), aResourceTrans("vRobot_pos", "atConveyers0", s"atConveyersReleasingProduct$X", "atConveyers1")))
    op(s"releaseProduct$X", SPAttributes(aCarrierTrans("vRobot_car", atStart = s"product${X}Welded")))

    op(s"removeProduct$X", SPAttributes(aCarrierTrans(s"vOutConveyer${X}_car", atStart = s"product$X"), aResourceTrans(s"vOutConveyer$X", "running", s"removingProduct$X", "running")))
  }
}

sealed trait SlowGripWhenOperatorPresent extends CollectorModel {
  //Operator
  v(name = "vOperator_present", idleValue = "idle")

  op("addOperator",SPAttributes(aResourceTrans("vOperator_present","idle","toIn","inside0")))
  op("inspectByOperator",SPAttributes(aResourceTrans("vOperator_present","inside0","inspecting","inside1")))
  op("removeOperator",SPAttributes(aResourceTrans("vOperator_present","inside1","toOut","idle")))

  op(s"addOperator", SPAttributes("simop" -> "1,272"))
  op(s"inspectByOperator", SPAttributes("simop" -> "1,282"))
  op(s"removeOperator", SPAttributes("simop" -> "1,277"))

  //Grip product A slow
  op(s"gripProductA_slow_1", SPAttributes(aResourceTrans("vRobot_pos", "atIn0", s"atInGrippingA0", "atInGrippingA1")))
  op(s"gripProductA_slow_2", SPAttributes(aResourceTrans("vRobot_pos", "atInGrippingA1", s"atInGrippingA2", "atIn1"))) //Has this second op to get right op seq in other service
  op(s"gripProductA_slow_1", SPAttributes(aCarrierTrans("vIn_car", atStart = s"productA")))
  op(s"gripProductA_slow_1", SPAttributes(aCarrierTrans("vRobot_car", atComplete = s"productA")))

  op(s"gripProductA_slow_1", SPAttributes("simop" -> "1,254"))
  op(s"gripProductA_slow_2", SPAttributes("simop" -> "1,269"))
}

sealed trait SimOps extends CollectorModel {
  op(s"addProductA", SPAttributes("simop" -> "1,99"))
  op(s"gripProductA", SPAttributes("simop" -> "1,144"))
  op(s"weldProductA", SPAttributes("simop" -> "1,185"))
  op(s"releaseProductA", SPAttributes("simop" -> "1,76"))
//  op(s"removeProductA", SPAttributes("simop" -> "1,242"))

  op(s"addProductB", SPAttributes("simop" -> "1,118"))
  op(s"gripProductB", SPAttributes("simop" -> "1,161"))
  op(s"weldProductB", SPAttributes("simop" -> "1,213"))
  op(s"releaseProductB", SPAttributes("simop" -> "1,106"))
//  op(s"removeProductB", SPAttributes("simop" -> "1,248"))

//  op(s"atConveyersToAtHome_Robot", SPAttributes("simop" -> "1,89"))
//  op(s"atHomeToAtIn_Robot", SPAttributes("simop" -> "1,133"))
//  op(s"atInToAtWeld_Robot", SPAttributes("simop" -> "1,171"))
//  op(s"atWeldToAtConveyers_Robot", SPAttributes("simop" -> "1,228"))

  op(s"tipDress", SPAttributes("simop" -> "1,125"))

}

sealed trait ExplicitPrePost extends CollectorModel {

  //Resources/Machines/Variables
  v(name = "vIn_car", domain = Seq("empty", "productA", "productB"), init = "empty", marked = "empty")

  v(name = "vRobot_car", domain = Seq("empty", "productA", "productAWelded", "productB", "productBWelded"), init = "empty", marked = "empty")
  v(name = "vRobot_pos", domain = Seq("atHome", "atIn", "atWeld", "atConveyers"), init = "atHome", marked = "atHome")

  v(name = "vOperator", domain = Seq("idle", "addingProductA", "addingProductB"), init = "idle", marked = "idle")

  v(name = "vTipDresser", domain = Seq("idle", "tipdressing"), init = "idle", marked = "idle")

  x("inZone", s"vOperator == addingProductA & vRobot_pos == atInGrippingB")
  x("inZone", s"vOperator == addingProductB & vRobot_pos == atInGrippingA")

  //Conveyers
  conveyerX("A")
  conveyerX("B")

  //Product A
  productX("A")

  //Product B
  productX("B")

  //Tip dressing
  op("tipDress", c("vTipDresser", "idle", "tipdressing", "idle"))

  //Robot movements
  lazy val staticRobotPoses = Map("atHome" -> Set("atIn"),
    "atIn" -> Set("atWeld"),
    "atWeld" -> Set("atConveyers"),
    "atConveyers" -> Set("atHome"))
  createMoveOperations(robotName = "Robot", staticRobotPoses = staticRobotPoses)

  //Macros-----------------------------
  def conveyerX(X: String) = {
    v(name = s"vOutConveyer$X", domain = Seq("running", s"removingProduct$X"), init = "running", marked = "running")
    v(name = s"vOutConveyer${X}_car", domain = Seq("empty", "partlyOccupied", "occupied"), init = "empty", marked = "empty")
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
    op(s"removeProduct$X", c(s"vOutConveyer$X", "running", s"removingProduct$X", "running"))

  }

}
