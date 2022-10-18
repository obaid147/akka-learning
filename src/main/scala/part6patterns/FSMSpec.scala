package part6patterns

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Cancellable, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{DurationDouble, DurationInt}

class FSMSpec extends TestKit(ActorSystem("ActorSytemFSM")) with ImplicitSender with WordSpecLike with BeforeAndAfterAll{
  // Finite state machine
  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  import FSMSpec._
  "A vending machine" should {
    "error when not initialized" in {
      val vendingMachine = system.actorOf(Props[VendingMachine])
      vendingMachine ! RequestProduct("coke")
      expectMsg(VendingError("Machine not initialized"))

    }

    "report a product not available" in {
      val vendingMachine = system.actorOf(Props[VendingMachine])
      vendingMachine ! Initialize(Map("coke" -> 10), Map("coke" -> 20))
      vendingMachine ! RequestProduct("pepsi")
      expectMsg(VendingError("Product not available"))
    }

    "throw a timeout if i don't insert money" in {
      val vendingMachine = system.actorOf(Props[VendingMachine])
      vendingMachine ! Initialize(Map("coke" -> 10), Map("coke" -> 30))
      vendingMachine ! RequestProduct("coke")
      expectMsg(Instruction(s"Please insert 30 rupees"))
      within(1.5.seconds) {
        expectMsg(VendingError("Request TimedOut"))
      }
    }

    "handle the reception partial money" in {
      val vendingMachine = system.actorOf(Props[VendingMachine])
      vendingMachine ! Initialize(Map("coke" -> 10), Map("coke" -> 30))
      vendingMachine ! RequestProduct("coke")
      expectMsg(Instruction(s"Please insert 30 rupees"))

      vendingMachine ! ReceiveMoney(20) // inserting less money
      expectMsg(Instruction("please insert remaining money:- 10 rupees"))

      within(1.5.seconds) {
        expectMsg(VendingError("Request TimedOut"))
        expectMsg(GiveBackChange(20))
      }
    }

    "deliver the product if i insert all money" in {
      val vendingMachine = system.actorOf(Props[VendingMachine])
      vendingMachine ! Initialize(Map("coke" -> 10), Map("coke" -> 40))
      vendingMachine ! RequestProduct("coke")
      expectMsg(Instruction(s"Please insert 40 rupees"))

      vendingMachine ! ReceiveMoney(40) // inserting less money
      expectMsg(Deliver("coke"))
    }

    "give back change and be able to request money for new product" in {
      val vendingMachine = system.actorOf(Props[VendingMachine])
      vendingMachine ! Initialize(Map("coke" -> 10), Map("coke" -> 30))
      vendingMachine ! RequestProduct("coke")
      expectMsg(Instruction(s"Please insert 30 rupees"))

      vendingMachine ! ReceiveMoney(40)
      expectMsg(Deliver("coke"))
      expectMsg(GiveBackChange(10))

      vendingMachine ! RequestProduct("coke")
      expectMsg(Instruction(s"Please insert 30 rupees"))
    }
  }
}

object FSMSpec {
  /*
  Vending Machine
  */

  case class Initialize(inventory: Map[String, Int], prices: Map[String, Int])
  case class RequestProduct(product: String)
  case class Instruction(instruction: String) // message VM will show on its screen
  case class ReceiveMoney(amount: Int)
  case class Deliver(product: String)
  case class GiveBackChange(amount: Int)

  case class VendingError(reason: String)
  case object ReceiveMoneyTimeout



  class VendingMachine extends Actor with ActorLogging {
    implicit val executionContext: ExecutionContext = context.dispatcher

    override def receive: Receive = idle

    def idle: Receive = {
      case Initialize(inventory, prices) => context.become(operational(inventory, prices))
      case _ => sender() ! VendingError("Machine not initialized")
    }

    def operational(inventory: Map[String, Int], prices: Map[String, Int]): Receive = {
      case RequestProduct(product) =>
        inventory.get(product) match {
          case None | Some(0) => sender() ! VendingError("Product not available")
          case Some(_) =>
            val price = prices(product)
            sender() ! Instruction(s"Please insert $price rupees")
            context.become(waitForMoney(inventory, prices, product, 0, startReceiveMoneyTimeoutSchedule, sender()))
        }
    }

    def waitForMoney(inventory: Map[String, Int],
                     prices: Map[String, Int],
                     product: String,
                     money: Int,
                     moneyTimeoutSchedule: Cancellable,
                     requester: ActorRef): Receive = {
      case ReceiveMoneyTimeout =>
        requester ! VendingError("Request TimedOut")
        if (money > 0) requester ! GiveBackChange(money)
        context.become(operational(inventory, prices))
      case ReceiveMoney(amount) =>
        moneyTimeoutSchedule.cancel()
        val price = prices(product)
        if (money + amount >= price) {
          // user buys product
          requester !  Deliver(product)

          // deliver change
          if (money + amount - price > 0) requester ! GiveBackChange(money + amount - price)

          // update inventory
          val newStock = inventory(product) - 1
          val newInventory = inventory + (product -> newStock)
          context.become(operational(newInventory, prices))
        } else {
          val remainingMoney = price - money - amount
          requester ! Instruction(s"please insert remaining money:- $remainingMoney rupees")
          context.become(waitForMoney(
            inventory, prices, product, // don't change
            money+amount, // user has inserted some money
            startReceiveMoneyTimeoutSchedule, // I need to set the timeout again
            requester))
        }
    }

    def startReceiveMoneyTimeoutSchedule: Cancellable = {
      context.system.scheduler.scheduleOnce(1.second){
        self ! ReceiveMoneyTimeout
      }
    }
  }

}
