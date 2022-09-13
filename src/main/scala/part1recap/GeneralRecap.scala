package part1recap

object GeneralRecap extends App {

  val aCondition: Boolean = false
  var aVariable = 4 // scala type inference
  aVariable += 1

  val aConditionVal = if(aCondition) 41 else false // expression

  //code block
  val aCodeBlock = {
    if(aCondition) 55
    else 50
  }

  //type
  // Unit, they use something but don't return anything
  val theUnit = println("Hey, Scala")

  // functions
  def aFunction(x: Int): Int = x + 1

  // recursion - Tail Recursion
  import scala.annotation.tailrec
  @tailrec
  def factorial(n: Int, acc: Int): Int = {
    if(n <= 0) acc
    else factorial(n-1, acc * n)
  }

  // OOP
  println("----- Runtime polymorphism")
  class Animal{
    def sound: String = "Animal Sound"
  }
  class Dog extends Animal {
    override def sound: String = "wof wof"
  }

  class Cat extends Animal {
    override def sound = "meow meow"
  }

  class Duck extends Animal {
    override def sound = "quack quack"
  }

  def printSoundOfAnimal(animal: Animal): String = animal.sound
  println(printSoundOfAnimal(new Dog))
  println(printSoundOfAnimal(new Cat))
  println(printSoundOfAnimal(new Duck))
  println("----- Runtime polymorphism")

  // trait, abstract

  trait Carnivore{
    def eat(a: Animal): Unit
  }
  class Crocodile extends Animal with Carnivore{
    // mixin
    override def eat(a: Animal): Unit = println("Crunch")

    override def sound: String = super.sound
  }

  // method notation
  val aDog: Animal = new Dog
  val aCrock = new Crocodile
  aCrock.eat(aDog)
  aCrock eat aDog

  // anonymous classes
  val aCarnivore = new Carnivore {
    override def eat(a: Animal): Unit = println("Roar")
  }
  aCarnivore eat aDog

  // Generics
  abstract class MyList[+A] // generic A, + means it's covariant
  //covariant =>  Type of MyList[Dog] is an extension of MyLit[Animals]
  // Companion objects
  object MyList

  //case classes
  case class Person(name: String, age: Int)

  //Exceptions
  val aPotentialFailure = try{
    throw new RuntimeException("Message Ex") // Nothing(super sub-type)
  }catch{
    case e: Exception => "Exception caught"
  } finally{
    println("Some logs")
  }

  //Functional Programming
  val incrementer = new Function1[Int, Int] {
    override def apply(v1: Int): Int = v1 + 1
  }
  val incremented = incrementer(10) // 11 calling object like function
  // incrementer.apply(10)

  val anonymousIncrementer = (x: Int) => x+1 // same as Function1
  // Int => Int == Function1[Int, Int]
  println(incrementer(0) == anonymousIncrementer(0))
  // FP is all about working with functions as first-class
  List(1,2,3).map(incrementer) // HOF

  //for comprehensions
  val pairs = for{
    num <- List(1,2,3,4)
    char <- List('a', 'b', 'c', 'd')
  } yield num +"-"+char

  //List(1,2,3,4).map(x => List('a', 'b', 'c', 'd').map(y => x+"-"+y))
  List(1,2,3,4).flatMap(num => List('a', 'b', 'c', 'd').map(char => num+"-"+char))


  /*Seq, Array, List,  tuples, sets, Map*/

  // other collections
  // Options, Try
  import scala.util.Try
  val anOption = Some(1)
  val aTry = Try{
    throw new RuntimeException("aa")
  }

  // patter
  val unknown = 2
  val order = unknown match {
    case 1 => "First"
    case 2 => "Second"
    case _ => "Unknown"
  }

  val bob = Person("bob", 22)
  val greeting = bob match { // decomposing Person
    case Person(n, _) => s"Hi my name is $n"
    case _ => "No Name here"
  }
}
