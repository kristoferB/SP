
import sp.domain._

class operationMaker {

  // Things for Guards--------------------------------------------------------------------------------------------------
  val buildSpace1Booked = Thing("buildSpace1Booked")
  //For palettpos1
  val buildSpace2Booked = Thing("buildSpace2Booked")
  //For palettpos2
  val buildSpace3Booked = Thing("buildSpace3Booked")
  //For palettpos3
  val buildSpace4Booked = Thing("buildSpace4Booked")
  //For palettpos4
  val buildSpotBooked = Thing("buildSpotBooked")
  //For the buildingspace were the tower should be built
  val R2Booked = Thing("R2Booked")
  //Book robot 2
  val R4Booked = Thing("R4Booked")
  //Book robot 4
  val R5Booked = Thing("R5Booked")
  //Book robot 5
  val R4Dodge = Thing("R4Dodge")
  //If robot 4 is in dodge pos
  val R5Dodge = Thing("R5Dodge")
  //If robot 4 is in dodge pos
  val H1NoneEmpty = Thing("H4Empty")
  val H1Up = Thing("H4Up")
  //Hiss 1
  val H2Empty = Thing("H4Empty")
  val H2Up = Thing("H4Up")
  //Hiss 2
  val buildPallet1Empty = Thing("buildPallet1Empty")
  //Status of buildpallet 1
  val buildPallet2Empty = Thing("buildPallet2Empty")
  //Status of buildpallet 2
  val buildingPalettComplete = Thing("(aR2PalettRemoveR5Pos1,aBookR2")
  //Status of buildningpallet
  val R2OPComplete = Thing("R2OPComplete")
  //Tells when R2 is done
  val R4OPComplete = Thing("R4OPComplete")
  //Tells when R4 is done
  val R5OPComplete = Thing("R5OPComplete")
  //Tells when R5 is done
  val R4HoldingCube = Thing("R4HoldingCube")
  //Tells if R4 is holding a cube
  val R5HoldingCube = Thing("R5HoldingCube")
  //Tells if R4 is holding a cube

  // Things for Actions-------------------------------------------------------------------------------------------------
  val R2PalettToR5Pos1 = Thing("R2PalettToR5Pos1")
  val R2PalettToR5Pos2 = Thing("R2PalettToR5Pos2")
  val R2PalettToR4Pos1 = Thing("R2PalletToR4Pos1")
  val R2PalettToR4Pos2 = Thing("R2PalettToR4Pos2")
  val R2PalettRemoveR5Pos1 = Thing("R2PalettRemoveR5Pos1")
  val R2PalettRemoveR5Pos2 = Thing("R2PalettRemoveR5Pos2")
  val R2PalettRemoveR4Pos1 = Thing("R2PalettRemoveR4Pos1")
  val R2PalettRemoveR4Pos2 = Thing("R2PalettRemoveR4Pos2")
  val R2PlaceBuildingPalett = Thing("R2PlaceBuildingPalett")
  val R2RemoveBuildingPalett = Thing("R2RemoveBuildingPalett")
  //discribes at which pallet (first diget) and position (seconde diget) R4 picks up
  val R4PickUpAt11 = Thing("R4PickUpAt11")
  val R4PickUpAt12 = Thing("R4PickUpAt12")
  val R4PickUpAt13 = Thing("R4PickUpAt13")
  val R4PickUpAt14 = Thing("R4PickUpAt14")
  val R4PickUpAt15 = Thing("R4PickUpAt15")
  val R4PickUpAt16 = Thing("R4PickUpAt16")
  val R4PickUpAt17 = Thing("R4PickUpAt17")
  val R4PickUpAt18 = Thing("R4PickUpAt18")
  //discribes at which pallet (first diget) and position (seconde diget) R4 picks up
  val R4PickUpAt21 = Thing("R4PickUpAt21")
  val R4PickUpAt22 = Thing("R4PickUpAt22")
  val R4PickUpAt23 = Thing("R4PickUpAt23")
  val R4PickUpAt24 = Thing("R4PickUpAt24")
  val R4PickUpAt25 = Thing("R4PickUpAt25")
  val R4PickUpAt26 = Thing("R4PickUpAt26")
  val R4PickUpAt27 = Thing("R4PickUpAt27")
  val R4PickUpAt28 = Thing("R4PickUpAt28")
  //discribes at which pallet (first diget) and position (seconde diget) R5 picks up
  val R5PickUpAt11 = Thing("R4PickUpAt11")
  val R5PickUpAt12 = Thing("R4PickUpAt12")
  val R5PickUpAt13 = Thing("R4PickUpAt13")
  val R5PickUpAt14 = Thing("R4PickUpAt14")
  val R5PickUpAt15 = Thing("R4PickUpAt15")
  val R5PickUpAt16 = Thing("R4PickUpAt16")
  val R5PickUpAt17 = Thing("R4PickUpAt17")
  val R5PickUpAt18 = Thing("R4PickUpAt18")
  //discribes at which pallet (first diget) and position (seconde diget) R5 picks up
  val R5PickUpAt21 = Thing("R4PickUpAt21")
  val R5PickUpAt22 = Thing("R4PickUpAt22")
  val R5PickUpAt23 = Thing("R4PickUpAt23")
  val R5PickUpAt24 = Thing("R4PickUpAt24")
  val R5PickUpAt25 = Thing("R4PickUpAt25")
  val R5PickUpAt26 = Thing("R4PickUpAt26")
  val R5PickUpAt27 = Thing("R4PickUpAt27")
  val R5PickUpAt28 = Thing("R4PickUpAt28")


