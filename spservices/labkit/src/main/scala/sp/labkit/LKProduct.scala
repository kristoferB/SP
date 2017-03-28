package sp.labkit

/**
  * Created by Erik on 10/03/2017.
  * Model representation of a product currently in production in the labkit system
  *
  * @param target which sensors this product need to pass before completion
  * @param current which sensors the product has already passed
  */
class LKProduct (val target: Char,
                 var current: Char
                ) {
  //TODO Location?
}
