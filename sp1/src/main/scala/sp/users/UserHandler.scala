package sp.users

/**
 * Created by Daniel on 2014-07-17.
 */

import akka.actor.Props
import akka.persistence._
import sp.system.messages._

class UserHandler() extends PersistentActor  {
  override def persistenceId = "UserHandler"


  case class UserMapState(userMap: Map[String,User] = Map()) {
    def updateUser(user: User): Map[String, User] = userMap + (user.id + "" -> user)
    def addUser(userToAdd: AddUser): Map[String, User] = userMap + (9 + "" -> User(9, userToAdd.userName, userToAdd.password, userToAdd.name))
    def getUserById(userId: Int): User = userMap(userId + "")
    def getUsers: Map[String, User] = userMap
    override def toString: String = userMap.toString
  }

  var state = UserMapState()
  var userByUserName: Map[String, User] = Map()

  def getUserByUserName(userName: String): User = {
    userByUserName(userName)
  }

  def updateState(cmd: Object): Unit = {
    def getUpdatedMap: Map[String, User] = {
      cmd match {
        case userToAdd: AddUser => state.addUser(userToAdd)
        case user: User => state.updateUser(user)
      }
    }
    state = state.copy(getUpdatedMap)
    userByUserName = for {
      (k, v) <- state.getUsers
    } yield {
      (v.userName, v)
    }
  }

  val receiveRecover: Receive = {
    case user: User                               => updateState(user)
    case userToAdd: AddUser                       => updateState(userToAdd)
    case SnapshotOffer(_, snapshot: UserMapState) => state = snapshot
  }

  val receiveCommand: Receive = {
    case userToAdd: AddUser =>
      persist(userToAdd) { userToAdd =>
        updateState(userToAdd)
        val addedUser = getUserByUserName(userToAdd.userName)
        sender ! addedUser
      }
    case userToUpdate: User =>
      persist(userToUpdate) { userToUpdate =>
        updateState(userToUpdate)
        sender ! userToUpdate
      }
    case GetUsers =>
      sender ! state.getUsers
    case "snap"  => saveSnapshot(state)
    case "print" => println(state)
  }
}

object UserHandler {
  def props = Props(classOf[UserHandler])
}