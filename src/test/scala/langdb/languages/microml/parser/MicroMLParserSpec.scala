package langdb.languages.microml.parser

import langdb.languages.microml.ast.{Term, Type}
import munit.FunSuite

class MicroMLParserSpec extends FunSuite:

  test("parse integer literal") {
    val result = MicroMLParser.parse("42")
    assertEquals(result, Right(Term.IntLit(42)))
  }

  test("parse string literal") {
    val result = MicroMLParser.parse("\"hello\"")
    assertEquals(result, Right(Term.StringLit("hello")))
  }

  test("parse boolean literals") {
    assertEquals(MicroMLParser.parse("true"), Right(Term.BoolLit(true)))
    assertEquals(MicroMLParser.parse("false"), Right(Term.BoolLit(false)))
  }

  test("parse variable") {
    val result = MicroMLParser.parse("x")
    assertEquals(result, Right(Term.Var("x")))
  }

  test("parse simple lambda") {
    val result = MicroMLParser.parse("fn x: Int => x")
    assertEquals(result, Right(Term.Lambda("x", Type.IntType, Term.Var("x"))))
  }

  test("parse lambda with body expression") {
    val result = MicroMLParser.parse("fn x: Int => x + 1")
    assertEquals(
      result,
      Right(Term.Lambda("x", Type.IntType, Term.Add(Term.Var("x"), Term.IntLit(1))))
    )
  }

  test("parse nested lambdas") {
    val result = MicroMLParser.parse("fn x: Int => fn y: Int => x + y")
    assertEquals(
      result,
      Right(
        Term.Lambda(
          "x",
          Type.IntType,
          Term.Lambda("y", Type.IntType, Term.Add(Term.Var("x"), Term.Var("y")))
        )
      )
    )
  }

  test("parse function application") {
    val result = MicroMLParser.parse("f x")
    assertEquals(result, Right(Term.App(Term.Var("f"), Term.Var("x"))))
  }

  test("parse multiple applications (left-associative)") {
    val result = MicroMLParser.parse("f x y")
    assertEquals(
      result,
      Right(Term.App(Term.App(Term.Var("f"), Term.Var("x")), Term.Var("y")))
    )
  }

  test("parse let binding") {
    val result = MicroMLParser.parse("let x = 5 in x + 1")
    assertEquals(
      result,
      Right(Term.Let("x", Term.IntLit(5), Term.Add(Term.Var("x"), Term.IntLit(1))))
    )
  }

  test("parse let with lambda value") {
    val result = MicroMLParser.parse("let double = fn x: Int => x + x in double 3")
    assertEquals(
      result,
      Right(
        Term.Let(
          "double",
          Term.Lambda("x", Type.IntType, Term.Add(Term.Var("x"), Term.Var("x"))),
          Term.App(Term.Var("double"), Term.IntLit(3))
        )
      )
    )
  }

  test("parse if expression") {
    val result = MicroMLParser.parse("if true then 1 else 2")
    assertEquals(result, Right(Term.If(Term.BoolLit(true), Term.IntLit(1), Term.IntLit(2))))
  }

  test("parse if with condition") {
    val result = MicroMLParser.parse("if x == 0 then 1 else x")
    assertEquals(
      result,
      Right(Term.If(Term.Eq(Term.Var("x"), Term.IntLit(0)), Term.IntLit(1), Term.Var("x")))
    )
  }

  test("parse addition") {
    val result = MicroMLParser.parse("1 + 2")
    assertEquals(result, Right(Term.Add(Term.IntLit(1), Term.IntLit(2))))
  }

  test("parse multiplication") {
    val result = MicroMLParser.parse("2 * 3")
    assertEquals(result, Right(Term.Mult(Term.IntLit(2), Term.IntLit(3))))
  }

  test("parse multiplication has higher precedence than addition") {
    val result = MicroMLParser.parse("1 + 2 * 3")
    assertEquals(
      result,
      Right(Term.Add(Term.IntLit(1), Term.Mult(Term.IntLit(2), Term.IntLit(3))))
    )
  }

  test("parse parentheses for grouping") {
    val result = MicroMLParser.parse("(1 + 2) * 3")
    assertEquals(
      result,
      Right(Term.Mult(Term.Add(Term.IntLit(1), Term.IntLit(2)), Term.IntLit(3)))
    )
  }

  test("parse equality") {
    val result = MicroMLParser.parse("x == 5")
    assertEquals(result, Right(Term.Eq(Term.Var("x"), Term.IntLit(5))))
  }

  test("parse logical and") {
    val result = MicroMLParser.parse("true && false")
    assertEquals(result, Right(Term.And(Term.BoolLit(true), Term.BoolLit(false))))
  }

  test("parse not") {
    val result = MicroMLParser.parse("not true")
    assertEquals(result, Right(Term.Not(Term.BoolLit(true))))
  }

  test("parse string concatenation") {
    val result = MicroMLParser.parse("\"hello\" ++ \"world\"")
    assertEquals(
      result,
      Right(Term.StringConcat(Term.StringLit("hello"), Term.StringLit("world")))
    )
  }

  test("parse print") {
    val result = MicroMLParser.parse("print 42")
    assertEquals(result, Right(Term.Print(Term.IntLit(42))))
  }

  test("parse println") {
    val result = MicroMLParser.parse("println 42")
    assertEquals(result, Right(Term.Println(Term.IntLit(42))))
  }

  test("parse complex expression") {
    val input =
      """
        let add = fn x: Int => fn y: Int => x + y in
        let result = add 5 10 in
        if result == 15 then println result else println 0
      """
    val result = MicroMLParser.parse(input)
    assert(result.isRight, s"Failed to parse: ${result.left.getOrElse("")}")
  }

  test("parse function types") {
    val result = MicroMLParser.parse("fn f: Int -> Int => f 5")
    assertEquals(
      result,
      Right(
        Term.Lambda(
          "f",
          Type.FunctionType(Type.IntType, Type.IntType),
          Term.App(Term.Var("f"), Term.IntLit(5))
        )
      )
    )
  }

  test("parse higher-order function types") {
    val result = MicroMLParser.parse("fn f: Int -> Int -> Int => f 1 2")
    assertEquals(
      result,
      Right(
        Term.Lambda(
          "f",
          Type.FunctionType(Type.IntType, Type.FunctionType(Type.IntType, Type.IntType)),
          Term.App(Term.App(Term.Var("f"), Term.IntLit(1)), Term.IntLit(2))
        )
      )
    )
  }

  test("reject keywords as identifiers") {
    val result = MicroMLParser.parse("fn")
    assert(result.isLeft, "Should not parse keyword 'fn' as identifier")
  }

  test("parse expression with whitespace and newlines") {
    val input =
      """
        let x = 5
        in
        x + 1
      """
    val result = MicroMLParser.parse(input)
    assertEquals(
      result,
      Right(Term.Let("x", Term.IntLit(5), Term.Add(Term.Var("x"), Term.IntLit(1))))
    )
  }

  test("parse nested if expressions") {
    val input  = "if x == 0 then 0 else if x == 1 then 1 else 2"
    val result = MicroMLParser.parse(input)
    assertEquals(
      result,
      Right(
        Term.If(
          Term.Eq(Term.Var("x"), Term.IntLit(0)),
          Term.IntLit(0),
          Term.If(Term.Eq(Term.Var("x"), Term.IntLit(1)), Term.IntLit(1), Term.IntLit(2))
        )
      )
    )
  }

  test("parse operators with correct associativity") {
    val result = MicroMLParser.parse("1 + 2 + 3")
    assertEquals(
      result,
      Right(Term.Add(Term.Add(Term.IntLit(1), Term.IntLit(2)), Term.IntLit(3)))
    )
  }
