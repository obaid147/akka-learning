package part5infra

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}

import scala.concurrent.{ExecutionContext, Future}

object Dispatchers extends App {

  class Counter extends Actor with ActorLogging {
    var count = 0

    override def receive: Receive = {
      case message =>
        count += 1
        log.info(s"[$count] $message")
    }
  }

  val system = ActorSystem("DispatchersDemo") // , ConfigFactory.load().getConfig("dispatchersDemo")

  /** Method -1 Programmatic/in code */
  /*val actors =
    for (i <- 1 to 10) yield system.actorOf(Props[Counter].withDispatcher("my-dispatcher"), s"counter_$i")

  val r = new Random()
  for (i <- 1 to 1000) actors(r.nextInt(10)) ! i*/
  // if we set fixed-pool-size to 1, this will be single threaded and counter is scheduled 30times in a row for 30 msg


  /** Method -2 from config */
  val rwjvmActor = system.actorOf(Props[Counter], "rwjvm") // not testing as it will yield same result as above.

  /** Dispatcher implements the ExecutionContext trait */
  // from TimersSchedulers.scala, we imported system.dispatcher as implicit executionContext for scheduling things.

  // Actor that run future inside of it when it receives a message
  class DBActor extends Actor with ActorLogging {
    // solution -1 using dedicated dispatcher
    implicit val executionContext: ExecutionContext = context.system.dispatchers.lookup("my-dispatcher")
    // solution -2 use Router

    override def receive: Receive = {
      case message =>
        Future { // This future will run on context dispatcher
          // wait on a resource
          Thread.sleep(5000)
          log.info(s"Success, $message")
        }
    }
  }

  val dbActor = system.actorOf(Props[DBActor], "DBActor")
  //  dbActor ! "Hey there DB Actor"

  val nonblockingActor = system.actorOf(Props[Counter])
  for (i <- 1 to 1000) {
    val msg = s"important message $i"
    dbActor ! msg
    nonblockingActor ! msg
  }

}
