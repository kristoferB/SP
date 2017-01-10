package sp.robotServices.core

import java.io.File

import com.typesafe.config.ConfigFactory

/**
  * Created by Daniel on 2016-06-11.
  */
object Config {
  // Config file
  val jarPath = new File(getClass.getProtectionDomain.getCodeSource.getLocation.toURI.getPath)
  val jarDir = jarPath.getParentFile.getPath
  val extConfFile = new File(jarDir, "application.conf")
  val config = if(extConfFile.exists && !extConfFile.isDirectory)
    ConfigFactory.parseFile(extConfFile)
  else
    ConfigFactory.load()
  val mqAddress = config.getString("activemq.address")
  val mqUser = config.getString("activemq.user")
  val mqPass = config.getString("activemq.pass")
  val mqTopic = config.getString("activemq.topic")
}
