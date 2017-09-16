package sp.devicehandler

// VOLVO trucks abilities. TODO: Move to its own node s
import akka.actor._
import sp.domain._
import sp.domain.Logic._
import java.util.UUID
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{ Put, Subscribe, Publish }
import scala.util.{Failure, Success, Try}
import sp.domain.logic.{PropositionParser, ActionParser}

import sp.abilityhandler.APIAbilityHandler


object Trucks {
  def props(ahid: ID) = Props(classOf[Trucks], ahid)
}

class Trucks(ahid: ID) extends Actor with Helpers {
  import context.dispatcher
  val mediator = DistributedPubSub(context.system).mediator

  val lf1vars = List(
    // operator load product
    v("lf1_startAddProduct", "OBF_IN_Frontlid_FM 82457762_start"),

    // load fixture 1
    v("lf1_productSensor", "126,=V1UQ51+BG1_to_7"),
    v("lf1_closeClamps", "126,=V1UQ51+KH1-QN2S"),
    v("lf1_openClamps", "126,=V1UQ51+KH1-QN2R"),
    v("lf1_clampsClosed", "126,=V1UQ51+UQ2.1-BGS"),
    v("lf1_clampsOpened", "126,=V1UQ51+UQ2.1-BGR")
  )

  val lf2vars = List(
    // operator load product
    v("lf2_startAddProduct", "OBF_IN_Frontlid_FM 82416263_start"),

    // load fixture 1
    v("lf2_productSensor", "126,=V1UQ52+BG1_to_2"),
    v("lf2_closeClamps", "126,=V1UQ52+KH1-QN2S"),
    v("lf2_openClamps", "126,=V1UQ52+KH1-QN2R"),
    v("lf2_clampsClosed", "126,=V1UQ52+UQ2.1-BGS"),
    v("lf2_clampsOpened", "126,=V1UQ52+UQ2.1-BGR")
  )

  // AR31, blue robot
  val ar31vars = List(
    v("ar31_startVacuum", "AR31_I_Vacuum_1_on"),
    v("ar31_stopVacuum", "AR31_I_Vacuum_1_off"),
    v("ar31_vacuumOn", "AR31_O_Vacuum_1_on"),
    v("ar31_vacuumOff", "AR31_O_Vacuum_1_off"),
    v("ar31_picklf1_seg1_start", "P2545UQ51PickSeg1_start"),
    v("ar31_picklf1_seg1_end", "P2545UQ51PickSeg1_end"),
    v("ar31_picklf1_seg2_start", "P2545UQ51PickSeg2_start"),
    v("ar31_picklf1_seg2_end", "P2545UQ51PickSeg2_end"),
    v("ar31_picklf1_seg3_start", "P2545UQ51PickSeg3_start"),
    v("ar31_picklf1_seg3_end", "P2545UQ51PickSeg3_end"),

    v("ar31_glue42_seg1_start", "P2545XQ42Seg1_start"),
    v("ar31_glue42_seg1_end", "P2545XQ42Seg1_end"),
    v("ar31_glue42_start", "P2545GlueXQ42_start"),
    v("ar31_glue42_end", "P2545GlueXQ42_end"),
    v("ar31_glue42_check_seg1_start", "P2545CheckGlueXQ42Seg1_start"),
    v("ar31_glue42_check_seg1_end", "P2545CheckGlueXQ42Seg1_end"),
    v("ar31_glue41_seg1_start", "P2545XQ41Seg1_start"),

    v("ar31_glue41_seg1_end", "P2545XQ41Seg1_end"),
    v("ar31_glue41_start", "P2545GlueXQ41_start"),
    v("ar31_glue41_end", "P2545GlueXQ41_end"),
    v("ar31_glue41_check_seg1_start", "P2545CheckGlueXQ41Seg1_start"),
    v("ar31_glue41_check_seg1_end", "P2545CheckGlueXQ41Seg1_end"),
    v("ar31_glue41_check_seg2_start", "P2545CheckGlueXQ41Seg2_start"),
    v("ar31_glue41_check_seg2_end", "P2545CheckGlueXQ41Seg2_end"),

    v("ar31_placelf2_seg1_start", "P2545UQ52PickSeg1_start"),
    v("ar31_placelf2_seg1_end", "P2545UQ52PickSeg1_end"),
    v("ar31_picklf2_seg2_start", "P2545UQ52PickSeg2_start"),
    v("ar31_picklf2_seg2_end", "P2545UQ52PickSeg2_end"),
    v("ar31_picklf2_seg3_start", "P2545UQ52PickSeg3_start"),
    v("ar31_picklf2_seg3_end", "P2545UQ52PickSeg3_end"),
    v("ar31_picklf2_seg4_start", "P2545UQ52PickSeg4_start"),
    v("ar31_picklf2_seg4_end", "P2545UQ52PickSeg4_end"),

    v("ar31_placett_seg1_start", "P2545UQ53PutSeg1_start"),
    v("ar31_placett_seg1_end", "P2545UQ53PutSeg1_end"),
    v("ar31_placett_seg2_start", "P2545UQ53PutSeg2_start"),
    v("ar31_placett_seg2_end", "P2545UQ53PutSeg2_end"),
    v("ar31_placett_seg3_start", "P2545UQ53PutSeg3_start"),
    v("ar31_placett_seg3_end", "P2545UQ53PutSeg3_end"),
    v("ar31_goto_home_start", "P2545Home_start"),
    v("ar31_goto_home_end", "P2545Home_end")
  )

