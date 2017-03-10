package sp.labkit

/**
  * Created by Erik on 10/03/2017.
  * A unique operation for one or more blocks of code to be executed in labkit
  *
  * @param name duh
  * @param resources hardware resources needed for execution
  * @param attributes what to use in the resources
  * @param preconditions representing the state labkit needs to be in before this operation can start
  * @param postconditions what can / can't be executed when this operation finishes
  * @param errors known errors that can occur during this operation
  *
  */
class LKOperation (val name: String,
                   val resources: List[LKResource],
                   val attributes: List[LKAttribute],
                   val preconditions: List[Boolean],
                   val postconditions: List[Boolean],
                   val errors: List[LKError]) {
  //TODO Scaladocs
  //TODO Proper classes (?) for pre -and postconditions
  //TODO IDable? Timestamps for scheduling
}
