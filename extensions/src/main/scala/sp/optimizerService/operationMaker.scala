import sp.domain._
import sp.psl._
import sp.domain.Logic._


class operationMaker {



  //Init parsers
  val parserA = sp.domain.logic.ActionParser(List())
  val parserG = sp.domain.logic.PropositionParser(List(cubeSpace1Booked, cubeSpace2Booked, cubeSpace3Booked))

  // Things for Guards
  val cubeSpace1Booked = Thing("cubeSpace1Booked")                              //v1
  val cubeSpace2Booked = Thing("cubeSpace2Booked")                              //v2
  val cubeSpace3Booked = Thing("cubeSpace3Booked")                              //v3
  val cubeSpace4Booked = Thing("cubedSpace4Booked")                              //v4
  val buildSpaceBooked = Thing("buildSpaceBooked")
  val R2booked = Thing("R2booked")
  val R5booked = Thing("R5booked")
  val R4booked = Thing("R5booked")
  val R4dodge = Thing("R4dodge")
  val R5dodge = Thing("R5dodge")
  val H4empty = Thing("H4empty")
  val H4raised = Thing("H4raised")

  // Things for Actions
  val R2moveCubePalletTo1 = Thing("R2moveBuildPalletTo1")
  val R2moveCubePalletTo2 = Thing("R2moveBuildPalletTo3")
  val R2moveCubePalletTo3 = Thing("R2moveBuildPalletTo3")
  val R2moveCubePalletTo4 = Thing("R2moveBuildPalletTo4")
  val R2moveBuildPallet = Thing("R2moveBuildPallet")

  //Create gaurds

  val cubeSpace1Booked = parserG.parseStr("cubeSpace1Booked == true").right.get
  val cubeSpace2Booked = parserG.parseStr("cubeSpace2Booked == true").right.get
  val cubeSpace3Booked = parserG.parseStr("cubeSpace3Booked == true").right.get
  val cubeSpace4Booked = parserG.parseStr("cubeSpace4Booked == true").right.get
  val R2booked = parserG.parseStr("R2booked == false").right.get
  val R4booked = parserG.parseStr("R4booked == false").right.get
  val R5booked = parserG.parseStr("R5booked == false").right.get
  val R4dodge = parserG.parseStr("R4dodge == true").right.get
  val R5dodge = parserG.parseStr("R4dodge == true").right.get

  //Create actions
  val aGenerateOperatorInstructions = parserA.parseStr("generateOperatorInstructions = True").right.get


  //Operations
  val init = Operation("Init", List(PropositionCondition(AND(List()), List(aGenerateOperatorInstructions))),SPAttributes(), ID.newID)

}


