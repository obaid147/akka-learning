package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ChildActorExercise extends App {

  //Distributed Word Counting

  object WordCounterMaster {
    case class Initialize(nChildren: Int)
    // Master will receive Initialize message and will create n children of type
    case class WordCountTask(id: Int, text: String) // WordCounterWorker will receive and will reply
    case class WordCountReply(id: Int, count: Int)
  }

  class WordCounterMaster extends Actor {
    import WordCounterMaster._
    override def receive: Receive = {
      case Initialize(nChildren) =>
        println("Initializing Master.....")
        val childrenRefs = for (i <- 1 to nChildren) yield context.actorOf(Props[WordCounterWorker], s"wcw_$i")
        context.become(withChildren(childrenRefs, 0, 1, Map()))
      }

    def withChildren(childrenRefs: Seq[ActorRef], currentChildIndex: Int,
                     currentTaskId: Int, requestMap: Map[Int, ActorRef]): Receive = {
      case text: String =>
        println(s"[Master] I have received: $text, I will send it to child:- $currentChildIndex")
        val originalSender = sender()
        val task = WordCountTask(currentTaskId, text)
        val childRef = childrenRefs(currentChildIndex)
        childRef ! task
        val nextChildIndex = (currentChildIndex + 1) % childrenRefs.length // round robin logic
        val newTaskId = currentTaskId + 1
        val newRequestMap = requestMap + (currentTaskId -> originalSender)
        context.become(withChildren(childrenRefs, nextChildIndex, newTaskId, newRequestMap))
      case WordCountReply(id, count) =>
        println(s"I have received reply for taskId: $id with:- $count")
        val originalSender = requestMap(id)
        originalSender ! count
        context.become(withChildren(childrenRefs, currentChildIndex, currentTaskId, requestMap - id))
    }
  }

  /** round robin logic
   * If we have 1 ... 5 workers, and 7 tasks, task1 worker1 ..... task5 worker5,
   * Then reset worker => worker0 task6 and worker1 task7
   * */
  class WordCounterWorker extends Actor {
    import WordCounterMaster._
    override def receive: Receive = {
      case WordCountTask(id, text) =>
        println(s"${self.path} I have received a taskID: $id with: $text")
        sender() ! WordCountReply(id, text.split(" ").length)
    }
  }

  class TestActor extends Actor {
    import WordCounterMaster._
    override def receive: Receive = {
      case "go" =>
      val master = context.actorOf(Props[WordCounterMaster], "master")
      master ! Initialize(3)
      val texts = List("I love akka", "Scala is super dope", "Yes", "me too")
      texts.foreach(text => master ! text)
      case count: Int => println(s"[test actor] I have received reply: $count")
    }
  }

  val system = ActorSystem("RoundRobinExercise")
  val testActor = system.actorOf(Props[TestActor], "TestActor")
  testActor ! "go"
}
