
package sp.optimizerService



import akka.actor._
import sp.domain.logic.IDAbleLogic
import scala.concurrent._
import sp.system.messages._
import sp.system._
import sp.domain._
import sp.domain.Logic._


object operationMaker extends SPService {
  val specification = SPAttributes(
    "service" -> SPAttributes(
      "group"-> "Operations",
      "description" -> "Makes operations"
    )
  )

  val transformation: List[TransformValue[_]] = List()
  def props = ServiceLauncher.props(Props(classOf[operationMaker]))
}


class operationMaker extends Actor with ServiceSupport {
  def receive = {
    case r @ Request(service, attr, ids, reqID) => {
      val replyTo = sender()
      implicit val rnr = RequestNReply(r, replyTo)



      //inti
      val init = Thing("init")
      val useTwoPalettes = Thing("useTwoPalettes")
      //For the buildingspace were the tower should be built
      val buildSpotBooked = Thing("buildSpotBooked")
      //Things for elivator 1
      val H1UpWithBuildPalette1 = Thing("H1UpWithBuildPalette1")
      val H1UpWithBuildPalette2 = Thing("H1UpWithBuildPalette2")
      //Things for elivator 2
      val H2UpWithBuildPalette1 = Thing("H2UpWithBuildPalette1")
      val H2UpWithBuildPalette2 = Thing("H2UpWithBuildPalette2")
      //Book robot 2
      val R2Booked = Thing("R2Booked")
      //Book robot 4
      val R4Booked = Thing("R4Booked")
      //Book robot 5
      val R5Booked = Thing("R5Booked")
      //Status of buildPalette 1
      val buildPalette1Empty = Thing("buildPalette1Empty")
      //Status of buildPalette 2
      val buildPalette2Empty = Thing("buildPalette2Empty")
      //Status of buildningPalette
      val buildingPaletteComplete = Thing("buildingPaletteComplete")
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
      //Things for status of palettes
      val BuildingPalette1In = Thing ("BuildingPalette1In")
      val BuildingPalette2In = Thing ("BuildingPalette2In")
      val BuildPaletteIn = Thing ("BuildingPaletteIn")

      //Vals for makeing Thnigs
      val fixtNo = 2
      val posFix = 8
      val row = 4
      val col = 4
      val rs = List("R4", "R5")
      val opNamePickedUpCubes = "PickedUpCubes"
      val opNamePutDownCubes = "PutDownCubes"
      val palette = "palette"
      val At = "At"
      val Space = "Space"
      val a = "a"
      val g = "g"

      //Ints for telling if ops are complete
      var Int1 = 0
      var Int2 = 0

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

      // Status of building palettes
      val listOfStatusBuildingPalettes = for {
        r <- rs
        f <- 1 to fixtNo
        t <- 1 to 2
      } yield{
        Thing(s"$palette$f$At$r$Space$t")
      }

      //Things for showing which cubes to be placed
      val cubesToBePlaced = "cubesToBePlaced"
      val listOfCubesToPlaced = for {
        f <- 1 to row
        p <- 1 to col
      } yield {
        Thing(s"$cubesToBePlaced$f$p")
      }

      //Iniate parsers--------------------------------------------------------------------------------------------------------

      var thingList: List[Thing] = List(init, R2Booked, R4Booked, R5Booked, buildSpotBooked, buildPalette1Empty,
        buildPalette2Empty, buildingPaletteComplete, R2OPComplete, R4OPComplete, R5OPComplete, R4HoldingCube, R5HoldingCube,
        BuildingPalette1In, BuildingPalette2In,H1UpWithBuildPalette1,H1UpWithBuildPalette2,H2UpWithBuildPalette1,H2UpWithBuildPalette2,
        BuildPaletteIn,useTwoPalettes) ++ listOfPickedUpCubes ++ listOfPutDownCubes ++ listOfStatusBuildingPalettes ++ listOfCubesToPlaced

      val parserG = sp.domain.logic.PropositionParser(thingList)

      val parserA = sp.domain.logic.ActionParser(thingList)




      //Create gaurds-------------------------------------------------------------------------------------------------------

      //init guard
      val gInit = parserG.parseStr("init == true").right.get
      val gUseTwoPalettes = parserG.parseStr("useTwoPalettes == true").right.get
      //Guard for elivator 1
      val gH1UpWithBuildPalette1 = parserG.parseStr("H1UpWithBuildPalette1 == true").right.get
      val gH1UpWithBuildPalette2 = parserG.parseStr("H1UpWithBuildPalette2 == true").right.get
      //Guard for elivator 2
      val gH2OutWithBuildPalette1 = parserG.parseStr("H2UpWithBuildPalette1 == true").right.get
      val gH2OutWithBuildPalette2 = parserG.parseStr("H2UpWithBuildPalette1 == true").right.get
      //Guard for status of buildpalette
      val gBuildingPalette1In = parserG.parseStr("BuildingPalette1 == true").right.get
      val gBuildingPalette2In = parserG.parseStr("BuildingPalette1 == true").right.get
      val gBuildPaletteIn = parserG.parseStr("BuildingPalette == true").right.get
      //Guard for booked buildspot
      val gBuildSpotBooked =  parserG.parseStr("buildSpotBooked == true").right.get
      //Guards for robots holding cubes
      val gR4HoldingCube = parserG.parseStr("R4HoldingCube == true").right.get
      val gR5HoldingCube = parserG.parseStr("R5HoldingCube == true").right.get
      //Guards for  robotbookings
      val gR2Booked = parserG.parseStr("R2Booked == true").right.get
      val gR4Booked = parserG.parseStr("R4Booked == true").right.get
      val gR5Booked = parserG.parseStr("R5Booked == true").right.get
//      //Guards for Operation Completion
//      val gR2OPComplete = parserG.parseStr("R2OPComplete == true").right.get
//      val gR4OPComplete = parserG.parseStr("R4OPComplete == true").right.get
//      val gR5OPComplete = parserG.parseStr("R5OPComplete == true").right.get
      //Guards which tells when buildpalettes is empty
      val gBuildPalette1Empty = parserG.parseStr("buildPalette1Empty == true").right.get
      val gBuildPalette2Empty = parserG.parseStr("buildPalette2Empty == true").right.get
      //Guard for when buildningpalette is complete
      val gBuildingPaletteComplete = parserG.parseStr("buildingPaletteComplete == true").right.get
      //Guard which tells if cube is picked up from buildingspace
      val gListOfPickedUpCubes = for {
        r <- rs
        f <- 1 to fixtNo
        p <- 1 to posFix
      } yield {
        parserG.parseStr(s"$r$opNamePickedUpCubes$f$p == true").right.get
      }
      //Guard which tells if cube is placeed at buildingspot
      val gListOfPutDownCubes = for {
        f <- 1 to row
        p <- 1 to col
      } yield {
        parserG.parseStr(s"$opNamePutDownCubes$f$p == true").right.get
      }
      //Guards för buildingPalettes
      val gListOfStatusBuildingPalettes = for {
        f <- 1 to fixtNo
        r <- rs
        t <- 1 to 2
      } yield{
        parserG.parseStr(s"$palette$f$At$r$Space$t == true").right.get
      }
      val gCubesToBePlaced = "CubesToBePlaced"
      val gListOfCubesToPlaced = for {
        f <- 1 to row
        p <- 1 to col
      } yield {
        parserG.parseStr(s"$gCubesToBePlaced$f$p == true").right.get
      }

      //Create actions--------------------------------------------------------------------------------------------------------
      // example val aGenerateOperatorInstructions = parserA.parseStr("generateOperatorInstructions = True").right.get
      //
      //Actions for elivator 1 palette 1
      val aH1UpWithBuildPalette1True = parserA.parseStr("H1UpWithBuildPalette1 = true").right.get
      val aH1UpWithBuildPalette1False = parserA.parseStr("H1UpWithBuildPalette1 = false").right.get
      //Actions for elivator 1 palette 2
      val aH1UpWithBuildPalette2True = parserA.parseStr("H1UpWithBuildPalette2 = true").right.get
      val aH1UpWithBuildPalette2False = parserA.parseStr("H1UpWithBuildPalette2 = false").right.get
      //Actions for elivator 2 palette 1
      val aH2OutWithBuildPalette1True = parserA.parseStr("H2UpWithBuildPalette1 = true").right.get
      val aH2OutWithBuildPalette1False = parserA.parseStr("H2UpWithBuildPalette1 = false").right.get
      //Actions for elivator 2 palette 2
      val aH2OutWithBuildPalette2True = parserA.parseStr("H2UpWithBuildPalette2 = true").right.get
      val aH2OutWithBuildPalette2False = parserA.parseStr("H2UpWithBuildPalette2 = false").right.get
      //Actions for changing status of palettes - TRUE
      val aBuildingPalette1In = parserA.parseStr("BuildingPalette1In = true").right.get
      val aBuildingPalette2In = parserA.parseStr("BuildingPalette2In = true").right.get
      val aBuildPaletteIn = parserA.parseStr("BuildingPaletteIn = true").right.get
      //-----False
      val aBuildingPalette1Out = parserA.parseStr("BuildingPalette1In = false").right.get
      val aBuildingPalette2Out = parserA.parseStr("BuildingPalette2In = false").right.get
      val aBuildPaletteOut = parserA.parseStr("BuildingPaletteIn = false").right.get
      //init guard
      val aInit = parserA.parseStr("init = true").right.get
      val aInitDone = parserA.parseStr("init = false").right.get
      val aUseTwoPalettesTrue = parserA.parseStr("useTwoPalettes = true").right.get
      val aUseTwoPalettesFalse = parserA.parseStr("useTwoPalettes = false").right.get
      //Actions for booking robots
      val aBookR2 = parserA.parseStr("R2Booked = true").right.get
      val aBookR4 = parserA.parseStr("R4Booked = true").right.get
      val aBookR5 = parserA.parseStr("R5Booked = true").right.get
      //Actions for unbooking robots
      val aUnBookR2 = parserA.parseStr("R2Booked = false").right.get
      val aUnBookR4 = parserA.parseStr("R4Booked = false").right.get
      val aUnBookR5 = parserA.parseStr("R5Booked = false").right.get
      //Action which book Buildspot
      val aBuildSpotBook = parserA.parseStr("buildSpotBooked = true").right.get
      //Action which unbook buildspot
      val aBuildSpotUnBook = parserA.parseStr("buildSpotBooked = false").right.get
      //Change status of bulding palettes
      val aNewBuildingPaletteComplete = parserA.parseStr("buildingPaletteComplete = false").right.get
      val aBuildingPaletteIsComplete = parserA.parseStr("buildingPaletteComplete = true").right.get
      //Actions which indicate if R4 or R5 is holding a cube
      val aR4HoldingCube = parserA.parseStr("R4HoldingCube = true").right.get
      val aR5HoldingCube = parserA.parseStr("R5HoldingCube = true").right.get
      //Actions which indicate if R4 or R5 is not holding a cube
      val aR4NotHoldingCube = parserA.parseStr("R4HoldingCube = false").right.get
      val aR5NotHoldingCube = parserA.parseStr("R5HoldingCube = false").right.get
      //Actions which tells when buildpalettes is not empty
      val aBuildPalette1Empty = parserA.parseStr("buildPalette1Empty = true").right.get
      val aBuildPalette2Empty = parserA.parseStr("buildPalette2Empty = true").right.get
      //Actions which tells when buildpalettes is empty
      val aBuildPalette1NotEmpty = parserA.parseStr("buildPalette1Empty = false").right.get
      val aBuildPalette2NotEmpty = parserA.parseStr("buildPalette2Empty = false").right.get
      //Action which tells if cube is picked up from buildingspace
      val aListOfPickedUpCubesTrue = for {
        r <- rs
        f <- 1 to fixtNo
        p <- 1 to posFix
      } yield {
        parserA.parseStr(s"$r$opNamePickedUpCubes$f$p = true").right.get
      }
      val aListOfPickedUpCubesFalse = for {
        r <- rs
        f <- 1 to fixtNo
        p <- 1 to posFix
      } yield {
        parserA.parseStr(s"$r$opNamePickedUpCubes$f$p = false").right.get
      }
      //Action which tells if cube is placeed at buildingspot
      val aListOfPutDownCubesTrue = for {
        f <- 1 to row
        p <- 1 to col
      } yield {
        parserA.parseStr(s"$opNamePutDownCubes$f$p = true").right.get
      }
      val aListOfPutDownCubesFalse = for {
        f <- 1 to row
        p <- 1 to col
      } yield {
        parserA.parseStr(s"$opNamePutDownCubes$f$p = false").right.get
      }
      //Actionss för buildingPalettes
      val aChangeStatusBuildingPalettesTrue = for {
        f <- 1 to fixtNo
        r <- rs
        t <- 1 to 2
      } yield{
        parserA.parseStr(s"$palette$f$At$r$Space$t = true").right.get
      }
      //Actionss för buildingPalettes
      val aChangeStatusBuildingPalettesFalse = for {
        f <- 1 to fixtNo
        r <- rs
        t <- 1 to 2
      } yield{
        parserA.parseStr(s"$palette$f$At$r$Space$t = false").right.get
      }
      val aCubesToBePlaced = "cubesToBePlaced"
      val aListOfCubesToPlacedTrue = for {
        f <- 1 to row
        p <- 1 to col
      } yield {
        parserA.parseStr(s"$aCubesToBePlaced$f$p = true").right.get
      }
      val aListOfCubesToPlacedFalse = for {
        f <- 1 to row
        p <- 1 to col
      } yield {
        parserA.parseStr(s"$aCubesToBePlaced$f$p = false").right.get
      }

      //Operations----------------------------------------------------------------------------------------------------------

      //Example
      // val init = Operation("Init", List(PropositionCondition(AND(List()), List(aGenerateOperatorInstructions))),SPAttributes(), ID.newID)
      //init OP
      val OInitOperation = Operation("initOperation",List(PropositionCondition(AND(List(gInit)),List(aInit,aBuildPaletteOut,aBuildingPalette1Out,
        aUnBookR2,aUnBookR4,aUnBookR5,aBuildPalette1NotEmpty,aBuildPalette2NotEmpty,aH1UpWithBuildPalette1False,aH1UpWithBuildPalette2False,
        aH2OutWithBuildPalette1False,aH2OutWithBuildPalette2False,aBuildSpotUnBook,aNewBuildingPaletteComplete,aR4NotHoldingCube,
        aR5NotHoldingCube,aUseTwoPalettesFalse
      )++aListOfPutDownCubesFalse++aListOfPickedUpCubesFalse++aListOfCubesToPlacedFalse++aChangeStatusBuildingPalettesFalse)))
      //use two pallets
      val OUseTwoPallets = Operation( "OUseTwoPallets",PropositionCondition(AlwaysTrue,List(aUseTwoPalettesTrue)))
      //operator Ops
      val OMoveInBuildingPalette1 = Operation("OMoveInBuildingPalette1", List(PropositionCondition(AND(List(NOT(gBuildingPalette1In),gInit)),List(aBuildingPalette1In,aInitDone))))
      val OMoveInBuildingPalette2 = Operation("OMoveInBuildingPalette1", List(PropositionCondition(AND(List(NOT(gBuildingPalette1In),gUseTwoPalettes)),List(aBuildingPalette2In))))
      val OMoveInBuildPalette = Operation("OMoveInBuildPalette", List(PropositionCondition(AND(List(gBuildPaletteIn)),List(aBuildPaletteIn))))
      //Elevator 1 Operations
      val OMoveUpPalette1WithElevator1 = Operation("OMoveUpPalette1WithElevator1",List(PropositionCondition(AND(List(gBuildingPalette1In)),List(aH1UpWithBuildPalette1True,aBuildingPalette1Out))))
      val OMoveDownPalette1WithElevator1 = Operation("OMoveDownPalette1WithElevator1",List(PropositionCondition(AND(List(gH1UpWithBuildPalette1,OR(List(gListOfStatusBuildingPalettes(0),gListOfStatusBuildingPalettes(1),gListOfStatusBuildingPalettes(2),gListOfStatusBuildingPalettes(3))))),List(aH1UpWithBuildPalette1False))))
      val OMoveUpPalette2WithElevator1 = Operation("OMoveUpPalette2WithElevator1",List(PropositionCondition(AND(List(gBuildingPalette2In)),List(aH1UpWithBuildPalette2True,aBuildingPalette2Out))))
      val OMoveDownPalette2WithElevator1 = Operation("OMoveDownPalette2WithElevator1",List(PropositionCondition(AND(List(gH1UpWithBuildPalette2,OR(List(gListOfStatusBuildingPalettes(4),gListOfStatusBuildingPalettes(5),gListOfStatusBuildingPalettes(6),gListOfStatusBuildingPalettes(7))))),List(aH1UpWithBuildPalette2False))))
      //R2 Operations
      val OR2Palette1ToR4Space1 = Operation("OR2Palette1ToR4Space1", List(PropositionCondition(AND(List(NOT(gListOfStatusBuildingPalettes(0)),NOT(gR2Booked),NOT(gR4Booked),gH1UpWithBuildPalette1)), List(aBookR2,aChangeStatusBuildingPalettesTrue(0)))))
      // (Pos1 clear) Action R2 Booked = True
      val OR2Palette1ToR4Space2 = Operation("OR2Palette1ToR4Space2", List(PropositionCondition(AND(List(NOT(gListOfStatusBuildingPalettes(1)),gListOfStatusBuildingPalettes(0),NOT(gR2Booked),NOT(gR4Booked),gH1UpWithBuildPalette1)), List(aBookR2,aChangeStatusBuildingPalettesTrue(1)))))
      // (Pos2 clear AND POS1 filled)
      val OR2Palette1ToR5Space1 = Operation("OR2Palette1ToR5Space1", List(PropositionCondition(AND(List(NOT(gListOfStatusBuildingPalettes(2)),NOT(gR2Booked),NOT(gR4Booked),gH1UpWithBuildPalette1)), List(aBookR2,aChangeStatusBuildingPalettesTrue(2)))))
      // (Pos1 clear)
      val OR2Palette1ToR5Space2 = Operation("OR2Palette1ToR5Space2", List(PropositionCondition(AND(List(NOT(gListOfStatusBuildingPalettes(3)),gListOfStatusBuildingPalettes(2),NOT(gR2Booked),NOT(gR4Booked),gH1UpWithBuildPalette1)), List(aBookR2,aChangeStatusBuildingPalettesTrue(3)))))
      // (Pos2 clear AND POS1 filled)
      val OR2Palette2ToR4Space1 = Operation("OR2Palette2ToR4Space1", List(PropositionCondition(AND(List(NOT(gListOfStatusBuildingPalettes(4)),NOT(gR2Booked),NOT(gR4Booked),gH1UpWithBuildPalette1)), List(aBookR2,aChangeStatusBuildingPalettesTrue(4)))))
      // (Pos1 clear) Action R2 Booked = True
      val OR2Palette2ToR4Space2 = Operation("OR2Palette2ToR4Space2", List(PropositionCondition(AND(List(NOT(gListOfStatusBuildingPalettes(5)),gListOfStatusBuildingPalettes(4),NOT(gR2Booked),NOT(gR4Booked),gH1UpWithBuildPalette1)), List(aBookR2,aChangeStatusBuildingPalettesTrue(5)))))
      // (Pos2 clear AND POS1 filled)
      val OR2Palette2ToR5Space1 = Operation("OR2Palette2ToR5Space1", List(PropositionCondition(AND(List(NOT(gListOfStatusBuildingPalettes(6)),NOT(gR2Booked),NOT(gR4Booked),gH1UpWithBuildPalette1)), List(aBookR2,aChangeStatusBuildingPalettesTrue(6)))))
      // (Pos1 clear)
      val OR2Palette2ToR5Space2 = Operation("OR2Palette2ToR5Space2", List(PropositionCondition(AND(List(NOT(gListOfStatusBuildingPalettes(7)),gListOfStatusBuildingPalettes(6),NOT(gR2Booked),NOT(gR4Booked),gH1UpWithBuildPalette1)), List(aBookR2,aChangeStatusBuildingPalettesTrue(7)))))
      // (Pos2 clear AND POS1 filled)
      val OR2PlaceBuildingPalette = Operation("OR2PlaceBuildingPalette", List(PropositionCondition(AND(List(gBuildPaletteIn,NOT(gR2Booked),NOT(gR4Booked),NOT(gR5Booked),NOT(gBuildSpotBooked))), List(aBookR2,aBuildSpotBook))))
      //
      val OR2RemoveBooking = Operation("OR2RemoveBooking", List(PropositionCondition(AND(List(gR2Booked)), List(aUnBookR2))))
      // After operations that books R2
      val OR4RemoveBooking = Operation("OR2RemoveBooking", List(PropositionCondition(AND(List(gR4Booked)), List(aUnBookR4))))
      // After operations that books R4
      val OR5RemoveBooking = Operation("OR2RemoveBooking", List(PropositionCondition(AND(List(gR5Booked)), List(aUnBookR5))))
      // After operations that books R5
      //operationerna nedan skall ändras så att de passar bättre och blir sumerade

      //Bygg en rad i taget och lägg in dodge och lagg in r4 plocka från 3,4 och gör om placeringar till 1,2,3,4 istället för 1,2


      //Wallschem ops
      val OWallSchemeOps = "OWallSchemeOps"
      val listOfWallSchemeOps = for{
        e <- 1 to 16 // 0 to 15 in list
      } yield{
        Operation (s"$OWallSchemeOps$e",List(PropositionCondition(AND(List(AlwaysTrue)),List(aListOfCubesToPlacedTrue(e-1)))))
      }
      //OPs for picking up cubes by R4 at space 1, row 1
      val OR4PickUpAt = "OR4PickUpAt"
      val OListR4PickUpAt11To14 = for {
        e <- 11 to 14 // 0 to 3 in list
      } yield {
        Operation(s"$OR4PickUpAt$e",List(PropositionCondition(AND(List(NOT(gR2Booked),NOT(gR5Booked),NOT(gR4Booked),OR(List(gListOfStatusBuildingPalettes(0),gListOfStatusBuildingPalettes(4))),gListOfCubesToPlaced(e-11))), List(aBookR4,aR4HoldingCube,aListOfPickedUpCubesTrue(e-11)))),SPAttributes("duration" -> 5))
      }
      //OPs for picking up cubes by R4 at space 1, row 2
      val OListR4PickUpAt15To18 = for {
        e <- 15 to 18 // 4 to 7 in list
      } yield {
        Operation(s"$OR4PickUpAt$e",List(PropositionCondition(AND(List(NOT(gR2Booked),NOT(gR5Booked),NOT(gR4Booked),OR(List(gListOfStatusBuildingPalettes(0),gListOfStatusBuildingPalettes(4))),gListOfCubesToPlaced(e-11),gListOfPickedUpCubes(e-4-11))), List(aBookR4,aR4HoldingCube,aListOfPickedUpCubesTrue(e-11)))),SPAttributes("duration" -> 5))
      }
      //OPs for picking up cubes by R4 at space 2, row 3 and 4
      val OListR4PickUpAt21To28 = for {
        e <- 21 to 28 // 8 to 15 in list
      } yield {
        Operation(s"$OR4PickUpAt$e",List(PropositionCondition(AND(List(NOT(gR2Booked),NOT(gR5Booked),NOT(gR4Booked),OR(List(gListOfStatusBuildingPalettes(1),gListOfStatusBuildingPalettes(5))),gListOfCubesToPlaced(e-13),gListOfPickedUpCubes(e-4-13))), List(aBookR4,aR4HoldingCube,aListOfPickedUpCubesTrue(e-13)))),SPAttributes("duration" -> 5))
      }
      //OPs for picking up cubes by R5 at space 1, row 1
      val OR5PickUpAt = "OR5PickUpAt"
      val OListR5PickUpAt11To14 = for {
        e <- 31 to 34 // 16 to 19 in list
      } yield {
        Operation(s"$OR5PickUpAt$e",List(PropositionCondition(AND(List(NOT(gR2Booked),NOT(gR5Booked),NOT(gR4Booked),OR(List(gListOfStatusBuildingPalettes(2),gListOfStatusBuildingPalettes(6))),gListOfCubesToPlaced(e-31))), List(aBookR5,aR5HoldingCube,aListOfPickedUpCubesTrue(e-15)))),SPAttributes("duration" -> 5))
      }
      //OPs for picking up cubes by R5 t space 1, row 2
      val OListR5PickUpAt15To18 = for {
        e <- 35 to 38 // 20 to 23 in list
      } yield {
        Operation(s"$OR5PickUpAt$e",List(PropositionCondition(AND(List(NOT(gR2Booked),NOT(gR5Booked),NOT(gR4Booked),OR(List(gListOfStatusBuildingPalettes(2),gListOfStatusBuildingPalettes(6))),gListOfCubesToPlaced(e-31),gListOfPickedUpCubes(e-19))), List(aBookR5,aR5HoldingCube,aListOfPickedUpCubesTrue(e-15)))),SPAttributes("duration" -> 5))
      }
      //OPs for picking up cubes by R5 t space 2, row 3 and 4
            val OListR5PickUpAt21To28 = for {
        e <- 41 to 48 // 24 to 31 in list
      } yield {
        Operation(s"$OR5PickUpAt$e",List(PropositionCondition(AND(List(NOT(gR2Booked),NOT(gR5Booked),NOT(gR4Booked),OR(List(gListOfStatusBuildingPalettes(3),gListOfStatusBuildingPalettes(7))),gListOfCubesToPlaced(e-33),gListOfPickedUpCubes(e-21))), List(aBookR5,aR5HoldingCube,aListOfPickedUpCubesTrue(e-33)))),SPAttributes("duration" -> 5))
      }

      //OPs for placing cubes with R4 11 - 14
      val OR4PlaceCubeAt = "OR4PlaceCubeAt"
      val OListR4PlaceCubeAt11To14 = for {
        e <- 11 to 14// 0 to 3 in list
      } yield {
        Operation(s"$OR4PlaceCubeAt$e",List(PropositionCondition(AND(List(NOT(gR4Booked),gR4HoldingCube,gListOfPickedUpCubes(e-11),NOT(gListOfPutDownCubes(e-11)))),List(aListOfPutDownCubesTrue(e-11),aR4NotHoldingCube))),SPAttributes("duration" -> 5))
      }
      //OPs for placing cubes with R4 21 - 24
      val OListR4PlaceCubeAt21To24 = for {
        e <- 21 to 24 // 4 to 7 in list
      } yield {
        Operation(s"$OR4PlaceCubeAt$e",List(PropositionCondition(AND(List(NOT(gR4Booked),gR4HoldingCube,gListOfPickedUpCubes(e-14),NOT(gListOfPutDownCubes(e-14)))),List(aListOfPutDownCubesTrue(e-14),aR4NotHoldingCube))),SPAttributes("duration" -> 5))
      }
      //OPs for placing cubes with R4 31 - 34
      val OListR4PlaceCubeAt31To34 = for {
        e <- 31 to 34 // 8 to 11 in list
      } yield {
        Operation(s"$OR4PlaceCubeAt$e",List(PropositionCondition(AND(List(NOT(gR4Booked),gR4HoldingCube,gListOfPickedUpCubes(e-23),NOT(gListOfPutDownCubes(e-23)))),List(aListOfPutDownCubesTrue(e-23),aR4NotHoldingCube))),SPAttributes("duration" -> 5))
      }
      val OListR4PlaceCubeAt41To44 = for {
        e <- 41 to 44 // 12 to 15 in list
      } yield {
        Operation(s"$OR4PlaceCubeAt$e",List(PropositionCondition(AND(List(NOT(gR4Booked),gR4HoldingCube,gListOfPickedUpCubes(e-29),NOT(gListOfPutDownCubes(e-29)))),List(aListOfPutDownCubesTrue(e-29),aR4NotHoldingCube))),SPAttributes("duration" -> 5))
      }

      //OPs for placing cubes with R5 11 - 14
      val OR5PlaceCubeAt = "OR5PlaceCubeAt"
      val OListR5PlaceCubeAt11To14 = for {
        e <- 11 to 14
      } yield {
        Operation(s"$OR5PlaceCubeAt$e",List(PropositionCondition(AND(List(NOT(gR5Booked),gR5HoldingCube,gListOfPickedUpCubes(e+5),NOT(gListOfPutDownCubes(e-11)))),List(aListOfPutDownCubesTrue(e-11),aR5NotHoldingCube))),SPAttributes("duration" -> 5))
      }
      //OPs for placing cubes with R5 21 - 24
      val OListR5PlaceCubeAt21To24 = for {
        e <- 21 to 24
      } yield {
        Operation(s"$OR5PlaceCubeAt$e",List(PropositionCondition(AND(List(NOT(gR5Booked),gR5HoldingCube,gListOfPickedUpCubes(e-1),NOT(gListOfPutDownCubes(e-17)))),List(aListOfPutDownCubesTrue(e-17),aR5NotHoldingCube))),SPAttributes("duration" -> 5))
      }
      //OPs for placing cubes with R5 31 - 44
      val OListR5PlaceCubeAt31To34 = for {
        e <- 31 to 34
      } yield {
        Operation(s"$OR5PlaceCubeAt$e",List(PropositionCondition(AND(List(NOT(gR5Booked),gR5HoldingCube,gListOfPickedUpCubes(e-7),NOT(gListOfPutDownCubes(e-23)))),List(aListOfPutDownCubesTrue(e-23),aR5NotHoldingCube))),SPAttributes("duration" -> 5))
      }
      val OListR5PlaceCubeAt41To44 = for {
        e <- 41 to 44
      } yield {
        Operation(s"$OR5PlaceCubeAt$e",List(PropositionCondition(AND(List(NOT(gR5Booked),gR5HoldingCube,gListOfPickedUpCubes(e-13),NOT(gListOfPutDownCubes(e-29)))),List(aListOfPutDownCubesTrue(e-29),aR5NotHoldingCube))),SPAttributes("duration" -> 5))
      }

      //Operation which tells when towers is comeplete
      // use XNOR for all 16 pos
       val OBuildingPaletteComplete = Operation("OBuildingPaletteComplete",PropositionCondition(EQ(Int1,Int2),List(aBuildingPaletteIsComplete,aBuildPalette1Empty,aBuildPalette2Empty)))
      // inrcement finns i propsistion condition och göra tester

      //Elevator 2 Operations
      val OMoveUpPalette1WithElevator2 = Operation("OMoveUpPalette1WithElevator1",List(PropositionCondition(AND(List(gBuildPalette1Empty)),List(aH2OutWithBuildPalette1True))))
      val OMoveUpPalette2WithElevator2 = Operation("OMoveUpPalette2WithElevator1",List(PropositionCondition(AND(List(gBuildPalette2Empty)),List(aH2OutWithBuildPalette2True))))

      //Remove building palettes ops
      val OR2Palette1RemoveR4Space1 = Operation("OR2Palette1RemoveR4Space1", List(PropositionCondition(AND(List(NOT(gR2Booked),gListOfStatusBuildingPalettes(0),gH2OutWithBuildPalette1,NOT(gBuildSpotBooked))), List(aBookR2,aChangeStatusBuildingPalettesFalse(0)))))
      // Operation R4BuildFromPos1 Done
      val OR2Palette1RemoveR4Space2 = Operation("OR2Palette1RemoveR4Space2", List(PropositionCondition(AND(List(NOT(gR2Booked),gListOfStatusBuildingPalettes(1),gH2OutWithBuildPalette1,NOT(gBuildSpotBooked))), List(aBookR2,aChangeStatusBuildingPalettesFalse(1)))))
      // Operation R4BuildFromPos2 Done
      val OR2Palette1RemoveR5Space1 = Operation("OR2Palette1RemoveR5Space1", List(PropositionCondition(AND(List(NOT(gR2Booked),gListOfStatusBuildingPalettes(2),gH2OutWithBuildPalette1,NOT(gBuildSpotBooked))), List(aBookR2,aChangeStatusBuildingPalettesFalse(2)))))
      // Operation R5BuildFromPos1 Done
      val OR2Palette1RemoveR5Space2 = Operation("OR2Palette1RemoveR5Space2", List(PropositionCondition(AND(List(NOT(gR2Booked),gListOfStatusBuildingPalettes(3),gH2OutWithBuildPalette1,NOT(gBuildSpotBooked))), List(aBookR2,aChangeStatusBuildingPalettesFalse(3)))))
      // Operation R5BuildFromPos2 Done
      val OR2Palette2RemoveR4Space1 = Operation("OR2Palette2RemoveR4Space1", List(PropositionCondition(AND(List(NOT(gR2Booked),gListOfStatusBuildingPalettes(4),gH2OutWithBuildPalette2,NOT(gBuildSpotBooked))), List(aBookR2,aChangeStatusBuildingPalettesFalse(4)))))
      // Operation R4BuildFromPos1 Done
      val OR2Palette2RemoveR4Space2 = Operation("OR2Palette2RemoveR4Space2", List(PropositionCondition(AND(List(NOT(gR2Booked),gListOfStatusBuildingPalettes(5),gH2OutWithBuildPalette2,NOT(gBuildSpotBooked))), List(aBookR2,aChangeStatusBuildingPalettesFalse(5)))))
      // Operation R4BuialdFromPos2 Done
      val OR2Palette2RemoveR5Space1 = Operation("OR2Palette2RemoveR5Space1", List(PropositionCondition(AND(List(NOT(gR2Booked),gListOfStatusBuildingPalettes(6),gH2OutWithBuildPalette2,NOT(gBuildSpotBooked))), List(aBookR2,aChangeStatusBuildingPalettesFalse(6)))))
      // Operation R5BuildFromPos1 Done
      val OR2Palette2RemoveR5Space2 = Operation("OR2Palette2RemoveR5Space2", List(PropositionCondition(AND(List(NOT(gR2Booked),gListOfStatusBuildingPalettes(7),gH2OutWithBuildPalette2,NOT(gBuildSpotBooked))), List(aBookR2,aChangeStatusBuildingPalettesFalse(7)))))
      //Op for removing complete tower
      val OR2RemoveBuildingPalette = Operation("OR2RemoveBuildingPalette", List(PropositionCondition(AND(List(NOT(gR2Booked),NOT(gR5Booked),NOT(gR4Booked),gBuildSpotBooked,gBuildingPaletteComplete)), List(aBookR2,aBuildSpotUnBook))))

      // Ops for moving lowering H2
      val OMoveOutPalette1WithElevator2 = Operation("OMoveUpPalette1WithElevator1",List(PropositionCondition(AND(List(gH2OutWithBuildPalette1,OR(List(gListOfStatusBuildingPalettes(0),gListOfStatusBuildingPalettes(1),gListOfStatusBuildingPalettes(2),gListOfStatusBuildingPalettes(3))))),List(aH2OutWithBuildPalette1False))))
      val OMoveOutPalette2WithElevator2 = Operation("OMoveUpPalette2WithElevator1",List(PropositionCondition(AND(List(gH2OutWithBuildPalette2,OR(List(gListOfStatusBuildingPalettes(4),gListOfStatusBuildingPalettes(5),gListOfStatusBuildingPalettes(6),gListOfStatusBuildingPalettes(7))))),List(aH2OutWithBuildPalette2False))))
      //LIST With all OPS

      val allOPs: List[Operation] = List(OMoveInBuildingPalette1,OMoveInBuildingPalette2,OMoveInBuildPalette,OR2Palette1ToR4Space1,
        OR2Palette1ToR4Space2,OR2Palette1ToR5Space1,OR2Palette1ToR5Space2,OR2Palette2ToR4Space1,OR2Palette2ToR4Space2,OR2Palette2ToR5Space1,
        OR2Palette2ToR5Space2,OMoveUpPalette1WithElevator1,OMoveDownPalette1WithElevator1,OMoveUpPalette2WithElevator1,OMoveDownPalette2WithElevator1,
        OR2PlaceBuildingPalette,OR2RemoveBooking,OR4RemoveBooking,OR5RemoveBooking,OMoveUpPalette1WithElevator2,OMoveUpPalette2WithElevator2,
        OR2Palette1RemoveR4Space1,OR2Palette1RemoveR4Space2,OR2Palette1RemoveR5Space1,OR2Palette1RemoveR5Space2,OR2Palette2RemoveR4Space1,
        OR2Palette2RemoveR4Space2,OR2Palette2RemoveR5Space1,OR2Palette2RemoveR5Space2,OR2RemoveBuildingPalette,OMoveOutPalette1WithElevator2,
        OMoveOutPalette2WithElevator2,OBuildingPaletteComplete
      )++OListR4PickUpAt11To14++OListR4PickUpAt15To18++OListR4PickUpAt21To28++OListR5PickUpAt11To14++OListR5PickUpAt15To18++OListR5PickUpAt21To28++OListR4PlaceCubeAt11To14++OListR4PlaceCubeAt21To24++OListR4PlaceCubeAt31To34++OListR4PlaceCubeAt41To44++OListR5PlaceCubeAt11To14++OListR5PlaceCubeAt21To24++OListR5PlaceCubeAt31To34++OListR5PlaceCubeAt41To44++listOfWallSchemeOps

      replyTo ! Response(thingList ++ allOPs ++ List(OInitOperation), SPAttributes(), rnr.req.service, rnr.req.reqID)
      self ! PoisonPill
    }
  }
}

