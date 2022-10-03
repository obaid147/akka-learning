package part2actors

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import com.typesafe.config.{Config, ConfigFactory}

object IntroAkkaConfig extends App {
  class SimpleLoggingActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case message =>
        log.info(message.toString)
    }

  }
  //Create a dummyConfig.conf file

  /** Ways to pass Configuration from .conf file to an ActorSystem
  * 1- inline configuration
  * */

  // inline
  val configString =
    """
    | akka {
    |   loglevel = "ERROR"
    | }
    |""".stripMargin

  val config: Config = ConfigFactory.parseString(configString)
  val actorSystem: ActorSystem = ActorSystem("ConfigurationDemo", ConfigFactory.load(config)) // User defined configs

  val simpleActor = actorSystem.actorOf(Props[SimpleLoggingActor])
  simpleActor ! "A message to remember."


  /**
   * 2 - Configuration in a file (most common)
   * under src/main/resource/application.conf
   */

  val defaultFileConfigSystem = ActorSystem("DefaultConfigFileDemo")
  // When we create NO CONFIGURATION, Scala looks for application.conf file under src/main/resources directory
  val defaultConfigActor = defaultFileConfigSystem.actorOf(Props[SimpleLoggingActor])
  defaultConfigActor ! "Remember me"


  /**
   * 3 - separate configurations in the same file.
   * We goto application.conf, We create a special namespace
   */
  val specialConfig = ConfigFactory.load().getConfig("mySpecialConfig")
  val specialConfigSystem = ActorSystem("SpecialConfigDemo", specialConfig)
  val specialConfigActor = specialConfigSystem.actorOf(Props[SimpleLoggingActor])
  specialConfigActor ! "Remember me."

  /**
   * 4 - separate configurations in different files.
   * Create a folder inside resources and create a file in it.
   * src/main.resources/secretFolder/secretConfiguration.config
   */
    // for any configuration to be useful, we need a config object
  val separateConfig = ConfigFactory.load("secretFolder/secretConfiguration.conf")
  println(s"Separate config log level ${separateConfig.getString("akka.loglevel")}")

  /**
   * 5 - Different file formats .JSON
   * Create json folder under resources folder and a file jsonConfig.json
   */
  val jsonConfig = ConfigFactory.load("json/jsonConfig.json")
  println(s"JSON config property:- ${jsonConfig.getString("aJsonProperty")}")
  println(s"JSON config log level:- ${jsonConfig.getString("akka.loglevel")}")
  // akka.loglevel return DEBUG || aJsonProperty returns the property value "Obaid"

  /**
   * 5 - .properties
   * In .properties we cannot do nested configurations. We have to declare them by their fully qualified names.
   */
  val propsConfig = ConfigFactory.load("props/propsConfiguration.properties")
  println(s"Properties config simpleProperty:- ${propsConfig.getString("my.simpleProperty")}")
  println(s"Properties config akka log level:- ${propsConfig.getString("akka.loglevel")}")
}
