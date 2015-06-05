package sp.domain

/**
 * Created by Kristofer on 2014-06-08.
 */
case class Operation(name: String,
                     conditions: List[Condition] = List(),
                     attributes: SPAttributes = SPAttributes(),
                     id: ID = ID.newID)
        extends IDAble {

}
