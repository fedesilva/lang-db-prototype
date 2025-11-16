package langdb.languages.nanoproc.typechecker

import cats.effect.unsafe.implicits.global
import langdb.languages.nanoproc.parser.NanoProcParser
import munit.FunSuite

class TypeCheckerSpec extends FunSuite:

  def parseAndCheck(source: String): Either[String, Unit] =
    NanoProcParser.parse(source) match
      case Left(err) => Left(s"Parse error: $err")
      case Right(program) =>
        TypeChecker
          .typeCheck(program)
          .attempt
          .unsafeRunSync()
          .left
          .map(_.getMessage)

  // Positive tests - should type check successfully

  test("type check simple procedure") {
    val result = parseAndCheck("""
      proc add(x: Int, y: Int): Int {
        return x + y;
      }
    """)
    assert(result.isRight, s"Expected success but got: $result")
  }

  test("type check procedure with local variables") {
    val result = parseAndCheck("""
      proc test(): Int {
        var x: Int = 5;
        var y: Int = 10;
        return x + y;
      }
    """)
    assert(result.isRight, s"Expected success but got: $result")
  }

  test("type check procedure with assignment") {
    val result = parseAndCheck("""
      proc test(): Int {
        var x: Int = 0;
        x = 42;
        return x;
      }
    """)
    assert(result.isRight, s"Expected success but got: $result")
  }

  test("type check procedure with if statement") {
    val result = parseAndCheck("""
      proc abs(x: Int): Int {
        if (x > 0) {
          return x;
        } else {
          return 0 - x;
        }
      }
    """)
    assert(result.isRight, s"Expected success but got: $result")
  }

  test("type check procedure with while loop") {
    val result = parseAndCheck("""
      proc factorial(n: Int): Int {
        var result: Int = 1;
        var i: Int = n;
        while (i > 0) {
          result = result * i;
          i = i - 1;
        }
        return result;
      }
    """)
    assert(result.isRight, s"Expected success but got: $result")
  }

  test("type check procedure call") {
    val result = parseAndCheck("""
      proc double(x: Int): Int {
        return x + x;
      }

      proc test(): Int {
        return double(5);
      }
    """)
    assert(result.isRight, s"Expected success but got: $result")
  }

  test("type check Unit return") {
    val result = parseAndCheck("""
      proc doNothing(): Unit {
        return ();
      }
    """)
    assert(result.isRight, s"Expected success but got: $result")
  }

  test("type check procedure with side effects") {
    val result = parseAndCheck("""
      proc greet(name: String): Unit {
        println("Hello, " ++ name);
        return ();
      }
    """)
    assert(result.isRight, s"Expected success but got: $result")
  }

  test("type check boolean operations") {
    val result = parseAndCheck("""
      proc test(x: Bool, y: Bool): Bool {
        return x && y;
      }
    """)
    assert(result.isRight, s"Expected success but got: $result")
  }

  test("type check string concatenation") {
    val result = parseAndCheck("""
      proc concat(a: String, b: String): String {
        return a ++ b;
      }
    """)
    assert(result.isRight, s"Expected success but got: $result")
  }

  // Negative tests - should catch type errors

  test("reject return type mismatch") {
    val result = parseAndCheck("""
      proc show(s: Int): String {
        return 1;
      }
    """)
    assert(result.isLeft, "Should reject return type mismatch")
    assert(
      result.left.getOrElse("").contains("Type mismatch"),
      s"Expected type mismatch error but got: $result"
    )
  }

  test("reject type mismatch in variable declaration") {
    val result = parseAndCheck("""
      proc test(): Int {
        var x: Int = "hello";
        return x;
      }
    """)
    assert(result.isLeft, "Should reject type mismatch in var decl")
    assert(result.left.getOrElse("").contains("Type mismatch"))
  }

  test("reject type mismatch in assignment") {
    val result = parseAndCheck("""
      proc test(): Int {
        var x: Int = 0;
        x = "hello";
        return x;
      }
    """)
    assert(result.isLeft, "Should reject type mismatch in assignment")
    assert(result.left.getOrElse("").contains("Type mismatch"))
  }

  test("reject unbound variable") {
    val result = parseAndCheck("""
      proc test(): Int {
        return x;
      }
    """)
    assert(result.isLeft, "Should reject unbound variable")
    assert(result.left.getOrElse("").contains("Unbound variable"))
  }

  test("reject unbound procedure") {
    val result = parseAndCheck("""
      proc test(): Int {
        return foo(5);
      }
    """)
    assert(result.isLeft, "Should reject unbound procedure")
    assert(result.left.getOrElse("").contains("Unbound procedure"))
  }

  test("reject wrong argument type") {
    val result = parseAndCheck("""
      proc double(x: Int): Int {
        return x + x;
      }

      proc test(): Int {
        return double("hello");
      }
    """)
    assert(result.isLeft, "Should reject wrong argument type")
    assert(result.left.getOrElse("").contains("Type mismatch"))
  }

  test("reject arity mismatch - too few arguments") {
    val result = parseAndCheck("""
      proc add(x: Int, y: Int): Int {
        return x + y;
      }

      proc test(): Int {
        return add(5);
      }
    """)
    assert(result.isLeft, "Should reject arity mismatch")
    assert(result.left.getOrElse("").contains("Arity mismatch"))
  }

  test("reject arity mismatch - too many arguments") {
    val result = parseAndCheck("""
      proc add(x: Int, y: Int): Int {
        return x + y;
      }

      proc test(): Int {
        return add(5, 10, 15);
      }
    """)
    assert(result.isLeft, "Should reject arity mismatch")
    assert(result.left.getOrElse("").contains("Arity mismatch"))
  }

  test("reject missing return in non-Unit procedure") {
    val result = parseAndCheck("""
      proc test(): Int {
        var x: Int = 42;
      }
    """)
    assert(result.isLeft, "Should reject missing return")
    assert(result.left.getOrElse("").contains("must return"))
  }

  test("reject addition with non-Int types") {
    val result = parseAndCheck("""
      proc test(): Int {
        return "hello" + "world";
      }
    """)
    assert(result.isLeft, "Should reject addition on strings")
    assert(result.left.getOrElse("").contains("Type mismatch"))
  }

  test("reject multiplication with non-Int types") {
    val result = parseAndCheck("""
      proc test(): Int {
        return "hello" * 5;
      }
    """)
    assert(result.isLeft, "Should reject multiplication on strings")
    assert(result.left.getOrElse("").contains("Type mismatch"))
  }

  test("reject comparison with mismatched types") {
    val result = parseAndCheck("""
      proc test(): Bool {
        return 5 == "hello";
      }
    """)
    assert(result.isLeft, "Should reject comparison of different types")
    assert(result.left.getOrElse("").contains("Type mismatch"))
  }

  test("reject numeric comparison on non-Int") {
    val result = parseAndCheck("""
      proc test(): Bool {
        return "hello" > "world";
      }
    """)
    assert(result.isLeft, "Should reject > on strings")
    assert(result.left.getOrElse("").contains("Type mismatch"))
  }

  test("reject logical and on non-Bool") {
    val result = parseAndCheck("""
      proc test(): Bool {
        return 5 && 10;
      }
    """)
    assert(result.isLeft, "Should reject && on non-Bool")
    assert(result.left.getOrElse("").contains("Type mismatch"))
  }

  test("reject not on non-Bool") {
    val result = parseAndCheck("""
      proc test(): Bool {
        return not 42;
      }
    """)
    assert(result.isLeft, "Should reject not on Int")
    assert(result.left.getOrElse("").contains("Type mismatch"))
  }

  test("reject if with non-Bool condition") {
    val result = parseAndCheck("""
      proc test(): Int {
        if (42) {
          return 1;
        } else {
          return 0;
        }
      }
    """)
    assert(result.isLeft, "Should reject if with non-Bool condition")
    assert(result.left.getOrElse("").contains("Type mismatch"))
  }

  test("reject if branches with different return types") {
    val result = parseAndCheck("""
      proc test(): Int {
        if (true) {
          return 1;
        } else {
          return "hello";
        }
      }
    """)
    assert(result.isLeft, "Should reject if branches with different types")
    assert(result.left.getOrElse("").contains("Type mismatch"))
  }

  test("reject while with non-Bool condition") {
    val result = parseAndCheck("""
      proc test(): Unit {
        while (42) {
          return ();
        }
        return ();
      }
    """)
    assert(result.isLeft, "Should reject while with non-Bool condition")
    assert(result.left.getOrElse("").contains("Type mismatch"))
  }

  test("reject string concatenation on non-String") {
    val result = parseAndCheck("""
      proc test(): String {
        return "hello" ++ 42;
      }
    """)
    assert(result.isLeft, "Should reject ++ on non-String")
    assert(result.left.getOrElse("").contains("Type mismatch"))
  }

  test("reject print with non-String") {
    val result = parseAndCheck("""
      proc test(): Unit {
        print(42);
        return ();
      }
    """)
    assert(result.isLeft, "Should reject print with non-String")
    assert(result.left.getOrElse("").contains("Type mismatch"))
  }

  test("reject println with non-String") {
    val result = parseAndCheck("""
      proc test(): Unit {
        println(42);
        return ();
      }
    """)
    assert(result.isLeft, "Should reject println with non-String")
    assert(result.left.getOrElse("").contains("Type mismatch"))
  }

  test("allow Unit procedure without explicit return") {
    val result = parseAndCheck("""
      proc doNothing(): Unit {
        var x: Int = 5;
      }
    """)
    // Unit procedures don't need explicit return
    assert(result.isRight, s"Expected success for Unit proc without return but got: $result")
  }

  test("reject division by zero is not a type error") {
    // Note: Division by zero is a runtime error, not a type error
    val result = parseAndCheck("""
      proc test(): Int {
        return 42 / 0;
      }
    """)
    assert(result.isRight, "Division by zero is not a type error")
  }
