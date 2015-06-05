package sp.virtcom

import sp.domain.SPAttributes
import sp.domain.Logic._

/**
 * Type based model of simple process in PSL.
 */
case class PSLFloorRoofCase() extends CollectorModel {
  //Floor
  v(name = "vKUKA_car", domain = Seq("empty", "floor"), init = "empty", marked = "empty")
  v(name = "vFixture_car", domain = Seq("empty", "floor", "floorRoof"), init = "empty", marked = "floorRoof")
  op("gripFloor_KUKA", c("vKUKA_pos", "atPallet", "atPalletWorking", "atPallet"), attributes = SPAttributes("simop" -> "op34"))
  op("gripFloor_KUKA", c("vKUKA_car", "empty", "floor"))
  op("fixateFloor_KUKA", c("vKUKA_pos", "atFixture", "atFixtureWorking", "atFixture"))
  op("fixateFloor_KUKA", c("vKUKA_car", "floor", "empty"))
  op("fixateFloor_KUKA", c("vFixture_car", "empty", "floor"))
  v(name = "vKUKA_pos", domain = Seq("atPallet", "atPalletWorking", "atFixture", "atFixtureWorking"))

  //Roof
  v(name = "vABB_car", domain = Seq("empty", "roof"), init = "empty", marked = "empty")
  op("gripRoof_ABB", c("vABB_pos", "atPallet", "atPalletWorking", "atPallet"))
  op("gripRoof_ABB", c("vABB_car", "empty", "roof"))
  op("fixateRoof_ABB", c("vABB_pos", "atFixture", "atFixtureWorking", "atFixture"))
  op("fixateRoof_ABB", c("vABB_car", "roof", "empty"))
  op("fixateRoof_ABB", c("vFixture_car", "floor", "floorRoof"))
  v(name = "vABB_pos", domain = Seq("atPallet", "atPalletWorking", "atFixture", "atFixtureWorking"))

  //Robot movements
  val staticRobotPos = Map("init" -> Set("pallet"),
    "pallet" -> Set("fixture"),
    "fixture" -> Set("pallet", "init"))

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
}
