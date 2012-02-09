package org.example

object Main extends App {

  println("A rendered Twirl TXT template:")
  println(org.txt.example.hello("Alice", 16))

  println("---");
  println("A rendered Twirl HTML template:")
  println(org.html.example.hello("Bob", 22))

  println("---");
  println("A rendered Twirl XML template:")
  println(org.xml.example.hello("Darth", 42))
}