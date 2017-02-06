package sp.calculator

import akka.actor._
import sp.domain.logic.IDAbleLogic
import scala.concurrent._
import sp.system.messages._
import sp.system._
import sp.domain._
import sp.domain.Logic._


object Calculator extends SPService {
  val specification = SPAttributes(
    "service" -> SPAttributes(
      "group"-> "calculations",
      "description" -> "A simple calculation service"
    ),
    "a"-> KeyDefinition("Int", List(), Some(0)),
    "b"-> KeyDefinition("Int", List(), Some(0)),
    "sign"-> KeyDefinition("String", List("+", "-"), Some("+"))
  )

  val transformTuple  = (
    TransformValue("a", _.getAs[Int]("a")),
    TransformValue("b", _.getAs[Int]("b")),
    TransformValue("sign", _.getAs[String]("sign"))
  )
  val transformation = transformToList(transformTuple.productIterator.toList)
  def props = ServiceLauncher.props(Props(classOf[Calculator]))
}


class Calculator extends Actor with ServiceSupport {
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
      //Status of buildpalette 1
      val buildPalette1Empty = Thing("buildPalette1Empty")
      //Status of buildpalette 2
      val buildPalette2Empty = Thing("buildPalette2Empty")
      //Status of buildningpalette
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
      } yield{
        Thing(s"$palette$f$At$r$Space$f")
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
        BuildPaletteIn,useTwoPalettes) ++ listOfPickedUpCubes ++ listOfPutDownCubes ++ listOfStatusBuildingPalettes

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
      //Guards for Operation Completion
      val gR2OPComplete = parserG.parseStr("R2OPComplete == true").right.get
      val gR4OPComplete = parserG.parseStr("R4OPComplete == true").right.get
      val gR5OPComplete = parserG.parseStr("R5OPComplete == true").right.get
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
        parserG.parseStr(s"$g$r$opNamePickedUpCubes$f$p == true").right.get
      }
      //Guard which tells if cube is placeed at buildingspot
      val gListOfPutDownCubes = for {
        f <- 1 to row
        p <- 1 to col
      } yield {
        parserG.parseStr(s"$g$opNamePutDownCubes$f$p == true").right.get
      }
      //Guards för buildingPalettes
      val gListOfStatusBuildingPalettes = for {
        f <- 1 to fixtNo
        r <- rs
        t <- 1 to 2
      } yield{
        parserG.parseStr(s"$g$palette$f$At$r$Space$t == true").right.get
      }
      val gCubesToBePlaced = "gCubesToBePlaced"
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
        parserA.parseStr(s"$a$r$opNamePickedUpCubes$f$p = true").right.get
      }
      val aListOfPickedUpCubesFalse = for {
        r <- rs
        f <- 1 to fixtNo
        p <- 1 to posFix
      } yield {
        parserA.parseStr(s"$a$r$opNamePickedUpCubes$f$p = false").right.get
      }
      //Action which tells if cube is placeed at buildingspot
      val aListOfPutDownCubesTrue = for {
        f <- 1 to row
        p <- 1 to col
      } yield {
        parserA.parseStr(s"$a$opNamePutDownCubes$f$p = true").right.get
      }
      val aListOfPutDownCubesFalse = for {
        f <- 1 to row
        p <- 1 to col
      } yield {
        parserA.parseStr(s"$a$opNamePutDownCubes$f$p = false").right.get
      }
      //Actionss för buildingPalettes
      val aChangeStatusBuildingPalettesTrue = for {
        f <- 1 to fixtNo
        r <- rs
        t <- 1 to 2
      } yield{
        parserA.parseStr(s"$a$palette$f$At$r$Space$t = true").right.get
      }
      //Actionss för buildingPalettes
      val aChangeStatusBuildingPalettesFalse = for {
        f <- 1 to fixtNo
        r <- rs
        t <- 1 to 2
      } yield{
        parserA.parseStr(s"$a$palette$f$At$r$Space$t = false").right.get
      }
      val aCubesToBePlaced = "aCubesToBePlaced"
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
      val OInitOperation = Operation("initOperation",List(PropositionCondition(AND(List(gInit)),List(aInitDone,aBuildPaletteOut,aBuildingPalette1Out,
        aUnBookR2,aUnBookR4,aUnBookR5,aBuildPalette1Empty,aBuildPalette2Empty,aH1UpWithBuildPalette1False,aH1UpWithBuildPalette2False,
        aH2OutWithBuildPalette1False,aH2OutWithBuildPalette2False,aBuildSpotUnBook,aNewBuildingPaletteComplete,aR4NotHoldingCube,
        aR5NotHoldingCube
      )++aListOfPutDownCubesFalse++aListOfPickedUpCubesFalse++aListOfCubesToPlacedFalse++aChangeStatusBuildingPalettesFalse)))
      //operator Ops
      val OMoveInBuildingPalette1 = Operation("OMoveInBuildingPalette1", List(PropositionCondition(parserG.parseStr("!aBuildingPalette1In").right.get,List(aBuildingPalette1In))))
      val OMoveInBuildingPalette2 = Operation("OMoveInBuildingPalette1", List(PropositionCondition(parserG.parseStr("!aBuildingPalette2In && gUseTwoPalettes").right.get,List(aBuildingPalette2In))))
      val OMoveInBuildPalette = Operation("OMoveInBuildPalette", List(PropositionCondition(parserG.parseStr("!aBuildPaletteIn").right.get,List(aBuildPaletteIn))))
      //Elevator 1 Operations
      val OMoveUpPalette1WithElevator1 = Operation("OMoveUpPalette1WithElevator1",List(PropositionCondition(AND(List(gBuildingPalette1In)),List(aH1UpWithBuildPalette1True,aBuildingPalette1Out))))
      val OMoveDownPalette1WithElevator1 = Operation("OMoveDownPalette1WithElevator1",List(PropositionCondition(AND(List(gH1UpWithBuildPalette1,OR(List(gListOfStatusBuildingPalettes(1),gListOfStatusBuildingPalettes(2),gListOfStatusBuildingPalettes(3),gListOfStatusBuildingPalettes(4))))),List(aH1UpWithBuildPalette1False))))
      val OMoveUpPalette2WithElevator1 = Operation("OMoveUpPalette2WithElevator1",List(PropositionCondition(AND(List(gBuildingPalette2In)),List(aH1UpWithBuildPalette2True,aBuildingPalette2Out))))
      val OMoveDownPalette2WithElevator1 = Operation("OMoveDownPalette2WithElevator1",List(PropositionCondition(AND(List(gH1UpWithBuildPalette2,OR(List(gListOfStatusBuildingPalettes(5),gListOfStatusBuildingPalettes(6),gListOfStatusBuildingPalettes(7),gListOfStatusBuildingPalettes(8))))),List(aH1UpWithBuildPalette2False))))
      //R2 Operations
      val OR2Palette1ToR4Space1 = Operation("OR2Palette1ToR4Space1", List(PropositionCondition(parserG.parseStr("!gListOfStatusBuildingPalettes(1) && !gR2Booked && !gR4Booked && gH1UpWithBuildPalette1").right.get, List(aBookR2,aChangeStatusBuildingPalettesTrue(1)))))
      // (Pos1 clear) Action R2 Booked = True
      val OR2Palette1ToR4Space2 = Operation("OR2Palette1ToR4Space2", List(PropositionCondition(parserG.parseStr("!gListOfStatusBuildingPalettes(2) && gListOfStatusBuildingPalettes(1) && !gR2Booked && !gR4Booked && gH1UpWithBuildPalette1").right.get, List(aBookR2,aChangeStatusBuildingPalettesTrue(2)))))
      // (Pos2 clear AND POS1 filled)
      val OR2Palette1ToR5Space1 = Operation("OR2Palette1ToR5Space1", List(PropositionCondition(parserG.parseStr("!gListOfStatusBuildingPalettes(3) && !gR2Booked && !gR4Booked && !gR5Booked && gH1UpWithBuildPalette1").right.get, List(aBookR2,aChangeStatusBuildingPalettesTrue(3)))))
      // (Pos1 clear)
      val OR2Palette1ToR5Space2 = Operation("OR2Palette1ToR5Space2", List(PropositionCondition(parserG.parseStr("!gListOfStatusBuildingPalettes(4) && gListOfStatusBuildingPalettes(3) && !gR2Booked && !gR4Booked && !gR5Booked && gH1UpWithBuildPalette1").right.get, List(aBookR2,aChangeStatusBuildingPalettesTrue(4)))))
      // (Pos2 clear AND POS1 filled)
      val OR2Palette2ToR4Space1 = Operation("OR2Palette2ToR4Space1", List(PropositionCondition(parserG.parseStr("!gListOfStatusBuildingPalettes(5) && !gR2Booked && !gR4Booked && gH1UpWithBuildPalette2").right.get, List(aBookR2,aChangeStatusBuildingPalettesTrue(5)))))
      // (Pos1 clear) Action R2 Booked = True
      val OR2Palette2ToR4Space2 = Operation("OR2Palette2ToR4Space2", List(PropositionCondition(parserG.parseStr("!gListOfStatusBuildingPalettes(6) && gListOfStatusBuildingPalettes(5) && !gR2Booked && !gR4Booked && gH1UpWithBuildPalette2").right.get, List(aBookR2,aChangeStatusBuildingPalettesTrue(6)))))
      // (Pos2 clear AND POS1 filled)
      val OR2Palette2ToR5Space1 = Operation("OR2Palette2ToR5Space1", List(PropositionCondition(parserG.parseStr("!gListOfStatusBuildingPalettes(7) && !gR2Booked && !gR4Booked && !gR5Booked && gH1UpWithBuildPalette2").right.get, List(aBookR2,aChangeStatusBuildingPalettesTrue(7)))))
      // (Pos1 clear)
      val OR2Palette2ToR5Space2 = Operation("OR2Palette2ToR5Space2", List(PropositionCondition(parserG.parseStr("!gListOfStatusBuildingPalettes(8) && gListOfStatusBuildingPalettes(7) && !gR2Booked && !gR4Booked && !gR5Booked && gH1UpWithBuildPalette2").right.get, List(aBookR2,aChangeStatusBuildingPalettesTrue(8)))))
      // (Pos2 clear AND POS1 filled)
      val OR2PlaceBuildingPalette = Operation("OR2PlaceBuildingPalette", List(PropositionCondition(parserG.parseStr("gBuildPaletteIn && !gR2Booked && !gR4Booked && !gR5Booked && !gBuildSpotBooked").right.get, List(aBookR2,aBuildSpotBook))))
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
        e <- 1 to 16
      } yield{
        Operation (s"$OWallSchemeOps$e",List(PropositionCondition(AND(List(AlwaysTrue)),List(aListOfCubesToPlacedTrue(e)))))
      }
      //OPs for picking up cubes by R4 at space 1, row 1
      val OR4PickUpAt1 = "OR4PickUpAt"
      val OListR4PickUpAt11To14 = for {
        e <- 1 to 4
      } yield {
        Operation(s"$OR4PickUpAt1$e",List(PropositionCondition(AND(List(NOT(gR2Booked),NOT(gR5Booked),NOT(gR4Booked),OR(List(gListOfStatusBuildingPalettes(1),gListOfStatusBuildingPalettes(5))),gListOfCubesToPlaced(e))), List(aBookR4,aR4HoldingCube,aListOfPickedUpCubesTrue(e)))),SPAttributes("duration" -> 5))
      }
      //OPs for picking up cubes by R4 at space 1, row 2
      val OListR4PickUpAt15To18 = for {
        e <- 5 to 8
      } yield {
        Operation(s"$OR4PickUpAt1$e",List(PropositionCondition(AND(List(NOT(gR2Booked),NOT(gR5Booked),NOT(gR4Booked),OR(List(gListOfStatusBuildingPalettes(1),gListOfStatusBuildingPalettes(5))),gListOfCubesToPlaced(e),gListOfPickedUpCubes(e-4))), List(aBookR4,aR4HoldingCube,aListOfPickedUpCubesTrue(e)))),SPAttributes("duration" -> 5))
      }
      //OPs for picking up cubes by R4 at space 2, row 3 and 4
      val OListR4PickUpAt21To28 = for {
        e <- 9 to 16
      } yield {
        Operation(s"$OR4PickUpAt1$e",List(PropositionCondition(AND(List(NOT(gR2Booked),NOT(gR5Booked),NOT(gR4Booked),OR(List(gListOfStatusBuildingPalettes(2),gListOfStatusBuildingPalettes(6))),gListOfCubesToPlaced(e),gListOfPickedUpCubes(e-4))), List(aBookR4,aR4HoldingCube,aListOfPickedUpCubesTrue(e)))),SPAttributes("duration" -> 5))
      }
      //OPs for picking up cubes by R5 at space 1, row 1
      val OR5PickUpAt1 = "OR5PickUpAt"
      val OListR5PickUpAt11To14 = for {
        e <- 17 to 20
      } yield {
        Operation(s"$OR5PickUpAt1$e",List(PropositionCondition(AND(List(NOT(gR2Booked),NOT(gR5Booked),NOT(gR4Booked),OR(List(gListOfStatusBuildingPalettes(3),gListOfStatusBuildingPalettes(7))),gListOfCubesToPlaced(e))), List(aBookR5,aR4HoldingCube,aListOfPickedUpCubesTrue(e)))),SPAttributes("duration" -> 5))
      }
      //OPs for picking up cubes by R5 t space 1, row 2
      val OListR5PickUpAt15To18 = for {
        e <- 21 to 24
      } yield {
        Operation(s"$OR4PickUpAt1$e",List(PropositionCondition(AND(List(NOT(gR2Booked),NOT(gR5Booked),NOT(gR4Booked),OR(List(gListOfStatusBuildingPalettes(3),gListOfStatusBuildingPalettes(7))),gListOfCubesToPlaced(e),gListOfPickedUpCubes(e-4))), List(aBookR5,aR4HoldingCube,aListOfPickedUpCubesTrue(e)))),SPAttributes("duration" -> 5))
      }
      //OPs for picking up cubes by R5 t space 2, row 3 and 4
      val OR5PickUpAt2 = "OR5PickUpAt"
      val OListR5PickUpAt25To32 = for {
        e <- 25 to 32
      } yield {
        Operation(s"$OR5PickUpAt2$e",List(PropositionCondition(AND(List(NOT(gR2Booked),NOT(gR5Booked),NOT(gR4Booked),OR(List(gListOfStatusBuildingPalettes(4),gListOfStatusBuildingPalettes(8))),gListOfCubesToPlaced(e),gListOfPickedUpCubes(e-4))), List(aBookR5,aR4HoldingCube,aListOfPickedUpCubesTrue(e)))),SPAttributes("duration" -> 5))
      }

      //OPs for placing cubes with R4 11 - 14
      val OR4PlaceCubeAt = "OR4PlaceCubeAt"
      val OListR4PlaceCubeAt11To14 = for {
        e <- 1 to 4
      } yield {
        Operation(s"$OR4PlaceCubeAt$e",List(PropositionCondition(AND(List(NOT(gR4Booked),gR4HoldingCube,gListOfPickedUpCubes(e),NOT(gListOfPutDownCubes(e)))), List(aListOfPutDownCubesTrue(e),aR4NotHoldingCube))),SPAttributes("duration" -> 5))
      }
      //OPs for placing cubes with R4 21 - 24
      val OListR4PlaceCubeAt21To24 = for {
        e <- 5 to 8
      } yield {
        Operation(s"$OR4PlaceCubeAt$e",List(PropositionCondition(AND(List(NOT(gR4Booked),gR4HoldingCube,gListOfPickedUpCubes(e),NOT(gListOfPutDownCubes(e)))), List(aListOfPutDownCubesTrue(e),aR4NotHoldingCube))),SPAttributes("duration" -> 5))
      }
      //OPs for placing cubes with R4 31 - 44
      val OListR4PlaceCubeAt31To44 = for {
        e <- 9 to 16
      } yield {
        Operation(s"$OR4PlaceCubeAt$e",List(PropositionCondition(AND(List(NOT(gR4Booked),gR4HoldingCube,gListOfPickedUpCubes(e),NOT(gListOfPutDownCubes(e)))), List(aListOfPutDownCubesTrue(e),aR4NotHoldingCube))),SPAttributes("duration" -> 5))
      }

      //OPs for placing cubes with R5 11 - 14
      val OR5PlaceCubeAt = "OR5PlaceCubeAt"
      val OListR5PlaceCubeAt31To14 = for {
        e <- 1 to 4
      } yield {
        Operation(s"$OR5PlaceCubeAt$e",List(PropositionCondition(AND(List(NOT(gR5Booked),gR5HoldingCube,gListOfPickedUpCubes(e+16),NOT(gListOfPutDownCubes(e)))), List(aListOfPutDownCubesTrue(e),aR5NotHoldingCube))),SPAttributes("duration" -> 5))
      }
      //OPs for placing cubes with R5 21 - 24
      val OListR5PlaceCubeAt14To24 = for {
        e <- 5 to 8
      } yield {
        Operation(s"$OR5PlaceCubeAt$e",List(PropositionCondition(AND(List(NOT(gR5Booked),gR5HoldingCube,gListOfPickedUpCubes(e+16),NOT(gListOfPutDownCubes(e)))), List(aListOfPutDownCubesTrue(e),aR5NotHoldingCube))),SPAttributes("duration" -> 5))
      }
      //OPs for placing cubes with R5 31 - 44
      val OListR5PlaceCubeAt31To44 = for {
        e <- 9 to 16
      } yield {
        Operation(s"$OR5PlaceCubeAt$e",List(PropositionCondition(AND(List(NOT(gR5Booked),gR5HoldingCube,gListOfPickedUpCubes(e+16),NOT(gListOfPutDownCubes(e)))), List(aListOfPutDownCubesTrue(e),aR5NotHoldingCube))),SPAttributes("duration" -> 5))
      }

      //Operation which tells when towers is comeplete
      // use XNOR for all 16 pos
      //  val OBuildingPaletteComplete = Operation("OBuildingPaletteComplete",PropositionCondition(AND(List(OR(List(gListOfPutDownCubes(1),gListOfCubesToPlaced(1))),OR(List(gListOfPutDownCubes(2),gListOfCubesToPlaced(2))))),List(aBuildingPaletteIsComplete,aBuildPalette1Empty,aBuildPalette2Empty)))
      // inrcement finns i propsistion condition och göra tester

      //Elevator 2 Operations
      val OMoveUpPalette1WithElevator2 = Operation("OMoveUpPalette1WithElevator1",List(PropositionCondition(AND(List(gBuildPalette1Empty)),List(aH2OutWithBuildPalette1True))))
      val OMoveUpPalette2WithElevator2 = Operation("OMoveUpPalette2WithElevator1",List(PropositionCondition(AND(List(gBuildPalette2Empty)),List(aH2OutWithBuildPalette2True))))

      //Remove building palettes ops
      val OR2Palette1RemoveR4Space1 = Operation("OR2Palette1RemoveR4Space1", List(PropositionCondition(parserG.parseStr("!gR2Booked && gListOfStatusBuildingPalettes(1) && gH2OutWithBuildPalette1 && gR4Dodge && !gBuildSpotBooked").right.get, List(aBookR2,aChangeStatusBuildingPalettesFalse(1)))))
      // Operation R4BuildFromPos1 Done
      val OR2Palette1RemoveR4Space2 = Operation("OR2Palette1RemoveR4Space2", List(PropositionCondition(parserG.parseStr("!gR2Booked && gListOfStatusBuildingPalettes(2) && gH2OutWithBuildPalette1 && gR4Dodge && !gBuildSpotBooked").right.get, List(aBookR2,aChangeStatusBuildingPalettesFalse(2)))))
      // Operation R4BuildFromPos2 Done
      val OR2Palette1RemoveR5Space1 = Operation("OR2Palette1RemoveR5Space1", List(PropositionCondition(parserG.parseStr("!gR2Booked && gListOfStatusBuildingPalettes(3) && gH2OutWithBuildPalette1 && gR4Dodge && gR5Dodge && !gBuildSpotBooked").right.get, List(aBookR2,aChangeStatusBuildingPalettesFalse(3)))))
      // Operation R5BuildFromPos1 Done
      val OR2Palette1RemoveR5Space2 = Operation("OR2Palette1RemoveR5Space2", List(PropositionCondition(parserG.parseStr("!gR2Booked && gListOfStatusBuildingPalettes(4) && gH2OutWithBuildPalette1 && gR4Dodge && gR5Dodge && !gBuildSpotBooked").right.get, List(aBookR2,aChangeStatusBuildingPalettesFalse(4)))))
      // Operation R5BuildFromPos2 Done
      val OR2Palette2RemoveR4Space1 = Operation("OR2Palette2RemoveR4Space1", List(PropositionCondition(parserG.parseStr("!gR2Booked && gListOfStatusBuildingPalettes(5) && gH2OutWithBuildPalette2 && gR4Dodge && !gBuildSpotBooked").right.get, List(aBookR2,aChangeStatusBuildingPalettesFalse(5)))))
      // Operation R4BuildFromPos1 Done
      val OR2Palette2RemoveR4Space2 = Operation("OR2Palette2RemoveR4Space2", List(PropositionCondition(parserG.parseStr("!gR2Booked && gListOfStatusBuildingPalettes(6) && gH2OutWithBuildPalette2 && gR4Dodge && !gBuildSpotBooked").right.get, List(aBookR2,aChangeStatusBuildingPalettesFalse(6)))))
      // Operation R4BuialdFromPos2 Done
      val OR2Palette2RemoveR5Space1 = Operation("OR2Palette2RemoveR5Space1", List(PropositionCondition(parserG.parseStr("!gR2Booked && gListOfStatusBuildingPalettes(7) && gH2OutWithBuildPalette2 && gR4Dodge && gR5Dodge && !gBuildSpotBooked").right.get, List(aBookR2,aChangeStatusBuildingPalettesFalse(7)))))
      // Operation R5BuildFromPos1 Done
      val OR2Palette2RemoveR5Space2 = Operation("OR2Palette2RemoveR5Space2", List(PropositionCondition(parserG.parseStr("!gR2Booked && gListOfStatusBuildingPalettes(8) && gH2OutWithBuildPalette2 && gR4Dodge && gR5Dodge && !gBuildSpotBooked").right.get, List(aBookR2,aChangeStatusBuildingPalettesFalse(8)))))
      //Op for removing complete tower
      val OR2RemoveBuildingPalette = Operation("OR2RemoveBuildingPalette", List(PropositionCondition(AND(List(NOT(gR2Booked),NOT(gR5Booked),NOT(gR4Booked),gBuildSpotBooked,gBuildingPaletteComplete)), List(aBookR2,aBuildSpotUnBook))))

      // Ops for moving lowering H2
      val OMoveOutPalette1WithElevator2 = Operation("OMoveUpPalette1WithElevator1",List(PropositionCondition(AND(List(gH2OutWithBuildPalette1,OR(List(gListOfStatusBuildingPalettes(1),gListOfStatusBuildingPalettes(2),gListOfStatusBuildingPalettes(3),gListOfStatusBuildingPalettes(4))))),List(aH2OutWithBuildPalette1False))))
      val OMoveOutPalette2WithElevator2 = Operation("OMoveUpPalette2WithElevator1",List(PropositionCondition(AND(List(gH2OutWithBuildPalette2,OR(List(gListOfStatusBuildingPalettes(5),gListOfStatusBuildingPalettes(6),gListOfStatusBuildingPalettes(7),gListOfStatusBuildingPalettes(8))))),List(aH2OutWithBuildPalette2False))))
      //LIST With all OPS

      val allOPs: List[Operation] = List(OMoveInBuildingPalette1,OMoveInBuildingPalette2,OMoveInBuildPalette,OR2Palette1ToR4Space1,
        OR2Palette1ToR4Space2,OR2Palette1ToR5Space1,OR2Palette1ToR5Space2,OR2Palette2ToR4Space1,OR2Palette2ToR4Space2,OR2Palette2ToR5Space1,
        OR2Palette2ToR5Space2,OMoveUpPalette1WithElevator1,OMoveDownPalette1WithElevator1,OMoveUpPalette2WithElevator1,OMoveDownPalette2WithElevator1,
        OR2PlaceBuildingPalette,OR2RemoveBooking,OR4RemoveBooking,OR5RemoveBooking,OMoveUpPalette1WithElevator2,OMoveUpPalette2WithElevator2,
        OR2Palette1RemoveR4Space1,OR2Palette1RemoveR4Space2,OR2Palette1RemoveR5Space1,OR2Palette1RemoveR5Space2,OR2Palette2RemoveR4Space1,
        OR2Palette2RemoveR4Space2,OR2Palette2RemoveR5Space1,OR2Palette2RemoveR5Space2,OR2RemoveBuildingPalette,OMoveOutPalette1WithElevator2,
        OMoveOutPalette2WithElevator2
      )++OListR4PickUpAt11To14++OListR4PickUpAt15To18++OListR4PickUpAt21To28++OListR5PickUpAt11To14++OListR5PickUpAt15To18++OListR5PickUpAt25To32++OListR4PlaceCubeAt11To14++OListR4PlaceCubeAt21To24++OListR4PlaceCubeAt31To44++OListR5PlaceCubeAt31To14++OListR5PlaceCubeAt14To24++OListR5PlaceCubeAt31To44++listOfWallSchemeOps


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


      replyTo ! Response(thingList ++ allOPs ++ List(OInitOperation), SPAttributes(), rnr.req.service, rnr.req.reqID)
      self ! PoisonPill
    }
  }


}




