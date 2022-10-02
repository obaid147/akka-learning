package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ActorCapabilities extends App {
  //  val actorSystem = ActorSystem("")
  class SimpleActor extends Actor {
    override def receive: PartialFunction[Any, Unit] = {
      case "Hi" => sender() ! "Hello, there" // reply back from bob to alice, context.sender()/sender() is bob.
      case message: String => println(s"[${context.self.path}] I have received a STRING:- $message")
      case number: Int => println(s"[simple actor] I have received a NUMBER:- $number")
      //case userDefined: SpecialMessage => println(s"[simple actor] I have received a User Defined Type:- ${userDefined.contents}")
      case SpecialMessage(contents) =>
        println(s"[simple actor] I have received a User Defined Type:- $contents")
      case SendMessageToYourself(content) =>
        self ! content // sending context to myself, which then triggers (case message handler in future, this will be printed)
      case SayHiTo(ref) => ref ! "Hi" //(self) message from alice to bob, alice receives message and sends to bob
      // compiler inject the self param lit of tell method coz (context.self means implicit final val self)

      // forward
      case WirelessPhoneMessage(content, ref) =>
        //        ref ! content
        ref forward (content + "s") // i keep the original sender of th WPM
      // bob will receive message content+s with big context as sender
    }
  }

  val system = ActorSystem("actorCapabilitiesDemo")
  val simpleActor = system.actorOf(Props[SimpleActor], "simpleActor")

  simpleActor ! "hello, actor"

  simpleActor ! 44 // who is the sender?

  // 1. messages can be of any type
  simpleActor ! 1000

  /** When we invoke the tell method,
   * akka retrieves the object(definition of receive method) that will be invoked on the message type that we sent.
   *
   * When we invoke 1000,
   * akka retrieves definition of receive method an then it will invoke it
   * with arg 1000 that we have used for sending
   * */

  // user defined type
  case class SpecialMessage(contents: String)

  simpleActor ! SpecialMessage("Some Special Contents")

  /** 1. messages can be of any type but under 2 conditions */
  //    a) messages must be IMMUTABLE.
  //    b) messages must be SERIALIZABLE.
  /** Serializable means that the JVM can transform it into a bitstream and send it to another JVM, whether
   * it's on the same machine or over the network.
   * So Serializable is a Java interface and there are a number of serialisation protocols. */

  // USE case classes and case objects but core principles are IMMUTABLE and SERIALIZABLE.

  /** 2. actors have information about their context and about themselves */
  /* Each actor has a member called context.
    * CONTEXT is a complex DS that has reference to information regarding the env this actor runs in.
    * Example:-
    * CONTEXT has access to the actor system this actor run on top of.
    * CONTEXT has access to this actor's own actor reference, which is called SELF.
    * So, CONTEXT.self is the actor reference of this actor. self is same as this in OOP.
    * case message: String => println(s"[${context.self}] I have received a STRING:- $message")
    * context.self === 'this' in OOPs// we can use self, context.self, context.self.path*/

  case class SendMessageToYourself(content: String)

  simpleActor ! SendMessageToYourself("I am an actor and I am proud of it")
  /* this matches case SendMessageToYourself, then self was told the contents of the message,
   * which then invoked the (case message handler again)
  * */

  /** * 3. actors can REPLY to messages by using their context */
  val alice: ActorRef = system.actorOf(Props[SimpleActor], "alice")
  val bob: ActorRef = system.actorOf(Props[SimpleActor], "bob")

  case class SayHiTo(ref: ActorRef) //these actor references are used by Akka to know which actor to send messages to.

  alice ! SayHiTo(bob) // make alice and bob communicate with each other

  /** 4. Dead letters */
  //  alice ! "Hi" // alice tries to send Hi to null, Actor.noSender: ActorRef which is null, Dead letters encountered.
  /*So this guy from User Alice to user dead letters was not delivered.
  Dead letters encountered.
  So Dead Letters is a fake actor inside AKKA, which takes care to receive the messages that are not sent to anyone.
  This is the garbage pool of messages, if you will.
  So if there is no sender, the reply will go to dead letters.*/

  /** 5. Forwarding Messages */
  // bob sends msg to alice, alice sends the msg to blake as "bob sent $message"
  case class WirelessPhoneMessage(content: String, ref: ActorRef)

  alice ! WirelessPhoneMessage("Hi", bob) //the original sender is the original sender of this invocation, which is no sender.

  /* alice receives this WirelessPhoneMessage with content Hi and reference as bob
  * alice reacts to the WPM by saying bob forward Hi+s*/


  // ---- WHat we learned so far...
  /**
   * Every Actor type derives from Actor trait which has an abstract method called receive that take no parameters
   * and returns a message handler object(Receive) which is an alias of PartialFunction from Any => Unit.
   * receive() method returns the message handler object(Receive), which is retrieved by AKKA when an actor receives a message,
   * and this handler is invoked when the actor actually processes a message on one of the
   * threads managed by the actor system.
   *
   * We need an infrastructure in the form of ActorSystem("AlphaNumericString")
   * Creating an Actor is done via the ActorSystem and not in the traditional way(new constructor):
   * In order to create an actor, we need to call the actorOf factory method from the ActorSystem.
   * We need to pass a Props Object which is a data structure which holds some (Create and Deploy) information,
   * this is some AKKA internals, and then the actor name...
   *
   * The only way we can communicate with an Actor is via sending messages
   * and the way to do that is by invoking the tell method with the message that we want to send.
   * Messages can be of any type as long as they are immutable and serializable.
   *
   * (akka actor principles)
   * Actors are fully encapsulated, we cannot create actor manually, cannot poke their data or directly call methods.
   * Actors run in parallel and they react to messages in a completely non-blocking & asynchronous fashion.
   *
   * (akka actor references)
   * We communicate with Actors by using ActorReference which are Immutable and Serializable.
   * Actors are aware of their iwn references, by using self or context.self.
   * Actors are aware of the actor reference that last sent a message to them (sender)
   * by using sender()/context.sender() and they can use that to reply messages with other messages.
   * */

}


  /** ------------------------------ Exercise 1
   * Create a Counter actor, that holds an Int counter
   * Increment Decrement and Print counter
   * */

