package org.example

object Main extends App {

  println("A rendered Twirl TXT template:")
  println(org.example.txt.hello("Alice", 16))

  println("---");
  println("A rendered Twirl HTML template:")
  println(org.example.html.hello("Bob", 22))

  println("---");
  println("A rendered Twirl XML template:")
  println(org.example.xml.hello("Darth", 42))
}