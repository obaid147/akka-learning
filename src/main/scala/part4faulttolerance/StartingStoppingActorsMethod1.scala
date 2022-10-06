package part4faulttolerance

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}

object StartingStoppingActorsMethod1 extends App {

  val system = ActorSystem("StartingStoppingActorsDemo")

  object Parent {
    case class StartChild(name: String)
    case class StopChild(name: String)
    case object Stop
  }

  class Parent extends Actor with ActorLogging {
    override def receive: Receive = withChildren(Map())
    import Parent._
    def withChildren(children: Map[String, ActorRef]): Receive = {
      case StartChild(name) =>
        log.info(s"Starting child $name")
        context.become(withChildren(children + (name -> context.actorOf(Props[Child], name))))
      case StopChild(name) =>
        log.info(s"Stopping child with the name $name")
        val childOption = children.get(name)
        childOption.foreach(childRef => context.stop(childRef)) // context.stop is nonblocking(asynchronous)
      case Stop =>
        log.info(s"Stopping myself")
        context.stop(self) // Stops itself, Asynchronous and also stops all of its children first then self.
      case message =>
        log.info(message.toString)
    }
  }

  class Child extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  import Parent._
  val parent = system.actorOf(Props[Parent], "parent")
  parent ! StartChild("child1")
  val child = system.actorSelection("/user/parent/child1")
  child ! "Hi Kid!"

  parent ! StopChild("child1") // after stopping child, The child still receives few messages before it completely stops
//  for(_ <- 1 to 10) child ! "are u there?"

  parent ! StartChild("child2")
  val child2 = system.actorSelection("/user/parent/child2")
  child2 ! "Hi, second child"

  parent ! Stop
  for(_ <- 1 to 20) parent ! "Parent are u still there???" // should not be received
  for(i <- 1 to 100) child2 ! s"[$i] Second kid are u still alive???"
}