  val ar41vars = List(
    v("ar41_lockTool", "AR41_I_LOCKTOOL"),
    v("ar41_unlockTool", "AR41_I_UNLOCKTOOL"),
    v("ar41_toolLocked", "AR41_O_LOCKTOOL"),
    v("ar41_toolUnlocked", "AR41_O_UNLOCKTOOL"),

    v("ar41_home_to_stand_start", "UQ42GrippHomeToStand_start"),
    v("ar41_home_to_stand_end", "UQ42GrippHomeToStand_end"),
    v("ar41_stand_to_get_check_start", "UQ42GrippStandToGetchk_start"),
    v("ar41_stand_to_get_check_end", "UQ42GrippStandToGetchk_end"),
    v("ar41_get_check_to_home_start", "UQ42GrippGetchkToHome_start"),
    v("ar41_get_check_to_home_end", "UQ42GrippGetchkToHome_end"),
    v("ar41_stand_to_put_check_start", "UQ42GrippStandToPutchk_start"),
    v("ar41_stand_to_put_check_end", "UQ42GrippStandToPutchk_end"),
    v("ar41_put_check_to_between_start", "UQ42GrippPutchkToBetw_start"),
    v("ar41_put_check_to_between_end", "UQ42GrippPutchkToBetw_end")
  )
  val allVars = lf1vars ++ lf2vars ++ ar31vars ++ ar41vars

  def p(cond: String,actions: List[String] = List()) = prop(allVars)(cond,actions)

  val lf1abs = List(
    a("lf1_startAddProduct",
      p("!lf1_startAddProduct && !lf1_productSensor", List("lf1_startAddProduct := true")),
      p("lf1_startAddProduct && !lf1_productSensor"),
      p("lf1_productSensor", List("lf1_startAddProduct := false"))),

    a("lf1_closeClamps",
      p("lf1_clampsOpened", List("lf1_closeClamps := true")),
      p("lf1_closeClamps && !lf1_clampsClosed && !lf1_clampsOpened"),
      p("lf1_clampsClosed", List("lf1_closeClamps := false"))),

    a("lf1_openClamps",
      p("lf1_clampsClosed", List("lf1_openClamps := true")),
      p("lf1_openClamps && !lf1_clampsClosed && !lf1_clampsOpened"),
      p("lf1_clampsOpened", List("lf1_openClamps := false")))
  )

