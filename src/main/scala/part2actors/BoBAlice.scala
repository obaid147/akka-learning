package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
object BoBAlice extends App {
  case class SayHiTo(s: ActorRef)
  case class WirelessPhoneMessage(content: String, ref: ActorRef)

  class SimpleActor extends Actor {
    override def receive: PartialFunction[Any, Unit] = {
      case "Hi" =>
        print(s"From  " + self.path.name + " actor: ")
        println("Hi")
        sender() ! "Hello, there"
      case message: String =>
        print(s"From  " + self.path.name + " actor: ")
        println("Simple String message")
        message
      case SayHiTo(ref) =>
        print(s"From " + self.path.name + " actor: ")
        println("SayHiToRef")
        ref ! "Hi"

      case WirelessPhoneMessage(content, ref) =>
        print(s"From " + self.path.name + " actor: ")
        println("forwarding..")
        ref forward content

    }
  }

  val system = ActorSystem("actorCapabilitiesDemo")
  val alice: ActorRef = system.actorOf(Props[SimpleActor], "alice")
  val bob: ActorRef = system.actorOf(Props[SimpleActor], "bob")

  alice ! SayHiTo(bob)
  //alice ! WirelessPhoneMessage("Hi", bob) // tell , fire and forget
  //alice.tell("Hi", ActorRef.noSender)

  /*implicit val to: Timeout = akka.util.Timeout(4.seconds)
  val result: Future[Any] = alice.ask("Hi")
  println(result)
  println("hello")*/
}