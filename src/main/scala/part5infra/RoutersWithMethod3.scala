package part5infra

import akka.actor.{Actor, ActorLogging, ActorSystem, Props, Terminated}
import akka.routing.{ActorRefRoutee, Broadcast, FromConfig, RoundRobinGroup, RoundRobinRoutingLogic, Router}
import com.typesafe.config.ConfigFactory

object RoutersWithMethod3 extends App {
  /** Method 3-  router with actors created elsewhere
   * This is called GROUP router
   *
   * ALSO The handling of special Messages*/

  class Master extends Actor {
    // step-1 create routees
    private val slaves = for (i <- 1 to 5) yield {
      val slave = context.actorOf(Props[Slave], s"slave_$i")
      context.watch(slave)
      ActorRefRoutee(slave)
    }
    // step-2 define router
    private val router = Router(RoundRobinRoutingLogic(), slaves)

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

  val system = ActorSystem("RoutersDemo3", ConfigFactory.load().getConfig("routersDemo"))

  //... in another part of the application, we create 5 slaves
  val slaveList = (1 to 5).map(i => system.actorOf(Props[Slave], s"slave_$i")).toList

  // need their path
  val slavePaths = slaveList.map(slaveRef => slaveRef.path.toString)

  // 3.1 in the code
  val groupMaster = system.actorOf(RoundRobinGroup(slavePaths).props())

  //  for(i <- 1 to 10) groupMaster ! s"[$i]Hey, groupMaster"

  // 3.2 from config
  val groupMaster2 = system.actorOf(FromConfig.props(), "groupMaster2")
  for(i <- 1 to 10) groupMaster2 ! s"[$i]Hey, groupMaster2"


  /***
   * special Message
   */
  groupMaster2 ! Broadcast("Hello Everyone")
  // This msg will be sent to every routed actor regardless of routing strategy

  // PoisonPill and Kill are NOT routed
  // AddRoutee, RemoveRoutee, GetRoutee handled by the routing actor
}
