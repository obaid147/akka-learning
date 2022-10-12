package part4faulttolerance

import akka.actor.SupervisorStrategy.{Escalate, Restart, Resume, Stop}
import akka.actor.{Actor, ActorRef, ActorSystem, AllForOneStrategy, OneForOneStrategy, Props, SupervisorStrategy, Terminated}
import akka.testkit.{EventFilter, ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

class SupervisionSpec extends TestKit(ActorSystem("SupervisionSpec")) with ImplicitSender
  with WordSpecLike with BeforeAndAfterAll{

  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  import SupervisionSpec._

  "a supervisor" should {
    "resume its child in case of a minor fault" in {
      val supervisor = system.actorOf(Props[Supervisor])
      supervisor ! Props[FussWordCount]
      val child = expectMsgType[ActorRef]

      child ! "I LOVE AKKA"
      child ! Report
      expectMsg(3)

      child ! "Akka is awesome because i am learning to think in a whole new way"
      child ! Report // RuntimeException => Resume
      expectMsg(3)
    }

    "restart its child in case of an empty sentence" in {
      val supervisor = system.actorOf(Props[Supervisor])
      supervisor ! Props[FussWordCount]
      val child = expectMsgType[ActorRef]

      child ! "I LOVE AKKA"
      child ! Report
      expectMsg(3)

      child ! "" // NullPointerException => Restart
      child ! Report
      expectMsg(0)
    }

    "terminate its child in case of a major error" in {
      val supervisor = system.actorOf(Props[Supervisor])
      supervisor ! Props[FussWordCount]
      val child = expectMsgType[ActorRef]

      watch(child) // will receive Terminated
      child ! "i Love Akka" // IllegalArgumentException => Stop
      val terminatedMessage = expectMsgType[Terminated]
      assert(terminatedMessage.actor == child)
    }

    "escalate an error when it doesn't know what to do" in {
      val supervisor = system.actorOf(Props[Supervisor], "Supervisor")
      supervisor ! Props[FussWordCount]
      val child = expectMsgType[ActorRef]

      watch(child)
      child ! 10 // Escalate
      val terminatedMessage = expectMsgType[Terminated]
      assert(terminatedMessage.actor == child)
    }
  }

  "a kinder supervisor" should {
    "not kill children in case its restarted or escalates failure" in {
      val supervisor = system.actorOf(Props[NoDeathOnRestartSupervisor], "NoDeathSupervisor")
      supervisor ! Props[FussWordCount]
      val child = expectMsgType[ActorRef]

      child ! "Akka is cool"
      child ! Report
      expectMsg(3)

      child ! 49
      child ! Report
      expectMsg(0)
    }
  }

  "An all-for-one supervisor" should {
    "apply all-for-one strategy" in {
      val supervisor = system.actorOf(Props[AllForOneSupervisor], "AllForOneSupervisor")
      supervisor ! Props[FussWordCount]
      val child1 = expectMsgType[ActorRef]

      supervisor ! Props[FussWordCount]
      val child2 = expectMsgType[ActorRef]

      child2 ! "Testing Supervision"
      child2 ! Report
      expectMsg(2)

      EventFilter[NullPointerException]() intercept {
        child1 ! ""
      }
      Thread.sleep(500)
      child2 ! Report // AllForOne means it will affect all the actor and not only the one who sent the message
      expectMsg(0)
    }
  }

}

object SupervisionSpec {

  class Supervisor extends Actor {
    override val supervisorStrategy: SupervisorStrategy = OneForOneStrategy(){
      case _ : NullPointerException => Restart
      case _ : IllegalArgumentException => Stop
      case _ : RuntimeException => Resume
      case _ : Exception => Escalate
    }

    override def receive: Receive = {
      case props: Props =>
        val childRef = context.actorOf(props)
        sender() ! childRef
    }
  }

  class NoDeathOnRestartSupervisor extends Supervisor {
    override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
      // empty
    }
  }

  class AllForOneSupervisor extends Supervisor {
    override val supervisorStrategy = AllForOneStrategy() {
      case _: NullPointerException => Restart
      case _: IllegalArgumentException => Stop
      case _: RuntimeException => Resume
      case _: Exception => Escalate
    }
  }

  case object Report
  class FussWordCount extends Actor {
    var words = 0
    override def receive: Receive = {
      case "" => throw new NullPointerException("sentence is empty")
      case sentence: String =>
        if (sentence.length > 20) throw new RuntimeException("sentence is too big")
        else if (! Character.isUpperCase(sentence(0))) throw new IllegalArgumentException("sentence must start with uppercase")
        else words += sentence.split(" ").length
      case Report => sender() ! words
      case _ => throw new Exception("can only receive strings")
    }
  }
}
