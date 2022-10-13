package part5infra

import akka.actor.{Actor, ActorLogging, ActorSystem, Props, Timers}

//import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.duration.DurationInt

object TimersSchedulers extends App {
  class SimpleActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  val system = ActorSystem("SchedulersTimersDemo")
  val simpleActor = system.actorOf(Props[SimpleActor])
  // // implicit val executionContext: ExecutionContextExecutor = system.dispatcher // implements the ExecutionContext interface

  import system.dispatcher // no need of implicit val or EC
  //  system.log.info("Scheduling reminder for SimpleActor")
  //  system.scheduler.scheduleOnce(1.second) {
  //    simpleActor ! "reminder"
  //  }
  //
  //  val routine: Cancellable = system.scheduler.schedule(1.second, 2.seconds){ // Repeated scheduler
  //    simpleActor ! "heart beat"
  //  } // A cancellable can be cancelled
  //
  //  system.scheduler.scheduleOnce(5.seconds){
  //    routine.cancel()
  //  } 

  system.log.info("----------------")

  /*class SelfClosingActor extends Actor with ActorLogging {
    var sch: Cancellable = createTimeoutWindow()

    def createTimeoutWindow(): Cancellable = {
      context.system.scheduler.scheduleOnce(1.second) {
        self ! "timeout"
      }
    }

    override def receive: Receive = {

      case "timeout" =>
        log.info("Stopping myself")
        context.stop(self)
      case message =>
        log.info(s"Received message $message staying alive")
        sch.cancel()
        sch = createTimeoutWindow()
    }
  }

  val selfClosingActor = system.actorOf(Props[SelfClosingActor], "SelfClosingActor")*/

//  system.scheduler.scheduleOnce(250.millis) {
//    selfClosingActor ! "Ping"
//  }
//  system.scheduler.scheduleOnce(2.seconds) {
//    system.log.info("sending pong to the self-closing actor")
//    selfClosingActor ! "pong"
//  }

  /** TIMER */
  case object TimerKey
  case object Start
  case object Reminder
  case object Stop

  class TimerBasedHeartBeatActor extends Actor with ActorLogging with Timers {
    timers.startSingleTimer(TimerKey, Start, 500.millis)

    override def receive: Receive = {
      case Start =>
        log.info("BootStrapping")
        timers.startPeriodicTimer(TimerKey, Reminder, 1.second)
      case Reminder => log.info("I am alive")
      case Stop =>
        log.warning("Stopping!")
        timers.cancel(TimerKey)
        context.stop(self)
    }
  }

  val timerHeartBeatActor = system.actorOf(Props[TimerBasedHeartBeatActor], "timerActor")
  system.scheduler.scheduleOnce(5.seconds) {
    timerHeartBeatActor ! Stop
  }
}
