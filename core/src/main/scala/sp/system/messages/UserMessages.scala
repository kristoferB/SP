package sp.system.messages

/**
 * Created by Daniel on 2014-07-17.
 */
// Common

case class UserDetails(id: Int, role: String)

// API Inputs
case class AddUser(userName: String, password: String, name: String)
case object GetUsers

// API Outputs
case class User(id: Int, userName: String, password: String, name: String)