  val parserG = sp.domain.logic.PropositionParser(List(buildSpace1Booked, buildSpace2Booked, buildSpace3Booked, buildSpace4Booked,
    R2Booked, R4Booked, R5Booked, R4Dodge, R5Dodge, H1NoneEmpty, H1Up, H2Up, H2Empty, buildSpotBooked,buildPallet1Empty,buildPallet2Empty,
    buildingPalettComplete,R2OPComplete,R4OPComplete,R5OPComplete,R4HoldingCube,R5HoldingCube))

  val parserA = sp.domain.logic.ActionParser(List(R2PalettToR5Pos1, R2PalettToR5Pos2, R2PalettToR4Pos1, R2PalettToR4Pos2, R2PalettRemoveR5Pos1,
    R2PalettRemoveR5Pos2, R2PalettRemoveR4Pos1, R2PalettRemoveR4Pos2, R2PlaceBuildingPalett, R2RemoveBuildingPalett, buildSpace1Booked,
    buildSpace2Booked, buildSpace3Booked, buildSpace4Booked, R2Booked, R4Booked, R5Booked,R4HoldingCube,R5HoldingCube,
    R4PickUpAt11, R4PickUpAt12, R4PickUpAt13, R4PickUpAt14, R4PickUpAt15, R4PickUpAt16, R4PickUpAt17, R4PickUpAt18,
    R4PickUpAt21, R4PickUpAt22, R4PickUpAt23, R4PickUpAt24, R4PickUpAt25, R4PickUpAt26, R4PickUpAt27, R4PickUpAt28,
    R5PickUpAt11, R5PickUpAt12, R5PickUpAt13, R5PickUpAt14, R5PickUpAt15, R5PickUpAt16, R5PickUpAt17, R5PickUpAt18,
    R5PickUpAt21, R5PickUpAt22, R5PickUpAt23, R5PickUpAt24, R5PickUpAt25, R5PickUpAt26, R5PickUpAt27, R5PickUpAt28))

  //Create gaurds-------------------------------------------------------------------------------------------------------
  val gBuildSpace1Booked = parserG.parseStr("buildSpace1Booked == true").right.get
  val gBuildSpace2Booked = parserG.parseStr("buildSpace2Booked == true").right.get
  val gBuildSpace3Booked = parserG.parseStr("buildSpace3Booked == true").right.get
  val gBuildSpace4Booked = parserG.parseStr("buildSpace4Booked == true").right.get
  val gR4HoldingCube = parserG.parseStr("R4HoldingCube == true").right.get
  val gR5HoldingCube = parserG.parseStr("R5HoldingCube == true").right.get
  val gR2Booked = parserG.parseStr("R2Booked == true").right.get
  val gR4Booked = parserG.parseStr("R4Booked == true").right.get
  val gR5Booked = parserG.parseStr("R5Booked == true").right.get
  val gR2OPComplete = parserG.parseStr("R2OPComplete == true").right.get
  val gR4OPComplete = parserG.parseStr("R4OPComplete == true").right.get
  val gR5OPComplete = parserG.parseStr("R5OPComplete == true").right.get
  val gR4dodge = parserG.parseStr("R4Dodge == true").right.get
  val gR5dodge = parserG.parseStr("R4Dodge == true").right.get
  val gBuildSpotBooked =  parserG.parseStr("buildSpotBooked == true").right.get
  val gBuildPallet1Empty = parserG.parseStr("buildPallet1Empty == true").right.get
  val gBuildPallet2Empty = parserG.parseStr("buildPallet2Empty == true").right.get
  val gBuildingPalettComplete = parserG.parseStr("buildingPalettComplete == true").right.get