  val lf2abs = List(
    a("lf2_startAddProduct",
      p("!lf2_startAddProduct && !lf2_productSensor", List("lf2_startAddProduct := true")),
      p("lf2_startAddProduct && !lf2_productSensor"),
      p("lf2_productSensor", List("lf2_startAddProduct := false"))),

    a("lf2_closeClamps",
      p("lf2_clampsOpened", List("lf2_closeClamps := true")),
      p("lf2_closeClamps && !lf2_clampsClosed && !lf2_clampsOpened"),
      p("lf2_clampsClosed", List("lf2_closeClamps := false"))),

    a("lf2_openClamps",
      p("lf2_clampsClosed", List("lf2_openClamps := true")),
      p("lf2_openClamps && !lf2_clampsClosed && !lf2_clampsOpened"),
      p("lf2_clampsOpened", List("lf2_openClamps := false")))
  )

  val ar31abs = List(
    a("ar31_startVacuum",
      p("ar31_vacuumOff", List("ar31_startVacuum := true")),
      p("ar31_startVacuum && !ar31_vacuumOn"),
      p("ar31_vacuumOn", List("ar31_startVacuum := false"))),

    a("ar31_stopVacuum",
      p("ar31_vacuumOn", List("ar31_stopVacuum := true")),
      p("ar31_stopVacuum && !ar31_vacuumOff"),
      p("ar31_vacuumOff", List("ar31_stopVacuum := false"))),

    ss(p,"ar31_picklf1_seg1", "ar31_picklf1_seg1_start", "ar31_picklf1_seg1_end"),
    ss(p,"ar31_picklf1_seg2", "ar31_picklf1_seg2_start", "ar31_picklf1_seg2_end"),
    ss(p,"ar31_picklf1_seg3", "ar31_picklf1_seg3_start", "ar31_picklf1_seg3_end"),

    ss(p,"ar31_glue42_seg1", "ar31_glue42_seg1_start", "ar31_glue42_seg1_end"),
    ss(p,"ar31_glue42", "ar31_glue42_start", "ar31_glue42_end"),
    ss(p,"ar31_glue42_check_seg1", "ar31_glue42_check_seg1_start", "ar31_glue42_check_seg1_end"),
    ss(p,"ar31_glue41_seg1", "ar31_glue41_seg1_start", "ar31_glue41_seg1_end"),
    ss(p,"ar31_glue41", "ar31_glue41_start", "ar31_glue41_end"),
    ss(p,"ar31_glue41_check_seg1", "ar31_glue41_check_seg1_start", "ar31_glue41_check_seg1_end"),
    ss(p,"ar31_glue41_check_seg2", "ar31_glue41_check_seg2_start", "ar31_glue41_check_seg2_end"),
    ss(p,"ar31_placelf2_seg1", "ar31_placelf2_seg1_start", "ar31_placelf2_seg1_end"),

    ss(p,"ar31_picklf2_seg2", "ar31_picklf2_seg2_start", "ar31_picklf2_seg2_end"),
    ss(p,"ar31_picklf2_seg3", "ar31_picklf2_seg3_start", "ar31_picklf2_seg3_end"),
    ss(p,"ar31_picklf2_seg4", "ar31_picklf2_seg4_start", "ar31_picklf2_seg4_end"),

    ss(p,"ar31_placett_seg1","ar31_placett_seg1_start", "ar31_placett_seg1_end"),
    ss(p,"ar31_placett_seg2","ar31_placett_seg2_start", "ar31_placett_seg2_end"),
    ss(p,"ar31_placett_seg3","ar31_placett_seg3_start", "ar31_placett_seg3_end"),
    ss(p,"ar31_goto_home","ar31_goto_home_start", "ar31_goto_home_end")
  )

  val ar41abs = List(
    a("ar41_lockTool",
      p("ar41_toolUnlocked", List("ar41_lockTool := true")),
      p("ar41_lockTool && !ar41_toolLocked"),
      p("ar41_toolLocked", List("ar41_lockTool := false"))),

    a("ar41_unlockTool",
      p("ar41_toolLocked", List("ar41_unlockTool := true")),
      p("ar41_unlockTool && !ar41_toolUnlocked"),
      p("ar41_toolUnlocked", List("ar41_unlockTool := false"))),

    ss(p, "ar41_home_to_stand", "ar41_home_to_stand_start", "ar41_home_to_stand_end"),
    ss(p, "ar41_stand_to_get_check", "ar41_stand_to_get_check_start", "ar41_stand_to_get_check_end"),
    ss(p, "ar41_get_check_to_home", "ar41_get_check_to_home_start", "ar41_get_check_to_home_end"),
    ss(p, "ar41_stand_to_put_check", "ar41_stand_to_put_check_start", "ar41_stand_to_put_check_end"),
    ss(p, "ar41_put_check_to_between", "ar41_put_check_to_between_start", "ar41_put_check_to_between_end")
  )

