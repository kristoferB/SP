package sp.labkit

/**
  * Created by Erik on 07/03/2017.
  *
  * Describes what a [[LKResource]] can do
  *
  * @param name Uneccessary identification likely removed at later stage
  * @param associatedSignals the signals in labkit that this attribute reads to / writes from
  */
class LKAttribute (var name: String,
                   var associatedSignals: List[LKSignal]) {

}
