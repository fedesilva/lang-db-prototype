package langdb.languages.microml.parser

import cats.effect.IO
import cats.syntax.all.*
import langdb.languages.microml.typechecker.TypeChecker

object ParserDemo:

  // Example programs in MicroML
  def examplePrograms(): List[(String, String)] = List(
    // Simple identity function
    (
      "Identity function",
      "fn x: Int => x"
    ),
    // Addition function
    (
      "Curried addition",
      "fn x: Int => fn y: Int => x + y"
    ),
    // Let binding with application
    (
      "Let with function",
      "let double = fn x: Int => x + x in double 5"
    ),
    // Conditional
    (
      "Conditional expression",
      "fn x: Int => if x == 0 then 1 else x * 2"
    ),
    // Complex nested expression
    (
      "Nested let bindings",
      """
        let add = fn x: Int => fn y: Int => x + y in
        let result = add 5 10 in
        if result == 15 then result else 0
      """
    ),
    // Higher-order function
    (
      "Higher-order function",
      "fn f: Int -> Int => fn x: Int => f (f x)"
    ),
    // String operations
    (
      "String concatenation",
      """let greeting = "Hello" in greeting ++ " World""""
    ),
    // Boolean logic
    (
      "Boolean logic",
      "fn x: Bool => fn y: Bool => not (x && y)"
    )
  )

  def demo(): IO[Unit] =
    for {
      _ <- IO.println("=== MicroML Parser Demo ===")
      _ <- IO.println("")
      _ <- examplePrograms().traverse { case (name, source) =>
        for {
          _ <- IO.println(s"Example: $name")
          _ <- IO.println(s"Source:\n$source")
          _ <- IO.println("")

          // Parse the source
          _ <- MicroMLParser.parse(source) match
            case Right(term) =>
              for {
                _ <- IO.println(s"Parsed AST: $term")
                _ <- IO.println("")

                // Type check the parsed term
                _ <- TypeChecker
                  .typeCheck(term)
                  .flatMap(typ => IO.println(s"Type: $typ"))
                  .handleErrorWith(err => IO.println(s"Type error: ${err.getMessage}"))
              } yield ()

            case Left(error) =>
              IO.println(s"Parse error:\n$error")

          _ <- IO.println("-" * 80)
          _ <- IO.println("")
        } yield ()
      }
      _ <- IO.println("Parser demo completed!")
    } yield ()
