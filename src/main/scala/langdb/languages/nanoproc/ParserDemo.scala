package langdb.languages.nanoproc

import cats.effect.IO
import cats.syntax.all.*
import langdb.languages.nanoproc.parser.NanoProcParser
import langdb.languages.nanoproc.typechecker.TypeChecker

object ParserDemo:

  // Example programs in NanoProc
  def examplePrograms(): List[(String, String)] = List(
    // Simple procedure with return
    (
      "Simple addition",
      """
      proc add(x: Int, y: Int): Int {
        return x + y;
      }
      """
    ),
    // Factorial with while loop
    (
      "Factorial (iterative)",
      """
      proc factorial(n: Int): Int {
        var result: Int = 1;
        var i: Int = n;
        while (i > 0) {
          result = result * i;
          i = i - 1;
        }
        return result;
      }
      """
    ),
    // Procedure with conditional
    (
      "Absolute value",
      """
      proc abs(x: Int): Int {
        if (x > 0) {
          return x;
        } else {
          return 0 + (0 + x);
        }
      }
      """
    ),
    // Multiple procedures
    (
      "Multiple procedures",
      """
      proc double(x: Int): Int {
        return x + x;
      }

      proc quadruple(x: Int): Int {
        var temp: Int = double(x);
        return double(temp);
      }
      """
    ),
    // Procedure with Unit return (side effects only)
    (
      "Procedure with side effects",
      """
      proc greet(name: String): Unit {
        println("Hello, " ++ name);
      }

      proc main(): Unit {
        greet("World");
      }
      """
    ),
    // Boolean logic
    (
      "Boolean operations",
      """
      proc isPositive(x: Int): Bool {
        return x > 0;
      }

      proc isEven(x: Int): Bool {
        var isPos: Bool = isPositive(x);
        return isPos && isPos;
      }
      """
    ),
    // Complex control flow
    (
      "Nested conditionals",
      """
      proc sign(x: Int): Int {
        if (x > 0) {
          return 1;
        } else {
          if (x == 0) {
            return 0;
          } else {
            return 0 + 1;
          }
        }
      }
      """
    ),
    // Variable reassignment
    (
      "Variable mutation",
      """
      proc counter(): Int {
        var count: Int = 0;
        count = count + 1;
        count = count + 1;
        count = count + 1;
        return count;
      }
      """
    )
  )

  def demo(): IO[Unit] =
    for
      _ <- IO.println("=== NanoProc Parser Demo ===")
      _ <- IO.println("")
      _ <- examplePrograms().traverse { case (name, source) =>
        for
          _ <- IO.println(s"Example: $name")
          _ <- IO.println(s"Source:$source")
          _ <- IO.println("")

          // Parse the source
          _ <- NanoProcParser.parse(source) match
            case Right(program) =>
              for
                _ <- IO.println(s"Parsed program with ${program.procs.length} procedure(s)")
                _ <- program.procs.traverse_ { proc =>
                  IO.println(
                    s"  - ${proc.name}(${proc.params.map(_._1).mkString(", ")}): ${proc.returnType}"
                  )
                }
                _ <- IO.println("")

                // Type check the program
                _ <- TypeChecker
                  .typeCheck(program)
                  .flatMap(_ => IO.println("Type check: ✓ passed"))
                  .handleErrorWith(err => IO.println(s"Type error: ${err.getMessage}"))
              yield ()

            case Left(error) =>
              IO.println(s"Parse error:\n$error")

          _ <- IO.println("-" * 80)
          _ <- IO.println("")
        yield ()
      }
      _ <- IO.println("Parser demo completed!")
    yield ()
