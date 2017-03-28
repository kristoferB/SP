package sp.labkit

/**
  * Created by Erik on 10/03/2017.
  * Description of a known error and its solution in labkit
  *
  * @param code error code for internal database
  * @param description explanation of what SP thinks has happened
  * @param solution how to reset/restart the labkit when this error occurs
  */
class LKError (val code: Int,
               val description: String,
               val solution: String
              ) {
//TODO Time stamp of some sort? Or is this part of exception?
}
