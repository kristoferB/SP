package sp.labkit

/**
  * Created by Erik on 07/03/2017.
  *
  * Representation of a hardware part of labkit
  *
  * @param name useless placeholder identifier
  * @param attributes attributes that this resource can perform without
  *                   regard to other parts of labkit
  * @param signals signals used to control the resource
  */
class LKResource (var name: String,
                  var attributes: List[LKAttribute],
                  var signals: List[Signal]
                 ) {

}
