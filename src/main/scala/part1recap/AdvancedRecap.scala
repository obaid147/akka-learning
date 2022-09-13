package part1recap

object AdvancedRecap extends App {
  //Partial Functions
  /*These are functions that are */
  val partialFunction: PartialFunction[Int, Int] = {
    case 1 => 11
    case 2 => 12
    case 5 => 19
  }
  //
  val simpleFunction =  (x:Int) => x match {
    case 1 => 11
    case 2 => 12
    case 5 => 19
  }

  val function: Int => Int = partialFunction

  val list = List(1,2)
  val modifiedList = list.map{
    case 1 => list.head
    case _ => list.tail
  }
  println(modifiedList)

  // lifted
  val lifted = partialFunction.lift // total function from Int => Option[Int]
  lifted(2) // Some(12)
  lifted(8) // None

  // orElse
  val ofChain = partialFunction.orElse[Int, Int]{
    case 8 => 900
  }
  ofChain(5) // 19
  ofChain(8) // now it will return 900 instead of None
  //ofChain(11) // MatchError

  // type aliases
  type ReceiveFunction = PartialFunction[Any, Unit]
  def receive: ReceiveFunction = {
    case 1 => println("hey")
    case _ => ()
  }

//  Implicits
  implicit val timeout = 2000
  def setTimeout(f: () => Unit)(implicit someIntType: Int): String = "abc"
  println(setTimeout(() => println())) // extra param omitted

  // Implicit Conversions
  //1 implicit def
  case class Person(name: String) {
    def greet = s"Hey, $name"
  }
  implicit def fromStringToPerson(str: String): Person = Person(str)
  println("Obaid".greet)// String becomes Person automatically
  // fromStringToPerson("Obaid").greet

  // 2. Implicit classes
  implicit class Dog(name: String) {
    def bark: String = s"$name can Bark"
  }
  println("Puppy".bark) // String "Puppy" becomes a Dog automatically
  // new Dog("Puppy).bark -- automatically done by compiler.

  // organize   Ordering is a method scala .sorted uses
  implicit val inverseOrdering: Ordering[Int] = Ordering.fromLessThan(_ > _)
  println(List(1,2,3).sorted) // This will return List(3,2,1)
  // because inverseOrdering will be injected by compiler into the sorted method's implicit param.
  // This implicit comes from local scope.

  // But with Futures, We used to get implicit from imported scope
  import scala.concurrent.ExecutionContext.Implicits.global
  // This global
  import scala.concurrent.Future
  val future = Future(10)

  // companion objects of types included in the call
  object Person{ // case class Person(name: String)
    implicit val personOrdering: Ordering[Person] = Ordering.fromLessThan((p1, p2) => p1.name.compareTo(p2.name) < 0)
    // alphabetic ordering by name of person
  }

  println(List(Person("Bob"), Person("Alice")).sorted)
}
