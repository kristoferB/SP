package sp.virtcom.modeledCases

import sp.domain.Logic._
import sp.domain.SPAttributes
import sp.virtcom.CollectorModel

/**
 * GKN weld station
 */
case class GKNcase(modelName: String = "GKN - weld station") extends CollectorModel {

  def hAtt(h: String) = SPAttributes("hierarchy" -> Set(h))

  //Loadstations------------------------------------------------------------------------------------------
  val loadStations = Set("1", "2")
  loadStations.foreach(nr => v(name = s"vLoadStn$nr", attributes = hAtt("LoadStn")))

  //Operator-----------------------------------------------------------------------------------------------
  def oop(n: String, att: SPAttributes) = op(n, hAtt("Operator") merge att)
  v(name = "vOperator_pos", idleValue = "outside", attributes = hAtt("Operator"))
  oop("enterLoadZone", SPAttributes(aResourceTrans("vOperator_pos", "outside", "outToIn", "inside")))
  loadStations.foreach { nr =>
    oop(s"loadLoadStn$nr", SPAttributes(aCarrierTrans(s"vLoadStn$nr", atComplete = "p1_blank")) merge
      SPAttributes(aResourceTrans("vOperator_pos", "inside", s"loadingAtLoadStn$nr", "inside")) merge
      hAtt("LoadStn")
    )
    oop(s"unloadLoadStn$nr", SPAttributes(aCarrierTrans(s"vLoadStn$nr", atStart = "p1_RFID2")) merge
      SPAttributes(aResourceTrans("vOperator_pos", "inside", s"unloadingAtLoadStn$nr", "inside")) merge
      hAtt("LoadStn")
    )
  }
  oop("exitLoadZone", SPAttributes(aResourceTrans("vOperator_pos", "inside", "inToOut", "outside")))

  loadStations.foreach { nr =>
    x("loadZone", s"vOperator_pos!=outside && vRobot_pos==grippingAtLoadStn$nr", attributes = hAtt("LoadStn"))
    x("loadZone", s"vOperator_pos!=outside && vRobot_pos==placeingAtLoadStn$nr", attributes = hAtt("LoadStn"))
  }

  //Robot------------------------------------------------------------------------------------------------
  //-----------------------------------------------------------------------------------------------------
  def rop(n: String, att: SPAttributes) = op(n, hAtt("Robot") merge att)
  v(name = "vRobot_pos", idleValue = "atHome", attributes = hAtt("Robot"))
  v(name = "vRobot_car", attributes = hAtt("Robot"))

  //Tools
  val weldGuns = Set("1", "2")
  weldGuns.foreach { nr =>
    rop(s"loadWeldGun$nr", SPAttributes(aCarrierTrans(s"vRobot_car", atComplete = s"weldGun$nr")) merge
      SPAttributes(aResourceTrans("vRobot_pos", "atHome", s"loadingAtWeldGunStand$nr", "atHome")) merge
      hAtt("ToolChange")
    )
    rop(s"unloadWeldGun$nr", SPAttributes(aCarrierTrans(s"vRobot_car", atStart = s"weldGun$nr")) merge
      SPAttributes(aResourceTrans("vRobot_pos", "atHome", s"unloadingAtWeldGunStand$nr", "atHome")) merge
      hAtt("ToolChange")
    )
  }
  rop("loadGripper", SPAttributes(aCarrierTrans(s"vRobot_car", atComplete = "gripper")) merge
    SPAttributes(aResourceTrans("vRobot_pos", "atHome", "loadingAtGripperStand", "atHome")) merge
    hAtt("ToolChange")
  )
  rop("unloadGripper", SPAttributes(aCarrierTrans(s"vRobot_car", atStart = "gripper")) merge
    SPAttributes(aResourceTrans("vRobot_pos", "atHome", "unloadingAtGripperStand", "atHome")) merge
    hAtt("ToolChange")
  )

  //Grip/place
  loadStations.foreach { nr =>
    rop(s"gripFromLoadStn$nr", SPAttributes(aCarrierTrans(s"vLoadStn$nr", atStart = "p1_blank")) merge
      SPAttributes(aCarrierTrans(s"vRobot_car", atStart = "gripper", atComplete = "p1_blank")) merge
      SPAttributes(aResourceTrans("vRobot_pos", "atHome", s"grippingAtLoadStn$nr", "atHome")) merge
      hAtt("LoadStn")
    )
    rop(s"placeInLoadStn$nr", SPAttributes(aCarrierTrans(s"vLoadStn$nr", atComplete = "p1_RFID2")) merge
      SPAttributes(aCarrierTrans(s"vRobot_car", atStart = "p1_RFID2", atComplete = "gripper")) merge
      SPAttributes(aResourceTrans("vRobot_pos", "atHome", s"placeingAtLoadStn$nr", "atHome")) merge
      hAtt("LoadStn")
    )
  }

  //RFID1
  rop("readRFID", SPAttributes(aCarrierTrans(s"vRobot_car", atStart = "p1_blank", atComplete = "p1_rfid1")) merge
    SPAttributes(aResourceTrans("vRobot_pos", "atHome", "readingAtRfidReader", "atHome"))
  )

  //Load in chamber
  v(name = "vChamber_car", attributes = hAtt("Chamber"))
  Set("blank", "rfid1").foreach { x =>
    rop(s"loadChamberWith_$x", SPAttributes(aCarrierTrans("vChamber_car", atComplete = "p1_weld0")) merge
      SPAttributes(aCarrierTrans(s"vRobot_car", atStart = s"p1_$x", atComplete = "gripper")) merge
      SPAttributes(aResourceTrans("vRobot_pos", "atHome", s"loading${x.capitalize}AtChamber", "atHome")) merge
      hAtt("Chamber")
    )
  }
}
