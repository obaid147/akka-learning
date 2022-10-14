package part5infra

import akka.actor.{Actor, ActorLogging, ActorSystem, PoisonPill, Props}
import akka.dispatch.{ControlMessage, PriorityGenerator, UnboundedPriorityMailbox}
import com.typesafe.config.{Config, ConfigFactory}

object Mailboxes extends App {

  val system = ActorSystem("MailboxDemo", ConfigFactory.load().getConfig("mailboxesDemo"))

  class SimpleActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case message =>
        log.info(message.toString)
    }
  }

  /**
   * Interesting case # 1 - custom priority mailbox
   * P0 - most important
   * P1
   * P2
   * P3.....
   * */
  // STEP 1- mailbox definition
  class SupportTicketPriorityMailbox(settings: ActorSystem.Settings, config: Config)
    extends UnboundedPriorityMailbox(
      PriorityGenerator { // PF from Any to a Number
        case message: String if message.startsWith("[P0]") => 0 // lower number means higher priority
        case message: String if message.startsWith("[P1]") => 1
        case message: String if message.startsWith("[P2]") => 2
        case message: String if message.startsWith("[P3]") => 3
        case _ => 4
      })

  // STEP 2- make it known in the config   #support-ticket-dispatcher
  //STEP 3- attach the dispatcher to an actor
  val supportTicketActor = system.actorOf(Props[SimpleActor].withDispatcher("support-ticket-dispatcher"), "TActor")
  supportTicketActor ! PoisonPill // messages after this will be sent to dead letter actor, BUT not with Priority Mailbox
  // Thread.sleep(1000) this will kill the actor in the mean-time and the below messages will be sent to dead letter actor.

  // after which time can i send another message and be prioritized accordingly? NOPE!!!

  /**Interesting case # 2 - control-aware mailbox
   * we will use unbounded controlled aware mailbox*/

  // STEP -1 Mark important messages as control messages
  case object ManagementTicket extends ControlMessage

  // STEP -2 configure, who gets the mailbox ---- make the actor attach to the mailbox
  // method #1 of attaching an actor with controlled mailbox
  val controlAwareActor = system.actorOf(Props[SimpleActor].withMailbox("control-mailbox"), "controlActor")
  /*controlAwareActor ! "[P0] this needs to be solved NOW!" // 2
  controlAwareActor ! "[P1] do this when you have time" // 3
  controlAwareActor ! ManagementTicket // will be evaluated first*/

    //method #2 deploymentConfig
  val altControlledAwareActor = system.actorOf(Props[SimpleActor], "altCtrlActor")
  altControlledAwareActor ! "[P0] this needs to be solved NOW!" // 2
  altControlledAwareActor ! "[P1] do this when you have time" // 3
  altControlledAwareActor ! ManagementTicket // before P0 and P1
}
