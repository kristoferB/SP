package sp.json

/**
 * Created by Kristofer on 2014-07-01.
 */
trait SPJsonRestAPI extends SPJsonDomain with SPJsonIDAble {

  import spray.json._
  import DefaultJsonProtocol._

    // REST API CLASSES
  import sp.server._

}
