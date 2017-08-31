package spgui.testing

import sp.domain._
import Logic._

object TestingAPI {
  sealed trait API
  case class X1(a: String, b: Int, c: Boolean)        extends API
  case class X2(a: String, b: Int, c: Boolean)            extends API
  case class X3(a: String, b: Int, c: Boolean)            extends API
  case class X4(a: String, b: Int, c: Boolean)            extends API
  case class X5(a: String, b: Int, c: Boolean)            extends API
  case class X6(a: String, b: Int, c: Boolean)            extends API
  case class X7(a: String, b: Int, c: Boolean)            extends API
  case class X8(a: String, b: Int, c: Boolean)            extends API
  case class X9(a: String, b: Int, c: Boolean)            extends API
  case class X10(a: String, b: Int, c: Boolean)           extends API
  case class X11(a: String, b: Int, c: Boolean)          extends API
  case class X12(a: String, b: Int, c: Boolean)          extends API
  case class X13(a: String, b: Int, c: Boolean)          extends API
  case class X14(a: String, b: Int, c: Boolean)          extends API
  case class X15(a: String, b: Int, c: Boolean)          extends API
  case class X16(a: String, b: Int, c: Boolean)          extends API
  case class X17(a: String, b: Int, c: Boolean)          extends API
  case class X18(a: String, b: Int, c: Boolean)          extends API
  case class Y1(a: String, b: Int, c: Boolean)        extends API
  case class Y2(a: String, b: Int, c: Boolean)            extends API
  case class Y3(a: String, b: Int, c: Boolean)            extends API
  case class Y4(a: String, b: Int, c: Boolean)            extends API
  case class Y5(a: String, b: Int, c: Boolean)            extends API
  case class Y6(a: String, b: Int, c: Boolean)            extends API
  case class Y7(a: String, b: Int, c: Boolean)            extends API
  case class Y8(a: String, b: Int, c: Boolean)            extends API
  case class Y9(a: String, b: Int, c: Boolean)            extends API
  case class Y10(a: String, b: Int, c: Boolean)           extends API



  //val k = deriveFormatISA[API]

  import play.api.libs.json._
  object API {
    implicit val fX1: JSFormat[X1] = Json.format[X1]
    implicit val fX2: JSFormat[X2] = Json.format[X2]
    implicit val fX3: JSFormat[X3] = Json.format[X3]
    implicit val fX4: JSFormat[X4] = Json.format[X4]
    implicit val fX5: JSFormat[X5] = Json.format[X5]
    implicit val fX6: JSFormat[X6] = Json.format[X6]
    implicit val fX7: JSFormat[X7] = Json.format[X7]
    implicit val fX8: JSFormat[X8] = Json.format[X8]
    implicit val fX9: JSFormat[X9] = Json.format[X9]
    implicit val fX10: JSFormat[X10] = Json.format[X10]
    implicit val fX11: JSFormat[X11] = Json.format[X11]
    implicit val fX12: JSFormat[X12] = Json.format[X12]
    implicit val fX13: JSFormat[X13] = Json.format[X13]
    implicit val fX14: JSFormat[X14] = Json.format[X14]
    implicit val fX15: JSFormat[X15] = Json.format[X15]
    implicit val fX16: JSFormat[X16] = Json.format[X16]
    implicit val fX17: JSFormat[X17] = Json.format[X17]
    implicit val fX18: JSFormat[X18] = Json.format[X18]
    implicit val fY1: JSFormat[Y1] = Json.format[Y1]
    implicit val fY2: JSFormat[Y2] = Json.format[Y2]
    implicit val fY3: JSFormat[Y3] = Json.format[Y3]
    implicit val fY4: JSFormat[Y4] = Json.format[Y4]
    implicit val fY5: JSFormat[Y5] = Json.format[Y5]
    implicit val fY6: JSFormat[Y6] = Json.format[Y6]
    implicit val fY7: JSFormat[Y7] = Json.format[Y7]
    implicit val fY8: JSFormat[Y8] = Json.format[Y8]
    implicit val fY9: JSFormat[Y9] = Json.format[Y9]
    implicit val fY10: JSFormat[Y10] = Json.format[Y10]

    def k = Json.format[TestingAPI.API]
    implicit val kk = k

  }

}