  //Create actions------------------------------------------------------------------------------------------------------
  // example val aGenerateOperatorInstructions = parserA.parseStr("generateOperatorInstructions = True").right.get
  //Actions for booking
  val aBookR2 = parserA.parseStr("R2Booked = true").right.get
  val aBookR4 = parserA.parseStr("R4Booked = true").right.get
  val aBookR5 = parserA.parseStr("R5Booked = true").right.get
  val aUnBookR2 = parserA.parseStr("R2Booked = false").right.get
  val aUnBookR4 = parserA.parseStr("R4Booked = false").right.get
  val aUnBookR5 = parserA.parseStr("R5Booked = false").right.get
  val aBuildSpace1Book = parserA.parseStr("buildSpace1Booked = true").right.get
  val aBuildSpace2Book = parserA.parseStr("buildSpace2Booked = true").right.get
  val aBuildSpace3Book = parserA.parseStr("buildSpace3Booked = true").right.get
  val aBuildSpace4Book = parserA.parseStr("buildSpace4Booked = true").right.get
  val aBuildSpace1UnBook = parserA.parseStr("buildSpace1UnBooked = false").right.get
  val aBuildSpace2UnBook = parserA.parseStr("buildSpace2UnBooked = false").right.get
  val aBuildSpace3UnBook = parserA.parseStr("buildSpace3UnBooked = false").right.get
  val aBuildSpace4UnBook = parserA.parseStr("buildSpace4UnBooked = false").right.get
  val aBuildSpotBook = parserA.parseStr("buildSpotBooked = true").right.get
  val aBuildSpotUnBook = parserA.parseStr("buildSpotBooked = false").right.get
  //Actions for moving bulding pallets
  val aR2PalettToR5Pos1 = parserA.parseStr("R2PalettToR5Pos1 = true").right.get
  val aR2PalettToR5Pos2 = parserA.parseStr("R2PalettToR5Pos2 = true").right.get
  val aR2PalettToR4Pos1 = parserA.parseStr("R2PalettToR4Pos1 = true").right.get
  val aR2PalettToR4Pos2 = parserA.parseStr("R2PalettToR4Pos2 = true").right.get
  val aR2PalettRemoveR5Pos1 = parserA.parseStr("R2PalettRemoveR5Pos1 = true").right.get
  val aR2PalettRemoveR5Pos2 = parserA.parseStr("R2PalettRemoveR5Pos2 = true").right.get
  val aR2PalettRemoveR4Pos1 = parserA.parseStr("R2PalettRemoveR4Pos1 = true").right.get
  val aR2PalettRemoveR4Pos2 = parserA.parseStr("R2PalettRemoveR4Pos2 = true").right.get
  val aR2PlaceBuildingPalett = parserA.parseStr("R2PlaceBuildingPalett = true").right.get
  val aR2RemoveBuildingPalett = parserA.parseStr("R2RemoveBuildingPalett = true").right.get
  //Actions for picking up cubes for R4 and pallet 1
  val aR4PickUpAt11 = parserA.parseStr("R4PickUpAt11 = true").right.get
  val aR4PickUpAt12 = parserA.parseStr("R4PickUpAt12 = true").right.get
  val aR4PickUpAt13 = parserA.parseStr("R4PickUpAt13 = true").right.get
  val aR4PickUpAt14 = parserA.parseStr("R4PickUpAt14 = true").right.get
  val aR4PickUpAt15 = parserA.parseStr("R4PickUpAt15 = true").right.get
  val aR4PickUpAt16 = parserA.parseStr("R4PickUpAt16 = true").right.get
  val aR4PickUpAt17 = parserA.parseStr("R4PickUpAt17 = true").right.get
  val aR4PickUpAt18 = parserA.parseStr("R4PickUpAt18 = true").right.get
  //Actions for picking up cubes for R4 and pallet 2
  val aR4PickUpAt21 = parserA.parseStr("R4PickUpAt21 = true").right.get
  val aR4PickUpAt22 = parserA.parseStr("R4PickUpAt22 = true").right.get
  val aR4PickUpAt23 = parserA.parseStr("R4PickUpAt23 = true").right.get
  val aR4PickUpAt24 = parserA.parseStr("R4PickUpAt24 = true").right.get
  val aR4PickUpAt25 = parserA.parseStr("R4PickUpAt25 = true").right.get
  val aR4PickUpAt26 = parserA.parseStr("R4PickUpAt26 = true").right.get
  val aR4PickUpAt27 = parserA.parseStr("R4PickUpAt27 = true").right.get
  val aR4PickUpAt28 = parserA.parseStr("R4PickUpAt28 = true").right.get
  //Actions for picking up cubes for R5 and pallet 1
  val aR5PickUpAt11 = parserA.parseStr("R5PickUpAt11 = true").right.get
  val aR5PickUpAt12 = parserA.parseStr("R5PickUpAt12 = true").right.get
  val aR5PickUpAt13 = parserA.parseStr("R5PickUpAt13 = true").right.get
  val aR5PickUpAt14 = parserA.parseStr("R5PickUpAt14 = true").right.get
  val aR5PickUpAt15 = parserA.parseStr("R5PickUpAt15 = true").right.get
  val aR5PickUpAt16 = parserA.parseStr("R5PickUpAt16 = true").right.get
  val aR5PickUpAt17 = parserA.parseStr("R5PickUpAt17 = true").right.get
  val aR5PickUpAt18 = parserA.parseStr("R5PickUpAt18 = true").right.get
  //Actions for picking up cubes for R5 and pallet 2
  val aR5PickUpAt21 = parserA.parseStr("R5PickUpAt21 = true").right.get
  val aR5PickUpAt22 = parserA.parseStr("R5PickUpAt22 = true").right.get
  val aR5PickUpAt23 = parserA.parseStr("R5PickUpAt23 = true").right.get
  val aR5PickUpAt24 = parserA.parseStr("R5PickUpAt24 = true").right.get
  val aR5PickUpAt25 = parserA.parseStr("R5PickUpAt25 = true").right.get
  val aR5PickUpAt26 = parserA.parseStr("R5PickUpAt26 = true").right.get
  val aR5PickUpAt27 = parserA.parseStr("R5PickUpAt27 = true").right.get
  val aR5PickUpAt28 = parserA.parseStr("R5PickUpAt28 = true").right.get
  //Actions which indicate if R4 or R5 is holding a cube
  val aR4HoldingCube = parserA.parseStr("R4HoldingCube = true").right.get
  val aR5HoldingCube = parserA.parseStr("R5HoldingCube = true").right.get
  val aR4NotHoldingCube = parserA.parseStr("R4HoldingCube = false").right.get
  val aR5NotHoldingCube = parserA.parseStr("R5HoldingCube = false").right.get

