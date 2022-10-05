package part3testing

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

import scala.util.Random

class TimedAssertionSpec extends TestKit(ActorSystem("TimedAssertionSpec", ConfigFactory.load().getConfig("specialTimedAssertionsConfig")))
  with ImplicitSender with WordSpecLike with BeforeAndAfterAll {

  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  import TimedAssertionSpec._
  import scala.concurrent.duration.DurationInt

  "A worker actor" should {
    val workerActor = system.actorOf(Props[WorkerActor])
    "reply with the meaning of life in a timely manner" in {
      within(500.millis, 1.second) {
        // This is time boxed test, the code here must happen between 500 mill & at most 1 sec.
        workerActor ! "work"
        expectMsg(WorkResult(11))
      }
    }
    "reply with valid work at a reasonable cadence" in {
      within(1.second) {
        workerActor ! "workSequence"
        // receiveWhile will stop once the no. of messages are exceeded.
        val results: Seq[Int] = receiveWhile[Int](max = 2.second, idle = 500.millis, messages = 10) {
          // If we lower any of the max, idle, Thread.sleep time the test won't pass as it wont process messages in less time
          case WorkResult(result) => result
        }
        assert(results.sum > 5) // test will fail if max, idle are lowered as the sum will not be > 5 in that time
      }
    }

    "reply to a test probe ina timely manner" in {
      within(1.second) {
        val probe = TestProbe()
        probe.send(workerActor, "work")
        probe.expectMsg(WorkResult(11))
        // expectMsg timeout is 3seconds Default, let's change that inside application.conf
        // added into ActorSystem(ConfigFactory.load().getConfig("specialTimedAssertionsConfig"))
      }
    }
  }
}

object TimedAssertionSpec {
  case class WorkResult(result: Int)

  class WorkerActor extends Actor {
    override def receive: Receive = {
      case "work" =>
        // long computation
        Thread.sleep(500)
        sender() ! WorkResult(11)
      case "workSequence" =>
        val r = new Random()
        for (_ <- 1 to 10) {
          Thread.sleep(r.nextInt(50))
          sender() ! WorkResult(1)
        }
    }
  }
}
