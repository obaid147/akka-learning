package playground

import akka.actor.ActorSystem

object PlayGround extends App {

  val actorSystem = ActorSystem(name = "HelloAkka")
  println(actorSystem.name)

}
