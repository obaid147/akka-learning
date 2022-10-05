package part3testing

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.testkit.{EventFilter, ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

class InterceptingLogsSpec extends TestKit(ActorSystem("InterceptingLogsSpec",
  ConfigFactory.load().getConfig("interceptingLogMessages")))
  with ImplicitSender with WordSpecLike with BeforeAndAfterAll {

  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  /* our CheckoutActor has private states, it does not share or receive anything from outside which is not good for tests*/
  /** TestKit has created EVENT FILTERS for this situation */

  import InterceptingLogsSpec._

  val item = "Akka Essentials"
  val creditCard = "1234-1234-1234-1234"
  val invalidCreditCard = "0000-0000-0000-0000"
  "A checkout flow" should {
    "correctly log dispatch of an order" in {
      // EvenFilter. Even though we cannot use Actor to test, we can use log messages to do so
      /** EventFilter.info() creates an EventFilter object which searches for log messages at INFO level */
      EventFilter.info(pattern = s"Order [0-9]+ item $item has been dispatched."/*, occurrences = 1*/) intercept {
        val checkoutRef = system.actorOf(Props[CheckoutActor])
        checkoutRef ! Checkout(item, invalidCreditCard)
      }
    }

    "freak out if payment is denied" in {
      EventFilter[RuntimeException](occurrences = Int.MaxValue) intercept {
        val checkoutRef = system.actorOf(Props[CheckoutActor])
        checkoutRef ! Checkout(item, creditCard)
      }
    }
  }

}

object InterceptingLogsSpec {

  case class Checkout(item: String, creditCard: String)

  case class AuthorizeCard(creditCard: String)

  case object PaymentAccepted

  case object PaymentDenied

  case object OrderConfirmed

  case class DispatchOrder(item: String)

  class CheckoutActor extends Actor {
    private val paymentManager = context.actorOf(Props[PaymentManager])
    private val fulfillmentManager = context.actorOf(Props[FulfillmentManager])

    override def receive: Receive = awaitingCheckout

    def awaitingCheckout: Receive = {
      case Checkout(item, card) =>
        paymentManager ! AuthorizeCard(card)
        context.become(pendingPayment(item))
    }

    def pendingPayment(item: String): Receive = {
      case PaymentAccepted =>
        fulfillmentManager ! DispatchOrder(item)
        context.become(pendingFulfilment())
      case PaymentDenied => throw new RuntimeException("I can't handle this!!!!!!!!")
    }

    def pendingFulfilment(): Receive = {
      case OrderConfirmed => context.become(awaitingCheckout)
    }
  }

  class PaymentManager extends Actor {
    override def receive: Receive = {
      case AuthorizeCard(card) =>
        if (card.startsWith("0")) sender() ! PaymentDenied
        else {
          sender() ! PaymentAccepted
        }
    }
  }

  class FulfillmentManager extends Actor with ActorLogging {

    override def receive: Receive = active(0)
    def active(orderId: Int): Receive = {
      case DispatchOrder(item: String) =>
        active(orderId + 1)
        log.info(s"Order $orderId for item $item has been dispatched.")
        sender() ! PaymentAccepted
    }
  }
}
