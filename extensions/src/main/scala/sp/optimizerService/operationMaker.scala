



import sp.domain._
import sp.psl._
import sp.domain.Logic._


class operationMaker {


  //Init parsers
  val parserG = sp.domain.logic.PropositionParser(List(cubeSpace1Booked, cubeSpace2Booked, cubeSpace3Booked, cubeSpace4Booked))
  val parserA = sp.domain.logic.ActionParser(List())

  // Things for Guards
  val cubeSpace1Booked = Thing("cubeSpace1Boked")                              //v1
  val cubeSpace2Booked = Thing("cubeSpace2Boked")                              //v2
  val cubeSpace3Booked = Thing("cubeSpace3Boked")                              //v3
  val cubeSpace4Booked = Thing("cubedSpace4Boked")                             //v4
  val buildSpaceBooked = Thing("buildSpaceBooked")
  val R2booked =  Thing("R2booked")
  val R5booked =  Thing("R5booked")
  val R4booked =  Thing("R5booked")
  val R4dodge =   Thing("R4dodge")
  val R5dodge =   Thing("R5dodge")

  // Things for Actions
  val R2moveCubePalletTo1 = Thing("R2moveBuildPalletTo1")
  val R2moveCubePalletTo2 = Thing("R2moveBuildPalletTo3")
  val R2moveCubePalletTo3 = Thing("R2moveBuildPalletTo3")
  val R2moveCubePalletTo4 = Thing("R2moveBuildPalletTo4")
  val R2moveBuildPallet = Thing("R2moveBuildPallet")



  //Create gaurds
  val buildSpace1Boked = parserG.parseStr("buildSpace1Boked == true").right.get
  val buildSpace2Boked = parserG.parseStr("buildSpace2Boked == true").right.get
  val buildSpace3Boked = parserG.parseStr("buildSpace3Boked == true").right.get
  val buildSpace4Boked = parserG.parseStr("buildSpace4Boked == true").right.get
  val R2booked = parserG.parseStr("R2booked == false").right.get
  val R4booked = parserG.parseStr("R4booked == false").right.get
  val R5booked = parserG.parseStr("R5booked == false").right.get
  val R4dodge = parserG.parseStr("R4dodge == true").right.get
  val R5dodge = parserG.parseStr("R4dodge == true").right.get

  //Create actions
  val aGenerateOperatorInstructions = parserA.parseStr("generateOperatorInstructions = True").right.get

  //Operations
  val init = Operation("Init", List(PropositionCondition(AND(List()), List(aGenerateOperatorInstructions))),SPAttributes(), ID.newID)
  val R2PalettToR5Pos1 = Operation
  val R2PalettToR5Pos2 = Operation
  val R2PalettToR4Pos1 = Operation
  val R2PalettToR4Pos2 = Operation
  val R2PalettRemoveR5Pos1 = Operation
  val R2PalettRemoveR5Pos2 = Operation
  val R2PalettRemoveR4Pos1 = Operation
  val R2PalettRemoveR4Pos2 = Operation
  val R2PlaceBuildingPalett = Operation
  val R2RemoveBuildingPalett = Operation
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