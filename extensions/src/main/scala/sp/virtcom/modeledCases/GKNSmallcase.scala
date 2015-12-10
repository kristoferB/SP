package sp.virtcom.modeledCases

import sp.domain.Logic._
import sp.domain.SPAttributes
import sp.virtcom.CollectorModel

/**
 * GKN weld station
 */
case class GKNSmallcase(modelName: String = "GKN - weld station") extends CollectorModel {

  def hAtt(h: String) = SPAttributes("hierarchy" -> Set(h))

  //Loadstations------------------------------------------------------------------------------------------
  val loadStations = Set(1, 2)
  loadStations.foreach(nr => v(name = s"vLoadStn$nr", attributes = hAtt("LoadStn")))

  //Welds-------------------------------------------------------------------------------------------------
  val nbrOfWelds = 1
  val welds = (1 to nbrOfWelds).toSeq

  //Weldgun stands-----------------------------------------------------------------------------------------
  val weldGunStands = Set(1)

  //Chamber-----------------------------------------------------------------------------------------------
  v(name = "vChamber_car", attributes = hAtt("Chamber"))
  v(name = "vChamber_status", idleValue = "closed", attributes = hAtt("Chamber"))
  op("openChamber", SPAttributes(aCarrierTrans("vChamber_status", atStart = "closed", atComplete = "opened")) merge hAtt("Chamber"))
  op("closeChamber", SPAttributes(aCarrierTrans("vChamber_status", atStart = "opened", atComplete = "closed")) merge hAtt("Chamber"))
  op("fillWithGas", SPAttributes(aResourceTrans("vChamber_gas", "idle", "filling", "idle")) merge hAtt("Chamber"))

  x("fillGas", "vChamber_gas==filling && vChamber_status!=closed", attributes = hAtt("Chamber"))
  x("fillGas", "vChamber_gas==filling && vChamber_car==empty", attributes = hAtt("Chamber"))

  welds.foreach { nr =>
    op(s"rotateToStartWeld$nr", SPAttributes(aCarrierTrans("vChamber_car", atStart = s"weld${nr - 1}", atComplete = s"weld${nr - 1}rot")) merge
      hAtt("Chamber")
    )
    //Prevent rotation when robot in chamber
  }

  //Operator-----------------------------------------------------------------------------------------------
  def oop(n: String, att: SPAttributes) = op(n, hAtt("Operator") merge att)
  v(name = "vOperator_pos", idleValue = "outside", attributes = hAtt("Operator"))
  oop("enterLoadStnZone", SPAttributes(aCarrierTrans("vOperator_pos", atStart = "outside", atComplete = "inside")))
  loadStations.foreach { nr =>
    oop(s"loadLoadStn$nr", SPAttributes(aCarrierTrans(s"vLoadStn$nr", atComplete = "blank")) merge
      SPAttributes(aResourceTrans("vOperator_pos", "inside", s"loadingAtLoadStn$nr", "inside")) merge
      hAtt("LoadStn")
    )
    oop(s"unloadLoadStn$nr", SPAttributes(aCarrierTrans(s"vLoadStn$nr", atStart = s"rfid${welds.last}")) merge
      SPAttributes(aResourceTrans("vOperator_pos", "inside", s"unloadingAtLoadStn$nr", "inside")) merge
      hAtt("LoadStn")
    )
  }
  oop("exitLoadStnZone", SPAttributes(aResourceTrans("vOperator_pos", "inside", "inToOut", "outside")))

  loadStations.foreach { nr =>
    x("loadStnZone", s"vOperator_pos!=outside && vRobot_pos==grippingAtLoadStn$nr", attributes = hAtt("LoadStn"))
    x("loadStnZone", s"vOperator_pos!=outside && vRobot_pos==placingAtLoadStn$nr", attributes = hAtt("LoadStn"))
  }

  //Robot------------------------------------------------------------------------------------------------
  //-----------------------------------------------------------------------------------------------------
  def rop(n: String, att: SPAttributes) = op(n, hAtt("Robot") merge att)
  v(name = "vRobot_pos", idleValue = "atHome", attributes = hAtt("Robot"))
  v(name = "vRobot_car", attributes = hAtt("Robot"))

  //Weldgun operations
  weldGunStands.foreach { nr =>
    rop(s"loadWeldGunFrom$nr", SPAttributes(aCarrierTrans(s"vRobot_car", atComplete = s"weldGun")) merge
      SPAttributes(aResourceTrans("vRobot_pos", "atHome", s"loadingAtWeldGunStand$nr", "atHome")) merge
      hAtt("ToolChange")
    )
    Set("", "_cut").foreach { y =>
      rop(s"unloadWeldGun${y}To$nr", SPAttributes(aCarrierTrans(s"vRobot_car", atStart = s"weldGun$y")) merge
        SPAttributes(aResourceTrans("vRobot_pos", "atHome", s"unloading${y}AtWeldGunStand$nr", "atHome")) merge
        hAtt("ToolChange")
      )
    }
  }
  rop(s"cutWeldGun", SPAttributes(aCarrierTrans(s"vRobot_car", atStart = s"weldGun", atComplete = s"weldGun_cut")) merge
    SPAttributes(aResourceTrans("vRobot_pos", "atCutter0", s"cuttingAtCutter", "atCutter1")) merge
    hAtt("Cutter")
  )
  x("cutterZone", "vRobot_pos==atCutter0 && vRobot_car!=weldGun", attributes = hAtt("Cutter"))

  //Gripper operations
  rop("loadGripper", SPAttributes(aCarrierTrans(s"vRobot_car", atComplete = "gripper")) merge
    SPAttributes(aResourceTrans("vRobot_pos", "atHome", "loadingAtGripperStand", "atHome")) merge
    hAtt("ToolChange")
  )
  rop("unloadGripper", SPAttributes(aCarrierTrans(s"vRobot_car", atStart = "gripper")) merge
    SPAttributes(aResourceTrans("vRobot_pos", "atHome", "unloadingAtGripperStand", "atHome")) merge
    hAtt("ToolChange")
  )

  //Grip/place product in loadStn
  loadStations.foreach { nr =>
    rop(s"gripFromLoadStn$nr", SPAttributes(aCarrierTrans(s"vLoadStn$nr", atStart = "blank")) merge
      SPAttributes(aCarrierTrans(s"vRobot_car", atStart = "gripper", atComplete = "blank")) merge
      SPAttributes(aResourceTrans("vRobot_pos", "atHome", s"grippingAtLoadStn$nr", "atHome")) merge
      hAtt("LoadStn")
    )
    rop(s"placeInLoadStn$nr", SPAttributes(aCarrierTrans(s"vLoadStn$nr", atComplete = s"rfid2")) merge
      SPAttributes(aCarrierTrans(s"vRobot_car", atStart = "rfid2", atComplete = "gripper")) merge
      SPAttributes(aResourceTrans("vRobot_pos", "atHome", s"placingAtLoadStn$nr", "atHome")) merge
      hAtt("LoadStn")
    )
  }

  //RFID
  rop("readRFID", SPAttributes(aCarrierTrans(s"vRobot_car", atStart = "blank", atComplete = "rfid1")) merge
    SPAttributes(aResourceTrans("vRobot_pos", "atHome", "readingAtRfidStand", "atHome"))
  )
  rop("writeRFID", SPAttributes(aCarrierTrans(s"vRobot_car", atStart = s"weld${welds.last}", atComplete = "rfid2")) merge
    SPAttributes(aResourceTrans("vRobot_pos", "atHome", "writingAtRfidStand", "atHome"))
  )

  //Load in chamber
  Set("blank", "rfid1").foreach { y =>
    rop(s"loadChamberWith_$y", SPAttributes(aCarrierTrans("vChamber_car", atComplete = "weld0")) merge
      SPAttributes(aCarrierTrans(s"vRobot_car", atStart = s"$y", atComplete = "gripper")) merge
      SPAttributes(aResourceTrans("vRobot_pos", "atHome", s"loading${y.capitalize}AtChamber", "atHome")) merge
      hAtt("Chamber")
    )
    x("chamberZone", s"vRobot_pos==loading${y.capitalize}AtChamber && vChamber_status!=opened", attributes = hAtt("Chamber") merge hAtt("Robot"))
  }
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