  //Operations----------------------------------------------------------------------------------------------------------

  //Example
  // val init = Operation("Init", List(PropositionCondition(AND(List()), List(aGenerateOperatorInstructions))),SPAttributes(), ID.newID)

  //R2 Operations
  //AND(List(NOT(gBuildSpace1Booked), NOT(gR2booked)))
  val OR2PalettToR5PalettSpace1 = Operation("OR2PalettToR5PalettSpace1",
    List(PropositionCondition(AND(List(NOT(gBuildSpace1Booked), NOT(gR2Booked), NOT(gR5Booked))), List(aBookR2,aR2PalettToR5Pos1,aBuildSpace1Book))))
  // (Pos1 clear) Action R2 Booked = True
  val OR2PalettToR5PalettSpace2 = Operation("OR2PalettToR5PalettSpace2",
    List(PropositionCondition(AND(List(NOT(gBuildSpace2Booked), NOT(gR2Booked), NOT(gR5Booked), gBuildSpace1Booked, gR5dodge)), List(aBookR2,aR2PalettToR5Pos2,aBuildSpace2Book))))
  // (Pos2 clear AND POS1 filled)
  val OR2PalettToR4PalettSpace1 = Operation("OR2PalettToR4PalettSpace1",
    List(PropositionCondition(AND(List(NOT(gBuildSpace3Booked), NOT(gR2Booked), NOT(gR4Booked))), List(aBookR2,aR2PalettToR4Pos1,aBuildSpace3Book))))
  // (Pos1 clear)
  val OR2PalettToR4PalettSpace2 = Operation("OR2PalettToR4PalettSpace2",
  List(PropositionCondition(AND(List(NOT(gBuildSpace4Booked), NOT(gR2Booked), NOT(gR4Booked), gBuildSpace3Booked)), List(aBookR2,aR2PalettToR4Pos1,aBuildSpace4Book))))
    // (Pos2 clear AND POS1 filled)
  val OR2PalettRemoveR5PalettSpace1 = Operation("OR2PalettRemoveR5PalettSpace1",
      List(PropositionCondition(AND(List(NOT(gR2Booked),OR(List(gBuildPallet1Empty,gBuildPallet2Empty)))), List(aR2PalettRemoveR5Pos1,aBookR2,aBuildSpace1UnBook))))
  // Operation R4BuildFromPos1 Done
  val OR2PalettRemoveR5PalettSpace2 = Operation("OR2PalettRemoveR5PalettSpace2",
    List(PropositionCondition(AND(List(NOT(gR2Booked),OR(List(gBuildPallet1Empty,gBuildPallet2Empty)))), List(aR2PalettRemoveR5Pos2,aBookR2,aBuildSpace2UnBook))))
  // Operation R4BuildFromPos2 Done
  val OR2PalettRemoveR4PalettSpace1 = Operation("OR2PalettRemoveR4PalettSpace1",
    List(PropositionCondition(AND(List(NOT(gR2Booked),OR(List(gBuildPallet1Empty,gBuildPallet2Empty)))), List(aR2PalettRemoveR4Pos1,aBookR2,aBuildSpace3UnBook))))
  // Operation R5BuildFromPos1 Done
  val OR2PalettRemoveR4PalettSpace2 = Operation("OR2PalettRemoveR4PalettSpace2",
    List(PropositionCondition(AND(List(NOT(gR2Booked),OR(List(gBuildPallet1Empty,gBuildPallet2Empty)))), List(aR2PalettRemoveR4Pos2,aBookR2,aBuildSpace4UnBook))))
  // Operation R5BuildFromPos2 Done
  val OR2PlaceBuildingPalett = Operation("OR2PlaceBuildingPalett",
    List(PropositionCondition(AND(List(NOT(gR2Booked),NOT(gR5Booked),NOT(gR4Booked),NOT(gBuildSpotBooked))), List(aBookR2,aR2PlaceBuildingPalett,aBuildSpotBook))))
  //
  val OR2RemoveBuildingPalett = Operation("OR2RemoveBuildingPalett",
  List(PropositionCondition(AND(List(NOT(gR2Booked),NOT(gR5Booked),NOT(gR4Booked),gBuildSpotBooked,gBuildingPalettComplete)), List(aBookR2,aBuildSpotUnBook,aR2RemoveBuildingPalett))))
  // Operation
  val OR2RemoveBookingR2 = Operation("OR2RemoveBookingR2", List(PropositionCondition(AND(List(gR2OPComplete)), List(aUnBookR2))))
  // After operations that books R2
  val OR2RemoveBookingR4 = Operation("OR2RemoveBookingR4", List(PropositionCondition(AND(List(gR4OPComplete)), List(aUnBookR4))))
  // After operations that books R4
  val OR2RemoveBookingR5 = Operation("OR2RemoveBookingR5", List(PropositionCondition(AND(List(gR5OPComplete)), List(aUnBookR5))))
  // After operations that books R5
  //OPs for picking up cubes by R4 at spot 1
  val OR4PickUpAt11 = Operation("OR4PickUpAt11", List(PropositionCondition(AND(List(NOT(gR4Booked),gBuildSpace1Booked)), List(aR4PickUpAt11,aBookR4,aR4HoldingCube))))
  val OR4PickUpAt12 = Operation("OR4PickUpAt12", List(PropositionCondition(AND(List(NOT(gR4Booked),gBuildSpace1Booked)), List(aR4PickUpAt12,aBookR4,aR4HoldingCube))))
  val OR4PickUpAt13 = Operation("OR4PickUpAt13", List(PropositionCondition(AND(List(NOT(gR4Booked),gBuildSpace1Booked)), List(aR4PickUpAt13,aBookR4,aR4HoldingCube))))
  val OR4PickUpAt14 = Operation("OR4PickUpAt14", List(PropositionCondition(AND(List(NOT(gR4Booked),gBuildSpace1Booked)), List(aR4PickUpAt14,aBookR4,aR4HoldingCube))))
  val OR4PickUpAt15 = Operation("OR4PickUpAt15", List(PropositionCondition(AND(List(NOT(gR4Booked),gBuildSpace1Booked)), List(aR4PickUpAt15,aBookR4,aR4HoldingCube))))
  val OR4PickUpAt16 = Operation("OR4PickUpAt16", List(PropositionCondition(AND(List(NOT(gR4Booked),gBuildSpace1Booked)), List(aR4PickUpAt16,aBookR4,aR4HoldingCube))))
  val OR4PickUpAt17 = Operation("OR4PickUpAt17", List(PropositionCondition(AND(List(NOT(gR4Booked),gBuildSpace1Booked)), List(aR4PickUpAt17,aBookR4,aR4HoldingCube))))
  val OR4PickUpAt18 = Operation("OR4PickUpAt18", List(PropositionCondition(AND(List(NOT(gR4Booked),gBuildSpace1Booked)), List(aR4PickUpAt18,aBookR4,aR4HoldingCube))))
  //OPs for picking up cubes by R4 at spot 2
  val OR4PickUpAt21 = Operation("OR4PickUpAt21", List(PropositionCondition(AND(List(NOT(gR4Booked),gBuildSpace2Booked)), List(aR4PickUpAt21,aBookR4,aR4HoldingCube))))
  val OR4PickUpAt22 = Operation("OR4PickUpAt22", List(PropositionCondition(AND(List(NOT(gR4Booked),gBuildSpace2Booked)), List(aR4PickUpAt22,aBookR4,aR4HoldingCube))))
  val OR4PickUpAt23 = Operation("OR4PickUpAt23", List(PropositionCondition(AND(List(NOT(gR4Booked),gBuildSpace2Booked)), List(aR4PickUpAt23,aBookR4,aR4HoldingCube))))
  val OR4PickUpAt24 = Operation("OR4PickUpAt24", List(PropositionCondition(AND(List(NOT(gR4Booked),gBuildSpace2Booked)), List(aR4PickUpAt24,aBookR4,aR4HoldingCube))))
  val OR4PickUpAt25 = Operation("OR4PickUpAt25", List(PropositionCondition(AND(List(NOT(gR4Booked),gBuildSpace2Booked)), List(aR4PickUpAt25,aBookR4,aR4HoldingCube))))
  val OR4PickUpAt26 = Operation("OR4PickUpAt26", List(PropositionCondition(AND(List(NOT(gR4Booked),gBuildSpace2Booked)), List(aR4PickUpAt26,aBookR4,aR4HoldingCube))))
  val OR4PickUpAt27 = Operation("OR4PickUpAt27", List(PropositionCondition(AND(List(NOT(gR4Booked),gBuildSpace2Booked)), List(aR4PickUpAt27,aBookR4,aR4HoldingCube))))
  val OR4PickUpAt28 = Operation("OR4PickUpAt28", List(PropositionCondition(AND(List(NOT(gR4Booked),gBuildSpace2Booked)), List(aR4PickUpAt28,aBookR4,aR4HoldingCube))))
  //OPs for picking up cubes by R5 at spot 1
  val OR5PickUpAt11 = Operation("OR5PickUpAt11", List(PropositionCondition(AND(List(NOT(gR5Booked),gBuildSpace3Booked)), List(aR5PickUpAt11,aBookR5,aR5HoldingCube))))
  val OR5PickUpAt12 = Operation("OR5PickUpAt12", List(PropositionCondition(AND(List(NOT(gR5Booked),gBuildSpace3Booked)), List(aR5PickUpAt12,aBookR5,aR5HoldingCube))))
  val OR5PickUpAt13 = Operation("OR5PickUpAt13", List(PropositionCondition(AND(List(NOT(gR5Booked),gBuildSpace3Booked)), List(aR5PickUpAt13,aBookR5,aR5HoldingCube))))
  val OR5PickUpAt14 = Operation("OR5PickUpAt14", List(PropositionCondition(AND(List(NOT(gR5Booked),gBuildSpace3Booked)), List(aR5PickUpAt14,aBookR5,aR5HoldingCube))))
  val OR5PickUpAt15 = Operation("OR5PickUpAt15", List(PropositionCondition(AND(List(NOT(gR5Booked),gBuildSpace3Booked)), List(aR5PickUpAt15,aBookR5,aR5HoldingCube))))
  val OR5PickUpAt16 = Operation("OR5PickUpAt16", List(PropositionCondition(AND(List(NOT(gR5Booked),gBuildSpace3Booked)), List(aR5PickUpAt16,aBookR5,aR5HoldingCube))))
  val OR5PickUpAt17 = Operation("OR5PickUpAt17", List(PropositionCondition(AND(List(NOT(gR5Booked),gBuildSpace3Booked)), List(aR5PickUpAt17,aBookR5,aR5HoldingCube))))
  val OR5PickUpAt18 = Operation("OR5PickUpAt18", List(PropositionCondition(AND(List(NOT(gR5Booked),gBuildSpace3Booked)), List(aR5PickUpAt18,aBookR5,aR5HoldingCube))))
  //OPs for picking up cubes by R5 at spot 2
  val OR5PickUpAt21 = Operation("OR5PickUpAt21", List(PropositionCondition(AND(List(NOT(gR5Booked),gBuildSpace4Booked)), List(aR5PickUpAt21,aBookR5,aR5HoldingCube))))
  val OR5PickUpAt22 = Operation("OR5PickUpAt22", List(PropositionCondition(AND(List(NOT(gR5Booked),gBuildSpace4Booked)), List(aR5PickUpAt22,aBookR5,aR5HoldingCube))))
  val OR5PickUpAt23 = Operation("OR5PickUpAt23", List(PropositionCondition(AND(List(NOT(gR5Booked),gBuildSpace4Booked)), List(aR5PickUpAt23,aBookR5,aR5HoldingCube))))
  val OR5PickUpAt24 = Operation("OR5PickUpAt24", List(PropositionCondition(AND(List(NOT(gR5Booked),gBuildSpace4Booked)), List(aR5PickUpAt24,aBookR5,aR5HoldingCube))))
  val OR5PickUpAt25 = Operation("OR5PickUpAt25", List(PropositionCondition(AND(List(NOT(gR5Booked),gBuildSpace4Booked)), List(aR5PickUpAt25,aBookR5,aR5HoldingCube))))
  val OR5PickUpAt26 = Operation("OR5PickUpAt26", List(PropositionCondition(AND(List(NOT(gR5Booked),gBuildSpace4Booked)), List(aR5PickUpAt26,aBookR5,aR5HoldingCube))))
  val OR5PickUpAt27 = Operation("OR5PickUpAt27", List(PropositionCondition(AND(List(NOT(gR5Booked),gBuildSpace4Booked)), List(aR5PickUpAt27,aBookR5,aR5HoldingCube))))
  val OR5PickUpAt28 = Operation("OR5PickUpAt28", List(PropositionCondition(AND(List(NOT(gR5Booked),gBuildSpace4Booked)), List(aR5PickUpAt28,aBookR5,aR5HoldingCube))))

}



