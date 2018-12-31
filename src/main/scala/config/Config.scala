package config

import config.Config.{configFile, getConfigDir}
import io.circe.Printer
import io.circe.parser.decode
import io.circe.syntax._
import io.circe.generic.auto._
import util.FileOps

object Config {
  private def getConfigDir(nodeName: String) = nodeName
  private def configFile(nodeName: String) = s"$nodeName/config"

  def read(nodeName: String, fileOps: FileOps): Config = {
    fileOps.readFile(configFile(nodeName)) match {
      case Right(contents) =>
        decode[Config] (contents) match {
          case Right (config) => config
          case Left (_) => throw new RuntimeException (s"Could not parse config for $nodeName.")
        }
      case Left(_) => Config(Nil)
    }
  }

}

case class Config(seeds: Seq[String]) {
  def write(nodeName: String, fileOps: FileOps): Unit = {
    fileOps.createDirIfNotExists(getConfigDir(nodeName))
    val configAsString = Printer(dropNullValues = true, preserveOrder = true, indent = "2").pretty(this.asJson)
    fileOps.writeFile(configFile(nodeName), configAsString)
  }

}
