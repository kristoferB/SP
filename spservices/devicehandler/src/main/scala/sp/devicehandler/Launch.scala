package sp.devicehandler

import akka.actor._

object HackTest {
  import sp.domain._
  import sp.domain.Logic._
  import java.util.UUID
  import akka.cluster.pubsub.DistributedPubSub
  import akka.cluster.pubsub.DistributedPubSubMediator.{ Put, Subscribe, Publish }

  def hackTest(system: ActorSystem) : Unit = {
    val mediator = DistributedPubSub(system).mediator
    val start = Thing("start")
    val start2 = Thing("start2")
    val start3 = Thing("start3")
    val end = Thing("end")
    val end2 = Thing("end2")
    val end3 = Thing("end3")
    val variables = List(start,end)
    val driverID = UUID.randomUUID()
    val driverStateMap: List[APIVirtualDevice.OneToOneMapper] = List(
      APIVirtualDevice.OneToOneMapper(start.id, driverID, "R82-88-17-41-080R01_B940WeldSeg2_start"),
      APIVirtualDevice.OneToOneMapper(end.id, driverID, "R82-88-17-41-080R01_B940WeldSeg2_end"),
      APIVirtualDevice.OneToOneMapper(start2.id, driverID, "R82-88-17-41-080R01_B940WeldSeg4_start"),
      APIVirtualDevice.OneToOneMapper(end2.id, driverID, "R82-88-17-41-080R01_B940WeldSeg4_end"),
      APIVirtualDevice.OneToOneMapper(start3.id, driverID, "R82-88-17-41-080R01_B941WeldSeg1_start"),
      APIVirtualDevice.OneToOneMapper(end3.id, driverID, "R82-88-17-41-080R01_B941WeldSeg1_end")
    )
    val resource = APIVirtualDevice.Resource("R82-88", UUID.randomUUID(), driverStateMap.map(_.thing).toSet, driverStateMap, SPAttributes())
    val setup = SPAttributes("url" -> "opc.tcp://localhost:12686",
      "identifiers" -> driverStateMap.map(_.driverIdentifier))
    val driver = APIVirtualDevice.Driver("opclocal", driverID, "OPCUA", setup)
    val bodyDriver = APIVirtualDevice.SetUpDeviceDriver(driver)
    val bodyResource = APIVirtualDevice.SetUpResource(resource)
    mediator ! Publish("services", SPMessage.makeJson[SPHeader, APIVirtualDevice.SetUpDeviceDriver](SPHeader(from = "hej"), bodyDriver))
    mediator ! Publish("services", SPMessage.makeJson[SPHeader, APIVirtualDevice.SetUpResource](SPHeader(from = "hej"), bodyResource))

    Thread.sleep(5000)
    println("----------------------------------------")
    val timeout = 500
    1 to 100 foreach { _ =>
      val bodyCommand = APIVirtualDevice.ResourceCommand(resource.id,
        Map(start.id -> SPValue(math.random < 0.5), end.id -> SPValue(math.random < 0.5),
        start2.id -> SPValue(math.random < 0.5), end2.id -> SPValue(math.random < 0.5),
        start3.id -> SPValue(math.random < 0.5), end3.id -> SPValue(math.random < 0.5)), timeout)
      mediator ! Publish("services", SPMessage.makeJson[SPHeader, APIVirtualDevice.ResourceCommand](SPHeader(from = "hej"), bodyCommand))
      Thread.sleep(1000)
    }
  }
}

object Launch extends App {
  implicit val system = ActorSystem("SP")
  val cluster = akka.cluster.Cluster(system)

  cluster.registerOnMemberUp {
    // Add root actors used in node here
    println("deviceHandler node has joined the cluster")
    val vdid = java.util.UUID.randomUUID()
    system.actorOf(VirtualDevice.props("vd", vdid), "vd")
    import sp.abilityhandler.AbilityHandler
    val ahid = java.util.UUID.randomUUID()
    system.actorOf(AbilityHandler.props("ah", ahid, vdid), "ah")
    import sp.abilityhandler.Sp1Talker
    //system.actorOf(Sp1Talker.props, "sp1talker")
    //system.actorOf(Trucks.props(ahid))

    //LabkitAbilities(ahid, system)

    // HackTest.hackTest(system)
    // HackTest.hackAbilities(system)
  }

  cluster.registerOnMemberRemoved{
    println("deviceHandler node has been removed from the cluster")
  }


  scala.io.StdIn.readLine("Press ENTER to exit cluster.\n")
  cluster.leave(cluster.selfAddress)

  scala.io.StdIn.readLine("Press ENTER to exit application.\n")
  system.terminate()

}
