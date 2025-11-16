package langdb.languages.nanoproc.parser

import langdb.languages.nanoproc.ast.Stmt
import munit.FunSuite

class NanoProcParserSpec extends FunSuite:

  test("parse integer literal") {
    val result = NanoProcParser.parse("""
      proc test(): Int {
        return 42;
      }
    """)
    assert(result.isRight)
  }

  test("parse string literal") {
    val result = NanoProcParser.parse("""
      proc test(): String {
        return "hello";
      }
    """)
    assert(result.isRight)
  }

  test("parse boolean literals") {
    val result = NanoProcParser.parse("""
      proc test(): Bool {
        return true;
      }
    """)
    assert(result.isRight)
  }

  test("parse unit literal") {
    val result = NanoProcParser.parse("""
      proc test(): Unit {
        return ();
      }
    """)
    assert(result.isRight)
  }

  test("parse variable declaration") {
    val result = NanoProcParser.parse("""
      proc test(): Int {
        var x: Int = 42;
        return x;
      }
    """)
    assert(result.isRight)
    val program = result.toOption.get
    assertEquals(program.procs.length, 1)
    assert(program.procs.head.body.stmts.head.isInstanceOf[Stmt.VarDecl])
  }

  test("parse assignment") {
    val result = NanoProcParser.parse("""
      proc test(): Int {
        var x: Int = 0;
        x = 42;
        return x;
      }
    """)
    assert(result.isRight)
  }

  test("parse simple addition") {
    val result = NanoProcParser.parse("""
      proc add(x: Int, y: Int): Int {
        return x + y;
      }
    """)
    assert(result.isRight)
  }

  test("parse multiplication") {
    val result = NanoProcParser.parse("""
      proc mult(x: Int, y: Int): Int {
        return x * y;
      }
    """)
    assert(result.isRight)
  }

  test("parse precedence: multiplication before addition") {
    val result = NanoProcParser.parse("""
      proc test(): Int {
        return 1 + 2 * 3;
      }
    """)
    assert(result.isRight)
  }

  test("parse equality comparison") {
    val result = NanoProcParser.parse("""
      proc test(x: Int): Bool {
        return x == 5;
      }
    """)
    assert(result.isRight)
  }

  test("parse logical and") {
    val result = NanoProcParser.parse("""
      proc test(x: Bool, y: Bool): Bool {
        return x && y;
      }
    """)
    assert(result.isRight)
  }

  test("parse not operator") {
    val result = NanoProcParser.parse("""
      proc test(x: Bool): Bool {
        return not x;
      }
    """)
    assert(result.isRight)
  }

  test("parse string concatenation") {
    val result = NanoProcParser.parse("""
      proc test(): String {
        return "hello" ++ "world";
      }
    """)
    assert(result.isRight)
  }

  test("parse if statement") {
    val result = NanoProcParser.parse("""
      proc test(x: Int): Int {
        if (x > 0) {
          return 1;
        } else {
          return 0;
        }
      }
    """)
    assert(result.isRight)
  }

  test("parse while loop") {
    val result = NanoProcParser.parse("""
      proc test(): Int {
        var i: Int = 10;
        while (i > 0) {
          i = i + 1;
        }
        return i;
      }
    """)
    assert(result.isRight)
  }

  test("parse procedure call") {
    val result = NanoProcParser.parse("""
      proc double(x: Int): Int {
        return x + x;
      }

      proc test(): Int {
        return double(5);
      }
    """)
    assert(result.isRight)
    val program = result.toOption.get
    assertEquals(program.procs.length, 2)
  }

  test("parse procedure with no parameters") {
    val result = NanoProcParser.parse("""
      proc getAnswer(): Int {
        return 42;
      }
    """)
    assert(result.isRight)
  }

  test("parse procedure with multiple parameters") {
    val result = NanoProcParser.parse("""
      proc add3(x: Int, y: Int, z: Int): Int {
        return x + y + z;
      }
    """)
    assert(result.isRight)
  }

  test("parse multiple procedures") {
    val result = NanoProcParser.parse("""
      proc first(): Int {
        return 1;
      }

      proc second(): Int {
        return 2;
      }

      proc third(): Int {
        return 3;
      }
    """)
    assert(result.isRight)
    val program = result.toOption.get
    assertEquals(program.procs.length, 3)
  }

  test("parse factorial") {
    val result = NanoProcParser.parse("""
      proc factorial(n: Int): Int {
        var result: Int = 1;
        var i: Int = n;
        while (i > 0) {
          result = result * i;
          i = i + 1;
        }
        return result;
      }
    """)
    assert(result.isRight)
  }

  test("parse nested if statements") {
    val result = NanoProcParser.parse("""
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
    """)
    assert(result.isRight)
  }

  test("parse print statement") {
    val result = NanoProcParser.parse("""
      proc test(): Unit {
        print("hello");
        return ();
      }
    """)
    assert(result.isRight)
  }

  test("parse println statement") {
    val result = NanoProcParser.parse("""
      proc test(): Unit {
        println("hello");
        return ();
      }
    """)
    assert(result.isRight)
  }

  test("parse expression statement") {
    val result = NanoProcParser.parse("""
      proc test(): Unit {
        println("side effect");
        return ();
      }
    """)
    assert(result.isRight)
  }

  test("parse complex program") {
    val result = NanoProcParser.parse("""
      proc double(x: Int): Int {
        return x + x;
      }

      proc quadruple(x: Int): Int {
        var temp: Int = double(x);
        return double(temp);
      }

      proc main(): Unit {
        var result: Int = quadruple(5);
        println("result");
        return ();
      }
    """)
    assert(result.isRight)
    val program = result.toOption.get
    assertEquals(program.procs.length, 3)
  }

  test("reject empty program") {
    val result = NanoProcParser.parse("")
    assert(result.isLeft)
  }

  test("reject procedure without return type") {
    val result = NanoProcParser.parse("""
      proc test() {
        return 42;
      }
    """)
    assert(result.isLeft)
  }

  test("reject statement without semicolon") {
    val result = NanoProcParser.parse("""
      proc test(): Int {
        var x: Int = 42
        return x;
      }
    """)
    assert(result.isLeft)
  }

  test("parse procedure with Unit return") {
    val result = NanoProcParser.parse("""
      proc doNothing(): Unit {
        return ();
      }
    """)
    assert(result.isRight)
  }

  test("parse variable shadowing in nested blocks") {
    val result = NanoProcParser.parse("""
      proc test(): Int {
        var x: Int = 1;
        if (true) {
          var x: Int = 2;
          return x;
        } else {
          return x;
        }
      }
    """)
    assert(result.isRight)
  }

  test("parse parentheses for grouping") {
    val result = NanoProcParser.parse("""
      proc test(): Int {
        return (1 + 2) * 3;
      }
    """)
    assert(result.isRight)
  }

  test("reject keywords as identifiers") {
    val result = NanoProcParser.parse("""
      proc test(): Int {
        var while: Int = 42;
        return while;
      }
    """)
    assert(result.isLeft)
  }
