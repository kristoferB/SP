package sp.optimizerService

import sp.domain.{Operation, Sequence}

/**
  * Created by Kristian Eide on 2016-03-29.
  */
class SOPMaker {
  var SOP = SOP()
  def makeSOP(ls: List[List[String]]): Unit = {
    SOP = SOP(Sequence())
  }

  def getStartOperation(): List[Operation] = {
    val list[Operation] =  List(OR2PlaceBuildingPalett, OR2PalettToR4PalettSpace1, OR2PalettToR4PalettSpace2)
  }

  def getEndOperations(): List[Operation] = {

  }

  def getCubePlacingOperations(ls: List[List[String]]): List[Operation] = {

  }
}
