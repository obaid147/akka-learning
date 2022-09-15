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
}
