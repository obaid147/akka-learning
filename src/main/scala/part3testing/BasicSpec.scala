package part3testing

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

import scala.concurrent.duration._
import scala.util.Random

class BasicSpec extends TestKit(ActorSystem("BasicSpec"))
  with ImplicitSender
  with WordSpecLike
  with BeforeAndAfterAll {

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
      case message: String => sender() ! message.toUpperCase
    }
  }

}