/*

  //Init parsers
  val parserG = sp.domain.logic.PropositionParser(List(newBuildOrder, systemReady))
  val parserA = sp.domain.logic.ActionParser(List(generateOperatorInstructions))

  // Things for Guards
  val newBuildOrder = Thing("newBuildOrder")
  val systemReady = Thing("systemReady")
  val operatorInstructionsGenerated = Thing("operatorInstructionsGenerated")
  val palettReady = Thing("palettReady")


  // Things for Actions
  val generateOperatorInstructions = Thing("generateOperatorInstructions")



  //Create gaurds
  val gNewBuildOrder = parserG.parseStr("newBuildOrder == true").right.get
  val gSystemReady = parserG.parseStr("systemReady == true").right.get
  //Create actions
  val aGenerateOperatorInstructions = parserA.parseStr("generateOperatorInstructions = True").right.get

  //Operations
  val init = Operation("Init", List(PropositionCondition(AND(List(gNewBuildOrder,gSystemReady)), List(aGenerateOperatorInstructions))),SPAttributes(), ID.newID)
  //När det finns instruktioner genererade och palett ej redo, visa instruktioner
  val materialInput = Operation("materialInput", List(PropositionCondition()))
  //När en palett är redo, kör band och sätt palett ej redo
  val flexLinkRun = Operation
*/
