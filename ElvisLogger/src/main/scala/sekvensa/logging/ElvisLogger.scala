package sekvensa.logging

import akka.actor._
import akka.persistence._
import com.codemettle.reactivemq.ReActiveMQMessages.GetAuthenticatedConnection
import org.json4s._
import org.json4s.JsonAST.JValue
import com.codemettle.reactivemq._
import com.codemettle.reactivemq.ReActiveMQMessages._
import com.codemettle.reactivemq.model._



/**
 * Created by kristofer on 18/02/15.
 */
class ElvisLogger extends PersistentActor {
  override def persistenceId = "ELVIS"

  //override def preStart() = ()
  // Add correct user and password here, TODO: move to configuration
  //ReActiveMQExtension(context.system).manager ! GetAuthenticatedConnection("nio://localhost:61616", "user", "pass")

  import org.json4s.native.Serialization
  import org.json4s.native.Serialization.{read, write}



  var currentState: List[ElvisPatient] = List()
  var playBack: List[String] = List()
  var theBus: Option[ActorRef] = None;

  import org.json4s._
  import org.json4s.native.JsonMethods._
  import org.json4s.native.Serialization
  import org.json4s.native.Serialization.{read, write}
  import com.github.nscala_time.time.Imports._
  implicit val formats = org.json4s.DefaultFormats ++ org.json4s.ext.JodaTimeSerializers.all


  println("Start of logger")

  def receiveCommand = {
    case ConnectionEstablished(request, c) => {
      println("connected:"+request)
      theBus = Some(c)



      val testSnap = SnapShot(List(ElvisPatient(1, DateTime.now, "hej", List(), "g", 1, "", "", 1, DateTime.now)))
      self ! testSnap
    }
    case ConnectionFailed(request, reason) => {
      println("failed:"+reason)
    }
    case mess @ AMQMessage(body, prop, headers) => {
      println(mess)
    }
    case s @ SnapShot(ps) => {
      val json = write(Map("patients"->ps))
      sendToEvah(json, "elvisSnapShot")

      if (currentState.isEmpty) {
        ps.foreach(p => println(s"${p.CareContactId}, ${p.Location}"))
        persist(s)(event => println("persisted a snapshot"))
        currentState = ps
      }
      else if (currentState != ps)  {
        val changes = ps.filterNot(currentState.contains)
        val removed = currentState.filterNot(p => ps.exists(_.CareContactId == p.CareContactId))
        changes.map{p =>
          val old = currentState.find(_.CareContactId == p.CareContactId)
          //println(s"OLD: $old")
          //println(s"NEW: $p")

          val diffP = diffPat(p, old)
          diffP match {
            case None => {
              val newPatient = NewPatient(getNow, p)
              persist(newPatient)(e => println(s"persisted a NEW: $e"))
            }
            case Some(d) => {
              val diffPatient = PatientDiff(d._1, d._2, d._3)
              persist(diffPatient) { e =>
                println("")
                println(s"persisted a Diff")
                println(s"old pat: $old")
                println(s"new pat: $p")
                println(s"diff: $diffPatient")
                println("")
              } //println(s"persisted a diff: $diffPatient"))
            }
          }
        }
        removed.map{p =>
          val removedPat = RemovedPatient(getNow, p)
          persist(removedPat)(e => println(s"persisted a remove: $e"))

        }
        currentState = ps
      }

    }
    case mess @ _ => println(s"ElvisLogger got: $mess")
  }
  var i = 0
  var xs = List[PatientDiff]()
  val receiveRecover: Receive = {
    case RecoveryCompleted => 
            println("******** START ************")
      //playBack.reverse.foreach(json => sendToEvah(json))
      //*******************
      println("[")
      playBack.reverse.foreach(x => println(x + ","))
      println("]")
      println("******** STOP ************")


    case d: PatientDiff => {
      val json = write(Map("diff"->d))
      playBack = json :: playBack
      //      xs = d :: xs
//      if (i == 10){
//        val j = SPAttributes("events"->xs).bodyToJson
//        println(j)
//      }
//      i += 1
    }
    case np: NewPatient =>
      val json = write(Map("new"->np))
      playBack = json :: playBack
    case s: SnapShot =>  {
      //println("Got snap")
      //val j = SPAttributes("hej"->s).bodyToJson
      //println(s)
      s.patients.foreach(p => {
        val json = write(Map("new"->toNewPat(p)))
        playBack = json :: playBack
      })
    };
    case r: RemovedPatient =>
      val json = write(Map("removed"->r))
      playBack = json :: playBack
  }


