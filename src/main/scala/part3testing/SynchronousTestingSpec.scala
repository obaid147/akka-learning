package part3testing

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{CallingThreadDispatcher, TestActorRef, TestProbe}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import scala.concurrent.duration.Duration

class SynchronousTestingSpec extends WordSpecLike with BeforeAndAfterAll{

  implicit val system: ActorSystem = ActorSystem("SynchronousTestingSpec")

  override def afterAll(): Unit = system.terminate()
  import SynchronousTestingSpec._
  "A counter" should {
    "synchronously increase it's counter" in {
      val counter = TestActorRef[Counter](Props[Counter]) // make system implicit
      counter ! Inc // counter has already received the message.
      /* sending this message is already happening on the same thread ie: the*/ /**CALLING THREAD*/

      assert(counter.underlyingActor.count == 1)
    }
    "synchronously increase its counter at the call of the receive function" in {
      val counter = TestActorRef[Counter](Props[Counter]) // make system implicit
      counter.receive(Inc)
      assert(counter.underlyingActor.count == 1)
    } // Above two tests are the same, happening on the same thread...

    "work on the calling thread dispatcher" in {
      /**Config this actor to run on the calling thread dispatcher
       * This means whatever message we send to counter, will happen on CALLING THREAD*/

      val counter = system.actorOf(Props[Counter].withDispatcher(CallingThreadDispatcher.Id))
      /** If we remove withDispatcher, counter will work asynchronously which means
       * probe.send(counter, Read) will take some time for the probe to receive the message 0
       * which means probe.expectMsg(Duration.Zero, 0) will fail as the probe ha to wait but it's duration is zero.
       */
      val probe = TestProbe()
      probe.send(counter, Read)
      // the probe has already received the count reply. as every interaction with counter happens on probe's CALLING THREAD
      probe.expectMsg(Duration.Zero, 0)
      // the probe will have already received message 0 because probe.send has already happened.
    }
  }
}

object SynchronousTestingSpec {
  case object Inc
  case object Read


  class Counter extends Actor {
    /*override def receive: Receive = vary(0)

    def vary(count: Int): Receive = {
      case Inc => vary(count + 1)
      case Read => sender() ! count
    }*/
    var count = 0
    override def receive: Receive = {
      case Inc => count += 1
      case Read => sender() ! count
    }
  }
}
