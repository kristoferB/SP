
import sp.domain._

class operationMaker {

//For palettpos1
  val R4BuildSpace1Book = Thing("R4BuildSpace1Booked")
//For palettpos2
  val R4BuildSpace2Book = Thing("R4BuildSpace2Booked")
//For palettpos3
  val R5BuildSpace1Book = Thing("R5BuildSpace1Booked")
//For palettpos4
  val R5BuildSpace2Book = Thing("R5BuildSpace2Booked")
//For the buildingspace were the tower should be built
  val buildSpotBooked = Thing("buildSpotBooked")
//Book robot 2
  val R2Booked = Thing("R2Booked")
//Book robot 4
  val R4Booked = Thing("R4Booked")
//Book robot 5
  val R5Booked = Thing("R5Booked")
//If robot 4 is in dodge pos
  val R4Dodge = Thing("R4Dodge")
//If robot 4 is in dodge pos
  val R5Dodge = Thing("R5Dodge")
//Hiss 1
  val H1NoneEmpty = Thing("H4Empty")
  val H1Up = Thing("H4Up")
//Hiss 2
  val H2Empty = Thing("H4Empty")
  val H2Up = Thing("H4Up")
//Status of buildpallet 1
  val buildPallet1Empty = Thing("buildPallet1Empty")
//Status of buildpallet 2
  val buildPallet2Empty = Thing("buildPallet2Empty")
//Status of buildningpallet
  val buildingPalettComplete = Thing("buildingPalettComplete")
//Tells when R2 is done
  val R2OPComplete = Thing("R2OPComplete")
//Tells when R4 is done
  val R4OPComplete = Thing("R4OPComplete")
//Tells when R5 is done
  val R5OPComplete = Thing("R5OPComplete")
//Tells if R4 is holding a cube
  val R4HoldingCube = Thing("R4HoldingCube")
//Tells if R4 is holding a cube
  val R5HoldingCube = Thing("R5HoldingCube")
//Things for status of Build spot


//Vals for makeing Thnigs
  val fixtNo = 2
  val posFix = 8
  val row = 4
  val col = 4
  val rs = List("R4", "R5")
  val opNamePickedUpCubes = "PickedUpCubes"
  val opNamePutDownCubes = "PutDownCubes"

//Makes Things for PickUpCubes
  val listOfPickedUpCubes = for {
    r <- rs
    f <- 1 to fixtNo
    p <- 1 to posFix
  } yield {
    Thing(s"$r$opNamePickedUpCubes$f$p")
  }

//Makes things for PutDownCubes
  val listOfPutDownCubes = for {
    f <- 1 to row
    p <- 1 to col
  } yield {
    Thing(s"$opNamePutDownCubes$f$p")
  }

//  val R2PalettToR5Pos1 = Thing("R2PalettToR5Pos1")
//  val R2PalettToR5Pos2 = Thing("R2PalettToR5Pos2")
//  val R2PalettToR4Pos1 = Thing("R2PalletToR4Pos1")
//  val R2PalettToR4Pos2 = Thing("R2PalettToR4Pos2")
//  val R2PalettRemoveR5Pos1 = Thing("R2PalettRemoveR5Pos1")
//  val R2PalettRemoveR5Pos2 = Thing("R2PalettRemoveR5Pos2")
//  val R2PalettRemoveR4Pos1 = Thing("R2PalettRemoveR4Pos1")
//  val R2PalettRemoveR4Pos2 = Thing("R2PalettRemoveR4Pos2")
//  val R2PlaceBuildingPalett = Thing("R2PlaceBuildingPalett")
//  val R2RemoveBuildingPalett = Thing("R2RemoveBuildingPalett")

//discribes at which pallet (first diget) and position (seconde diget) R4 picks up
//  val R4PickUpAt: List[Thing] = R4PickUpAt + rR2 = Thing("R4PickUpAt + rR2")
//  val R4PickUpAt11 = Thing("R4PickUpAt11")
//  val R4PickUpAt12 = Thing("R4PickUpAt12")
//  val R4PickUpAt13 = Thing("R4PickUpAt13")
//  val R4PickUpAt14 = Thing("R4PickUpAt14")
//  val R4PickUpAt15 = Thing("R4PickUpAt15")
//  val R4PickUpAt16 = Thing("R4PickUpAt16")
//  val R4PickUpAt17 = Thing("R4PickUpAt17")
//  val R4PickUpAt18 = Thing("R4PickUpAt18")
//  //discribes at which pallet (first diget) and position (seconde diget) R4 picks up
//  val R4PickUpAt21 = Thing("R4PickUpAt21")
//  val R4PickUpAt22 = Thing("R4PickUpAt22")
//  val R4PickUpAt23 = Thing("R4PickUpAt23")
//  val R4PickUpAt24 = Thing("R4PickUpAt24")
//  val R4PickUpAt25 = Thing("R4PickUpAt25")
//  val R4PickUpAt26 = Thing("R4PickUpAt26")
//  val R4PickUpAt27 = Thing("R4PickUpAt27")
//  val R4PickUpAt28 = Thing("R4PickUpAt28")
//  //discribes at which pallet (first diget) and position (seconde diget) R5 picks up
//  val R5PickUpAt11 = Thing("R4PickUpAt11")
//  val R5PickUpAt12 = Thing("R4PickUpAt12")
//  val R5PickUpAt13 = Thing("R4PickUpAt13")
//  val R5PickUpAt14 = Thing("R4PickUpAt14")
//  val R5PickUpAt15 = Thing("R4PickUpAt15")
//  val R5PickUpAt16 = Thing("R4PickUpAt16")
//  val R5PickUpAt17 = Thing("R4PickUpAt17")
//  val R5PickUpAt18 = Thing("R4PickUpAt18")
//  //discribes at which pallet (first diget) and position (seconde diget) R5 picks up
//  val R5PickUpAt21 = Thing("R4PickUpAt21")
//  val R5PickUpAt22 = Thing("R4PickUpAt22")
//  val R5PickUpAt23 = Thing("R4PickUpAt23")
//  val R5PickUpAt24 = Thing("R4PickUpAt24")
//  val R5PickUpAt25 = Thing("R4PickUpAt25")
//  val R5PickUpAt26 = Thing("R4PickUpAt26")
//  val R5PickUpAt27 = Thing("R4PickUpAt27")
//  val R5PickUpAt28 = Thing("R4PickUpAt28")
//Discrabes things were R4 can put cubes
//  val R4PutCubeAt11 = Thing("R4PutCubeAt11")
//  val R4PutCubeAt12 = Thing("R4PutCubeAt12")
//  val R4PutCubeAt13 = Thing("R4PutCubeAt13")
//  val R4PutCubeAt14 = Thing("R4PutCubeAt14")
//  val R4PutCubeAt21 = Thing("R4PutCubeAt21")
//  val R4PutCubeAt22 = Thing("R4PutCubeAt22")
//  val R4PutCubeAt23 = Thing("R4PutCubeAt23")
//  val R4PutCubeAt24 = Thing("R4PutCubeAt24")
//  val R4PutCubeAt31 = Thing("R4PutCubeAt31")
//  val R4PutCubeAt32 = Thing("R4PutCubeAt32")
//  val R4PutCubeAt33 = Thing("R4PutCubeAt33")
//  val R4PutCubeAt34 = Thing("R4PutCubeAt34")
//  val R4PutCubeAt41 = Thing("R4PutCubeAt41")
//  val R4PutCubeAt42 = Thing("R4PutCubeAt42")
//  val R4PutCubeAt43 = Thing("R4PutCubeAt43")
//  val R4PutCubeAt44 = Thing("R4PutCubeAt44")
//  //Discrabes things were R5 can put cubes
//  val R5PutCubeAt11 = Thing("R5PutCubeAt11")
//  val R5PutCubeAt12 = Thing("R5PutCubeAt12")
//  val R5PutCubeAt13 = Thing("R5PutCubeAt13")
//  val R5PutCubeAt14 = Thing("R5PutCubeAt14")
//  val R5PutCubeAt21 = Thing("R5PutCubeAt21")
//  val R5PutCubeAt22 = Thing("R5PutCubeAt22")
//  val R5PutCubeAt23 = Thing("R5PutCubeAt23")
//  val R5PutCubeAt24 = Thing("R5PutCubeAt24")
//  val R5PutCubeAt31 = Thing("R5PutCubeAt31")
//  val R5PutCubeAt32 = Thing("R5PutCubeAt32")
//  val R5PutCubeAt33 = Thing("R5PutCubeAt33")
//  val R5PutCubeAt34 = Thing("R5PutCubeAt34")
//  val R5PutCubeAt41 = Thing("R5PutCubeAt41")
//  val R5PutCubeAt42 = Thing("R5PutCubeAt42")
//  val R5PutCubeAt43 = Thing("R5PutCubeAt43")
//  val R5PutCubeAt44 = Thing("R5PutCubeAt44")


//Iniate parsers--------------------------------------------------------------------------------------------------------

