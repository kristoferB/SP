//package sp.json
//
//  import spray.json._
//  import sp.system.messages._
//
///**
// * Created by Kristofer on 2014-07-01.
// */
//trait SPJsonMessages extends SPJsonDomain with SPJsonIDAble  {
//
//  import DefaultJsonProtocol._
//
//  implicit val cmFormat = jsonFormat3(CreateModel)
//  implicit val cm2Format = jsonFormat2(CreateModelNewID)
//  implicit val gidFormat = jsonFormat2(GetIds)
////  implicit val gopsFormat = jsonFormat1(GetOperations)
////  implicit val gtFormat = jsonFormat1(GetThings)
////  implicit val gspFormat = jsonFormat1(GetSpecs)
////  implicit val gqFormat = jsonFormat2(GetQuery)
//  implicit val gDiffFormat = jsonFormat2(GetDiff)
//
//  implicit val uidsFormat = jsonFormat3(UpdateIDs)
//
//  implicit val mdiffFormat = jsonFormat7(ModelDiff)
//  implicit val modelInfoFormat = jsonFormat4(ModelInfo)
//
//
//  implicit val esFormat = jsonFormat1(SPErrorString)
//  implicit val euFormat = jsonFormat2(UpdateError)
//
//  implicit val rtkiFormat = jsonFormat2(RuntimeKindInfo)
//  implicit val rtiFormat = jsonFormat4(CreateRuntime)
//  implicit val rSMFormat = jsonFormat2(SimpleMessage)
//
//  // UserActor Messages
//  implicit val userDetailFormat = jsonFormat2(UserDetails)
//  implicit val addUserFormat = jsonFormat3(AddUser)
//  implicit val userFormat = jsonFormat4(User)
//
//}
