package sp.virtcom.modeledCases

import sp.domain.SPAttributes
import sp.domain.Logic._
import sp.virtcom.CollectorModel

/**
 * Type based model of simple process in PSL.
 */
case class PSLFloorRoofCase() extends CollectorModel {
  //Carriers
  v(name = "vKUKA_car", domain = Seq("empty", "floor"), init = "empty", marked = "empty")
  v(name = "vABB_car", domain = Seq("empty", "roof"), init = "empty", marked = "empty")
  v(name = "vPalletFloor_car", domain = Seq("empty", "floor"), init = "empty", marked = "empty")
  v(name = "vPalletRoof_car", domain = Seq("empty", "roof"), init = "empty", marked = "empty")
  v(name = "vFixtureFloor_car", domain = Seq("empty", "floor"), init = "empty", marked = "empty")
  v(name = "vFixtureRoof_car", domain = Seq("empty", "roof"), init = "empty", marked = "empty")
  v(name = "vFixtureFloorRoof_car", domain = Seq("empty", "floorRoof"), init = "empty", marked = "empty")

  v(name = "vOutside", domain = Seq("idle","addingFloor", "addingRoof", "removingFloorRoof"), init = "idle", marked = "idle")

  //Floor
  op("addFloor", c("vPalletFloor_car", "empty", "floor"))
  op("addFloor", c("vOutside", "idle", "addingFloor", "idle"))

  op("gripFloor_KUKA", c("vPalletFloor_car", "floor", "empty"))
  op("gripFloor_KUKA", c("vKUKA_pos", "atPallet", "atPalletWorking", "atPallet"), attributes = SPAttributes("simop" -> "op34"))
  op("gripFloor_KUKA", c("vKUKA_car", "empty", "floor"))

  op("fixateFloor_KUKA", c("vKUKA_pos", "atFixture", "atFixtureWorking", "atFixture"))
  op("fixateFloor_KUKA", c("vKUKA_car", "floor", "empty"))
  op("fixateFloor_KUKA", c("vFixtureFloor_car", "empty", "floor"))
  v(name = "vKUKA_pos", domain = Seq("atPallet", "atPalletWorking", "atFixture", "atFixtureWorking"))
  x("NoFloorOnAFloorRoof", "vFixtureFloor_car==floor & vFixtureFloorRoof_car==floorRoof")

  //Roof
  op("addRoof", c("vPalletRoof_car", "empty", "roof"))
  op("addRoof", c("vOutside", "idle", "addingRoof", "idle"))

  op("gripRoof_ABB", c("vPalletRoof_car", "roof", "empty"))
  op("gripRoof_ABB", c("vABB_pos", "atPallet", "atPalletWorking", "atPallet"))
  op("gripRoof_ABB", c("vABB_car", "empty", "roof"))

  op("fixateRoof_ABB", c("vABB_pos", "atFixture", "atFixtureWorking", "atFixture"))
  op("fixateRoof_ABB", c("vABB_car", "roof", "empty"))
  op("fixateRoof_ABB", c("vFixtureRoof_car", "empty", "roof"))
  v(name = "vABB_pos", domain = Seq("atPallet", "atPalletWorking", "atFixture", "atFixtureWorking"))
  x("ARoofMustBeOnAFloor", "vFixtureRoof_car==roof & vFixtureFloor_car==empty")
  x("NoRoofOnAFloorRoof", "vFixtureRoof_car==roof & vFixtureFloorRoof_car==floorRoof")

  //FloorRoof
  op("weldFloorRoof", c("vFixtureFloor_car", "floor", "empty"))
  op("weldFloorRoof", c("vFixtureRoof_car", "roof", "empty"))
  op("weldFloorRoof", c("vFixtureFloorRoof_car", "empty", "floorRoof"))
  v(name = "vWelder", domain = Seq("idle", "welding"), init = "idle", marked = "idle")
  op("weldFloorRoof", c("vWelder", "idle", "welding", "idle"))

  op("removeFloorRoof", c("vFixtureFloorRoof_car", "floorRoof", "empty"))
  op("removeFloorRoof", c("vOutside", "idle", "removingFloorRoof", "idle"))

  //Robot movements
  val staticRobotPoses = Map("atInit" -> Set("atPallet"),
    "atPallet" -> Set("atFixture"),
    "atFixture" -> Set("atInit"))

  //ABB poses
  v("vABB_pos", init = s"atInit", marked = s"atInit")
  createMoveOperations(robotName = "ABB", staticRobotPoses = staticRobotPoses)

  //KUKA poses
  v("vKUKA_pos", init = s"atInit", marked = s"atInit")
  createMoveOperations(robotName = "KUKA", staticRobotPoses = staticRobotPoses)

  x("PalletZone", "vABB_pos==atPalletWorking & vKUKA_pos==atPalletWorking")
  x("FixtureZone", "vABB_pos==atFixtureWorking & vKUKA_pos==atFixtureWorking")
  x("FixtureZone", "vABB_pos==atFixtureWorking & vWelder==welding")
  x("FixtureZone", "vWelder==welding & vKUKA_pos==atFixtureWorking")
}