// val sopSeq = SOP(Sequence(o11, o12, o13), Sequence(o21, o22, o23), Parallel(o11,o12))
/*
val thaSop = SOP(
  Sequence(
      OR2PlaceBuildingPalette,OR2Palette1ToR4Space1,OR2Palette2ToR4Space2,
      OListR4PickUpAt11To14(1),OListR4PlaceCubeAt11To14(1),OListR4PickUpAt11To14(2),OListR4PlaceCubeAt11To14(2),OListR4PickUpAt11To14(3),
      OListR4PlaceCubeAt11To14(3),OListR4PickUpAt11To14(4),OListR4PlaceCubeAt11To14(4),OListR4PickUpAt15To18(1),OListR4PlaceCubeAt21To24(1),
      OListR4PickUpAt15To18(2),OListR4PlaceCubeAt21To24(2),OListR4PickUpAt15To18(3),OListR4PlaceCubeAt21To24(3),OListR4PickUpAt15To18(4),
      OListR4PlaceCubeAt21To24(4),OListR4PickUpAt21To28(1),OListR4PlaceCubeAt31To44(1),OListR4PickUpAt21To28(2),OListR4PlaceCubeAt31To44(2),
      OListR4PickUpAt21To28(3),OListR4PlaceCubeAt31To44(3),OListR4PickUpAt21To28(4),OListR4PlaceCubeAt31To44(4),OListR4PickUpAt21To28(5),
      OListR4PlaceCubeAt31To44(5),OListR4PickUpAt21To28(6),OListR4PlaceCubeAt31To44(6),OListR4PickUpAt21To28(7),OListR4PlaceCubeAt31To44(7),
      OListR4PickUpAt21To28(8),OListR4PlaceCubeAt31To44(8),
      OR2RemoveBuildingPalette, OR2Palette1RemoveR4Space1, OR2Palette1RemoveR4Space2
  )
//Here is were the next tower would be.
)
*/
//(operation = f), indicates finished




/*
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
//OPs for picking up cubes by R4 at spot 1
  val OR4PickUpAt11 = Operation("OR4PickUpAt11", List(PropositionCondition(parserG.parseStr("").right.get, List(aListOfPickedUpCubes(1),aBookR4,aR4HoldingCube))),SPAttributes("duration" -> 1))
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
*/
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
