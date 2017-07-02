package sekvensa.logging

import akka.actor.ActorSystem
import org.json4s.JValue
import scala.concurrent._

//import sekvensa.elvis._
import com.github.nscala_time.time.Imports._
//import scala.concurrent.duration._


case class ElvisPatient(CareContactId: Int,
                        CareContactRegistrationTime: DateTime,
                        DepartmentComment: String,
                        Events: List[ElvisEvent],
                        Location: String,
                        PatientId: Int,
                        ReasonForVisit: String,
                        Team: String,
                        VisitId: Int,
                        VisitRegistrationTime: DateTime)



case class ElvisEvent(CareEventId: Int,
                      Category: String,
                      End: DateTime,
                      Start: DateTime,
                      Title: String,
                      Type: String,
                      Value: String,
                      VisitId: Int)


case class PatientDiff(updates: Map[String, JValue], newEvents: List[ElvisEvent], removedEvents: List[ElvisEvent])
case class NewPatient(timestamp: DateTime, patient: ElvisPatient)
case class RemovedPatient(timestamp: DateTime, patient: ElvisPatient)
case class SnapShot(patients: List[ElvisPatient])

object LoggIt extends App {

  val system = ActorSystem("SP")
  val cluster = akka.cluster.Cluster(system)

  /*
  val logger = system.actorOf(ElvisLogger.props, "logger")

  //val comm = system.actorOf(ElvisComm.props(logger), "comm")

  //comm ! "GET"

  //scala.io.StdIn.readLine()

  Console.readLine() // wait for enter to exit
  system.terminate()
  */


cluster.registerOnMemberUp {

    // Start all you actors here.
    println("ElvisLogger node has joined the cluster")
    system.actorOf(ElvisLogger.props)

  }
  cluster.registerOnMemberRemoved{
    println("ElvisLogger node has been removed from the cluster")
  }

  scala.io.StdIn.readLine("Press ENTER to exit cluster.\n")
  cluster.leave(cluster.selfAddress)

}

