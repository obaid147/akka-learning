package part5infra

import akka.actor.{Actor, ActorLogging, ActorSystem, Props, Terminated}
import akka.routing.{ActorRefRoutee, FromConfig, RoundRobinPool, RoundRobinRoutingLogic, Router}
import com.typesafe.config.ConfigFactory

object RoutersWithMethod1and2_1and2_2 extends App {

  /*Spread work among actors of same kind
  * Routers are usually middle level actors that forward messages to other actors those are Routees
  * either created by routers themselves or from outside.*/

  class Master extends Actor {
    // Master has to somehow route all messages to slave
    /** Method #1 -- manual router */

    // step-1 create routees
    private val slaves = for (i <- 1 to 5) yield {
      val slave = context.actorOf(Props[Slave], s"slave_$i")
      context.watch(slave)
      ActorRefRoutee(slave)
    }

    // step-2 define router
    private val router = Router(RoundRobinRoutingLogic(), slaves) // routing logic

    /** Supported options for routing logic
     * 1- RoundRobinRoutingLogic
     * 2- Random
     * 3- Smallest mailbox, sends message to the actor with the fewest messages in the queue.
     * 4- Broadcast, that sends same message to all the routees
     * 5- Scatter-gather-first, broadcasts and waits for the first reply and all the other replies are discarded
     * 6- Tail-chopping, forwards next message to each actor sequentially until first reply is received, others discarded
     * 7- Consistent-hashing, In which all messages with same hash get to the same actor */

    override def receive: Receive = {
      // step-3 route the messages
      case message => router.route(message, sender())
      // step-4 handle termination/lifecycle of the routees
      case Terminated(ref) =>
        router.removeRoutee(ref)
        val newSlave = context.actorOf(Props[Slave])
        context.watch(newSlave)
        router.addRoutee(newSlave)
    }
  }

  class Slave extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  val system = ActorSystem("RouterDemo", ConfigFactory.load().getConfig("routersDemo"))
  val master = system.actorOf(Props[Master], "MASTER")

  //  for(i <- 1 to 10) master ! s"[$i]hi there..."

  /** Method- 2.1  A router with its own children (Programmatically / in code)
   * This is called POOL router */
  val poolMaster = system.actorOf(RoundRobinPool(5).props(Props[Slave]), "simplePoolMaster")
  // RoundRobinPool(5) will create 5 actors of type Props[Slave] => 5 slaves created under itself, and also managed.
  // for(i <- 1 to 10) poolMaster ! s"[$i]Hey, PoolMaster"
  /** This does the same thing as the complicated Method-1 out of the box*/


  /** Method 2.2 From Configuration*/
  // applications.conf and also in ActorSystem
  val poolMaster2 = system.actorOf(FromConfig.props(Props[Slave]), "poolMaster2")
  for(i <- 1 to 10) poolMaster2 ! s"[$i]Hey, PoolMaster2"
}
