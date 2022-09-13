package part1recap

object ThreadModelLimitations extends App{
  /** 1. OOP encapsulation is only available in single threaded model.
   * */

  // 1
  class BankAccount(private var amount: Int) {
    override def toString: String = ""+amount
    def withdraw(money: Int) = this.synchronized{
      this.amount -= money
    }
    def deposit(money: Int) = this.synchronized{
      this.amount += money
    }
    def getBalance: Int = amount
  }

//  val account = new BankAccount(2000)
//  for(_ <- 1 to 1000){
//    new Thread(() => account.withdraw(1)).start()
//  }
//  for (_ <- 1 to 1000) {
//    new Thread(() => account.deposit(1)).start()
//  }
//
//  println(account.getBalance)
  // OOP Encapsulation is broken in MULTI THREADED Environment
  // Synchronization locks to the rescue

  /** 2. delegating something to a thread is a pain.
   * */
  // you have a running thread and want to pass runnable to that thread?

  // consumers
  var task: Runnable = null
  val runningThread: Thread = new Thread(() => {
    while (true){
      while(task == null){
        runningThread.synchronized{
          println("BG waiting for a task")
          runningThread.wait() // waiting for a task from Main
        }
      }
      task.synchronized{
        println("BG i have a task") // task given
        task.run() // running the task
        task = null // waiting for task again
      }
    }
  })

  // Producer
  def delegateToBGThread(runnable: Runnable) = {
    if (task == null) task = runnable
    runningThread.synchronized{
      runningThread.notify()
    }
  }

  /*runningThread.start()
  Thread.sleep(500)
  delegateToBGThread(() => println(100)) // task given
  Thread.sleep(1000)
  delegateToBGThread(() => println("This should run in the BG(Background)"))*/

  /**3. Tracing and dealing with errors in MultiThreaded  env is a pain*/
  // computer sum of first 1M numbers in between 10Threads.

  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.Future
  val futures =
    (0 to 9).map{ i =>
      100000 * i until 100000 * (i+1) // 0 - 99999, 100000 - 199999, 200000 - 299999 etc
       }.map{range =>
          Future{
            Thread.sleep(500)
            if (range.contains(65124)) throw new RuntimeException("Invalid Number")
            range.sum
          }
        }

  val sumFuture = Future.reduceLeft(futures)(_+_) // Future with sum of all numbers
  Thread.sleep(200)
  /*sumFuture.onComplete{
    case Success(value) => println(value)
    case Failure(e) => e.getMessage
  }*/
  sumFuture.onComplete(println)

  Thread.sleep(3000)
}
