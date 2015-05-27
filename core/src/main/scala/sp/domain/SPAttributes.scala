package sp.domain

import org.json4s._

/**
 * Created by kristofer on 15-05-26.
 */
// Moved to package object in domain 15-05-26
//case class SPAttributes(attr: JObject = JObject(List()))
//case class SPValue(value: JValue = JNothing)

// Here we define all case classes that we used in attributes

/**
 * Used in services and gui to define what type an attribute value should have
 * If it should be a list or object, that is used instead and then this class
 * as the leaf
 * @param isa We use: string, int, double, boolean, id, time, ...
 * @param default
 */
case class SPValueDefinition(isa: String, default: SPValue)



case class StateVariable(domain: SVDomain,
                        init: Option[SPValue] = None,
                        goal: Option[SPValue] = None)

sealed trait SVDomain
case class DomainList(domain: List[SPValue]) extends SVDomain
case class DomainRange(range: Range) extends SVDomain
case object DomainBool extends SVDomain