object Exercise1 extends App {
  // Put messages that actor supports in actor's companion object
  object Counter {
    case object Increment

    case object Decrement

    case object Print
  }

  class Counter extends Actor {

    import Counter._

    var counter: Int = 0

    override def receive: Receive = {
      case Increment => counter += 1
      case Decrement => counter -= 1
      case Print => println(s"[counter] My current count is:- $counter")
    }
  }
  val system = ActorSystem("CountingSystem")
  val counterActor = system.actorOf(Props[Counter], "counterActor")

  import Counter._

  counterActor ! Increment
  counterActor ! Print
  counterActor ! Increment
  counterActor ! Print
  counterActor ! Decrement
  counterActor ! Decrement
  counterActor ! Print

  (1 to 5) foreach (_ => counterActor ! Increment)
  counterActor ! Print
  (1 to 3) foreach (_ => counterActor ! Decrement)
  counterActor ! Print
}

/** Exercise 2
 * Create a BankAccount as an Actor
 * This Actor will be able to receive some messages to
 * -Deposit an amount and to
 * -Withdraw an amount
 * -Statement
 *
 * Rather than printing things to console,
 * Actor will react to deposit and withdraw by sending back or replying with a Success or Failure of each of these
 * three bank operations....
 *
 * Actor :-
 * receives:
 * -Deposit
 * -Withdraw
 * -Statement
 * replies with:-
 * -Success
 * -Failure
 * */
object Exercise2 extends App {
  object BankAccount {
    case class Deposit(amount: Int)

    case class Withdraw(amount: Int)

    case object Statement

    case class TransactionSuccess(message: String)

    case class TransactionFailure(reason: String)
  }

  class BankAccount extends Actor {

    import BankAccount._

    var balance = 100000

    override def receive: PartialFunction[Any, Unit] = {
      // POINT-2. BankAccount Actor replying to the 4 messages sent by Person Actor
      case Deposit(amount) =>
        if (amount <= 0) sender() ! TransactionFailure("Invalid amount")
        else {
          balance += amount
          sender() ! TransactionSuccess(s"Successfully deposited $amount")
        }

      case Withdraw(amount) =>
        if (balance < amount) sender() ! TransactionFailure("Insufficient Balance")
        else if (amount < 0) TransactionFailure("Invalid withdraw amount")
        else {
          balance -= amount
          sender() ! TransactionSuccess(s"Successfully withdrew $amount")
        }
      case Statement => sender() ! s"Your balance is:- $balance"
    }
  }

  object Person {
    case class LiveTheLife(account: ActorRef) // Actor parameter
  }

  class Person extends Actor {

    import BankAccount._
    import Person.LiveTheLife

    override def receive: Receive = {
      case LiveTheLife(account) =>
        // POINT-1. Person Actor Sending 4 messages to BankAccount Actor
        account ! Deposit(10)
        account ! Withdraw(900000)
        account ! Withdraw(10000)
        account ! Statement
      // POINT-3. PersonActor receives reply's from BankActor and prints them to console.
      case message => println(message.toString)
    }
  }
  val system = ActorSystem("PersonSystem")
  val bankActor = system.actorOf(Props[BankAccount], "bankActor")
  val personActor = system.actorOf(Props[Person], "personActor")

  import Person._
  personActor ! LiveTheLife(bankActor)

  /** Control flow point 1, 2 and 3 */


  /** Questions
   * Can we assume any ordering of the messages?
   * if i sent 10 messages to same actor, can i assume they arrive in particular order
   *
   * Aren't we causing race conditions(if multiple threads are accessing var)?
   * Do we need some synchronization to prevent race conditions on actor's internal state.
   *
   * What does Asynchronous actually mean for actors? */
}
