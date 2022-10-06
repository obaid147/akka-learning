package part4faulttolerance

import akka.actor.{Actor, ActorLogging, ActorSystem, Kill, PoisonPill, Props}

object StartingStoppingActorsMethod2 extends App {
  /**
   * method 2 - using special messages -- PoisonPill and Kill
   * */
  val system = ActorSystem("StartingStoppingActorsMethod2")

//  class Parent extends Actor with ActorLogging
  class Child extends Actor with ActorLogging {
    override def receive: Receive = {
      case message =>
        log.info(message.toString)
    }
  }

  val looseActor = system.actorOf(Props[Child], "looseActor")
  looseActor ! "Hey, looseActor"
  looseActor ! PoisonPill // PoisonPill is a special message that triggers the STOPPING of an Actor.
  looseActor ! "looseActor are you there???"

  val abruptlyTerminatedActor = system.actorOf(Props[Child], "abruptlyTerminatedActor")
  abruptlyTerminatedActor ! "You are about to be terminated"
  abruptlyTerminatedActor ! Kill // Little more brutal than PoisonPil, KILL make the actor throw ActorKilledException
  abruptlyTerminatedActor ! "abruptlyTerminatedActor you there???"
}
