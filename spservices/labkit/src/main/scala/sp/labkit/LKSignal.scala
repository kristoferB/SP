package sp.labkit

/**
  * Created by Erik on 07/03/2017.
  *
  * Signal used to update labkit but with additional functionality / information
  *
  * @param name corresponding name in labkits statemessage
  * @param inSignal true if SP writes to this signal, false if SP reads from this signal
  */
class LKSignal(var name: String,
               var inSignal: Boolean
             ) {
//TODO do we want to save the data of each signal? needs both Booleans and Ints
}
