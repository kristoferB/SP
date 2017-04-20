package sp.labkit

import akka.actor._
import sp.abilityhandler.AbilityHandler
import sp.devicehandler.VirtualDevice
import sp.labkit.operations._
import sp.runners.OperationRunner


object Launch extends App {
  implicit val system = ActorSystem("SP")
  val cluster = akka.cluster.Cluster(system)

  cluster.registerOnMemberUp {
    // Add root actors used in node here

    val vdid = java.util.UUID.randomUUID()
    system.actorOf(VirtualDevice.props("vd", vdid), "vd")
    import sp.abilityhandler.AbilityHandler
    val ahid = java.util.UUID.randomUUID()
    system.actorOf(AbilityHandler.props("ah", ahid, vdid), "ah")
    system.actorOf(OperationRunner.props)

    // load the abilities and the OPC
    // this must be launched after the ability handler and the OPC
    sp.labkit.operations.LoadLabkitAbilities(system)

    system.actorOf(LabkitOperationService.props, "LabkitOperations")



    // Below is for the old labkit demo
//    println("labkit node has joined the cluster")
//    system.actorOf(OPC.props, "OPC")
//    system.actorOf(OPMakerLabKit.props, "opMakerLabKit")
//    system.actorOf(ProductAggregator.props, "ProductAggregator")
//    system.actorOf(ResourceAggregator.props, "ResourceAggregator")
  }

  cluster.registerOnMemberRemoved{
    println("labkit node has been removed from the cluster")
  }


  scala.io.StdIn.readLine("Press ENTER to exit cluster.\n")
  cluster.leave(cluster.selfAddress)

  scala.io.StdIn.readLine("Press ENTER to exit application.\n")
  system.terminate()

}
