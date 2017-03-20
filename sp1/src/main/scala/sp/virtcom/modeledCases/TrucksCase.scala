package sp.virtcom.modeledCases

import sp.domain.Logic._
import sp.domain.SPAttributes
import sp.virtcom.CollectorModel

case class TrucksCase(modelName: String = "ENTOC Trucks cell") extends CollectorModel {

  def hAtt(h: String) = SPAttributes("hierarchy" -> Set(h))
  def ab(name: String) = SPAttributes("ability" -> name)

  // Loadfixture1
  {
    val device = "LoadFixture1"; val h = hAtt(device)
    v(name = "lf1Container", idleValue = "empty", attributes = h)
    v(name = "lf1Clamps", idleValue = "opened", attributes = h)
    op("lf1LoadPart", SPAttributes(aCarrierTrans("lf1Container", atStart = "empty", atComplete = "hasPart")) merge ab("lf1_startAddProduct") merge h)
    op("lf1CloseClamps", SPAttributes(aCarrierTrans("lf1Clamps", atStart = "opened", atComplete = "closed")) merge ab("lf1_closeClamps") merge h)
    op("lf1OpenClamps", SPAttributes(aCarrierTrans("lf1Clamps", atStart = "closed", atComplete = "opened")) merge ab("lf1_openClamps") merge h)
  }

  // Loadfixture2
  {
    val device = "LoadFixture2"; val h = hAtt(device)
    v(name = "lf2Container", idleValue = "empty", attributes = h)
    v(name = "lf2Clamps", idleValue = "opened", attributes = h)
    op("lf2LoadPart", SPAttributes(aCarrierTrans("lf2Container", atStart = "empty", atComplete = "hasPart")) merge ab("lf2_startAddProduct") merge h)
    op("lf2CloseClamps", SPAttributes(aCarrierTrans("lf2Clamps", atStart = "opened", atComplete = "closed")) merge ab("lf2_closeClamps") merge h)
    op("lf2OpenClamps", SPAttributes(aCarrierTrans("lf2Clamps", atStart = "closed", atComplete = "opened")) merge ab("lf2_openClamps") merge h)
  }

  // AR31, blue robot
  {
    val device = "AR31"; val h = hAtt(device)

    v(name = "AR31Part", idleValue = "empty", attributes = h)
    v(name = "AR31Pos", idleValue = "home", attributes = h)


    op("AR31PickLF1Seg1", SPAttributes(aResourceTrans("AR31Pos", "home", "home_to_atLF1", "atLF1")) merge ab("ar31_picklf1_seg1") merge h)

    op("AR31PickLF1", SPAttributes(
      aCarrierTrans("AR31Part", atStart = "empty", atComplete = "partA"),
      aResourceTrans("AR31Pos", "atLF1", "atLF1Picking", "atLF1")
    ) merge ab("ar31_startVacuum") merge h)
    x("PickPartLF1", s"lf1Container != hasPart && AR31Pos == atLF1Picking")

    op("AR31PickLF1Seg2", SPAttributes(aResourceTrans("AR31Pos", "atLF1", "atLF1_to_atLF1_2", "atLF1_2"),
      aCarrierTrans("lf1Container", atStart = "hasPart", atComplete = "empty")) merge ab("ar31_picklf1_seg2") merge h)
    x("PickPartLF1Lift", s"AR31Part != partA && AR31Pos == atLF1_to_atLF1_2")

    op("AR31PickLF1Seg3", SPAttributes(aResourceTrans("AR31Pos", "atLF1_2", "atLF1_2_to_atLF1_3", "atLF1_3")) merge ab("ar31_picklf1_seg3") merge h)

    op("AR31Glue42Seg1", SPAttributes(aResourceTrans("AR31Pos", "atLF1_3", "atLF1_3_to_atGlue42", "atGlue42")) merge ab("ar31_glue42_seg1") merge h)
    op("AR31Glue42", SPAttributes(aResourceTrans("AR31Pos", "atGlue42", "Gluing42", "atGlue41_seg1")) merge ab("ar31_glue42") merge h)
    // ss(p,"ar31_glue42_check_seg1", "ar31_glue42_check_seg1_start", "ar31_glue42_check_seg1_end"),
    op("AR31Glue41Seg1", SPAttributes(aResourceTrans("AR31Pos", "atGlue41_seg1", "atGlue41_seg1_atGlue41", "atGlue41")) merge ab("ar31_glue41_seg1") merge h)
    op("AR31Glue41", SPAttributes(aResourceTrans("AR31Pos", "atGlue41", "Gluing41", "atGlue41_done")) merge ab("ar31_glue41") merge h)
    // ss(p,"ar31_glue41_check_seg1", "ar31_glue41_check_seg1_start", "ar31_glue41_check_seg1_end"),
    // ss(p,"ar31_glue41_check_seg2", "ar31_glue41_check_seg2_start", "ar31_glue41_check_seg2_end"),

    op("AR31PlaceLF2Seg1", SPAttributes(aResourceTrans("AR31Pos", "atGlue41_done", "atGlue41_done_to_atPlaceLF2Seg1", "atPlaceLF2Seg1")) merge ab("ar31_placelf2_seg1") merge h)
    x("PutPartLF2", s"lf2Container != hasPart && AR31Pos == atGlue41_done_to_atPlaceLF2Seg1")

    // drop glued segment
    op("AR31PutLF2", SPAttributes(
      aCarrierTrans("AR31Part", atStart = "partA", atComplete = "empty"),
      aResourceTrans("AR31Pos", "atPlaceLF2Seg1", "atLF2Placing", "atPlaceLF2Seg1")) merge ab("ar31_stopVacuum") merge h)

    op("AR31PickLF2", SPAttributes(
      aCarrierTrans("AR31Part", atStart = "empty", atComplete = "partA"),
      aResourceTrans("AR31Pos", "atPlaceLF2Seg1", "atLF2Picking", "atPlaceLF2Seg1")) merge ab("ar31_startVacuum") merge h)

    op("AR31PickLF2Seg2", SPAttributes(aResourceTrans("AR31Pos", "atPlaceLF2Seg1", "atPlaceLF2Seg1_to_atPlaceLF2Seg2", "atPickLF2Seg2")
     , aCarrierTrans("lf2Container", atStart = "hasPart", atComplete = "empty")
    ) merge ab("ar31_picklf2_seg2") merge h)

    op("AR31PickLF2Seg3", SPAttributes(aResourceTrans("AR31Pos", "atPickLF2Seg2", "atPickLF2Seg2_to_atPickLF2Seg3", "atPickLF2Seg3")) merge ab("ar31_picklf2_seg3") merge h)
    op("AR31PickLF2Seg4", SPAttributes(aResourceTrans("AR31Pos", "atPickLF2Seg3", "atPickLF2Seg3_to_atPickLF2Seg4", "atPickLF2Seg4")) merge ab("ar31_picklf2_seg4") merge h)

    op("AR31PlaceTTSeg1", SPAttributes(aResourceTrans("AR31Pos", "atPickLF2Seg4", "atPickLF2Seg4_to_atPlaceTTSeg1", "atPlaceTTSeg1")) merge ab("ar31_placett_seg1") merge h)
    op("AR31PlaceTTSeg2", SPAttributes(aResourceTrans("AR31Pos", "atPlaceTTSeg1", "atPlaceTTSeg1_to_atPlaceTTSeg2", "atPlaceTTSeg2")) merge ab("ar31_placett_seg2") merge h)
    op("AR31PlaceTTSeg3", SPAttributes(aResourceTrans("AR31Pos", "atPlaceTTSeg2", "atPlaceTTSeg2_to_atPlaceTTSeg3", "atPlaceTTSeg3")) merge ab("ar31_placett_seg3") merge h)
    op("AR31ToHomeTT", SPAttributes(aResourceTrans("AR31Pos", "atPlaceTTSeg3", "atPlaceTTSeg3_to_home", "home"),
      aCarrierTrans("AR31Part", atStart = "partA", atComplete = "empty")) merge ab("ar31_goto_home") merge h)

    // temp stuff to complete the cycle early
    op("AR31ToHome", SPAttributes(
      aResourceTrans("AR31Pos", "atGlue41_done", "atGlue41_done_to_home", "home"),
      aCarrierTrans("AR31Part", atStart = "partA", atComplete = "empty")) merge ab("ar31_goto_home") merge h)
  }

}
