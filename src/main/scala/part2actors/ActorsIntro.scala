package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ActorsIntro extends App{

  //part1 Creating ActorSystem
  val actorSystem = ActorSystem(name = "FirstActorSystem")
  println(actorSystem.name) // FirstActorSystem

  //part2 creating actor
  class WordCountActor extends Actor{
    var totalWordCount = 0
    def receive: PartialFunction[Any, Unit] = {
      case message: String =>
        println(s"[word counter] I have received a message:- $message")
        totalWordCount += message.split(" ").length
      case msg => println(s"[Word Counter] I cannot understand ${msg.toString}")
    }
  }

  /** Communicating with this actor
   * First instantiate an actorSys not the way we used to do in OOPs. but by invoking the actor system
   * Then send it a message */

  //  part 3  instantiate our actor
  val wordCounter: ActorRef = actorSystem.actorOf(Props[WordCountActor], name="wordCounter")
  // returnType is ActorRef which is a DS, why? so that we cannot call actor instance as we used to in OOPs
  // We can communicate with this actor via actor Reference.

  // part 4 communicating with actor
  wordCounter ! "I am learning akka and it's cool"
  wordCounter.!("New Message") //
}
