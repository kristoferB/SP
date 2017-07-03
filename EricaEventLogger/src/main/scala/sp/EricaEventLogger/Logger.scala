package sp.EricaEventLogger

import akka.persistence._

import sp.gPubSub.API_Data.EricaEvent

class Logger extends PersistentActor {
  override def persistenceId = "EricaEventLogger"

  override def receiveCommand = {
    case ev: EricaEvent => persist(ev)(ev => println("EricaEventLogger persisted " + ev))
  }

  override def receiveRecover = {
    case ev: EricaEvent => println("EricaEventLogger recovered " + ev)
    case RecoveryCompleted => println("EricaEventLogger recovery completed")
  }
}
