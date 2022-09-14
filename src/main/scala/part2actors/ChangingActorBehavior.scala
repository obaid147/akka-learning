package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import part2actors.ChangingActorBehavior.Mom.MomStart

object ChangingActorBehavior extends App{

  object FussyKid{
    case object KidAccept
    case object KidReject
    val HAPPY = ":)"
    val SAD = ":`("
  }
  class FussyKid extends Actor{
    import FussyKid._
    import Mom._

    var kidState = HAPPY // internal state of kid
    override def receive: Receive = {
      case Food(VEGETABLE) => kidState = SAD
      case Food(CHOCOLATE) => kidState = HAPPY
      case Ask(_) =>
        if(kidState == HAPPY) sender() ! KidAccept
        else sender() ! KidReject
    }
  }

  // Creating a stateless FussyKid ie:- without using var(mutable)...
  class StatelessFussyKid extends Actor{
    import FussyKid._
    import Mom._
    override def receive: Receive = {
      happyReceive
    }

    def happyReceive: Receive = {
      case Food(VEGETABLE) => context.become(sadReceive)//change my receive handler to sadReceive because state is HAPPY
      case Food(CHOCOLATE) => // stay happy
      case Ask(_) => sender() ! KidAccept
    }
    def sadReceive: Receive = {
      case Food(VEGETABLE) => // stay sad
      case Food(CHOCOLATE) => context.become(happyReceive)//change my receive handler to happyReceive because state is SAD
      case Ask(_) => sender() ! KidReject
    }
  }


  object Mom{
    case class MomStart(kidRef: ActorRef)
    case class Food(food: String)
    case class Ask(message: String) // question
    val VEGETABLE = "veggies"
    val CHOCOLATE = "chocolate"
  }
  class Mom extends Actor{
    import FussyKid._
    import Mom._
    override def receive: Receive = {
      case MomStart(kidRef) =>
        // test our interaction
        kidRef ! Food(VEGETABLE)
        kidRef ! Ask("Do you want to play?")
      case KidAccept => println("Yay, my kid is happy")
      case KidReject => println("My kid is sad, but at least she is healthy!")
    }
  }

  val system = ActorSystem("changingActorBehaviorDemo")

  val fussyKidActor = system.actorOf(Props[FussyKid], "fussyKidActorRef")
  val momActor = system.actorOf(Props[Mom], "MomActorRef")
  momActor ! MomStart(fussyKidActor)

  val statelessFussyKid = system.actorOf(Props[StatelessFussyKid], "statelessFussyKid")
  momActor ! MomStart(statelessFussyKid)
  /*
    mom receives MomSTart
      kid receives Food(veg) -> kid will change handler to sadReceive
      kid receives Ask(play?) -> kid replies with sadReceive handler =>
    mom receives KidReject
  */
}
