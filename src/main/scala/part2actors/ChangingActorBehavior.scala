package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import part2actors.ChangingActorBehavior.Mom.MomStart

object ChangingActorBehavior extends App{

  object FussyKid{
    case object KidAccept
    case object KidReject
    val HAPPY = ":)"
    val SAD = ":`("
  }
  class FussyKid extends Actor{
    import FussyKid._
    import Mom._

    var kidState = HAPPY // internal state of kid
    override def receive: Receive = {
      case Food(VEGETABLE) => kidState = SAD
      case Food(CHOCOLATE) => kidState = HAPPY
      case Ask(_) =>
        if(kidState == HAPPY) sender() ! KidAccept
        else sender() ! KidReject
    }
  }

  // Creating a stateless FussyKid ie:- without using var(mutable)...
  class StatelessFussyKid extends Actor{
    import FussyKid._
    import Mom._
    override def receive: Receive = {
      happyReceive
    }

    def happyReceive: Receive = {
      case Food(VEGETABLE) => context.become(sadReceive, false)
      case Food(CHOCOLATE) =>
      case Ask(_) => sender() ! KidAccept
    }
    def sadReceive: Receive = {
      case Food(VEGETABLE) => context.become(sadReceive, false)
      case Food(CHOCOLATE) => context.unbecome()
      case Ask(_) => sender() ! KidReject
    }
    /** become() takes 2 arguments, Receiver object and
     * a boolean(discardOld message handler)
     * true means:- fully replace old message handler with new handler....
     *  like replace happyHandler when Food is Vegetable with sadHandler
     *
     * false :- Stacks the new message handler on top of old message handler.
     * stack.push(happyReceive), stack.push(sadReceive)
     * stack:- TOS-> sadReceive
     * head-> akka work on the TOP of stack.
     *
     * unbecome() takes no params it just pop the TOP/head of the stack. and works on the handler at TOP of stack
     * */
  }


  object Mom{
    case class MomStart(kidRef: ActorRef)
    case class Food(food: String)
    case class Ask(message: String) // question
    val VEGETABLE = "veggies"
    val CHOCOLATE = "chocolate"
  }
  class Mom extends Actor{
    import FussyKid._
    import Mom._
    override def receive: Receive = {
      case MomStart(kidRef) =>
        // test our interaction
        kidRef ! Food(VEGETABLE)
        kidRef ! Food(VEGETABLE)
        kidRef ! Food(CHOCOLATE)
        kidRef ! Food(CHOCOLATE)
        kidRef ! Ask("Do you want to play?")
      case KidAccept => println("Yay, my kid is happy")
      case KidReject => println("My kid is sad, but at least she is healthy!")
    }
  }

  val system = ActorSystem("changingActorBehaviorDemo")

  val fussyKidActor = system.actorOf(Props[FussyKid], "fussyKidActorRef")
  val momActor = system.actorOf(Props[Mom], "MomActorRef")
//  momActor ! MomStart(fussyKidActor)

  val statelessFussyKid = system.actorOf(Props[StatelessFussyKid], "statelessFussyKid")
  momActor ! MomStart(statelessFussyKid)
  /*
    mom receives MomSTart
      kid receives Food(veg) -> kid will change handler to sadReceive
      kid receives Ask(play?) -> kid replies with sadReceive handler =>
    mom receives KidReject
  */

  /* both discardOld parameters of happy and sad Handler are FALSE
      now, we send 2 messages to statelessFussyKid
  * Food(Veg) ==> message handler transforms to sadHandler replacing happy
  * Food(CHOCOLATE) ==> message handler transforms to happyHandler replacing sad

  1. Without BOOLEAN arg of become(),
      implementation will replace HappyHandler with SadHandler, then will replace sadHandler with happyHandler.

    -------- context.become(mandatory Receive param, bool discardOld) --------------
  2. With BOOLEAN arg of become() in both sad and happy handler is false,
      Stack at start:-
      1. happyReceive

      statelessFussyKid receives these 2 messages in order:---
        1. Food(veg)
        2. Food(choco)
            Stack :-  TOS is happyReceive at start
            stack.push(sadReceive)
            stack.push(happyReceive)

            final MESSAGE HANDLING STACK:-
            1. happyReceive
            2. sadReceive
            3. happyReceive

-------- context.unbecome() --------------
  we use context.unbecome() to go back to old receive handler in the stack

  new behavior:- context.unbocome   .... where sadHandler(veg) => context.become(sadReceive, false)
    Messages to statelessFussyKid
      1. Food(veg)
      2. Food(Veg)
      3. Food(Choco)
      4. Food(Choco)

    Stack:- TOS happyReceive

    stack.push(sadReceive), stack.push(sadReceive) ---for messages 1 and 2
    updated stack:-
      1. sadReceive
      2. sadReceive
      3. HappyReceive

    stack.pop() ---- for message 3 unbecome
    updated stack:-
      1. sadReceive
      2. HappyReceive

    stack.pop() ----- for message 4 unbecome
    updated stack
      1. HappyReceive

  ---- kid is sad with these messages
  kidRef ! Food(VEGETABLE)
  kidRef ! Food(VEGETABLE)
  kidRef ! Food(CHOCOLATE)

  ---- kid is happy with these messages
  kidRef ! Food(VEGETABLE)
  kidRef ! Food(VEGETABLE)
  kidRef ! Food(CHOCOLATE)
  kidRef ! Food(CHOCOLATE)
  */

