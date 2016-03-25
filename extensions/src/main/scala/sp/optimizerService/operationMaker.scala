



import sp.domain._
import sp.psl._
import sp.domain.Logic._


class operationMaker {


  //Init parsers
  val parserG = sp.domain.logic.PropositionParser(List(buildSpace1Booked, buildSpace2Booked, buildSpace3Booked,
    buildSpace4Booked,R2Booked,R4Booked,R5Booked, R4Dodge, R5Dodge))
  val parserA = sp.domain.logic.ActionParser(List())

  // Things for Guards
  val buildSpace1Booked = Thing("buildSpace1Booked") //For palettpos1
  val buildSpace2Booked = Thing("buildSpace2Booked") //For palettpos2
  val buildSpace3Booked = Thing("buildSpace3Booked") //For palettpos3
  val buildSpace4Booked = Thing("buildSpace4Booked") //For palettpos4
  val buildSpotBooked = Thing("buildSpotBooked")     //For the buildingspace were the tower should be built
  val R2Booked =  Thing("R2Booked")               //Book robot 2
  val R4Booked =  Thing("R4Booked")               //Book robot 4
  val R5Booked =  Thing("R5Booked")               //Book robot 5
  val R4Dodge =   Thing("R4Dodge")                //If robot 4 is in dodge pos
  val R5Dodge =   Thing("R5Dodge")                //If robot 4 is in dodge pos

  // Things for Actions
  val aR2MoveCubePalletTo1 = Thing("R2MoveBuildPalletTo1")    //Move the palett from the elevator to pos 1
  val aR2MoveCubePalletTo2 = Thing("R2MoveBuildPalletTo3")    //Move the palett from the elevator to pos 2
  val aR2MoveCubePalletTo3 = Thing("R2MoveBuildPalletTo3")    //Move the palett from the elevator to pos 3
  val aR2MoveCubePalletTo4 = Thing("R2MoveBuildPalletTo4")    //Move the palett from the elevator to pos 4
  val aR2MoveBuildPallet = Thing("R2MoveBuildPallet")         //Move the building platform from the elevator to buildningplace



  //Create gaurds
  val gBuildSpace1Booked = parserG.parseStr("buildSpace1Booked == true").right.get
  val gBuildSpace2Booked = parserG.parseStr("buildSpace2Booked == true").right.get
  val gBuildSpace3Booked = parserG.parseStr("buildSpace3Booked == true").right.get
  val gBuildSpace4Booked = parserG.parseStr("buildSpace4Booked == true").right.get
  val gR2booked = parserG.parseStr("R2Booked == false").right.get
  val gR4booked = parserG.parseStr("R4Booked == false").right.get
  val gR5booked = parserG.parseStr("R5Booked == false").right.get
  val gR4dodge = parserG.parseStr("R4Dodge == true").right.get
  val gR5dodge = parserG.parseStr("R4Dodge == true").right.get

  //Create actions
  // example val aGenerateOperatorInstructions = parserA.parseStr("generateOperatorInstructions = True").right.get
  val aBookR2 = parserA.parseStr("R2Booked = true").right.get
  val aBookR4 = parserA.parseStr("R4Booked = true").right.get
  val aBookR5 = parserA.parseStr("R5Booked = true").right.get
  val aUnBookR2 = parserA.parseStr("R2Booked = false").right.get
  val aUnBookR4 = parserA.parseStr("R4Booked = false").right.get
  val aUnBookR5 = parserA.parseStr("R5Booked = false").right.get
  

  //Operations
  //Example
 // val init = Operation("Init", List(PropositionCondition(AND(List()), List(aGenerateOperatorInstructions))),SPAttributes(), ID.newID)

  //R2 Operations
  //AND(List(NOT(gBuildSpace1Booked), NOT(gR2booked)))
  val OR2PalettToR5PalettSpace1 = Operation("OR2PalettToR5PalettSpace1", List(PropositionCondition(AND(List(NOT(gBuildSpace1Booked), NOT(gR2booked))),List(aBookR2))))
  // (Pos1 clear) Action R2 Booked = True
  val OR2PalettToR5PalettSpace2 = Operation("OR2PalettToR5PalettSpace2", List(PropositionCondition(AND(List(NOT(gBuildSpace2Booked), NOT(gR2booked))),List(aBookR2))))
  // (Pos2 clear AND POS1 filled)
  val OR2PalettToR4PalettSpace1 = Operation ("OR2PalettToR4PalettSpace1", List(PropositionCondition(AND(List(NOT(gBuildSpace3Booked), NOT(gR2booked))),List(aBookR2))))
  // (Pos1 clear)
  val OR2PalettToR4PalettSpace2 = Operation("OR2PalettToR4PalettSpace2", List(PropositionCondition(AND(List(NOT(gBuildSpace4Booked), NOT(gR2booked))),List(aBookR2))))
  // (Pos2 clear AND POS1 filled)
  val OR2PalettRemoveR5PalettSpace1 = Operation ("OR2PalettRemoveR5PalettSpace1", List(PropositionCondition(OR(List()),List())))
  // Operation R4BuildFromPos1 Done
  val OR2PalettRemoveR5PalettSpace2 = Operation ("OR2PalettRemoveR5PalettSpace2", List(PropositionCondition(OR(List()),List())))
  // Operation R4BuildFromPos2 Done
  val OR2PalettRemoveR4PalettSpace1 = Operation ("OR2PalettRemoveR4PalettSpace1", List(PropositionCondition(OR(List()),List())))
  // Operation R5BuildFromPos1 Done
  val OR2PalettRemoveR4PalettSpace2 = Operation ("OR2PalettRemoveR4PalettSpace2", List(PropositionCondition(OR(List()),List())))
  // Operation R5BuildFromPos2 Done
  val OR2PlaceBuildingPalett =   Operation ("OR2PlaceBuildingPalett", List(PropositionCondition(OR(List()),List())))
  //
  val OR2RemoveBuildingPalett =  Operation // Operation
  val OR2RemoveBooking =  Operation("OR2RemoveBooking", List(PropositionCondition(OR(List()),List()))) // After operations that books R2

  val OR4BuildFromPos1 = Operation // Operation OR2PlaceBuildingPalett done och R4Pos1 bokad och R4 ej bokad
  val OR4BuildFromPos2 = Operation // Operation OR2PlaceBuildingPalett done och R4Pos2 bokad och R4 ej bokad
  val OR4RemoveBooking = Operation // After operations that books R5

  val OR5BuildFromPos1 = Operation // Operation OR2PlaceBuildingPalett done och R5Pos1 bokad och R5 ej bokad
  val OR5BuildFromPos2 = Operation // Operation OR2PlaceBuildingPalett done och R5Pos2 bokad och R5 ej bokad
  val OR5RemoveBooking = Operation // After operations that books R5
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