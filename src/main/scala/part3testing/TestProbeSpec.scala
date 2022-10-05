package part3testing

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
/** Flow steps 1, 2 ......*/

/** Test Probes are useful for interactions with multiple actors
 * Creating TestProbe; val probe = TestProbe("TestProbeName")
 * TestProbes are actors with assertion capabilities
 * They can send message, probe.send(actorUnderTest, "a message")
 * They can reply with a message to probes last sender, probe.reply("a message")
 * TestProbe has same assertions as the testActor
 * It can watch when other Actors STOP*/
class TestProbeSpec extends TestKit(ActorSystem("TestProbeSpec"))
  with ImplicitSender with WordSpecLike with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import TestProbeSpec._

  "A master actor" should {
    "register a worker" in {
      val master = system.actorOf(Props[Master], "master")
      val worker = TestProbe("worker") // TestProbe is a special actor with some assertion capabilities.

      master ! Register(worker.ref) // ref is the actor reference within this TestProbe
      expectMsg(RegistrationACK)
    }
  }

  "A master actor" should {
    "send the work to worker actor" in {
      val master = system.actorOf(Props[Master])
      val worker = TestProbe("worker")
      master ! Register(worker.ref)
      expectMsg(RegistrationACK)

      val workLoadString = "I Love Akka"
      master ! Work(workLoadString) /**1 Master Receives Work*/

      /**Testing the interaction b/w Master and Worker(TestProbe ) Actor*/
      worker.expectMsg(WorkerWork(workLoadString, testActor)) /**2 Master forwards the WorkerWork to TestProbe*/
      // testActor => originalRequester
      //testActor is member of testkit which is implicitly passed as a sender of every message that we send to
      //actors under test
      worker.reply(WorkCompleted(3, testActor)) /**3 TestProbe replies with WorkCompleted */

      /**4 Reaction to WorkCompleted MasterActor sends back Report(3) to our testActor which we are asserting below*/
      expectMsg(Report(3))
    }

    // test masterActor with 2 pieces of work
    "aggregate data correctly" in {
      val master = system.actorOf(Props[Master]) //1 We Create Master
      val worker = TestProbe("worker")//2 We create testProbe
      master ! Register(worker.ref)//3 We Register the worker to the master
      expectMsg(RegistrationACK) //4 We expect the RegistrationAck

      val workLoadString = "I Love Akka"
      //5 We send master 2 Works messages
      master ! Work(workLoadString) // Report(3)
      master ! Work(workLoadString) // Report(6)

      // in the mean time, i don't have Worker
      worker.receiveWhile() {
            // `` means exact same, As long as Worker receives this exact message
        case WorkerWork(`workLoadString`, `testActor`) => worker.reply(WorkCompleted(3, testActor))
      }

      //6 And the we expect Report 3 and 6 in return. and in the mean time,
      // We are programming the slave to assert that it only receives WorkerWork with workLoadString & testActor
      // If it does, We will reply, we make testProbe reply with WorkCompleted(3. testActor),
      // Which then the master receives and then it replies to the testActor with Report(3) & Report(6)
      expectMsg(Report(3))
      expectMsg(Report(6))
    }
  }


}

object TestProbeSpec {
  /*
  send work to master
    master send worker piece of work
    worker aggregates the results
  master send total word count to original requester*/
  case object RegistrationACK

  case class Register(workerRef: ActorRef)

  case class WorkerWork(text: String, originalRequester: ActorRef)

  case class Work(text: String)

  case class WorkCompleted(count: Int, originalRequester: ActorRef)

  case class Report(count: Int)

  class Master extends Actor {
    override def receive: Receive = {
      case Register(workerRef) =>
        sender() ! RegistrationACK
        context.become(online(workerRef, 0))
      case _ => //
    }

    def online(workerRef: ActorRef, totalWordCount: Int): Receive = {
      case Work(text) => workerRef ! WorkerWork(text, sender())
      case WorkCompleted(count, originalRequester) =>
        val newTotalWordCount = totalWordCount + count
        originalRequester ! Report(newTotalWordCount)
        context.become(online(workerRef, newTotalWordCount))
    }
  }

  // class Worker extends Actor // which we do not need....
}
