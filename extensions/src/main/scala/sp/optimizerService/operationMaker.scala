
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
  //Discrabes things were R4 can put cubes
  val R4PutCubeAt11 = Thing("R4PutCubeAt11")
  val R4PutCubeAt12 = Thing("R4PutCubeAt12")
  val R4PutCubeAt13 = Thing("R4PutCubeAt13")
  val R4PutCubeAt14 = Thing("R4PutCubeAt14")
  val R4PutCubeAt21 = Thing("R4PutCubeAt21")
  val R4PutCubeAt22 = Thing("R4PutCubeAt22")
  val R4PutCubeAt23 = Thing("R4PutCubeAt23")
  val R4PutCubeAt24 = Thing("R4PutCubeAt24")
  val R4PutCubeAt31 = Thing("R4PutCubeAt31")
  val R4PutCubeAt32 = Thing("R4PutCubeAt32")
  val R4PutCubeAt33 = Thing("R4PutCubeAt33")
  val R4PutCubeAt34 = Thing("R4PutCubeAt34")
  val R4PutCubeAt41 = Thing("R4PutCubeAt41")
  val R4PutCubeAt42 = Thing("R4PutCubeAt42")
  val R4PutCubeAt43 = Thing("R4PutCubeAt43")
  val R4PutCubeAt44 = Thing("R4PutCubeAt44")
  //Discrabes things were R5 can put cubes
  val R5PutCubeAt11 = Thing("R5PutCubeAt11")
  val R5PutCubeAt12 = Thing("R5PutCubeAt12")
  val R5PutCubeAt13 = Thing("R5PutCubeAt13")
  val R5PutCubeAt14 = Thing("R5PutCubeAt14")
  val R5PutCubeAt21 = Thing("R5PutCubeAt21")
  val R5PutCubeAt22 = Thing("R5PutCubeAt22")
  val R5PutCubeAt23 = Thing("R5PutCubeAt23")
  val R5PutCubeAt24 = Thing("R5PutCubeAt24")
  val R5PutCubeAt31 = Thing("R5PutCubeAt31")
  val R5PutCubeAt32 = Thing("R5PutCubeAt32")
  val R5PutCubeAt33 = Thing("R5PutCubeAt33")
  val R5PutCubeAt34 = Thing("R5PutCubeAt34")
  val R5PutCubeAt41 = Thing("R5PutCubeAt41")
  val R5PutCubeAt42 = Thing("R5PutCubeAt42")
  val R5PutCubeAt43 = Thing("R5PutCubeAt43")
  val R5PutCubeAt44 = Thing("R5PutCubeAt44")

