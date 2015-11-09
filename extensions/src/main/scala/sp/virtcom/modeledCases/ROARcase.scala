package sp.virtcom.modeledCases

import sp.domain.SPAttributes
import sp.domain.Logic._
import sp.virtcom.CollectorModel

/**
 * To pick up a single bin
 */
case class ROARcase(modelName: String = "ROAR - single bin") extends CollectorModel {

  def hAtt(h: String) = SPAttributes("hierarchy" -> Set(h))

  v(name = "vQuad", idleValue = "inDock", domain = Seq("searchingForBins"), attributes = hAtt("Quad"))
  v(name = "vBinDetection_mockup", idleValue = "idle", attributes = hAtt("Quad"))
  op("searchForBins", hAtt("Quad") merge
    SPAttributes(aResourceTrans("vQuad", "inDock", "searchingForBins", "inDock")))
  op("newBinDetected_onlyInMockup", hAtt("Quad") merge
    SPAttributes(aResourceTrans("vBinDetection_mockup", "idle", "detecting", "idle")) merge
    SPAttributes("preGuard" -> Set("vQuad == searchingForBins")) merge
    SPAttributes(aCarrierTrans("vDetectedBins", atStart = "non", atComplete = "oneBin"))
  )

  //  op("dock", hAtt("Quad"))
  v(name = "vDetectedBins", idleValue = "non", attributes = hAtt("PathPlanner"))
  v(name = "vPath", attributes = hAtt("PathPlanner"))
  v(name = "vPathPlanner", idleValue = "idle", attributes = hAtt("PathPlanner"))
  op("calculatePathToTruck", hAtt("PathPlanner") merge
    SPAttributes(aResourceTrans("vPathPlanner", "idle", "planningPathToTruck", "idle")) merge
    SPAttributes(aCarrierTrans("vPath", atComplete = "toTruckPath"))
  )
  op("calculatePathToBin", hAtt("PathPlanner") merge
    SPAttributes(aResourceTrans("vPathPlanner", "idle", "planningToBin", "idle")) merge
    SPAttributes(aCarrierTrans("vPath", atComplete = "toBinPath")) merge
    SPAttributes(aCarrierTrans("vDetectedBins", atStart = "oneBin", atComplete = "non"))
  )
  op("calculatePathToOldBin", hAtt("PathPlanner") merge
    SPAttributes(aResourceTrans("vPathPlanner", "idle", "planningToOldBin", "idle")) merge
    SPAttributes(aCarrierTrans("vPath", atComplete = "toOldBinPath"))
  )

  //  domain = Seq("IdleAtTruck, GoingToBin, AtFullBin, PickingUpFullBin, GoingToTruckWithBin, At-
  //    TruckLift, BinPlaced,WaitingForBinToBeEmptied, PickingUpEmptyBin, PickedUpEmptyBin, ReturningEmp-
  //    tyBin, AtEmptyBin, DroppingBin, ReturningToTruckEmptyHanded)
  v(name = "roary_load", idleValue = "no", attributes = hAtt("Roary"))
  v(name = "roary_pos", idleValue = "idleAtTruck", attributes = hAtt("Roary"))
  op("goToBin", hAtt("Roary")
    merge SPAttributes(aResourceTrans("roary_pos", "idleAtTruck", "goingToBin", "atFullBin")) merge
    SPAttributes("preGuard" -> Set("roary_load == no")) merge
    SPAttributes(aCarrierTrans("vPath", atStart = "toBinPath"))
  )
  op("pickUpBin", hAtt("Roary") merge
    SPAttributes(aResourceTrans("roary_pos", "atFullBin", "pickingUpFullBin", "atFullBin")) merge
    SPAttributes(aResourceTrans("roary_load", "no", "noToFull", "full")))
  op("returnToTruckWithBin", hAtt("Roary") merge
    SPAttributes(aResourceTrans("roary_pos", "atFullBin", "goingToTruckWithBin", "atTruckLift")) merge
    SPAttributes("preGuard" -> Set("roary_load == full")) merge
    SPAttributes(aCarrierTrans("vPath", atStart = "toTruckPath"))
  )
  op("placeBinAtTruckLift", hAtt("Roary") merge
    SPAttributes(aResourceTrans("roary_pos", "atTruckLift", "placingBinAtLift", "binPlaced")) merge
    SPAttributes(aCarrierTrans("roary_load", atStart = "full", atComplete = "no")) merge
    SPAttributes(aCarrierTrans("vTruckLift_load", atStart = "no", atComplete = "fullbin"))
  )
  op("backAwayFromLift", hAtt("Roary") merge
    SPAttributes(aResourceTrans("roary_pos", "binPlaced", "backingAway", "waitingForBinToBeEmptied")) merge
    SPAttributes("preGuard" -> Set("roary_load == no")))
  op("pickUpEmptyBin", hAtt("Roary") merge
    SPAttributes(aResourceTrans("roary_pos", "waitingForBinToBeEmptied", "pickingUpEmptyBin", "pickedUpEmptyBin")) merge
    SPAttributes(aResourceTrans("roary_load", "no", "noToEmpty", "empty")) merge
    SPAttributes(aCarrierTrans("vTruckLift_load", atStart = "empty", atComplete = "no"))
  )
  op("returnBin", hAtt("Roary") merge
    SPAttributes(aResourceTrans("roary_pos", "pickedUpEmptyBin", "returningEmptyBin", "atEmptyBin")) merge
    SPAttributes("preGuard" -> Set("roary_load == empty")) merge
    SPAttributes(aCarrierTrans("vPath", atStart = "toOldBinPath"))
  )
  op("dropBin", hAtt("Roary") merge
    SPAttributes(aResourceTrans("roary_pos", "atEmptyBin", "droppingBin", "atEmptyBin")) merge
    SPAttributes(aResourceTrans("roary_load", "empty", "emptyToNo", "no")))
  op("returnHome", hAtt("Roary") merge
    SPAttributes(aResourceTrans("roary_pos", "atEmptyBin", "returningToTruckEmptyHanded", "idleAtTruck")) merge
    SPAttributes("preGuard" -> Set("roary_load == no")) merge
    SPAttributes(aCarrierTrans("vPath", atStart = "toTruckPath"))
  )

  v(name = "vTruckLift_pos", idleValue = "atHome", attributes = hAtt("TruckLift"))
  v(name = "vTruckLift_load", idleValue = "no", attributes = hAtt("TruckLift"))
  op("emptyingBin", hAtt("TruckLift") merge
    SPAttributes(aResourceTrans("vTruckLift_pos", "atHome", "emptying", "atHome")) merge
    SPAttributes(aCarrierTrans("vTruckLift_load", atStart = "fullbin", atComplete = "empty"))
  )

  x("truckLiftZone", "vTruckLift_pos == emptying && roary_pos != waitingForBinToBeEmptied")

}
