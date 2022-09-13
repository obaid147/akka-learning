package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
/**
 * The actor system as a collaborating ensemble of actors is the natural unit for managing shared facilities like
 * scheduling services, configuration, logging, etc.
 * Several actor systems with different configurations may co-exist within the same JVM without problems,
 * there is no global shared state within Akka itself, however the most common scenario will only involve
 * a single actor system per JVM.
 */
object ActorWithConstructorArgs extends App{
  /* Question:- How do we instantiate actor with constructor args?
  *  Answer:-
  *   1. Calling apply method of Props(legal but discouraged)
  *         val obj: ActorRef = actorSystem.actorOf(Props(new ChangeWordCaseActor("obiad")))
  *
  *   2. We create a companion object(best practise)
  * */

  val actorSystem = ActorSystem("actorWithConstructorArguments") // create ActorSystem

  object Person{ // define a companion object
    // define a method based on some arguments
    def props(name: String): Props =
      Props( // method creates a akka.actor.Props object
        new Person(
          name // with an actor instance
        ) // with our constructor arguments
      )
  }

  // -------1.  instantiate actor with constructor args using apply method of Props?
  class Person(name: String) extends Actor { //create actor
    def receive: Receive = {
      case "hi" => println(s"Received message:- $name")
      case msg => println(s"Received :- $msg")
    }
  }
  val person1: ActorRef = actorSystem.actorOf(Props(new Person("Obaid"))) // create actorRef
  val person2: ActorRef = actorSystem.actorOf(Props(new Person("Fayaz"))) // create actorRef

  person1 ! "This is a message"
  person1 ! "hi"
  person2 ! "hi"

  // We created a person actor by passing it Props with an actor instance inside,
  // This is legal but discouraged.

  //--- best practice is to create an companion object of actor
  val personProps = actorSystem.actorOf(Person.props("oby"))
  personProps ! "hi"

  /** So, this is the best practise for creating actors with constructor arguments.
   * Define a companion object and define a method that, based on some arguments,
   * it creates a props object with an actor instance.
   * */
}
