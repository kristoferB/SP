package sp.domain

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
  }

  implicit class getPAttr(attr: SPAttributes) {
    def getParentAttrib = {
      attr.getAsID("parent").map(ParentAttrib)
    }
  }

}

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

