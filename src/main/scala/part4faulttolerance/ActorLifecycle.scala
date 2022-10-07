package part4faulttolerance

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
//                              ActorSystem.PNG

/** #1 ***** Actor Instance ***** Denoted by diamond.
 * Has Methods.
 * May have Internal state.
 *
 * #2 ***** Actor Reference (incarnation) ***** denoted by big blue circle with instance and mail-box inside.
 * created with context/system.actorOf.
 * has mailbox and can receive and process messages.
 * contains one actor instance at any one time.
 * contains UUID given by the actor system.
 *
 * #3 ***** Actor Path ***** denoted by yellow plane with actor reference inside.
 * may or may not have actorRef inside.
 * */
object ActorLifecycle extends App {
  /** Actor can be...
   * started = create a new ActorRef with a UUID at a given path.
   * suspended = The ActorRef will enqueue but not process more messages.
   * resumed = The ActorRef will continue processing more messages.
   *
   * restarted = Restarting is trickier:-
   * Let's assume we have an actor which has an Actor Instance(diamond shape)
   * # Steps
   * 1- ActorReference is suspended.
   * 2- Then the Actor Instance is swapped in a number of steps:-
   * a) old instance calls LifeCycle Method called preRestart()
   * b) Then Actor Instance is released and new Actor Instance comes back to take it's place.
   * c) Then new Actor Instance calls postRestart() and then
   * 3- The Actor Reference is resumed.
   * "Internal state is destroyed on restart as the actorInstance is completely swapped.
   *
   * stopped = Releases the Actor Reference which occupies the given Actor Path inside of the Actor System.
   * call postStop
   * all watching actors receives Terminated(ref) if they registered for death watch.
   * "After both of these steps are completed, The Actor Ref may be released which means the Actor Path can then be
   * occupied by another Actor Reference which has a diff UUID given by ActorSystem. So, this is diff ActorRef,
   * which means that as a result of stopping, all messages currently enqueued on that Actor Ref will be lost"
   * */

  object StartChild

  class LifeCycleActor extends Actor with ActorLogging {

    // preStart is called before actorInstance has a chance to process any messages.
    //override def preStart(): Unit = log.info("I am Starting.....")

//    override def postStop(): Unit = log.info("I have Stopped.....")
    // byDefault these 2 methods don't do anything

    override def receive: Receive = {
      case StartChild =>
        context.actorOf(Props[LifeCycleActor], "child")
    }
  }

  val system = ActorSystem("LifeCycleDemo")
  val parent = system.actorOf(Props[LifeCycleActor], "parent")

//  parent ! StartChild
//  parent ! PoisonPill // first child stops then parent

  /**
   * restart
   * */
  object Fail
  object FailChild
  object CheckChild
  object Check

  class Parent extends Actor {
    private val child: ActorRef = context.actorOf(Props[Child], "supervisorChild")

    override def receive: Receive = {
      case FailChild => child ! Fail
      case CheckChild => child ! Check
    }
  }

  class Child extends Actor with ActorLogging {

    override def preStart(): Unit = log.info("Supervised child started..")

    override def postStop(): Unit = log.info("Supervised child stopped..")

    override def preRestart(reason: Throwable, message: Option[Any]): Unit =
    // preRestart is called by OLD actorInstance b4 its swapped during the restart procedure.
      log.info(s"Supervised actor pre-restarted because of ${reason.getMessage}")

    override def postRestart(reason: Throwable): Unit =
    // postRestart is called by NEW actorInstance that has just been inserted at the restart procedure.
      log.info(s"Supervised actor post-restarted")

    override def receive: Receive = {
      case Fail =>
        log.warning("child will fail now")
        throw new RuntimeException("I Failed")
        // even if child actor threw an exception previously, child was still restarted and was able to send messages.
      /**
       * Supervision strategy:-
       * If an ACTOR threw an EXCEPTION while processing a message, this message that caused EXCEPTION to be thrown
       * is REMOVED from the QUEUE and not put back in the MAILBOX again.
       * """"""""Actor is restarted and mailbox is untouched.""""""""
       */
      case Check =>
        log.info("alive and kicking")
    }
  }

  val supervisor = system.actorOf(Props[Parent], "supervisor")
  supervisor ! FailChild
  supervisor ! CheckChild

}
