import akka.actor.{Actor, ActorRef, ActorSystem, PoisonPill, Props, Terminated}

abstract class Msg
case class Info(msg:String) extends Msg
case class NewMessage(from:String,msg:String) extends Msg
case class Send(msg:String) extends Msg
case class Connect(username:String) extends Msg
case class BroadCast(msg:String) extends Msg
case object Disconnect

class Server extends Actor {
  var clients: List[(String, ActorRef)] = List[(String,ActorRef)]()
  override def receive: Receive = {
    case Connect(username) =>
      broadcast(Info(s"$username has joined the chat"))
      clients = (username,sender) :: clients
      context.watch(sender)
    case BroadCast(message) =>
      val clientOption = clients.find(_._2 == sender)
      if(clientOption.isDefined) {
        val username = clientOption.get._1
        broadcast(NewMessage(username,message))
      }
    case Terminated(client) =>
      val clientOption = clients.find(_._2 == sender)
      clients = clients.filterNot(_._2 == client)
      if(clientOption.isDefined) {
        val username = clientOption.get._1
        broadcast(Info(s"$username has left chat"))
      }

  }

  def broadcast(info:Msg): Unit = {
    clients.foreach(_._2 ! info)
  }
}

class Client(username:String,server:ActorRef) extends Actor {
  server ! Connect(username)
  override def receive: Receive = {
    case info:Info =>
      println(s"[$username's client]- ${info.msg}")
    case send: Send =>
      server ! BroadCast(send.msg)
    case newMsg:NewMessage =>
      if(username != newMsg.from)
      println(s"[$username's client]- from = ${newMsg.from},message = ${newMsg.msg}")
    case Disconnect => self ! PoisonPill
  }
}

object Client {
  def props(username:String,server: ActorRef): Props =
    Props(
      new Client(username, server)
    )
}


object BroadcastChat extends App {

  val system = ActorSystem("Broadcaster")

  val server: ActorRef = system.actorOf(Props[Server],"Server")

  val client1 = system.actorOf(Client.props("amr",server),"Client1")
  val client2: ActorRef = system.actorOf(Client.props("oby",server),"Client2")

  Thread.sleep(300)

  client2 ! Send("Hi all")

  Thread.sleep(300)
  val client3 = system.actorOf(Client.props("faw",server),"Client3")
  val client4 = system.actorOf(Client.props("raf",server),"Client4")

//  Thread.sleep(1000)

//  client1 ! Send("lechmovo sarneee")
//  Thread.sleep(1000)

//  client4 ! Disconnect

}