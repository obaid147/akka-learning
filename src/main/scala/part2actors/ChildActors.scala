package part2actors

import akka.actor.{Actor, ActorRef, ActorSelection, ActorSystem, Props}
import part2actors.ChildActors.CreditCard.{AttachToAccount, CheckStatus}

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

  // --------------- 1 Actor Hierarchies.
  // Parent  ---> Child ---> GrandChild.

  /** Is Parent the top most at hierarchy NO */

  // ---------------- 2. Guardian Actors
  /** Guardian-actors are top-level actors in akka actor system:-
  AKKA actor system has its own actors for managing various things, eg:- Managing logging system.
  * 1. /system   --- System Guardian (Manages all system actors)
  * 2. /user     --- User Guardian (Every actor we create using (system.actorOf) is owned by /user
  * 3. /         --- Root Guardian (Manages both the system and user level guardian, Top of sys and user.
  */

  // --------------3. Actor Selection
  /** Finding an Actor by Path
   * We can locate an actor by its path, The we can use that to send a message.
   * If path is invalid:- ActorSelection object will contain no actor. Message will be dropped. [Dead Letter]
   * ActorSelection has ActorRef under the hood.
   * */
  val findActor: ActorSelection = system.actorSelection("/usr/Parent/Child")
  findActor ! "I found u"

  Thread.sleep(2000)
  println("DANGER")
  // -------------4. Danger
  /** NEVER PASS MUTABLE ACTOR STATE, OR 'THIS' REFERENCE, To CHILD ACTORS.
   * This breaks the actor encapsulation. and child can access parent methods without sending messages.*/
  object NaiveBankAccount{
    case class Deposit(amount: Int)
    case class Withdraw(amount: Int)
    case object InitializeAccount
  }
  class NaiveBankAccount extends Actor{
    var amount = 0
    import CreditCard._
    import NaiveBankAccount._
    override def receive: Receive = {
      case InitializeAccount =>
        val creditCardRef = context.actorOf(Props[CreditCard], "card")
        creditCardRef ! AttachToAccount(this)
      case Deposit(funds) => deposit(funds)
      case Withdraw(funds) => withdraw(funds)
    }

    def deposit(funds: Int) = {
      println(s"${self.path} depositing $funds on top of $amount")
      amount += funds
    }
    def withdraw(funds: Int) = {
      println(s"${self.path} withdrawing $funds from $amount")
      amount -= funds
    }
  }

  object CreditCard{
    case class AttachToAccount(bankAccountRef: NaiveBankAccount) // !!
    case object CheckStatus
  }
  class CreditCard extends Actor{
    override def receive: Receive = {
      case AttachToAccount(account) => context.become(attachedToAccount(account))
    }
    def attachedToAccount(account: NaiveBankAccount): Receive = {
      case CheckStatus => println(s"${self.path} your message has been processed")
        account.withdraw(1) // problem as child can access parent methods
    }
  }

  import CreditCard._
  import NaiveBankAccount._
  val bankAccountRef = system.actorOf(Props[NaiveBankAccount], "account")
  bankAccountRef ! InitializeAccount
  bankAccountRef ! Deposit(100)
  Thread.sleep(500)
  val creditCardSelection = system.actorSelection("/user/account/card")
  creditCardSelection ! CheckStatus // This should just return a status
}