  val abs = lf1abs ++ lf2abs ++ ar31abs ++ ar41abs

  // setup driver
  val driverID = UUID.randomUUID()
  def sm(vars: List[Thing]): List[APIVirtualDevice.OneToOneMapper] = vars.flatMap { v =>
    v.attributes.getAs[String]("drivername").map(dn => APIVirtualDevice.OneToOneMapper(v.id, driverID, dn))
  }
  val setup = SPAttributes("url" -> "opc.tcp://localhost:12686", "identifiers" -> sm(allVars).map(_.driverIdentifier))
  val driver = APIVirtualDevice.Driver("opclocal", driverID, "OPCUA", setup)
  mediator ! Publish(APIVirtualDevice.topicRequest, SPMessage.makeJson[SPHeader, APIVirtualDevice.SetUpDeviceDriver](SPHeader(from = "hej"), APIVirtualDevice.SetUpDeviceDriver(driver)))

  // setup resources
  val lf1 = APIVirtualDevice.Resource("loadFixture1", UUID.randomUUID(), lf1vars.map(_.id).toSet, sm(lf1vars), SPAttributes())
  val lf2 = APIVirtualDevice.Resource("loadFixture2", UUID.randomUUID(), lf2vars.map(_.id).toSet, sm(lf2vars), SPAttributes())
  val ar31 = APIVirtualDevice.Resource("ar31", UUID.randomUUID(), ar31vars.map(_.id).toSet, sm(ar31vars), SPAttributes())
  val ar41 = APIVirtualDevice.Resource("ar41", UUID.randomUUID(), ar41vars.map(_.id).toSet, sm(ar41vars), SPAttributes())

  val resources = List(lf1, lf2, ar31, ar41)

  resources.foreach { res =>
    val body = APIVirtualDevice.SetUpResource(res)
    mediator ! Publish(APIVirtualDevice.topicRequest, SPMessage.makeJson[SPHeader, APIVirtualDevice.SetUpResource](SPHeader(from = "hej"), body))
  }

  // setup abilities
  abs.foreach { ab =>
    val body = APIAbilityHandler.SetUpAbility(ab)
    val msg = SPMessage.makeJson[SPHeader, APIAbilityHandler.SetUpAbility](SPHeader(to = ahid.toString, from = "hej"), body)
    mediator ! Publish(APIAbilityHandler.topicRequest, msg)
  }

  def receive = {
    case x => println(x)
  }

}

trait Helpers {
  import sp.abilityhandler.APIAbilityHandler.Ability

  def ss(p:(String,List[String])=>Condition, n:String, startvar:String, endvar:String) =
    a(n,
      p(s"!${startvar} && !${endvar}", List(s"${startvar} := true")),
      p(s"${startvar} && !${endvar}", List()),
      p(s"${startvar} && ${endvar}", List(s"${startvar} := false")))

  def a(n:String, pre:Condition, exec:Condition, post:Condition) =
    Ability(n, UUID.randomUUID(), pre, exec, post)
  def v(name: String, drivername: String) = Thing(name, SPAttributes("drivername" -> drivername))
  def prop(vars: List[IDAble])(cond: String,actions: List[String] = List()) = {
    def c(condition: String): Option[Proposition] = {
      PropositionParser(vars).parseStr(condition) match {
        case Right(p) => Some(p)
        case Left(err) => println(s"Parsing failed on condition: $condition: $err"); None
      }
    }

    def a(actions: List[String]): List[Action] = {
      actions.flatMap { action =>
        ActionParser(vars).parseStr(action) match {
          case Right(a) => Some(a)
          case Left(err) => println(s"Parsing failed on action: $action: $err"); None
        }
      }
    }
    Condition(c(cond).get, a(actions))
  }
}
