package sp.system

import akka.actor.{ ExtendedActorSystem, Extension, ExtensionKey }

object SPSettings extends ExtensionKey[SPSettings]

/**
 * The settings for Sequence planner read from the config file:
 *   core settings are read into predfined vals, but if you add
 *   your own settings, access them via
 */
class SPSettings(system: ExtendedActorSystem) extends Extension {

  /**
   * Return Config to access settings not defined in this file. See com.typesafe.config
   * Core SP configs should be defined as vals instead
   */
  val config = system.settings.config

  /**
   * The network interface the SP gui gets bound to, e.g. `"localhost"`.
   */
  val interface: String = system.settings.config getString "service.interface"

  /**
   * The port the the SP gui gets bound to, e.g. `8080`.
   */
  val port: Int = system.settings.config getInt "service.port"


}