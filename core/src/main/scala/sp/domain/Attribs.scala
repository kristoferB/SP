package sp.domain


/**
  * Attributes used by operations defining possible resources that can execute it
 * @param resources
 */
case class ResoursAttrib(resources: List[ID])

/**
  * Used by objects that have a well defined parent like stateVariables
 * @param parent
 */
case class ParentAttrib(parent: ID)

/**
  * Used by StateVariables to either have a list of possible values
 * or a range for int values. If the stateVariables does not encode
 * any domain, any value is allowed.
 */
sealed trait DomainAttrib
case class DomainListAttrib(domain: List[SPAttributeValue]) extends DomainAttrib
case class RangeAttrib(lower: Int, upper: Int) extends DomainAttrib


/**
 * Include predefined attributes
 *
 *
 */

/**
 * Import this were you want to use predefs
 */
object Attribs {
  implicit class getRAttr(attr: SPAttributes) {
    def getResourceAttrib = {
      val listOfThings = attr.getAsList("resources") map(_.flatMap(_.asID))
      listOfThings map(ResoursAttrib)
    }
    def saveResourceAttrib(ra: ResoursAttrib): SPAttributes = {
      attr + ("resources" -> ListPrimitive(ra.resources map IDPrimitive))
    }
  }

  implicit class getPAttr(attr: SPAttributes) {
    def getParentAttrib = {
      attr.getAsID("parent").map(ParentAttrib)
    }
    def saveParentAttrib(pa: ParentAttrib): SPAttributes = {
      attr + ("parent" -> IDPrimitive(pa.parent))
    }
  }

  implicit class getStateAttr(attr: SPAttributes) {
    def getStateAttr(key: String): Option[State] = {
      attr.getAsList(key) map( li =>
          MapState((li flatMap {
            case MapPrimitive(keyValues) => {
              val id = keyValues.get("id") flatMap(_.asID)
              val value = keyValues.get("value")
              for {
                theID <- id
                theValue <- value
              } yield theID -> theValue
            }
            case _ => None
          }).toMap)
        )
    }
  }

  implicit class getDomainAttr(attr: SPAttributes) {
    def getDomainAttrib = {
      val d = attr.getAttribute(List("domain", "list")) flatMap
        (_.asList map DomainListAttrib)
      val r = attr.getAttribute(List("domain", "range")) flatMap
        extractRange
      if (d.isEmpty) r else d
    }
    def saveParentAttrib(da: DomainAttrib): SPAttributes = {
      val add: MapPrimitive = da match {
        case DomainListAttrib(xs) => MapPrimitive(Map("list" ->  ListPrimitive(xs)))
        case RangeAttrib(l,h) => MapPrimitive(Map("range" -> MapPrimitive(Map(
          "lower" -> IntPrimitive(l),
          "upper" -> IntPrimitive(h)
        ))))
      }
      attr + ("domain" -> add)
    }
    def extractRange(v: SPAttributeValue) = {
      for {
        map <- v.asMap
        low <- map.get("lower")
        lower <- low.asInt
        up <- map.get("upper")
        upper <- up.asInt
      } yield RangeAttrib(lower, upper)
    }
  }


  //TODO: Fix this later but we probably need HList 140825
//  case class AttribExtractor[T](extr: SPAttributes => Option[T], mess: String)
//
//  implicit class getOrFailAttr(attr: SPAttributes) {
//    def extract(List[AttribExtractor]): Either[String, Map] = {
//      attr.getAsList(key) map( li =>
//        MapState((li flatMap {
//          case MapPrimitive(keyValues) => {
//            val id = keyValues.get("id") flatMap(_.asID)
//            val value = keyValues.get("value")
//            for {
//              theID <- id
//              theValue <- value
//            } yield theID -> theValue
//          }
//          case _ => None
//        }).toMap)
//        )
//    }
//  }

}
