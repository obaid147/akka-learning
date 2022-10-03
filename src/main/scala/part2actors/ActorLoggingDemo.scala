package part2actors

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.event.Logging

object ActorLoggingDemo extends App {
  class SimpleActorWithExplicitLogger extends Actor {
    val logger = Logging(context.system, this)
    override def receive: Receive = {
      case message => logger.info(message.toString) // LOG it
    }
  }
  /* LOGGING in generally done on 4 levels
  * 1 - DEBUG
  * 2 - INFO
  * 3 - WARNING/WARN
  * 4 - ERROR
  * */
  val system = ActorSystem("LoggingDemo")

  val actor = system.actorOf(Props[SimpleActorWithExplicitLogger], "ActorRef")

  actor ! "LOGGING simple message"
}

/**
 * Logging is done asynchronously, minimise performance
 * Akka logging is done with actors!
 * Logging does not depend on a particular implementation, Default logger dumps
    things to standard output. We can insert other loggers as well, eg: SLF4J
 * We can change it with simple configuration.(introAkkaConfig)
 */
object ActorLoggingTrait extends App {
  class ActorWithLogging extends Actor with ActorLogging {
    override def receive: Receive = {
      case (a, b) => log.info("Two things: {} and {}", a, b)
                  // interpolate into this the actual values that we are going to pass.
      case message => log.info(message.toString)
    }
  }

  val system = ActorSystem("LoggingDemo")

  val simplerActor = system.actorOf(Props[ActorWithLogging], "simpleActor")

  simplerActor ! "Logging a simple message by extending a trait"
  simplerActor ! (4, 9)
}