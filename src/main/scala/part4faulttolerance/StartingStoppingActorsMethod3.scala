package part4faulttolerance

import akka.actor.{Actor, ActorLogging, ActorSystem, PoisonPill, Props, Terminated}
import part4faulttolerance.StartingStoppingActorsMethod3.Watcher.StartChild

object StartingStoppingActorsMethod3 extends App {

  /**
   * Method 3 - Death Watch - context.watch (Mechanism for being notified when an Actor dies.)
   * context.unwatch (Is very useful when we expect a reply from an actor until we get a response we register for
      its death watch because it might die in the meantime.And when you get the response that you want,
      you naturally unsubscribe from the actor's Death Watch because you don't care if it's alive anymore.)
   * */
  val system = ActorSystem("StartingStoppingActorsMethod3")

  object Watcher {
    case class StartChild(name: String)
  }

  class Watcher extends Actor with ActorLogging {

    import Watcher._

    override def receive: Receive = {
      case StartChild(name) =>
        val child = context.actorOf(Props[Child], name)
        log.info(s"Started and watching child $name")
        context.watch(child) //registers this actor for the death of the child.
        // When child dies, this Actor will receive a special terminated message.
        // (context.unwatch)  don't care if child is alive anymore)
      case Terminated(ref) =>
        log.info(s"The reference i am watching $ref has been stopped")
        /**
         * This Watcher Actor is able to create a child.
         * And Registers itself for the death of the child.
         * When the child stops, WatcherActor receives a special Terminated(ChildReference) message from akka.
        * */
    }
  }

  class Child extends Actor with ActorLogging {
    override def receive: Receive = {
      case message =>
        log.info(message.toString)
    }
  }

  val watcher = system.actorOf(Props[Watcher], "watcher")
  watcher ! StartChild("watchedChild") // Watcher creates/starts a child with the name watchedChild

  val watchedChild = system.actorSelection("/user/watcher/watchedChild")
  Thread.sleep(500) // let's wait so that Actor has been created 1/2 second
  watchedChild ! "Hey CHILD"

  watchedChild ! PoisonPill
  // PoisonPill will trigger the death of watchedChild and because the Watcher(Parent) has registered itself
  // for the death of its child. It will then receive Terminated message.

}
