package playground

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

object MultiThreadingRecap extends App{
  // creating a new thread
  /*val aThread = new Thread(new Runnable {
    override def run(): Unit = println("I am running in parallel")
  })*/
  val aThread = new Thread(() => println("I am running in parallel"))
  aThread.start()
  aThread.join()

  val threadHey = new Thread( () => (1 to 1000).foreach(_ => println("Hey"))) // run()
  val threadBye = new Thread( () => (1 to 1000).foreach(_ => println("Byeeeeee"))) // run()

  threadHey.start()
  threadBye.start()

  // diff run() produce diff problems!
  class BankAccount(@volatile private var amount: Int){
    def withdraw(money: Int) = this.amount -= money
    def withdrawSafe(money: Int) = this.synchronized{
      this.amount -= money
    }
  }
  /* BA(10000) balance
  *  Thread1 -> Withdraw(1000)
  *  Thread2 -> Withdraw(2000)
  *  Threads should block to complete read and write operation
  *  reading the amount to withdraw and writing to update the balance
  *  But
  *  Thread1 -> this.amount -= .... // preempted by the OS, prevented by os to work at the moment.
  *  Thread2 -> this.amount -= 2000 // balance is 8000
  *  Thread1 -> this.amount -= 1000 // balance 9000 because this.amount in Thread1 was 10000
  *  result => 9000
  *  This is not Atomic(thread safe) using Synchronized
  * */
  // @volatile only solve the Atomic(read) part we still need Synchronized for write part.

  // inter thread communication on JVM
  //  1. wait-notify mechanism

  // Scala Futures
  import scala.concurrent.Future
  import scala.concurrent.ExecutionContext.Implicits.global
  // We create Futures by calling the apply method from companion object of Future type
  val future = Future { // different thread
    Thread.sleep(500)
    10
  }
  // callbacks
  future.onComplete{
    case Success(10) => println("success")
    case Failure(_) => println("something wrong")
  }

  //monadic
  val aProcessFuture = future.map(_+1)

  val aFlatFuture = future.flatMap{ value =>
    Thread.sleep(200)
    Future(value + 10)
  }

  val aFilteredFuture = future.filter(_ % 2 == 0) // either
  println(aFilteredFuture)

  Thread.sleep(2000)
  val aNonsenseFuture = for{
    futureRes <- future
    filteredRes <- aFilteredFuture
  } yield futureRes + filteredRes

  println(Await.result(aNonsenseFuture, 5.second))
  print(aNonsenseFuture)
  Thread.sleep(3000)

  // andThe, recover, recoverWIth

  // Promises no need to much on this
}
