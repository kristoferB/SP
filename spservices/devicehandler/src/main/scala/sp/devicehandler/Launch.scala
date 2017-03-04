package sp.devicehandler

import akka.actor._

object HackTest {
  import sp.domain._
  import sp.domain.Logic._
  import java.util.UUID
  import akka.cluster.pubsub.DistributedPubSub
  import akka.cluster.pubsub.DistributedPubSubMediator.{ Put, Subscribe, Publish }
  import sp.messages._
  import sp.messages.Pickles._
  import scala.util.{Failure, Success, Try}

  def hackTest(system: ActorSystem) : Unit = {
    val mediator = DistributedPubSub(system).mediator
    val setup = SPAttributes("url" -> "opc.tcp://localhost:12686",
      "identifiers" -> List("R82-88-17-41-080R01_B940WeldSeg2_end", "R82-88-17-41-080R01_B940WeldSeg2_start"))
    val driver = APIVirtualDevice.Driver("opclocal", UUID.randomUUID(), "OPCUA", setup)
    val start = Thing("start")
    val end = Thing("end")
    val variables = List(start,end)
    val driverStateMap = List(
      APIVirtualDevice.OneToOneMapper(start.id.value, driver.id, "R82-88-17-41-080R01_B940WeldSeg2_start"),
      APIVirtualDevice.OneToOneMapper(end.id.value, driver.id, "R82-88-17-41-080R01_B940WeldSeg2_end")
    )
    val resource = APIVirtualDevice.Resource("R82-88", UUID.randomUUID(), driverStateMap, SPAttributes())

    val header = SPHeader(from = "hej")
    val bodyDriver = APIVirtualDevice.SetUpDeviceDriver(driver)
    val bodyResource = APIVirtualDevice.SetUpResource(resource)
    val bodyCommand = APIVirtualDevice.ResourceCommand(resource.id, Map(start.id.value -> SPValue(math.random < 0.5)))
    val bodyCommand2 = APIVirtualDevice.ResourceCommand(resource.id, Map(end.id.value -> SPValue(math.random < 0.5)))
    SPMessage.make(header, bodyDriver).map { m => mediator ! Publish("services", m.toJson) }
    SPMessage.make(header, bodyResource).map { m => mediator ! Publish("services", m.toJson) }
    Thread.sleep(5000)
    SPMessage.make(header, bodyCommand).map { m => mediator ! Publish("services", m.toJson) }
    Thread.sleep(500)
    SPMessage.make(header, bodyCommand2).map { m => mediator ! Publish("services", m.toJson) }
  }
}


object Launch extends App {
  implicit val system = ActorSystem("SP")
  val cluster = akka.cluster.Cluster(system)

  cluster.registerOnMemberUp {
    // Add root actors used in node here
    println("deviceHandler node has joined the cluster")
    system.actorOf(VirtualDevice.props("vd", java.util.UUID.randomUUID()), "vd")

    HackTest.hackTest(system)
  }

  cluster.registerOnMemberRemoved{
    println("deviceHandler node has been removed from the cluster")
  }


  scala.io.StdIn.readLine("Press ENTER to exit cluster.\n")
  cluster.leave(cluster.selfAddress)

  scala.io.StdIn.readLine("Press ENTER to exit application.\n")
  system.terminate()

}
