package sp.optimizerService


import sp.domain._
import sp.psl._
import sp.domain.Logic._

/**
  * Created by Dell on 2016-03-22.
  */
class operationMaker {
  //Operation Init
  /*
  Thing (Namn, attributes: ?SPAttributes = SPAttributes()?, id)

  Operation(name: String,
                     conditions: List[Condition] = List(),
                     attributes: SPAttributes = SPAttributes(),
                     id: ID = ID.newID)
För exempel:
sp/core/src/test/scala/sp/services/relations/variableOperationMapperTest.scala

   */
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
}
