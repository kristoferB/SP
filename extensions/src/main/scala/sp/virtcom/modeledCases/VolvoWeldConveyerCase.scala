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

  //Resources/Machines/Variables
    v(name = "vIn_car", domain = Seq("empty", "productA", "productB"), init = "empty", marked = "empty")

    v(name = "vRobot_car", domain = Seq("empty", "productA", "productAWelded", "productB", "productBWelded"), init = "empty", marked = "empty")
    v(name = "vRobot_pos", domain = Seq("atHome", "atIn", "atWeld", "atConveyers"), init = "atHome", marked = "atHome")
  v(name = "vRobot_pos", idleValue = "atHome")

    v(name = "vOperator", domain = Seq("idle", "addingProductA", "addingProductB"), init = "idle", marked = "idle")
  v(name = "vOperator", idleValue = "idle")

    v(name = "vTipDresser", domain = Seq("idle", "tipdressing"), init = "idle", marked = "idle")
  v(name = "vTipDresser", idleValue = "idle")
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
  op("tipDress", attributes = SPAttributes(aResourceTrans("vTipDresser", "idle", "tipdressing", "idle")))
  x("weldZone", operations = "tipDress")

  //Robot movements
  lazy val staticRobotPoses = Map("atHome" -> Set("atIn"),
    "atIn" -> Set("atWeld"),
    "atWeld" -> Set("atConveyers"),
    "atConveyers" -> Set("atHome"))
    createMoveOperations(robotName = "Robot", staticRobotPoses = staticRobotPoses)

  // PS Visualization
    op(s"addProductA", attributes = SPAttributes("simop" -> "1,98"))
    op(s"gripProductA", attributes = SPAttributes("simop" -> "1,145"))
    op(s"weldProductA", attributes = SPAttributes("simop" -> "1,178"))
    op(s"releaseProductA", attributes = SPAttributes("simop" -> "1,75"))
    op(s"removeProductA", attributes = SPAttributes("simop" -> "1,235"))

    op(s"addProductB", attributes = SPAttributes("simop" -> "1,117"))
    op(s"gripProductB", attributes = SPAttributes("simop" -> "1,158"))
    op(s"weldProductB", attributes = SPAttributes("simop" -> "1,206"))
    op(s"releaseProductB", attributes = SPAttributes("simop" -> "1,105"))
    op(s"removeProductB", attributes = SPAttributes("simop" -> "1,241"))

    op(s"atConveyersToAtHome_Robot", attributes = SPAttributes("simop" -> "1,89"))
    op(s"atHomeToAtIn_Robot", attributes = SPAttributes("simop" -> "1,133"))
    op(s"atInToAtWeld_Robot", attributes = SPAttributes("simop" -> "1,171"))
    op(s"atWeldToAtConveyers_Robot", attributes = SPAttributes("simop" -> "1,228"))

    op(s"tipDress", attributes = SPAttributes("simop" -> "1,124"))

  //Macros-----------------------------
  def conveyerX(X: String) = {
    v(name = s"vOutConveyer$X", domain = Seq("running", s"removingProduct$X"), init = "running", marked = "running")
    v(name = s"vOutConveyer${X}_car", domain = Seq("empty", "partlyOccupied", "occupied"), init = "empty", marked = "empty")
  }

  def productX(X: String) = {
        op(s"addProduct$X", c("vIn_car", "empty", s"product$X"))
        op(s"addProduct$X", c("vOperator", "idle", s"addingProduct$X", "idle"))
    op(s"addProduct$X", attributes = SPAttributes(aCarrierTrans("vIn_car", atExecute = s"partlyProduct$X", atComplete = s"product$X"), aResourceTrans("vOperator", "idle", s"addingProduct$X", "idle")))

        x("inZone", s"vOperator == addingProduct$X & vRobot_pos == atInGripping$X", operations = Set(s"addProduct$X",s"gripProduct$X"))
    x("inZone", operations = Set(s"addProduct$X", s"gripProduct$X"))

        op(s"gripProduct$X", c("vIn_car", s"product$X", "empty"))
        op(s"gripProduct$X", c("vRobot_car", "empty", s"product$X"))
        op(s"gripProduct$X", c("vRobot_pos", "atIn", s"atInGripping$X", "atIn"))
        v(name = "vRobot_pos", domain = s"atInGripping$X")
    op(s"gripProduct$X", attributes = SPAttributes(aResourceTrans("vRobot_pos", "atIn0", s"atInGripping$X", "atIn1")))
    op(s"gripProduct$X", attributes = SPAttributes(aCarrierTrans("vIn_car", atStart = s"product$X", atExecute = s"partlyProduct$X")))
    op(s"gripProduct$X", attributes = SPAttributes(aCarrierTrans("vRobot_car", atExecute = s"partlyProduct$X", atComplete = s"product$X")))

        op(s"weldProduct$X", c("vRobot_pos", "atWeld", s"atWeldWelding$X", "atWeld"))
        op(s"weldProduct$X", c("vRobot_car", s"product$X", s"product${X}Welded"))
        v(name = "vRobot_pos", domain = s"atWeldWelding$X")
    op(s"weldProduct$X", attributes = SPAttributes(aResourceTrans("vRobot_pos", "atWeld0", s"atWeldWelding$X", "atWeld1")))
    op(s"weldProduct$X", attributes = SPAttributes(aCarrierTrans("vRobot_car", atStart = s"product$X", atExecute = s"partlyProduct${X}Welded", atComplete = s"product${X}Welded")))

        x("weldZone", s"vRobot_pos == atWeldWelding$X & vTipDresser == tipdressing", operations = s"weldProduct$X")
    x("weldZone", operations = s"weldProduct$X")

        op(s"releaseProduct$X", c(s"vOutConveyer${X}_car", "empty", "occupied"))
        op(s"releaseProduct$X", c("vRobot_car", s"product${X}Welded", "empty"))
        op(s"releaseProduct$X", c("vRobot_pos", "atConveyers", s"atConveyersReleasingProduct$X", "atConveyers"))
        v(name = "vRobot_pos", domain = s"atConveyersReleasingProduct$X")
    op(s"releaseProduct$X", attributes = SPAttributes(aCarrierTrans(s"vOutConveyer${X}_car", atExecute = s"partlyProduct$X", atComplete = s"product$X"), aResourceTrans("vRobot_pos", "atConveyers0", s"atConveyersReleasingProduct$X", "atConveyers1")))
    op(s"weldProduct$X", attributes = SPAttributes(aCarrierTrans("vRobot_car", atStart = s"product${X}Welded", atExecute = s"partlyProduct${X}Welded")))

        op(s"removeProduct$X", c(s"vOutConveyer${X}_car", "occupied", "partlyOccupied", "empty"))
    op(s"removeProduct$X", attributes = SPAttributes(aCarrierTrans(s"vOutConveyer${X}_car", atStart = s"product$X", atExecute = s"partlyProduct$X"),aResourceTrans(s"vConveyer$X", "running", s"removingProduct$X", "running")))

    product(s"Product$X", Seq(s"addProduct$X", s"gripProduct$X", s"weldProduct$X", s"releaseProduct$X", s"removeProduct$X"))
  }
}
