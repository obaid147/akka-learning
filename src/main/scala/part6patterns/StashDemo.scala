package part6patterns

import akka.actor.{Actor, ActorLogging, ActorSystem, Props, Stash}

object StashDemo extends App {

  /** stashes allow actors to set messages for later that they can't ot should not process at this exact moment in time
   * When actor changes behaviour by calling context.become/unbecome,
   * It is usually a good time to prepend them to the mailbox and start processing them again */

  /* ResourceActor
     When Open - it can receive read and write requests to the resource.
     Otherwise - it will postpone all read and write requests until the state is open.

     ResourceActor is closed
      - Open => switch to open state
      - Read, Write messages are postponed

     ResourceActor is Open
      - Read, Write are handled
      - close => switch to close state

      MAILBOX of Actor [open, read, read, write]
      behaviour -> switch to open state, read data, read data, write data

      MAILBOX of Actor [read, open, write]
      behaviour -> Postponed(put the read in Stash[special memory zone]), switch to open state, Stash is prepended,
                    read data, write data.
  * */

  case object Open

  case object Close

  case object Read

  case class Write(data: String)

  // Step 1 - mixin Stash trait
  class ResourceActor extends Actor with ActorLogging with Stash {
    private var innerData: String = ""

    override def receive: Receive = closed

    def closed: Receive = {
      case Open =>
        log.info("opening resource")
        // Step 3 - When you switch the message handler
        unstashAll() // prepending messages hoping that the next message handler can handle those messages.
        context.become(open)
      case message =>
        log.info(s"Stashing $message because I can't handle it in closed state")
        // STEP 2 - stash away what you cannot handle
        stash() // stashing message by simply calling stash()
    }

    def open: Receive = {
      case Read =>
        log.info(s"Read the $innerData")
      case Write(data) => // replace the innerData with new data from Write
        log.info(s"I am writing $data")
        innerData = data
      case Close =>
        log.info("Closing resource")
        unstashAll()
        context.become(closed)
      case message =>
        log.info(s"Stashing $message because I can't handle it in open state")
        stash()
    }
  }

  val system = ActorSystem("StashDemo")
  val resourceActor = system.actorOf(Props[ResourceActor], "ResourceActor")

  //  resourceActor ! Write("I love stash")
  resourceActor ! Read // stashed
  resourceActor ! Open // Switch to open state; Read the ""
  resourceActor ! Open // stashed
  resourceActor ! Write("I love stash") // Write is handled coz we are still in open state ---I love stash
  resourceActor ! Close // closing resource; switch to closed state; opening resource; switch back to open state
  resourceActor ! Read // Read the I love stash
}
