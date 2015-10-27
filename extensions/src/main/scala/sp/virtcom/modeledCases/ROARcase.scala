package sp.virtcom.modeledCases

import sp.domain.SPAttributes
import sp.domain.Logic._
import sp.virtcom.CollectorModel

/**
 * To pick up a single bin
 */
case class ROARcase(modelName: String = "ROAR - single bin") extends CollectorModel {

  def hAtt(h: String) = SPAttributes("hierarchy" -> h)

  op("searchForBins", hAtt("Quad"))
  op("dock", hAtt("Quad"))
  op("calculatePaths", hAtt("PathPlanner"))

  //  domain = Seq("IdleAtTruck, GoingToBin, AtFullBin, PickingUpFullBin, GoingToTruckWithBin, At-
  //    TruckLift, BinPlaced,WaitingForBinToBeEmptied, PickingUpEmptyBin, PickedUpEmptyBin, ReturningEmp-
  //    tyBin, AtEmptyBin, DroppingBin, ReturningToTruckEmptyHanded)
  v(name = "roary_load", idleValue = "no", attributes = hAtt("Roary"))
  v(name = "roary_pos", idleValue = "idleAtTruck", attributes = hAtt("Roary"))
  op("goToBin", hAtt("Roary")
    merge SPAttributes(aResourceTrans("roary_pos", "idleAtTruck", "goingToBin", "atFullBin")) merge
    SPAttributes("preGuard" -> Set("roary_load == no")))
  op("pickUpBin", hAtt("Roary") merge
    SPAttributes(aResourceTrans("roary_pos", "atFullBin", "pickingUpFullBin", "atFullBin")) merge
    SPAttributes(aResourceTrans("roary_load", "no", "noToFull", "full")))
  op("returnToTruckWithBin", hAtt("Roary") merge
    SPAttributes(aResourceTrans("roary_pos", "atFullBin", "goingToTruckWithBin", "atTruckLift")) merge
    SPAttributes("preGuard" -> Set("roary_load == full")))
  op("placeBinAtTruckLift", hAtt("Roary") merge
    SPAttributes(aResourceTrans("roary_pos", "atTruckLift", "placingBinAtLift", "binPlaced")) merge
    SPAttributes(aResourceTrans("roary_load", "full", "fullToNo", "no")))
  op("backAwayFromLift", hAtt("Roary") merge
    SPAttributes(aResourceTrans("roary_pos", "binPlaced", "backingAway", "waitingForBinToBeEmptied")) merge
    SPAttributes("preGuard" -> Set("roary_load == no")))
  op("pickUpEmptyBin", hAtt("Roary") merge
    SPAttributes(aResourceTrans("roary_pos", "waitingForBinToBeEmptied", "pickingUpEmptyBin", "pickedUpEmptyBin")) merge
    SPAttributes(aResourceTrans("roary_load", "no", "noToEmpty", "empty")))
  op("returnBin", hAtt("Roary") merge
    SPAttributes(aResourceTrans("roary_pos", "pickedUpEmptyBin", "returningEmptyBin", "atEmptyBin")) merge
    SPAttributes("preGuard" -> Set("roary_load == empty")))
  op("dropBin", hAtt("Roary") merge
    SPAttributes(aResourceTrans("roary_pos", "atEmptyBin", "droppingBin", "atEmptyBin")) merge
    SPAttributes(aResourceTrans("roary_load", "empty", "emptyToNo", "no")))
  op("returnHome", hAtt("Roary") merge
    SPAttributes(aResourceTrans("roary_pos", "atEmptyBin", "returningToTruckEmptyHanded", "idleAtTruck")) merge
    SPAttributes("preGuard" -> Set("roary_load == no")))

  op("grippingBin", hAtt("TruckLift"))
  op("emptyingBin", hAtt("TruckLift"))
  op("releasingBin", hAtt("TruckLift"))

}
