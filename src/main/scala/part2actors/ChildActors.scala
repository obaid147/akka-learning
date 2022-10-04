package part2actors

import akka.actor.{Actor, ActorRef, ActorSelection, ActorSystem, Props}
//import part2actors.ChildActors.CreditCard.{AttachToAccount, CheckStatus}

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

        // create a new actor inside receive message handler of this actor.
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
  parent ! CreateChild("Child") // After creating child, Parent will change it's message handler to withChild
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
                     If we get an exception here, the whole ActorSystem will be down.
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

    def deposit(funds: Int): Unit = {
      println(s"${self.path} depositing $funds on top of $amount")
      amount += funds
    }
    def withdraw(funds: Int): Unit = {
      println(s"${self.path} withdrawing $funds from $amount")
      amount -= funds
    }
  }

  object CreditCard{
    case class AttachToAccount(bankAccountRef: NaiveBankAccount) // !!
    case object CheckStatus
  }
  class CreditCard extends Actor{
    import CreditCard._
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
  creditCardSelection ! CheckStatus // This should just return a status but it deducts 1 Rs funds from my account
}

object Exercise111 extends App {
  // Distributed word counting

  object WordCounterMaster {
    case class Initialize(nChildren: Int)
    case class WordCountTask(id: Int, text: String)
    case class WordCountReply(id: Int, count: Int)
  }
  class WordCounterMaster extends Actor {
    import WordCounterMaster._
    override def receive: Receive = {
      case Initialize(nChildren) =>
        println("[Master] initializing....")
        val childrenRefs = for(i <- 1 to nChildren) yield context.actorOf(Props[WordCounterWorker], s"wcw$i")
        context.become(withChildren(childrenRefs, 0, 0, Map()))
    }

    def withChildren(childrenRefs: Seq[ActorRef], currentChildIndex: Int, currentTaskID: Int, requestMap: Map[Int, ActorRef]): Receive = {
      case text: String =>
        println(s"[master] i have received $text - I will send it to child $currentChildIndex")
        val originalSender = sender()
        val task = WordCountTask(currentTaskID ,text)
        val childRef = childrenRefs(currentChildIndex)
        childRef ! task
        val newTaskID = currentTaskID + 1
        val nextChildIndex = (currentChildIndex + 1) % childrenRefs.length
        val newRequestMap = requestMap + (currentTaskID -> originalSender)
        context.become(withChildren(childrenRefs, nextChildIndex, newTaskID, newRequestMap))
      case WordCountReply(id, count) =>
        // who should we send count, is it sender() no it is not as last message was received from child actor.
        // We need to keep track of the original Requester. // newTaskID, requestMap
        println(s"[Master] I have received a reply $id with $count")
        val originalSender = requestMap(id)
        originalSender ! count
        context.become(withChildren(childrenRefs, currentChildIndex, currentTaskID, requestMap - id))
    }
  }

  class WordCounterWorker extends Actor {
    import WordCounterMaster._
    override def receive: Receive = {
      case WordCountTask(id, text) =>
        println(s"[Worker] ${self.path} I have received a task $id with $text")
        sender() ! WordCountReply(id, text.split(" ").length)
    }
  }

  class TestActor extends Actor {
    import WordCounterMaster._
    override def receive: Receive = {
      case "go" =>
        val master = counterSystem.actorOf(Props[WordCounterMaster], "Master")
        master ! Initialize(3)
        val texts = List("i love akka", "scala is super dope", "yes", "me too")
        texts.foreach(text => master ! text)
      case count: Int =>
        println(s"[test actor] I received a reply: $count")
    }
  }

  val counterSystem = ActorSystem("RoundRobinWordCountExercise")

  val testActor = counterSystem.actorOf(Props[TestActor], "TestActor")

  testActor ! "go"
  // round robin logic, 1,2,3,4,5 workers will complete 7 tasks like: 1,2,3,4,5,1,2*/
}
