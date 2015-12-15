package sp.virtcom.modeledCases

import sp.domain.Logic._
import sp.domain.SPAttributes
import sp.virtcom.CollectorModel

/**
 * GKN weld station
 */
case class GKNSmallcase(modelName: String = "GKN - weld station") extends CollectorModel {

  def hAtt(h: String) = SPAttributes("hierarchy" -> Set(h))

  def psAtt() = SPAttributes("simop" -> "X","starting_signal"->"X","ending_signal"->"X","autostart"->false)

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

  //Grip/place product at loadStn
  val loadStations = Set(1, 2)
  loadStations.foreach { nr =>
    rop(s"PickFixture$nr", SPAttributes(aCarrierTrans(s"vRobot_car", atStart = "tool", atComplete = "tool_fixture")) merge
      SPAttributes(aResourceTrans("vRobot_pos", "in", s"pickingAtLoadStn$nr", "in"))
    )
    rop(s"ParkFixture$nr", SPAttributes(aCarrierTrans(s"vRobot_car", atStart = "tool_fixture_welded", atComplete = "tool")) merge
      SPAttributes(aResourceTrans("vRobot_pos", "in", s"parkingAtLoadStn$nr", "in"))
    )
  }

  //RFID
  rop("CheckProductVariant", SPAttributes(aCarrierTrans(s"vRobot_car", atStart = "tool_fixture", atComplete = "tool_fixture_rfid")) merge
    SPAttributes(aResourceTrans("vRobot_pos", "in", "readingAtRfidStand", "in"))
  )

  //Load in chamber
  rop(s"FixtureToChamber", SPAttributes(aCarrierTrans("vChamber_car", atStart = "empty", atComplete = "weld0")) merge
    SPAttributes(aCarrierTrans(s"vRobot_car", atStart = "tool_fixture_rfid", atComplete = "tool")) merge
    SPAttributes(aResourceTrans("vRobot_pos", "in", s"fixtureToChamber", "weldGunStand"))
  )
  x("chamberZone", s"vRobot_pos==fixtureToChamber && vChamber_status!=opened", attributes = hAtt("GKN"))

  //Unload in chamber
  rop("FixtureFromChamber", SPAttributes(aCarrierTrans("vChamber_car", atStart = s"weld1", atComplete = "empty")) merge
    SPAttributes(aCarrierTrans(s"vRobot_car", atStart = s"tool", atComplete = s"tool_fixture_welded")) merge
    SPAttributes(aResourceTrans("vRobot_pos", "weldGunStand", s"fixtureFromChamber", "in"))
  )
  x("chamberZone", s"vRobot_pos==fixtureFromChamber && vChamber_status!=opened", attributes = hAtt("GKN"))

  //Weld in chamber
  rop(s"Weld", SPAttributes(aCarrierTrans("vChamber_car", atStart = s"weld0", atComplete = s"weld1")) merge
    SPAttributes(aCarrierTrans("vRobot_car", atStart = s"tool_cut", atComplete = s"tool")) merge
    SPAttributes(aResourceTrans("vRobot_pos", "weldGunStand", s"welding", "weldGunStand"))
  )

  x("chamberZone", s"vRobot_pos==welding && vChamber_status!=closed", attributes = hAtt("GKN"))
}
