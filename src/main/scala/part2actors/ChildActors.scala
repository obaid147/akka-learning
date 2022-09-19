package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ChildActors extends App{
  // Actors create other actors by invoking context.actorOf

  object Parent{
    // create 2 messages that this parent supports
    case class CreateChild(name: String)
    case class TellChild(message: String)
  }
  class Parent extends Actor{
    import Parent._

    override def receive: Receive = {
      case CreateChild(name) =>
        println(s"${self.path} creating child")

        // create a new actor inside receive message handler inside this actor.
        val childRef: ActorRef = context.actorOf(Props[Child], name)
        context.become(withChild(childRef)) // change message handler
    }

    def withChild(childRef: ActorRef): Receive = {
      case TellChild(message) => childRef forward message
      // forward the message to childRef
    }
  }

  class Child extends Actor{
    override def receive: Receive = {
      case message => println(s"${self.path} I got message:- $message")
    }
  }

  val system = ActorSystem("ParentChildDemo")

  val parent = system.actorOf(Props[Parent], "Parent")
  import Parent._
  parent ! CreateChild("Child")
  /*As a reaction to CreateChild, Parent will create a new Actor of type Child with a name "Child"
  * and then parent changes it's message handler*/

  /*Parent changes it's message handler*/
  parent ! TellChild("Hey Kid!")
}
