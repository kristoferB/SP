package sp.virtcom

import sp.domain.SPAttributes
import sp.domain.Logic._

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
  val staticRobotPos = Map("init" -> Set("pallet"),
    "pallet" -> Set("fixture"),
    "fixture" -> Set("init"))

  def createMoveOperations(robot: String) = {
    staticRobotPos.foreach {
      case (source, targets) =>
        targets.foreach { target =>
          val inBetweenValue = s"${source}To${target.capitalize}"
          val robot_pos = s"v${robot}_pos"
          op(s"${inBetweenValue}_$robot", c(robot_pos, s"at${source.capitalize}", inBetweenValue, s"at${target.capitalize}"))
          v(robot_pos, domain = Seq(s"at${source.capitalize}", inBetweenValue, s"at${target.capitalize}"))
        }

    }
  }

  //ABB poses
  v("vABB_pos", init = s"atInit", marked = s"atInit")
  createMoveOperations("ABB")

  //KUKA poses
  v("vKUKA_pos", init = s"atInit", marked = s"atInit")
  createMoveOperations("KUKA")

  x("PalletZone", "vABB_pos==atPalletWorking & vKUKA_pos==atPalletWorking")
  x("FixtureZone", "vABB_pos==atFixtureWorking & vKUKA_pos==atFixtureWorking")
  x("FixtureZone", "vABB_pos==atFixtureWorking & vWelder==welding")
  x("FixtureZone", "vWelder==welding & vKUKA_pos==atFixtureWorking")
}