  val parserG = sp.domain.logic.PropositionParser(List(R4BuildSpace1Book, R4BuildSpace2Book, R5BuildSpace1Book,
    R5BuildSpace2Book, R2Booked, R4Booked, R5Booked, R4Dodge, R5Dodge, H1NoneEmpty, H1Up, H2Up, H2Empty,
    buildSpotBooked, buildPallet1Empty, buildPallet2Empty, buildingPalettComplete, R2OPComplete, R4OPComplete,
    R5OPComplete, R4HoldingCube, R5HoldingCube) ++ listOfPickedUpCubes ++ listOfPutDownCubes)

  val parserA = sp.domain.logic.ActionParser(List( R2Booked, R4Booked, R5Booked, R4HoldingCube,
    R5HoldingCube) ++ listOfPickedUpCubes ++ listOfPutDownCubes)

//Create gaurds-------------------------------------------------------------------------------------------------------
//Guards for buildspace
  val gR4BuildSpace1Booked = parserG.parseStr("R4BuildSpace1Book == true").right.get
  val gR4BuildSpace2Booked = parserG.parseStr("R4BuildSpace2Book == true").right.get
  val gR5BuildSpace1Booked = parserG.parseStr("R5BuildSpace1Book == true").right.get
  val gR5BuildSpace2Booked = parserG.parseStr("R5BuildSpace2Book == true").right.get
//Guard for booked buildspot
  val gBuildSpotBooked =  parserG.parseStr("buildSpotBooked == true").right.get
//Guard which tells if cube is picked up from buildingspace
  val gListOfPickedUpCubes = for {
    r <- rs
    f <- 1 to fixtNo
    p <- 1 to posFix
  } yield {
    parserG.parseStr(s"$r$opNamePickedUpCubes$f$p = true").right.get
  }
//Guard which tells if cube is placeed at buildingspot
  val gListOfPutDownCubes = for {
    f <- 1 to row
    p <- 1 to col
  } yield {
  parserG.parseStr(s"$opNamePutDownCubes$f$p = true").right.get
  }
//Guards for robots holding cubes
  val gR4HoldingCube = parserG.parseStr("R4HoldingCube == true").right.get
  val gR5HoldingCube = parserG.parseStr("R5HoldingCube == true").right.get
//Guards for  robotbookings
  val gR2Booked = parserG.parseStr("R2Booked == true").right.get
  val gR4Booked = parserG.parseStr("R4Booked == true").right.get
  val gR5Booked = parserG.parseStr("R5Booked == true").right.get
//Guards for Operation Completion
  val gR2OPComplete = parserG.parseStr("R2OPComplete == true").right.get
  val gR4OPComplete = parserG.parseStr("R4OPComplete == true").right.get
  val gR5OPComplete = parserG.parseStr("R5OPComplete == true").right.get
//Guards which tells if robot is dodgeing
  val gR4Dodge = parserG.parseStr("R4Dodge == true").right.get
  val gR5Dodge = parserG.parseStr("R4Dodge == true").right.get
//Guards which tells when buildpallets is empty
  val gBuildPallet1Empty = parserG.parseStr("buildPallet1Empty == true").right.get
  val gBuildPallet2Empty = parserG.parseStr("buildPallet2Empty == true").right.get
//Guard for when buildningpallet is complete
  val gBuildingPalettComplete = parserG.parseStr("buildingPalettComplete == true").right.get
//Guard which tells status about H1
  val gH1NoneEmpty = parserG.parseStr("H1NoneEmpt == true").right.get
  val gH1Up = parserG.parseStr("H1Up == true").right.get
//Guard which tells status about H1
  val gH2Empty = parserG.parseStr("H2Empty == true").right.get
  val gH2Up = parserG.parseStr("H2Up == true").right.get

//Create actions--------------------------------------------------------------------------------------------------------
// example val aGenerateOperatorInstructions = parserA.parseStr("generateOperatorInstructions = True").right.get
//Actions for booking robots
  val aBookR2 = parserA.parseStr("R2Booked = true").right.get
  val aBookR4 = parserA.parseStr("R4Booked = true").right.get
  val aBookR5 = parserA.parseStr("R5Booked = true").right.get
//Actions for unbooking robots
  val aUnBookR2 = parserA.parseStr("R2Booked = false").right.get
  val aUnBookR4 = parserA.parseStr("R4Booked = false").right.get
  val aUnBookR5 = parserA.parseStr("R5Booked = false").right.get
//Actions which book BuildSpaces
  val aR4BuildSpace1Book = parserA.parseStr("R4BuildSpace1Book = true").right.get
  val aR4BuildSpace2Book = parserA.parseStr("R4BuildSpace2Book = true").right.get
  val aR5BuildSpace1Book = parserA.parseStr("R5BuildSpace1Book = true").right.get
  val aR5BuildSpace2Book = parserA.parseStr("R5BuildSpace2Book = true").right.get
//Actions which unbook BuildSpaces
  val aBuildSpaceR1UnBook = parserA.parseStr("buildSpaceR1UnBooked = false").right.get
  val aBuildSpaceR2UnBook = parserA.parseStr("buildSpaceR2UnBooked = false").right.get
  val aBuildSpaceL1UnBook = parserA.parseStr("buildSpaceL1UnBooked = false").right.get
  val aBuildSpaceL2UnBook = parserA.parseStr("buildSpaceL2UnBooked = false").right.get
//Action which book Buildspot
  val aBuildSpotBook = parserA.parseStr("buildSpotBooked = true").right.get
//Action which unbook buildspot
  val aBuildSpotUnBook = parserA.parseStr("buildSpotBooked = false").right.get
//Action which tells if cube is picked up from buildingspace
  val aListOfPickedUpCubes = for {
    r <- rs
    f <- 1 to fixtNo
    p <- 1 to posFix
  } yield {
    parserA.parseStr(s"$r$opNamePickedUpCubes$f$p == true").right.get
  }
//Action which tells if cube is placeed at buildingspot
  val aListOfPutDownCubes = for {
    f <- 1 to row
    p <- 1 to col
  } yield {
    parserA.parseStr(s"$opNamePutDownCubes$f$p == true").right.get
  }
//Change status of bulding pallets
  val aNewBuildingPalettComplete = parserA.parseStr("buildingPalettComplete = false").right.get
  val aBuildingPalettIsComplete = parserA.parseStr("buildingPalettComplete = true").right.get
//Actions which indicate if R4 or R5 is holding a cube
  val aR4HoldingCube = parserA.parseStr("R4HoldingCube = true").right.get
  val aR5HoldingCube = parserA.parseStr("R5HoldingCube = true").right.get
//Actions which indicate if R4 or R5 is not holding a cube
  val aR4NotHoldingCube = parserA.parseStr("R4HoldingCube = false").right.get
  val aR5NotHoldingCube = parserA.parseStr("R5HoldingCube = false").right.get
////Actions for moving bulding pallets
//  val aR2PalettToR5Pos1 = parserA.parseStr("R2PalettToR5Pos1 = true").right.get
//  val aR2PalettToR5Pos2 = parserA.parseStr("R2PalettToR5Pos2 = true").right.get
//  val aR2PalettToR4Pos1 = parserA.parseStr("R2PalettToR4Pos1 = true").right.get
//  val aR2PalettToR4Pos2 = parserA.parseStr("R2PalettToR4Pos2 = true").right.get
//  val aR2PalettRemoveR5Pos1 = parserA.parseStr("R2PalettRemoveR5Pos1 = true").right.get
//  val aR2PalettRemoveR5Pos2 = parserA.parseStr("R2PalettRemoveR5Pos2 = true").right.get
//  val aR2PalettRemoveR4Pos1 = parserA.parseStr("R2PalettRemoveR4Pos1 = true").right.get
//  val aR2PalettRemoveR4Pos2 = parserA.parseStr("R2PalettRemoveR4Pos2 = true").right.get
//  val aR2PlaceBuildingPalett = parserA.parseStr("R2PlaceBuildingPalett = true").right.get
//  val aR2RemoveBuildingPalett = parserA.parseStr("R2RemoveBuildingPalett = true").right.get
////Actions for picking up cubes for R4 and pallet 1
//  val aR4PickUpAt11 = parserA.parseStr("R4PickUpAt11 = true").right.get
//  val aR4PickUpAt12 = parserA.parseStr("R4PickUpAt12 = true").right.get
//  val aR4PickUpAt13 = parserA.parseStr("R4PickUpAt13 = true").right.get
//  val aR4PickUpAt14 = parserA.parseStr("R4PickUpAt14 = true").right.get
//  val aR4PickUpAt15 = parserA.parseStr("R4PickUpAt15 = true").right.get
//  val aR4PickUpAt16 = parserA.parseStr("R4PickUpAt16 = true").right.get
//  val aR4PickUpAt17 = parserA.parseStr("R4PickUpAt17 = true").right.get
//  val aR4PickUpAt18 = parserA.parseStr("R4PickUpAt18 = true").right.get
//  //Actions for picking up cubes for R4 and pallet 2
//  val aR4PickUpAt21 = parserA.parseStr("R4PickUpAt21 = true").right.get
//  val aR4PickUpAt22 = parserA.parseStr("R4PickUpAt22 = true").right.get
//  val aR4PickUpAt23 = parserA.parseStr("R4PickUpAt23 = true").right.get
//  val aR4PickUpAt24 = parserA.parseStr("R4PickUpAt24 = true").right.get
//  val aR4PickUpAt25 = parserA.parseStr("R4PickUpAt25 = true").right.get
//  val aR4PickUpAt26 = parserA.parseStr("R4PickUpAt26 = true").right.get
//  val aR4PickUpAt27 = parserA.parseStr("R4PickUpAt27 = true").right.get
//  val aR4PickUpAt28 = parserA.parseStr("R4PickUpAt28 = true").right.get
//  //Actions for picking up cubes for R5 and pallet 1
//  val aR5PickUpAt11 = parserA.parseStr("R5PickUpAt11 = true").right.get
//  val aR5PickUpAt12 = parserA.parseStr("R5PickUpAt12 = true").right.get
//  val aR5PickUpAt13 = parserA.parseStr("R5PickUpAt13 = true").right.get
//  val aR5PickUpAt14 = parserA.parseStr("R5PickUpAt14 = true").right.get
//  val aR5PickUpAt15 = parserA.parseStr("R5PickUpAt15 = true").right.get
//  val aR5PickUpAt16 = parserA.parseStr("R5PickUpAt16 = true").right.get
//  val aR5PickUpAt17 = parserA.parseStr("R5PickUpAt17 = true").right.get
//  val aR5PickUpAt18 = parserA.parseStr("R5PickUpAt18 = true").right.get
//  //Actions for picking up cubes for R5 and pallet 2
//  val aR5PickUpAt21 = parserA.parseStr("R5PickUpAt21 = true").right.get
//  val aR5PickUpAt22 = parserA.parseStr("R5PickUpAt22 = true").right.get
//  val aR5PickUpAt23 = parserA.parseStr("R5PickUpAt23 = true").right.get
//  val aR5PickUpAt24 = parserA.parseStr("R5PickUpAt24 = true").right.get
//  val aR5PickUpAt25 = parserA.parseStr("R5PickUpAt25 = true").right.get
//  val aR5PickUpAt26 = parserA.parseStr("R5PickUpAt26 = true").right.get
//  val aR5PickUpAt27 = parserA.parseStr("R5PickUpAt27 = true").right.get
//  val aR5PickUpAt28 = parserA.parseStr("R5PickUpAt28 = true").right.get
//  //Discrabes Actions were R4 can put cubes
//  val aR4PutCubeAt11 = parserA.parseStr("R4PutCubeAt11 = true").right.get
//  val aR4PutCubeAt12 = parserA.parseStr("R4PutCubeAt12 = true").right.get
//  val aR4PutCubeAt13 = parserA.parseStr("R4PutCubeAt13 = true").right.get
//  val aR4PutCubeAt14 = parserA.parseStr("R4PutCubeAt14 = true").right.get
//  val aR4PutCubeAt21 = parserA.parseStr("R4PutCubeAt21 = true").right.get
//  val aR4PutCubeAt22 = parserA.parseStr("R4PutCubeAt22 = true").right.get
//  val aR4PutCubeAt23 = parserA.parseStr("R4PutCubeAt23 = true").right.get
//  val aR4PutCubeAt24 = parserA.parseStr("R4PutCubeAt24 = true").right.get
//  val aR4PutCubeAt31 = parserA.parseStr("R4PutCubeAt31 = true").right.get
//  val aR4PutCubeAt32 = parserA.parseStr("R4PutCubeAt32 = true").right.get
//  val aR4PutCubeAt33 = parserA.parseStr("R4PutCubeAt33 = true").right.get
//  val aR4PutCubeAt34 = parserA.parseStr("R4PutCubeAt34 = true").right.get
//  val aR4PutCubeAt41 = parserA.parseStr("R4PutCubeAt41 = true").right.get
//  val aR4PutCubeAt42 = parserA.parseStr("R4PutCubeAt42 = true").right.get
//  val aR4PutCubeAt43 = parserA.parseStr("R4PutCubeAt43 = true").right.get
//  val aR4PutCubeAt44 = parserA.parseStr("R4PutCubeAt44 = true").right.get
//  //Discrabes Actions were R5 can put cubes
//  val aR5PutCubeAt11 = parserA.parseStr("R5PutCubeAt11 = true").right.get
//  val aR5PutCubeAt12 = parserA.parseStr("R5PutCubeAt12 = true").right.get
//  val aR5PutCubeAt13 = parserA.parseStr("R5PutCubeAt13 = true").right.get
//  val aR5PutCubeAt14 = parserA.parseStr("R5PutCubeAt14 = true").right.get
//  val aR5PutCubeAt21 = parserA.parseStr("R5PutCubeAt21 = true").right.get
//  val aR5PutCubeAt22 = parserA.parseStr("R5PutCubeAt22 = true").right.get
//  val aR5PutCubeAt23 = parserA.parseStr("R5PutCubeAt23 = true").right.get
//  val aR5PutCubeAt24 = parserA.parseStr("R5PutCubeAt24 = true").right.get
//  val aR5PutCubeAt31 = parserA.parseStr("R5PutCubeAt31 = true").right.get
//  val aR5PutCubeAt32 = parserA.parseStr("R5PutCubeAt32 = true").right.get
//  val aR5PutCubeAt33 = parserA.parseStr("R5PutCubeAt33 = true").right.get
//  val aR5PutCubeAt34 = parserA.parseStr("R5PutCubeAt34 = true").right.get
//  val aR5PutCubeAt41 = parserA.parseStr("R5PutCubeAt41 = true").right.get
//  val aR5PutCubeAt42 = parserA.parseStr("R5PutCubeAt42 = true").right.get
//  val aR5PutCubeAt43 = parserA.parseStr("R5PutCubeAt43 = true").right.get
//  val aR5PutCubeAt44 = parserA.parseStr("R5PutCubeAt44 = true").right.get

