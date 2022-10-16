package part6patterns

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

//step1  import ask and pipe
import akka.pattern.ask
import akka.pattern.pipe


class AskSpec extends TestKit(ActorSystem("AskSpec"))
  with ImplicitSender with WordSpecLike with BeforeAndAfterAll {

  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  import AskSpec._

  "An authenticator" should {
    authenticatorTestSuite(Props[AuthManager])
  }

  "A piped authenticator" should {
    authenticatorTestSuite(Props[PipedAuthManager])
  }


    def authenticatorTestSuite(props: Props): Unit = {
      import AuthManager._
      "fail to authenticate a non registered uer" in {
        val pipedAuthManager = system.actorOf(props)
        pipedAuthManager ! Authenticate("obaid", "akka")
        expectMsg(AuthFailure(AuthManager.AUTH_FAILURE_NOT_FOUND))
      }
      "fail to authenticate if invalid password" in {
        val pipedAuthManager = system.actorOf(props)
        pipedAuthManager ! RegisterUser("oby", "oby")
        pipedAuthManager ! Authenticate("oby", "akka")
        expectMsg(AuthFailure(AUTH_FAILURE_PASSWORD_INCORRECT))
      }
      "successfully authenticate a registered user" in {
        val pipedAuthManager = system.actorOf(props)
        pipedAuthManager ! RegisterUser("oby", "oby")
        pipedAuthManager ! Authenticate("oby", "oby")
        expectMsg(AuthSuccess)
    }
  }

}

object AskSpec {
  // assume this code is somewhere else in our app
  case class Read(key: String)
  case class Write(key: String, value: String)

  class KVActor extends Actor with ActorLogging {
    override def receive: Receive = online(Map())

    def online(kv: Map[String, String]): Receive = {
      case Read(key) =>
        log.info(s"Trying to read the value at the key: $key")
        sender() ! kv.get(key) // Option[String]
      case Write(k, v) =>
        log.info(s"Writing the value $v for the key $k")
        context.become(online(kv + (k -> v)))
    }
  }

  // user auth Actor
  case class RegisterUser(username: String, password: String)
  case class Authenticate(username: String, password: String)
  case object AuthSuccess
  case class AuthFailure(message: String)

  object AuthManager {
    val AUTH_FAILURE_NOT_FOUND = "username not found"
    val AUTH_FAILURE_PASSWORD_INCORRECT = "password incorrect"
    val AUTH_FAILURE_SYSTEM = "system error"
  }

  class AuthManager extends Actor with ActorLogging {
    import AuthManager._
    // step -2 logistics
    import scala.concurrent.duration.DurationInt
    implicit val timeout: Timeout = Timeout(1.second)
    implicit val executionContext: ExecutionContext = context.dispatcher
    //

    protected val authDB: ActorRef = context.actorOf(Props[KVActor], "kvActor")

    override def receive: Receive = {
      case RegisterUser(username, password) => authDB ! Write(username, password)
      case Authenticate(username, password) => handleAuthentication(username, password)

    }

    def handleAuthentication(username: String, password: String): Unit = {
      val originalSender = sender()
      // step 3 ask the actor
      val future = authDB ? Read(username)  // Future[Any]
      // step 4 Handle the future for eg-> with onComplete
      future.onComplete {
        // step 5 Never call methods on actor instance / Access Mutable state in onComplete.
        case Success(None) => originalSender ! AuthFailure(AUTH_FAILURE_NOT_FOUND)
        case Success(Some(dbPassword)) =>
          if (dbPassword == password) originalSender ! AuthSuccess
          else originalSender ! AuthFailure(AUTH_FAILURE_PASSWORD_INCORRECT)
        case Failure(_) => originalSender ! AuthFailure(AUTH_FAILURE_SYSTEM)
      }
    }
  }

  class PipedAuthManager extends AuthManager {
    override def handleAuthentication(username: String, password: String): Unit = {
      // step 3 ask the actor
      val future: Future[Any] = authDB ? Read(username) // Future[Any]
      // step 4 process the future until you get responses you  will send back
      val passwordFuture: Future[Option[String]] = future.mapTo[Option[String]] // Future[Option[String]]

      import AuthManager._
      val responseFuture = passwordFuture.map {
        case None => AuthFailure(AUTH_FAILURE_NOT_FOUND)
        case Some(dbPassword) =>
          if (dbPassword == password) AuthSuccess
          else AuthFailure(AUTH_FAILURE_PASSWORD_INCORRECT)
      } // Future[Any] will be completed with the responses i will send back

      // step 5  pipe the resulting future to the actor you want to send the result to
      /*
      * When future completes, send the response back to the actor ref in the arg list
      * */
      responseFuture.pipeTo(sender())
    }
  }

}