  /** Exercise - 1
   * create a stateless Counter Actor*/
  object Counter {
    case object Increment
    case object Decrement
    case object Print
  }

  class Counter extends Actor {
    import Counter._
    override def receive: Receive = counterReceive(0)
    def counterReceive(counter: Int): Receive = {
      case Increment => context.become(counterReceive(counter+1))
      case Decrement => context.become(counterReceive(counter-1))
      case Print => println(s"Counter at $counter")
    }
  }

  val counterActor = system.actorOf(Props[Counter], "counterActor")

  import Counter._
//  (1 to 5) foreach (_ => counterActor ! Increment)
  counterActor ! Increment
  counterActor ! Increment
  counterActor ! Increment
  counterActor ! Print
  counterActor ! Decrement
  counterActor ! Print

  //  (1 to 3) foreach (_ => counterActor ! Decrement)
//  counterActor ! Print


  /** Exercise - 2
   * create a Simplified voting system
   *
   * We have 2 kinds of Actors in this voting system*/

  /**
   * 1.If we send this msg to the citizen, mark the citizen as having voted for this candidate.
   * 2.Citizen will receive this Vote exactly once and once it receives the Vote,
   *    it will turn itself into a state of having voted with a candidate.
   * 3.VoteAggregator will be able to send messages to the citizen asking them who they voted.
   * 4.VoteAggregator having received an AggregateVotes message will then ask each citizen in return a message
   *    called VoteStatusRequest
   * 5.VoteAggregator having received an AggregateVotes message from this application,
   *    It will then use every single citizen as an ActorRef from the parameter and it will send each of those citizen
   *    one of these messages VoteStatusRequest.
   *    and each Citizen will reply with a case class VoteStatusReply, where each msg will contain the candidate.
   */
  //Citizen will handle some messages for voting
  case class Vote(candidate: String)
  case object VoteStatusRequest
  case class VoteStatusReply(candidates: Option[String])
  class Citizen extends Actor {
    var candidate: Option[String] = None
    override def receive: Receive = {
      case Vote(c) => candidate = Some(c)
      case VoteStatusRequest => sender() ! VoteStatusReply(candidate)
    }
  }

  case class AggregateVotes(citizen: Set[ActorRef])
  class VoteAggregator extends Actor {
    var stillWaiting: Set[ActorRef] = Set()
    var currentStats: Map[String, Int] = Map()
    override def receive: Receive = {
      case AggregateVotes(citizens) =>
        stillWaiting = citizens
        citizens.foreach(citizenRef =>
          citizenRef ! VoteStatusRequest)
      case VoteStatusReply(None) => // citizen has not voted yet
        sender() ! VoteStatusRequest // might infinite loop, if a citizen never voted
      case VoteStatusReply(Some(candidate)) =>
        val newStillWaiting = stillWaiting - sender()
        val currentVotesOfCandidate = currentStats.getOrElse(candidate, 0)
        currentStats += (candidate -> (currentVotesOfCandidate + 1))
        if (newStillWaiting.isEmpty) println(s"[Aggregator] pool stats:- $currentStats")
        else stillWaiting = newStillWaiting


    }
  }

  val alice = system.actorOf(Props[Citizen], "citizenAlice")
  val bob = system.actorOf(Props[Citizen], "citizenBob")
  val charlie = system.actorOf(Props[Citizen], "citizenCharlie")
  val daniel = system.actorOf(Props[Citizen], "citizenDaniel")

  alice ! Vote("Martin") // alice votes for Martin // 1
  bob ! Vote("Jonas") // bob votes for Jonas // 1
  charlie ! Vote("Roland") // charlie votes for Roland // 1
  daniel ! Vote("Roland") // daniel votes for Roland // 2

  val voteAggregator = system.actorOf(Props[VoteAggregator], "voteAggregator")
  voteAggregator ! AggregateVotes(Set(alice, bob, charlie, daniel))

  /* EndResult     MAP[Candidate, number of candidate votes]
    --> print the status of the votes
    [Aggregator] pool stats: Map(Roland -> 2, Martin -> 1, Jonas -> 1)
   */

  /** Code description
   * We have 4 actors and each of them votes for their own candidate.

   * VoteAgg receives this message AggVotes with the 4 citizens.
   * as a reaction to it, AggVotes sends each citizen a VoteStatusRequest.(VoteAggregator class).

   * All of the 4 citizens we have, Will react to this VoteStatusRequest by sending back VoteStatusReply
   *  with their own candidate to the sender which is the VoteAgg Actor.

   * VoteStatusReply(Some(candidate)) will receive 4 VoteStatusReply's with Some(candidate)
   * as reaction to each 4 of them, VoteAgg will update it's currentStats.

   * Finally at the end,
   * When the VoteAgg receives it's last message, newStillWaiting will be empty and prints to console.
   * */
}