  def sendToEvah(json: String, topic: String = "elvisPlayBack") = {
    theBus.foreach{bus => bus ! SendMessage(Topic(topic), AMQMessage(json))}
  }

  def toNewPat(p: ElvisPatient)= {
    val t = p.CareContactRegistrationTime
    NewPatient(t,p)
  }


  def diffPat(curr: ElvisPatient, old: Option[ElvisPatient])={
    old.map {
      case prev: ElvisPatient => {
        (Map(
          "CareContactId" -> Some(Extraction.decompose(curr.CareContactId)),
          "CareContactRegistrationTime" -> diffThem(prev.CareContactRegistrationTime, curr.CareContactRegistrationTime),
          "DepartmentComment" -> diffThem(prev.DepartmentComment, curr.DepartmentComment),
          "Location" -> diffThem(prev.Location, curr.Location),
          "PatientId" -> Some(Extraction.decompose(curr.PatientId)),
          "ReasonForVisit" -> diffThem(prev.ReasonForVisit, curr.ReasonForVisit),
          "Team" -> diffThem(prev.Team, curr.Team),
          "VisitId" -> diffThem(prev.VisitId, curr.VisitId),
          "VisitRegistrationTime" -> diffThem(prev.VisitRegistrationTime, curr.VisitRegistrationTime),
          "timestamp" -> Some(Extraction.decompose(getNow))
        ).filter(kv=> kv._2 != None).map(kv=> kv._1 -> kv._2.get),
          curr.Events.filterNot(prev.Events.contains),
          prev.Events.filterNot(curr.Events.contains))
      }
    }

  }

  def diffThem[T](prev: T, current: T): Option[JValue]= {
    if (prev == current) None
    else Some(Extraction.decompose(current))
  }

  def getNow = {
    DateTime.now(DateTimeZone.forID("Europe/Stockholm"))
  }



  // copy from SP
  type SPAttributes = JObject
  //val SPAttributes = JObject
  type SPValue = JValue
  //val SPValue = JValue

  object SPAttributes {
    def apply[T](pair: (String, T)*)(implicit formats : org.json4s.Formats, mf : scala.reflect.Manifest[T]): SPAttributes = {
      val res = pair.map{
        case (key, value) => key -> SPValue(value)
      }
      JObject(res.toList)
    }
    def apply() = JObject()
    def apply(fs: List[JField]): JObject = JObject(fs.toList)
    def fromJson(json: String) = {
      try {
        org.json4s.native.JsonMethods.parse(json) match {
          case x: SPAttributes => Some(x)
          case x: JValue => None
        }
      } catch {
        case e: Exception => None
      }
    }
  }

  object SPValue {
    def apply[T](v: T)(implicit formats : org.json4s.Formats, mf : scala.reflect.Manifest[T]): SPValue = {
      Extraction.decompose(v)
    }
    def apply(s: String) = JString(s)
    def apply(i: Int) = JInt(i)
    def apply(b: Boolean) = JBool(b)
    def fromJson(json: String): Option[SPValue] = {
      try {
        Some(org.json4s.native.JsonMethods.parse(json))
      } catch {
        case e: Exception => None
      }
    }
  }



}



object ElvisLogger {
  def props = Props[ElvisLogger]




}