  //Operations----------------------------------------------------------------------------------------------------------

  //Example
  // val init = Operation("Init", List(PropositionCondition(AND(List()), List(aGenerateOperatorInstructions))),SPAttributes(), ID.newID)

  //R2 Operations
  //AND(List(NOT(gBuildSpace1Booked), NOT(gR2booked)))


  val OR2PalettToR4PalettSpace1 = Operation("OR2PalettToR5PalettSpace1", List(PropositionCondition(AND(List(NOT(gR4BuildSpace1Booked), NOT(gR2Booked), gR5Dodge, gR4Dodge, gH1Up)), List(aBookR2,aR4BuildSpace1Book))))
  // (Pos1 clear) Action R2 Booked = True
  val OR2PalettToR4PalettSpace2 = Operation("OR2PalettToR5PalettSpace2", List(PropositionCondition(AND(List(NOT(gR4BuildSpace2Booked), NOT(gR2Booked), gR5Dodge, gR4Dodge, gH1Up, gR4BuildSpace1Booked)), List(aBookR2,aR4BuildSpace2Book))))
  // (Pos2 clear AND POS1 filled)
  val OR2PalettToR5PalettSpace1 = Operation("OR2PalettToR4PalettSpace1", List(PropositionCondition(AND(List(NOT(gR5BuildSpace1Booked), NOT(gR2Booked), NOT(gR4Booked), gH1Up)), List(aBookR2,aR5BuildSpace1Book))))
  // (Pos1 clear)
  val OR2PalettToR5PalettSpace2 = Operation("OR2PalettToR4PalettSpace2", List(PropositionCondition(AND(List(NOT(gR5BuildSpace2Booked), NOT(gR2Booked), NOT(gR4Booked), gH1Up, gR5BuildSpace1Booked)), List(aBookR2,aR5BuildSpace2Book))))
    // (Pos2 clear AND POS1 filled)
  val OR2PalettRemoveR5PalettSpace1 = Operation("OR2PalettRemoveR5PalettSpace1", List(PropositionCondition(AND(List(NOT(gR2Booked),OR(List(gBuildPallet1Empty,gBuildPallet2Empty)), gR4BuildSpace1Booked, gR5Dodge, gR4Dodge, NOT(gBuildSpotBooked))), List(aBookR2,aBuildSpaceR1UnBook))))
  // Operation R4BuildFromPos1 Done
  val OR2PalettRemoveR5PalettSpace2 = Operation("OR2PalettRemoveR5PalettSpace2", List(PropositionCondition(AND(List(NOT(gR2Booked),OR(List(gBuildPallet1Empty,gBuildPallet2Empty)), gR4BuildSpace2Booked, gR5Dodge, gR4Dodge, NOT(gBuildSpotBooked))), List(,aBookR2,aBuildSpaceR2UnBook))))
  // Operation R4BuialdFromPos2 Done
  val OR2PalettRemoveR4PalettSpace1 = Operation("OR2PalettRemoveR4PalettSpace1", List(PropositionCondition(AND(List(NOT(gR2Booked),OR(List(gBuildPallet1Empty,gBuildPallet2Empty)), gR5BuildSpace1Booked, NOT(gR4Booked), NOT(gBuildSpotBooked))), List(aBookR2,aBuildSpaceL1UnBook))))
  // Operation R5BuildFromPos1 Done
  val OR2PalettRemoveR4PalettSpace2 = Operation("OR2PalettRemoveR4PalettSpace2", List(PropositionCondition(AND(List(NOT(gR2Booked),OR(List(gBuildPallet1Empty,gBuildPallet2Empty)), gR5BuildSpace2Booked, NOT(gR4Booked), NOT(gBuildSpotBooked))), List(aBookR2,aBuildSpaceL2UnBook))))
  // Operation R5BuildFromPos2 Done
  val OR2PlaceBuildingPalett = Operation("OR2PlaceBuildingPalett", List(PropositionCondition(AND(List(NOT(gR2Booked),NOT(gR5Booked),NOT(gR4Booked),NOT(gBuildSpotBooked),gH1Up)), List(aBookR2,aBuildSpotBook))))
  //
  val OR2RemoveBuildingPalett = Operation("OR2RemoveBuildingPalett", List(PropositionCondition(AND(List(NOT(gR2Booked),NOT(gR5Booked),NOT(gR4Booked),gBuildSpotBooked,gBuildingPalettComplete)), List(aBookR2,aBuildSpotUnBook))))
  // Operation
  val OR2RemoveBooking = Operation("OR2RemoveBooking", List(PropositionCondition(AND(List(gR2Booked)), List(aUnBookR2))))
  // After operations that books R2
  val OR4RemoveBooking = Operation("OR2RemoveBooking", List(PropositionCondition(AND(List(gR4Booked)), List(aUnBookR4))))
  // After operations that books R4
  val OR5RemoveBooking = Operation("OR2RemoveBooking", List(PropositionCondition(AND(List(gR5Booked)), List(aUnBookR5))))
  // After operations that books R5
  //operationerna nedan skall ändras så att de passar bättre och blir sumerade
  //OPs for picking up cubes by R4 at spot 1
  val OR4PickUpAt11 = Operation("OR4PickUpAt11", List(PropositionCondition(AND(List(NOT(gR4Booked),gR4BuildSpace1Booked)), List(aListOfPickedUpCubes(1),aBookR4,aR4HoldingCube))))
  val OR4PickUpAt12 = Operation("OR4PickUpAt12", List(PropositionCondition(AND(List(NOT(gR4Booked),gR4BuildSpace1Booked)), List(aListOfPickedUpCubes(2),aBookR4,aR4HoldingCube))))
  val OR4PickUpAt13 = Operation("OR4PickUpAt13", List(PropositionCondition(AND(List(NOT(gR4Booked),gR4BuildSpace1Booked)), List(aListOfPickedUpCubes(3),aBookR4,aR4HoldingCube))))
  val OR4PickUpAt14 = Operation("OR4PickUpAt14", List(PropositionCondition(AND(List(NOT(gR4Booked),gR4BuildSpace1Booked)), List(aListOfPickedUpCubes(4),aBookR4,aR4HoldingCube))))
  val OR4PickUpAt15 = Operation("OR4PickUpAt15", List(PropositionCondition(AND(List(NOT(gR4Booked),gR4BuildSpace1Booked)), List(aListOfPickedUpCubes(5),aBookR4,aR4HoldingCube))))
  val OR4PickUpAt16 = Operation("OR4PickUpAt16", List(PropositionCondition(AND(List(NOT(gR4Booked),gR4BuildSpace1Booked)), List(aListOfPickedUpCubes(6),aBookR4,aR4HoldingCube))))
  val OR4PickUpAt17 = Operation("OR4PickUpAt17", List(PropositionCondition(AND(List(NOT(gR4Booked),gR4BuildSpace1Booked)), List(aListOfPickedUpCubes(7),aBookR4,aR4HoldingCube))))
  val OR4PickUpAt18 = Operation("OR4PickUpAt18", List(PropositionCondition(AND(List(NOT(gR4Booked),gR4BuildSpace1Booked)), List(aListOfPickedUpCubes(8),aBookR4,aR4HoldingCube))))
  //OPs for picking up cubes by R4 at spot 2
  val OR4PickUpAt21 = Operation("OR4PickUpAt21", List(PropositionCondition(AND(List(NOT(gR4Booked),gR4BuildSpace2Booked)), List(aListOfPickedUpCubes(9),aBookR4,aR4HoldingCube))))
  val OR4PickUpAt22 = Operation("OR4PickUpAt22", List(PropositionCondition(AND(List(NOT(gR4Booked),gR4BuildSpace2Booked)), List(aListOfPickedUpCubes(10),aBookR4,aR4HoldingCube))))
  val OR4PickUpAt23 = Operation("OR4PickUpAt23", List(PropositionCondition(AND(List(NOT(gR4Booked),gR4BuildSpace2Booked)), List(aListOfPickedUpCubes(11),aBookR4,aR4HoldingCube))))
  val OR4PickUpAt24 = Operation("OR4PickUpAt24", List(PropositionCondition(AND(List(NOT(gR4Booked),gR4BuildSpace2Booked)), List(aListOfPickedUpCubes(12),aBookR4,aR4HoldingCube))))
  val OR4PickUpAt25 = Operation("OR4PickUpAt25", List(PropositionCondition(AND(List(NOT(gR4Booked),gR4BuildSpace2Booked)), List(aListOfPickedUpCubes(13),aBookR4,aR4HoldingCube))))
  val OR4PickUpAt26 = Operation("OR4PickUpAt26", List(PropositionCondition(AND(List(NOT(gR4Booked),gR4BuildSpace2Booked)), List(aListOfPickedUpCubes(14),aBookR4,aR4HoldingCube))))
  val OR4PickUpAt27 = Operation("OR4PickUpAt27", List(PropositionCondition(AND(List(NOT(gR4Booked),gR4BuildSpace2Booked)), List(aListOfPickedUpCubes(15),aBookR4,aR4HoldingCube))))
  val OR4PickUpAt28 = Operation("OR4PickUpAt28", List(PropositionCondition(AND(List(NOT(gR4Booked),gR4BuildSpace2Booked)), List(aListOfPickedUpCubes(16),aBookR4,aR4HoldingCube))))
  //OPs for picking up cubes by R5 at spot 1
  val OR5PickUpAt11 = Operation("OR5PickUpAt11", List(PropositionCondition(AND(List(NOT(gR5Booked),gR5BuildSpace1Booked)), List(aListOfPickedUpCubes(17),aBookR5,aR5HoldingCube))))
  val OR5PickUpAt12 = Operation("OR5PickUpAt12", List(PropositionCondition(AND(List(NOT(gR5Booked),gR5BuildSpace1Booked)), List(aListOfPickedUpCubes(18),aBookR5,aR5HoldingCube))))
  val OR5PickUpAt13 = Operation("OR5PickUpAt13", List(PropositionCondition(AND(List(NOT(gR5Booked),gR5BuildSpace1Booked)), List(aListOfPickedUpCubes(19),aBookR5,aR5HoldingCube))))
  val OR5PickUpAt14 = Operation("OR5PickUpAt14", List(PropositionCondition(AND(List(NOT(gR5Booked),gR5BuildSpace1Booked)), List(aListOfPickedUpCubes(20),aBookR5,aR5HoldingCube))))
  val OR5PickUpAt15 = Operation("OR5PickUpAt15", List(PropositionCondition(AND(List(NOT(gR5Booked),gR5BuildSpace1Booked)), List(aListOfPickedUpCubes(21),aBookR5,aR5HoldingCube))))
  val OR5PickUpAt16 = Operation("OR5PickUpAt16", List(PropositionCondition(AND(List(NOT(gR5Booked),gR5BuildSpace1Booked)), List(aListOfPickedUpCubes(22),aBookR5,aR5HoldingCube))))
  val OR5PickUpAt17 = Operation("OR5PickUpAt17", List(PropositionCondition(AND(List(NOT(gR5Booked),gR5BuildSpace1Booked)), List(aListOfPickedUpCubes(23),aBookR5,aR5HoldingCube))))
  val OR5PickUpAt18 = Operation("OR5PickUpAt18", List(PropositionCondition(AND(List(NOT(gR5Booked),gR5BuildSpace1Booked)), List(aListOfPickedUpCubes(24),aBookR5,aR5HoldingCube))))
  //OPs for picking up cubes by R5 at spot 2
  val OR5PickUpAt21 = Operation("OR5PickUpAt21", List(PropositionCondition(AND(List(NOT(gR5Booked),gR5BuildSpace2Booked)), List(aListOfPickedUpCubes(25),aBookR5,aR5HoldingCube))))
  val OR5PickUpAt22 = Operation("OR5PickUpAt22", List(PropositionCondition(AND(List(NOT(gR5Booked),gR5BuildSpace2Booked)), List(aListOfPickedUpCubes(26),aBookR5,aR5HoldingCube))))
  val OR5PickUpAt23 = Operation("OR5PickUpAt23", List(PropositionCondition(AND(List(NOT(gR5Booked),gR5BuildSpace2Booked)), List(aListOfPickedUpCubes(27),aBookR5,aR5HoldingCube))))
  val OR5PickUpAt24 = Operation("OR5PickUpAt24", List(PropositionCondition(AND(List(NOT(gR5Booked),gR5BuildSpace2Booked)), List(aListOfPickedUpCubes(28),aBookR5,aR5HoldingCube))))
  val OR5PickUpAt25 = Operation("OR5PickUpAt25", List(PropositionCondition(AND(List(NOT(gR5Booked),gR5BuildSpace2Booked)), List(aListOfPickedUpCubes(29),aBookR5,aR5HoldingCube))))
  val OR5PickUpAt26 = Operation("OR5PickUpAt26", List(PropositionCondition(AND(List(NOT(gR5Booked),gR5BuildSpace2Booked)), List(aListOfPickedUpCubes(30),aBookR5,aR5HoldingCube))))
  val OR5PickUpAt27 = Operation("OR5PickUpAt27", List(PropositionCondition(AND(List(NOT(gR5Booked),gR5BuildSpace2Booked)), List(aListOfPickedUpCubes(31),aBookR5,aR5HoldingCube))))
  val OR5PickUpAt28 = Operation("OR5PickUpAt28", List(PropositionCondition(AND(List(NOT(gR5Booked),gR5BuildSpace2Booked)), List(aListOfPickedUpCubes(32),aBookR5,aR5HoldingCube))))
  //Ops for placing cubes with R4
  val OR4PutCubeAt11 = Operation("OR4PutCubeAt11", List(PropositionCondition(AND(List(NOT(gR4Booked),gR4HoldingCube)), List(aListOfPutDownCubes(1),aR4NotHoldingCube))))
  val OR4PutCubeAt12 = Operation("OR4PutCubeAt12", List(PropositionCondition(AND(List(NOT(gR4Booked),gR4HoldingCube)), List(aListOfPutDownCubes(2),aR4NotHoldingCube))))
  val OR4PutCubeAt13 = Operation("OR4PutCubeAt13", List(PropositionCondition(AND(List(NOT(gR4Booked),gR4HoldingCube)), List(aListOfPutDownCubes(3),aR4NotHoldingCube))))
  val OR4PutCubeAt14 = Operation("OR4PutCubeAt14", List(PropositionCondition(AND(List(NOT(gR4Booked),gR4HoldingCube)), List(aListOfPutDownCubes(4),aR4NotHoldingCube))))
  val OR4PutCubeAt21 = Operation("OR4PutCubeAt21", List(PropositionCondition(AND(List(NOT(gR4Booked),gR4HoldingCube)), List(aListOfPutDownCubes(5),aR4NotHoldingCube))))
  val OR4PutCubeAt22 = Operation("OR4PutCubeAt22", List(PropositionCondition(AND(List(NOT(gR4Booked),gR4HoldingCube)), List(aListOfPutDownCubes(6),aR4NotHoldingCube))))
  val OR4PutCubeAt23 = Operation("OR4PutCubeAt23", List(PropositionCondition(AND(List(NOT(gR4Booked),gR4HoldingCube)), List(aListOfPutDownCubes(7),aR4NotHoldingCube))))
  val OR4PutCubeAt24 = Operation("OR4PutCubeAt24", List(PropositionCondition(AND(List(NOT(gR4Booked),gR4HoldingCube)), List(aListOfPutDownCubes(8),aR4NotHoldingCube))))
  val OR4PutCubeAt31 = Operation("OR4PutCubeAt31", List(PropositionCondition(AND(List(NOT(gR4Booked),gR4HoldingCube)), List(aListOfPutDownCubes(9),aR4NotHoldingCube))))
  val OR4PutCubeAt32 = Operation("OR4PutCubeAt32", List(PropositionCondition(AND(List(NOT(gR4Booked),gR4HoldingCube)), List(aListOfPutDownCubes(10),aR4NotHoldingCube))))
  val OR4PutCubeAt33 = Operation("OR4PutCubeAt33", List(PropositionCondition(AND(List(NOT(gR4Booked),gR4HoldingCube)), List(aListOfPutDownCubes(11),aR4NotHoldingCube))))
  val OR4PutCubeAt34 = Operation("OR4PutCubeAt34", List(PropositionCondition(AND(List(NOT(gR4Booked),gR4HoldingCube)), List(aListOfPutDownCubes(12),aR4NotHoldingCube))))
  val OR4PutCubeAt41 = Operation("OR4PutCubeAt41", List(PropositionCondition(AND(List(NOT(gR4Booked),gR4HoldingCube)), List(aListOfPutDownCubes(13),aR4NotHoldingCube))))
  val OR4PutCubeAt42 = Operation("OR4PutCubeAt42", List(PropositionCondition(AND(List(NOT(gR4Booked),gR4HoldingCube)), List(aListOfPutDownCubes(14),aR4NotHoldingCube))))
  val OR4PutCubeAt43 = Operation("OR4PutCubeAt43", List(PropositionCondition(AND(List(NOT(gR4Booked),gR4HoldingCube)), List(aListOfPutDownCubes(15),aR4NotHoldingCube))))
  val OR4PutCubeAt44 = Operation("OR4PutCubeAt44", List(PropositionCondition(AND(List(NOT(gR4Booked),gR4HoldingCube)), List(aListOfPutDownCubes(16),aR4NotHoldingCube))))
  //Ops for placing cubes with R5
  val OR5PutCubeAt11 = Operation("OR5PutCubeAt11", List(PropositionCondition(AND(List(NOT(gR5Booked),gR5HoldingCube)), List(aListOfPutDownCubes(1),aR5NotHoldingCube))))
  val OR5PutCubeAt12 = Operation("OR5PutCubeAt12", List(PropositionCondition(AND(List(NOT(gR5Booked),gR5HoldingCube)), List(aListOfPutDownCubes(2),aR5NotHoldingCube))))
  val OR5PutCubeAt13 = Operation("OR5PutCubeAt13", List(PropositionCondition(AND(List(NOT(gR5Booked),gR5HoldingCube)), List(aListOfPutDownCubes(3),aR5NotHoldingCube))))
  val OR5PutCubeAt14 = Operation("OR5PutCubeAt14", List(PropositionCondition(AND(List(NOT(gR5Booked),gR5HoldingCube)), List(aListOfPutDownCubes(4),aR5NotHoldingCube))))
  val OR5PutCubeAt21 = Operation("OR5PutCubeAt21", List(PropositionCondition(AND(List(NOT(gR5Booked),gR5HoldingCube)), List(aListOfPutDownCubes(5),aR5NotHoldingCube))))
  val OR5PutCubeAt22 = Operation("OR5PutCubeAt22", List(PropositionCondition(AND(List(NOT(gR5Booked),gR5HoldingCube)), List(aListOfPutDownCubes(6),aR5NotHoldingCube))))
  val OR5PutCubeAt23 = Operation("OR5PutCubeAt23", List(PropositionCondition(AND(List(NOT(gR5Booked),gR5HoldingCube)), List(aListOfPutDownCubes(7),aR5NotHoldingCube))))
  val OR5PutCubeAt24 = Operation("OR5PutCubeAt24", List(PropositionCondition(AND(List(NOT(gR5Booked),gR5HoldingCube)), List(aListOfPutDownCubes(8),aR5NotHoldingCube))))
  val OR5PutCubeAt31 = Operation("OR5PutCubeAt31", List(PropositionCondition(AND(List(NOT(gR5Booked),gR5HoldingCube)), List(aListOfPutDownCubes(9),aR5NotHoldingCube))))
  val OR5PutCubeAt32 = Operation("OR5PutCubeAt32", List(PropositionCondition(AND(List(NOT(gR5Booked),gR5HoldingCube)), List(aListOfPutDownCubes(10),aR5NotHoldingCube))))
  val OR5PutCubeAt33 = Operation("OR5PutCubeAt33", List(PropositionCondition(AND(List(NOT(gR5Booked),gR5HoldingCube)), List(aListOfPutDownCubes(11),aR5NotHoldingCube))))
  val OR5PutCubeAt34 = Operation("OR5PutCubeAt34", List(PropositionCondition(AND(List(NOT(gR5Booked),gR5HoldingCube)), List(aListOfPutDownCubes(12),aR5NotHoldingCube))))
  val OR5PutCubeAt41 = Operation("OR5PutCubeAt41", List(PropositionCondition(AND(List(NOT(gR5Booked),gR5HoldingCube)), List(aListOfPutDownCubes(13),aR5NotHoldingCube))))
  val OR5PutCubeAt42 = Operation("OR5PutCubeAt42", List(PropositionCondition(AND(List(NOT(gR5Booked),gR5HoldingCube)), List(aListOfPutDownCubes(14),aR5NotHoldingCube))))
  val OR5PutCubeAt43 = Operation("OR5PutCubeAt43", List(PropositionCondition(AND(List(NOT(gR5Booked),gR5HoldingCube)), List(aListOfPutDownCubes(15),aR5NotHoldingCube))))
  val OR5PutCubeAt44 = Operation("OR5PutCubeAt44", List(PropositionCondition(AND(List(NOT(gR5Booked),gR5HoldingCube)), List(aListOfPutDownCubes(16),aR5NotHoldingCube))))

// val sopSeq = SOP(Sequence(o11, o12, o13), Sequence(o21, o22, o23), Parallel(o11,o12))
  val thaSop = SOP(
    Sequence(
        OR2PlaceBuildingPalett,OR2PalettToR4PalettSpace1, OR2PalettToR4PalettSpace2,OR4PickUpAt15,OR4PutCubeAt12,
        OR4PickUpAt11,OR4PutCubeAt11, OR4PickUpAt17,OR4PutCubeAt12,OR4PickUpAt12, OR4PutCubeAt21,OR4PickUpAt17,
        OR4PutCubeAt32,OR4PickUpAt13,OR4PutCubeAt31,OR4PickUpAt18,OR4PutCubeAt42, OR4PickUpAt14,OR4PutCubeAt41,
        OR4PickUpAt11,OR4PutCubeAt13,OR4PickUpAt15,OR4PutCubeAt14,OR4PickUpAt12, OR4PutCubeAt23,OR4PickUpAt16,
        OR4PutCubeAt24,OR4PickUpAt13,OR4PutCubeAt33,OR4PickUpAt17,OR4PutCubeAt34, OR4PickUpAt14,OR4PutCubeAt43,
        OR4PickUpAt18,OR4PutCubeAt44,OR2RemoveBuildingPalett, OR2PalettRemoveR4PalettSpace1,
        OR2PalettRemoveR4PalettSpace2
    )
  //Here is were the next tower would be.
  )
//(operation = f), indicates finished
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