//Iniate parsers--------------------------------------------------------------------------------------------------------

  val parserG = sp.domain.logic.PropositionParser(List(buildSpace1Booked, buildSpace2Booked, buildSpace3Booked, buildSpace4Booked,
    R2Booked, R4Booked, R5Booked, R4Dodge, R5Dodge, H1NoneEmpty, H1Up, H2Up, H2Empty, buildSpotBooked,buildPallet1Empty,buildPallet2Empty,
    buildingPalettComplete,R2OPComplete,R4OPComplete,R5OPComplete,R4HoldingCube,R5HoldingCube))

  val parserA = sp.domain.logic.ActionParser(List(R2PalettToR5Pos1, R2PalettToR5Pos2, R2PalettToR4Pos1, R2PalettToR4Pos2, R2PalettRemoveR5Pos1,
    R2PalettRemoveR5Pos2, R2PalettRemoveR4Pos1, R2PalettRemoveR4Pos2, R2PlaceBuildingPalett, R2RemoveBuildingPalett, buildSpace1Booked,
    buildSpace2Booked, buildSpace3Booked, buildSpace4Booked, R2Booked, R4Booked, R5Booked,R4HoldingCube,R5HoldingCube,
    R4PickUpAt11, R4PickUpAt12, R4PickUpAt13, R4PickUpAt14, R4PickUpAt15, R4PickUpAt16, R4PickUpAt17, R4PickUpAt18,
    R4PickUpAt21, R4PickUpAt22, R4PickUpAt23, R4PickUpAt24, R4PickUpAt25, R4PickUpAt26, R4PickUpAt27, R4PickUpAt28,
    R5PickUpAt11, R5PickUpAt12, R5PickUpAt13, R5PickUpAt14, R5PickUpAt15, R5PickUpAt16, R5PickUpAt17, R5PickUpAt18,
    R5PickUpAt21, R5PickUpAt22, R5PickUpAt23, R5PickUpAt24, R5PickUpAt25, R5PickUpAt26, R5PickUpAt27, R5PickUpAt28,
    R4PutCubeAt11,R4PutCubeAt12,R4PutCubeAt13,R4PutCubeAt14,R4PutCubeAt21,R4PutCubeAt22,R4PutCubeAt23,R4PutCubeAt24,
    R4PutCubeAt31,R4PutCubeAt32,R4PutCubeAt33,R4PutCubeAt34,R4PutCubeAt41,R4PutCubeAt42,R4PutCubeAt43,R4PutCubeAt44,
    R5PutCubeAt11,R5PutCubeAt12,R5PutCubeAt13,R5PutCubeAt14,R5PutCubeAt21,R5PutCubeAt22,R5PutCubeAt23,R5PutCubeAt24,
    R5PutCubeAt31,R5PutCubeAt32,R5PutCubeAt33,R5PutCubeAt34,R5PutCubeAt41,R5PutCubeAt42,R5PutCubeAt43,R5PutCubeAt44))

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
  //Discrabes Actions were R4 can put cubes
  val aR4PutCubeAt11 = parserA.parseStr("R4PutCubeAt11").right.get
  val aR4PutCubeAt12 = parserA.parseStr("R4PutCubeAt12").right.get
  val aR4PutCubeAt13 = parserA.parseStr("R4PutCubeAt13").right.get
  val aR4PutCubeAt14 = parserA.parseStr("R4PutCubeAt14").right.get
  val aR4PutCubeAt21 = parserA.parseStr("R4PutCubeAt21").right.get
  val aR4PutCubeAt22 = parserA.parseStr("R4PutCubeAt22").right.get
  val aR4PutCubeAt23 = parserA.parseStr("R4PutCubeAt23").right.get
  val aR4PutCubeAt24 = parserA.parseStr("R4PutCubeAt24").right.get
  val aR4PutCubeAt31 = parserA.parseStr("R4PutCubeAt31").right.get
  val aR4PutCubeAt32 = parserA.parseStr("R4PutCubeAt32").right.get
  val aR4PutCubeAt33 = parserA.parseStr("R4PutCubeAt33").right.get
  val aR4PutCubeAt34 = parserA.parseStr("R4PutCubeAt34").right.get
  val aR4PutCubeAt41 = parserA.parseStr("R4PutCubeAt41").right.get
  val aR4PutCubeAt42 = parserA.parseStr("R4PutCubeAt42").right.get
  val aR4PutCubeAt43 = parserA.parseStr("R4PutCubeAt43").right.get
  val aR4PutCubeAt44 = parserA.parseStr("R4PutCubeAt44").right.get
  //Discrabes Actions were R5 can put cubes
  val aR5PutCubeAt11 = parserA.parseStr("R5PutCubeAt11").right.get
  val aR5PutCubeAt12 = parserA.parseStr("R5PutCubeAt12").right.get
  val aR5PutCubeAt13 = parserA.parseStr("R5PutCubeAt13").right.get
  val aR5PutCubeAt14 = parserA.parseStr("R5PutCubeAt14").right.get
  val aR5PutCubeAt21 = parserA.parseStr("R5PutCubeAt21").right.get
  val aR5PutCubeAt22 = parserA.parseStr("R5PutCubeAt22").right.get
  val aR5PutCubeAt23 = parserA.parseStr("R5PutCubeAt23").right.get
  val aR5PutCubeAt24 = parserA.parseStr("R5PutCubeAt24").right.get
  val aR5PutCubeAt31 = parserA.parseStr("R5PutCubeAt31").right.get
  val aR5PutCubeAt32 = parserA.parseStr("R5PutCubeAt32").right.get
  val aR5PutCubeAt33 = parserA.parseStr("R5PutCubeAt33").right.get
  val aR5PutCubeAt34 = parserA.parseStr("R5PutCubeAt34").right.get
  val aR5PutCubeAt41 = parserA.parseStr("R5PutCubeAt41").right.get
  val aR5PutCubeAt42 = parserA.parseStr("R5PutCubeAt42").right.get
  val aR5PutCubeAt43 = parserA.parseStr("R5PutCubeAt43").right.get
  val aR5PutCubeAt44 = parserA.parseStr("R5PutCubeAt44").right.get

  //Operations----------------------------------------------------------------------------------------------------------

  //Example
  // val init = Operation("Init", List(PropositionCondition(AND(List()), List(aGenerateOperatorInstructions))),SPAttributes(), ID.newID)

  //R2 Operations
  //AND(List(NOT(gBuildSpace1Booked), NOT(gR2booked)))
  val OR2PalettToR5PalettSpace1 = Operation("OR2PalettToR5PalettSpace1", List(PropositionCondition(AND(List(NOT(gBuildSpace1Booked), NOT(gR2Booked), NOT(gR5Booked))), List(aBookR2,aR2PalettToR5Pos1,aBuildSpace1Book))))
  // (Pos1 clear) Action R2 Booked = True
  val OR2PalettToR5PalettSpace2 = Operation("OR2PalettToR5PalettSpace2", List(PropositionCondition(AND(List(NOT(gBuildSpace2Booked), NOT(gR2Booked), NOT(gR5Booked), gBuildSpace1Booked, gR5dodge)), List(aBookR2,aR2PalettToR5Pos2,aBuildSpace2Book))))
  // (Pos2 clear AND POS1 filled)
  val OR2PalettToR4PalettSpace1 = Operation("OR2PalettToR4PalettSpace1", List(PropositionCondition(AND(List(NOT(gBuildSpace3Booked), NOT(gR2Booked), NOT(gR4Booked))), List(aBookR2,aR2PalettToR4Pos1,aBuildSpace3Book))))
  // (Pos1 clear)
  val OR2PalettToR4PalettSpace2 = Operation("OR2PalettToR4PalettSpace2", List(PropositionCondition(AND(List(NOT(gBuildSpace4Booked), NOT(gR2Booked), NOT(gR4Booked), gBuildSpace3Booked)), List(aBookR2,aR2PalettToR4Pos1,aBuildSpace4Book))))
    // (Pos2 clear AND POS1 filled)
  val OR2PalettRemoveR5PalettSpace1 = Operation("OR2PalettRemoveR5PalettSpace1", List(PropositionCondition(AND(List(NOT(gR2Booked),OR(List(gBuildPallet1Empty,gBuildPallet2Empty)))), List(aR2PalettRemoveR5Pos1,aBookR2,aBuildSpace1UnBook))))
  // Operation R4BuildFromPos1 Done
  val OR2PalettRemoveR5PalettSpace2 = Operation("OR2PalettRemoveR5PalettSpace2", List(PropositionCondition(AND(List(NOT(gR2Booked),OR(List(gBuildPallet1Empty,gBuildPallet2Empty)))), List(aR2PalettRemoveR5Pos2,aBookR2,aBuildSpace2UnBook))))
  // Operation R4BuildFromPos2 Done
  val OR2PalettRemoveR4PalettSpace1 = Operation("OR2PalettRemoveR4PalettSpace1", List(PropositionCondition(AND(List(NOT(gR2Booked),OR(List(gBuildPallet1Empty,gBuildPallet2Empty)))), List(aR2PalettRemoveR4Pos1,aBookR2,aBuildSpace3UnBook))))
  // Operation R5BuildFromPos1 Done
  val OR2PalettRemoveR4PalettSpace2 = Operation("OR2PalettRemoveR4PalettSpace2", List(PropositionCondition(AND(List(NOT(gR2Booked),OR(List(gBuildPallet1Empty,gBuildPallet2Empty)))), List(aR2PalettRemoveR4Pos2,aBookR2,aBuildSpace4UnBook))))
  // Operation R5BuildFromPos2 Done
  val OR2PlaceBuildingPalett = Operation("OR2PlaceBuildingPalett", List(PropositionCondition(AND(List(NOT(gR2Booked),NOT(gR5Booked),NOT(gR4Booked),NOT(gBuildSpotBooked))), List(aBookR2,aR2PlaceBuildingPalett,aBuildSpotBook))))
  //
  val OR2RemoveBuildingPalett = Operation("OR2RemoveBuildingPalett", List(PropositionCondition(AND(List(NOT(gR2Booked),NOT(gR5Booked),NOT(gR4Booked),gBuildSpotBooked,gBuildingPalettComplete)), List(aBookR2,aBuildSpotUnBook,aR2RemoveBuildingPalett))))
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
  //Ops for placing cubes with R4
  val OR4PutCubeAt11 = Operation("OR4PutCubeAt11", List(PropositionCondition(AND(List(NOT(gR4Booked),gR4HoldingCube)), List(aR4PutCubeAt11,aR4NotHoldingCube))))
  val OR4PutCubeAt12 = Operation("OR4PutCubeAt12", List(PropositionCondition(AND(List(NOT(gR4Booked),gR4HoldingCube)), List(aR4PutCubeAt12,aR4NotHoldingCube))))
  val OR4PutCubeAt13 = Operation("OR4PutCubeAt13", List(PropositionCondition(AND(List(NOT(gR4Booked),gR4HoldingCube)), List(aR4PutCubeAt13,aR4NotHoldingCube))))
  val OR4PutCubeAt14 = Operation("OR4PutCubeAt14", List(PropositionCondition(AND(List(NOT(gR4Booked),gR4HoldingCube)), List(aR4PutCubeAt14,aR4NotHoldingCube))))
  val OR4PutCubeAt21 = Operation("OR4PutCubeAt21", List(PropositionCondition(AND(List(NOT(gR4Booked),gR4HoldingCube)), List(aR4PutCubeAt21,aR4NotHoldingCube))))
  val OR4PutCubeAt22 = Operation("OR4PutCubeAt22", List(PropositionCondition(AND(List(NOT(gR4Booked),gR4HoldingCube)), List(aR4PutCubeAt22,aR4NotHoldingCube))))
  val OR4PutCubeAt23 = Operation("OR4PutCubeAt23", List(PropositionCondition(AND(List(NOT(gR4Booked),gR4HoldingCube)), List(aR4PutCubeAt23,aR4NotHoldingCube))))
  val OR4PutCubeAt24 = Operation("OR4PutCubeAt24", List(PropositionCondition(AND(List(NOT(gR4Booked),gR4HoldingCube)), List(aR4PutCubeAt24,aR4NotHoldingCube))))
  val OR4PutCubeAt31 = Operation("OR4PutCubeAt31", List(PropositionCondition(AND(List(NOT(gR4Booked),gR4HoldingCube)), List(aR4PutCubeAt31,aR4NotHoldingCube))))
  val OR4PutCubeAt32 = Operation("OR4PutCubeAt32", List(PropositionCondition(AND(List(NOT(gR4Booked),gR4HoldingCube)), List(aR4PutCubeAt32,aR4NotHoldingCube))))
  val OR4PutCubeAt33 = Operation("OR4PutCubeAt33", List(PropositionCondition(AND(List(NOT(gR4Booked),gR4HoldingCube)), List(aR4PutCubeAt33,aR4NotHoldingCube))))
  val OR4PutCubeAt34 = Operation("OR4PutCubeAt34", List(PropositionCondition(AND(List(NOT(gR4Booked),gR4HoldingCube)), List(aR4PutCubeAt34,aR4NotHoldingCube))))
  val OR4PutCubeAt41 = Operation("OR4PutCubeAt41", List(PropositionCondition(AND(List(NOT(gR4Booked),gR4HoldingCube)), List(aR4PutCubeAt41,aR4NotHoldingCube))))
  val OR4PutCubeAt42 = Operation("OR4PutCubeAt42", List(PropositionCondition(AND(List(NOT(gR4Booked),gR4HoldingCube)), List(aR4PutCubeAt42,aR4NotHoldingCube))))
  val OR4PutCubeAt43 = Operation("OR4PutCubeAt43", List(PropositionCondition(AND(List(NOT(gR4Booked),gR4HoldingCube)), List(aR4PutCubeAt43,aR4NotHoldingCube))))
  val OR4PutCubeAt44 = Operation("OR4PutCubeAt44", List(PropositionCondition(AND(List(NOT(gR4Booked),gR4HoldingCube)), List(aR4PutCubeAt44,aR4NotHoldingCube))))
  //Ops for placing cubes with R5
  val OR5PutCubeAt11 = Operation("OR5PutCubeAt11", List(PropositionCondition(AND(List(NOT(gR5Booked),gR5HoldingCube)), List(aR5PutCubeAt11,aR5NotHoldingCube))))
  val OR5PutCubeAt12 = Operation("OR5PutCubeAt12", List(PropositionCondition(AND(List(NOT(gR5Booked),gR5HoldingCube)), List(aR5PutCubeAt12,aR5NotHoldingCube))))
  val OR5PutCubeAt13 = Operation("OR5PutCubeAt13", List(PropositionCondition(AND(List(NOT(gR5Booked),gR5HoldingCube)), List(aR5PutCubeAt13,aR5NotHoldingCube))))
  val OR5PutCubeAt14 = Operation("OR5PutCubeAt14", List(PropositionCondition(AND(List(NOT(gR5Booked),gR5HoldingCube)), List(aR5PutCubeAt14,aR5NotHoldingCube))))
  val OR5PutCubeAt21 = Operation("OR5PutCubeAt21", List(PropositionCondition(AND(List(NOT(gR5Booked),gR5HoldingCube)), List(aR5PutCubeAt21,aR5NotHoldingCube))))
  val OR5PutCubeAt22 = Operation("OR5PutCubeAt22", List(PropositionCondition(AND(List(NOT(gR5Booked),gR5HoldingCube)), List(aR5PutCubeAt22,aR5NotHoldingCube))))
  val OR5PutCubeAt23 = Operation("OR5PutCubeAt23", List(PropositionCondition(AND(List(NOT(gR5Booked),gR5HoldingCube)), List(aR5PutCubeAt23,aR5NotHoldingCube))))
  val OR5PutCubeAt24 = Operation("OR5PutCubeAt24", List(PropositionCondition(AND(List(NOT(gR5Booked),gR5HoldingCube)), List(aR5PutCubeAt24,aR5NotHoldingCube))))
  val OR5PutCubeAt31 = Operation("OR5PutCubeAt31", List(PropositionCondition(AND(List(NOT(gR5Booked),gR5HoldingCube)), List(aR5PutCubeAt31,aR5NotHoldingCube))))
  val OR5PutCubeAt32 = Operation("OR5PutCubeAt32", List(PropositionCondition(AND(List(NOT(gR5Booked),gR5HoldingCube)), List(aR5PutCubeAt32,aR5NotHoldingCube))))
  val OR5PutCubeAt33 = Operation("OR5PutCubeAt33", List(PropositionCondition(AND(List(NOT(gR5Booked),gR5HoldingCube)), List(aR5PutCubeAt33,aR5NotHoldingCube))))
  val OR5PutCubeAt34 = Operation("OR5PutCubeAt34", List(PropositionCondition(AND(List(NOT(gR5Booked),gR5HoldingCube)), List(aR5PutCubeAt34,aR5NotHoldingCube))))
  val OR5PutCubeAt41 = Operation("OR5PutCubeAt41", List(PropositionCondition(AND(List(NOT(gR5Booked),gR5HoldingCube)), List(aR5PutCubeAt41,aR5NotHoldingCube))))
  val OR5PutCubeAt42 = Operation("OR5PutCubeAt42", List(PropositionCondition(AND(List(NOT(gR5Booked),gR5HoldingCube)), List(aR5PutCubeAt42,aR5NotHoldingCube))))
  val OR5PutCubeAt43 = Operation("OR5PutCubeAt43", List(PropositionCondition(AND(List(NOT(gR5Booked),gR5HoldingCube)), List(aR5PutCubeAt43,aR5NotHoldingCube))))
  val OR5PutCubeAt44 = Operation("OR5PutCubeAt44", List(PropositionCondition(AND(List(NOT(gR5Booked),gR5HoldingCube)), List(aR5PutCubeAt44,aR5NotHoldingCube))))


// val sopSeq = SOP(Sequence(o11, o12, o13), Sequence(o21, o22, o23), Parallel(o11,o12))
  val thaSop = SOP(Sequence(OR2PlaceBuildingPalett,OR2PalettToR5PalettSpace1),Parallel(Sequence(OR2PalettToR4PalettSpace1),Sequence()))

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
