package sp.domain

/**
 * Started more simple then before. Now we need to encode if it is a resource or entitiy in attributes
 * Also hierarchy will be defined by attributes. When a good structure have been found, maybe we move out
 * some attributes
 *
 * Created by Kristofer on 2014-06-11.
 */
case class Thing(name: String,
                 attributes: SPAttributes = Attr(),
                 id: ID = ID.newID) extends IDAble




