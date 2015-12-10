package sp.virtcom.modeledCases

import sp.domain.Logic._
import sp.domain.SPAttributes
import sp.virtcom.CollectorModel

/**
 * GKN weld station
 */
case class GKNSmallcase(modelName: String = "GKN - weld station") extends CollectorModel {

  def hAtt(h: String) = SPAttributes("hierarchy" -> Set(h))

  def psAtt() = SPAttributes("simop" -> "X","starting_signal"->"X","ending_signal"->"X")

  //Loadstations------------------------------------------------------------------------------------------
  val loadStations = Set(1, 2)
  loadStations.foreach(nr => v(name = s"vLoadStn$nr", attributes = hAtt("LoadStn")))

  //Welds-------------------------------------------------------------------------------------------------
  val nbrOfWelds = 1
  val welds = (1 to nbrOfWelds).toSeq

  //Weldgun stands-----------------------------------------------------------------------------------------
  val weldGunStands = Set(1)

  //Chamber-----------------------------------------------------------------------------------------------
  v(name = "vChamber_car", attributes = hAtt("GKN"))
  v(name = "vChamber_status", idleValue = "closed", attributes = hAtt("GKN"))
  op("ChamberOpen", SPAttributes(aCarrierTrans("vChamber_status", atStart = "closed", atComplete = "opened")) merge hAtt("GKN") merge psAtt())
  op("ChamberClose", SPAttributes(aCarrierTrans("vChamber_status", atStart = "opened", atComplete = "closed")) merge hAtt("GKN") merge psAtt())

  //Robot------------------------------------------------------------------------------------------------
  //-----------------------------------------------------------------------------------------------------
  def rop(n: String, att: SPAttributes) = op(n, hAtt("GKN") merge att merge psAtt())
  v(name = "vRobot_pos", idleValue = "in", attributes = hAtt("GKN"))
  v(name = "vRobot_car", idleValue = "tool", attributes = hAtt("GKN"))

  rop(s"WireCut", SPAttributes(aCarrierTrans(s"vRobot_car", atStart = "tool", atComplete = s"tool_cut")) merge
    SPAttributes(aResourceTrans("vRobot_pos", "weldGunStand", s"cuttingAtWireCut", "weldGunStand"))
  )
  x("cutterZone", "vRobot_pos==cuttingAtWireCut && vRobot_car!=tool", attributes = hAtt("GKN"))

  //Grip/place product at loadStn
  loadStations.foreach { nr =>
    rop(s"PickFixture$nr", SPAttributes(aCarrierTrans(s"vRobot_car", atStart = "tool", atComplete = "tool_fixture")) merge
      SPAttributes(aResourceTrans("vRobot_pos", "in", s"pickingAtLoadStn$nr", "in"))
    )
    rop(s"parkFixture$nr", SPAttributes(aCarrierTrans(s"vRobot_car", atStart = "tool_fixture_welded", atComplete = "tool")) merge
      SPAttributes(aResourceTrans("vRobot_pos", "in", s"parkingAtLoadStn$nr", "in"))
    )
  }

  //RFID
  rop("CheckProductVariant", SPAttributes(aCarrierTrans(s"vRobot_car", atStart = "tool_fixture", atComplete = "tool_fixture_rfid")) merge
    SPAttributes(aResourceTrans("vRobot_pos", "in", "readingAtRfidStand", "in"))
  )

  //Load in chamber
    rop(s"FixtureToChamberWith", SPAttributes(aCarrierTrans("vChamber_car", atComplete = "weld0")) merge
      SPAttributes(aCarrierTrans(s"vRobot_car", atStart = "tool_fixture_rfid", atComplete = "tool")) merge
      SPAttributes(aResourceTrans("vRobot_pos", "in", s"loadingAtChamber", "weldGunStand"))
    )
    x("chamberZone", s"vRobot_pos==loadingAtChamber && vChamber_status!=opened", attributes = hAtt("GKN"))

  //Unload in chamber
  rop("unloadChamber", SPAttributes(aCarrierTrans("vChamber_car", atStart = s"weld${welds.last}")) merge
    SPAttributes(aCarrierTrans(s"vRobot_car", atStart = s"gripper", atComplete = s"weld${welds.last}")) merge
    SPAttributes(aResourceTrans("vRobot_pos", "atHome", s"unloadingAtChamber", "atHome")) merge
    hAtt("Chamber")
  )
  x("chamberZone", s"vRobot_pos==unloadingAtChamber && vChamber_status!=opened", attributes = hAtt("Chamber") merge hAtt("Robot"))

  //Weld in chamber
  welds.foreach { nr =>
    op(s"weld$nr", SPAttributes(aCarrierTrans("vChamber_car", atStart = s"weld${nr - 1}rot", atComplete = s"weld$nr")) merge
      SPAttributes(aCarrierTrans("vRobot_car", atStart = s"weldGun_cut", atComplete = s"weldGun")) merge
      SPAttributes(aResourceTrans("vRobot_pos", "atChamber", s"welding${nr}AtChamber", "atChamber"))
    )
    x("chamberZone", s"vRobot_pos==welding${nr}AtChamber && vChamber_status!=closed", attributes = hAtt("Chamber") merge hAtt("Robot"))
  }

  //Robot movements
  lazy val staticRobotPoses = SPAttributes(
    "atHome" -> Set(
      SPAttributes("to" -> "atChamber", "simop" -> "X") merge hAtt("Robot"),
      SPAttributes("to" -> "atCutter0", "simop" -> "X") merge hAtt("Robot")
    ),
    "atChamber" -> Set(
      SPAttributes("to" -> "atHome", "simop" -> "X") merge hAtt("Robot"),
      SPAttributes("to" -> "atCutter0", "simop" -> "X") merge hAtt("Robot")
    ),
    "atCutter1" -> Set(
      SPAttributes("to" -> "atHome", "simop" -> "X") merge hAtt("Robot"),
      SPAttributes("to" -> "atChamber", "simop" -> "X") merge hAtt("Robot")
    )
  )
  robotMovements("v", "Robot", "_pos", staticRobotPoses, hAtt("Robot"))
  x("chamberZone", s"vRobot_pos==atHomeToAtChamber && vChamber_status!=opened", attributes = hAtt("Chamber") merge hAtt("Robot"))
  x("chamberZone", s"vRobot_pos==atChamberToAtHome && vChamber_status!=opened", attributes = hAtt("Chamber") merge hAtt("Robot"))
  x("chamberZone", s"vRobot_pos==atCutter1ToAtChamber && vChamber_status!=opened", attributes = hAtt("Chamber") merge hAtt("Robot"))
  x("chamberZone", s"vRobot_pos==atChamberToAtCutter0 && vChamber_status!=opened", attributes = hAtt("Chamber") merge hAtt("Robot"))

}
