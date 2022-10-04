package part3testing

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

import scala.concurrent.duration._
import scala.util.Random

class BasicSpec extends TestKit(ActorSystem("BasicSpec"))
  with ImplicitSender
  with WordSpecLike // should, in
  /**WordSpecLike is a class that facilitates a “behavior-driven” style of development (BDD),
   * in which tests are combined with text that specifies the behavior the tests verify.
   * If you need to mix the behavior of WordSpec into some other class, you can use this trait instead,
   * because class WordSpec does nothing more than extend this trait and add a nice toString implementation.*/

  with BeforeAndAfterAll {
  /**BeforeAndAfterAll is a Stackable trait that can be mixed into suites that need methods invoked
      before and after executing the suite.
   * BeforeAndAfterAll trait allows code to be executed before and/or after all the tests and nested
   * suites of a suite are run. This trait overrides run and calls the beforeAll method, then calls super.run.
   * After the super.run invocation completes, whether it returns normally or completes abruptly with an exception,
   * this trait's run method will invoke afterAll. */

  override def afterAll(): Unit = { // after ActorSystem is DONE. TearDown method.
    TestKit.shutdownActorSystem(system) // system is a member of TestKit
  }

  import BasicSpec._

  "A Simple Actor" should { // test suite1
    "send back the same message" in { // test1
      val echoActor = system.actorOf(Props[SimpleActor])
      val message = "hello, test"
      echoActor ! message

      expectMsg(message) // akka.
    }
  }

  /** WHO IS at the receiving END of these expecting assertions? We are sending message, but who is the sender!!!???
   * testActor is passed implicitly as the sender of every message we send
   * because we mixed-in the Implicit Sender trait */

  "A Black Hole Actor" should {
    "send back some message" in {
      val blackHole = system.actorOf(Props[BlackHole])
      val message = "hello, test"
      blackHole ! message

      //  expectMsg(message) // fails if a timeout of 3 seconds or configured timeout
      expectNoMessage(1.second)
    }
  }

  "A Lab Test Actor" should {
    val labTestActor = system.actorOf(Props[LabTestActor])

    "turn a string to uppercase" in {
      labTestActor ! "i love akka"
      //      expectMsg("I LOVE AKKA")
      val reply = expectMsgType[String]

      assert(reply == "I LOVE AKKA")
    }

    "reply to greeting" in {
      labTestActor ! "greeting"
      expectMsgAnyOf("Hi", "Hello")
    }
    "reply with favorite tech" in {
      labTestActor ! "favoriteTech"
      expectMsgAllOf("Scala", "Akka")
    }
    "reply with cool tech in a different way" in {
      labTestActor ! "favoriteTech"
      val messages = receiveN(2) // Seq[Any] if we receive less than 2 messages in within 3 seconds, this test will fail.
      // free to do more complicated assertions
    }

    "reply with cool tech in a fancy way" in {
      labTestActor ! "favoriteTech"
      expectMsgPF() { // only care that PartialFunction is defined for the messages, This is the most powerful test suite...
        case "Scala" =>
        case "Akka" =>
      }
    }
  }

}

object BasicSpec {
  class SimpleActor extends Actor {
    override def receive: Receive = {
      case message => sender() ! message
    }
  }

  class BlackHole extends Actor {
    override def receive: Receive = Actor.emptyBehavior
  }

  class LabTestActor extends Actor {
    val random = new Random()
    override def receive: Receive = {
      case "greeting" =>
        if (random.nextBoolean()) sender() ! "Hi"
        else sender() ! "Hello"
      case "favoriteTech" =>
        sender() ! "Scala"
        sender() ! "Akka"
      case message: String => sender() ! message.toUpperCase
    }
  }

}
