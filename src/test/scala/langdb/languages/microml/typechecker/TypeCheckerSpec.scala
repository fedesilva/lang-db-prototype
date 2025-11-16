package langdb.languages.microml.typechecker

import cats.effect.unsafe.implicits.global
import langdb.languages.microml.ast.Type
import langdb.languages.microml.parser.MicroMLParser
import munit.FunSuite

class TypeCheckerSpec extends FunSuite:

  def parseAndCheck(source: String): Either[String, Type] =
    MicroMLParser.parse(source) match
      case Left(err) => Left(s"Parse error: $err")
      case Right(term) =>
        TypeChecker
          .typeCheck(term)
          .attempt
          .unsafeRunSync()
          .left
          .map(_.getMessage)

  // Positive tests - should type check successfully

  test("type check integer literal") {
    val result = parseAndCheck("42")
    assertEquals(result, Right(Type.IntType))
  }

  test("type check string literal") {
    val result = parseAndCheck("\"hello\"")
    assertEquals(result, Right(Type.StringType))
  }

  test("type check boolean literal") {
    val result = parseAndCheck("true")
    assertEquals(result, Right(Type.BoolType))
  }

  test("type check unit literal") {
    val result = parseAndCheck("()")
    assertEquals(result, Right(Type.UnitType))
  }

  test("type check simple lambda") {
    val result = parseAndCheck("fn x: Int => x")
    assertEquals(result, Right(Type.FunctionType(Type.IntType, Type.IntType)))
  }

  test("type check lambda with expression body") {
    val result = parseAndCheck("fn x: Int => x + 1")
    assertEquals(result, Right(Type.FunctionType(Type.IntType, Type.IntType)))
  }

  test("type check nested lambdas") {
    val result = parseAndCheck("fn x: Int => fn y: Int => x + y")
    assertEquals(
      result,
      Right(Type.FunctionType(Type.IntType, Type.FunctionType(Type.IntType, Type.IntType)))
    )
  }

  test("type check function application") {
    val result = parseAndCheck("(fn x: Int => x + 1) 5")
    assertEquals(result, Right(Type.IntType))
  }

  test("type check let binding") {
    val result = parseAndCheck("let x = 5 in x + 1")
    assertEquals(result, Right(Type.IntType))
  }

  test("type check let with lambda") {
    val result = parseAndCheck("let double = fn x: Int => x + x in double 3")
    assertEquals(result, Right(Type.IntType))
  }

  test("type check if expression") {
    val result = parseAndCheck("if true then 1 else 2")
    assertEquals(result, Right(Type.IntType))
  }

  test("type check arithmetic operations") {
    val result = parseAndCheck("1 + 2 * 3")
    assertEquals(result, Right(Type.IntType))
  }

  test("type check comparison") {
    val result = parseAndCheck("5 == 10")
    assertEquals(result, Right(Type.BoolType))
  }

  test("type check boolean operations") {
    val result = parseAndCheck("true && false")
    assertEquals(result, Right(Type.BoolType))
  }

  test("type check string concatenation") {
    val result = parseAndCheck("\"hello\" ++ \"world\"")
    assertEquals(result, Right(Type.StringType))
  }

  test("type check higher-order function") {
    val result = parseAndCheck("fn f: Int -> Int => fn x: Int => f (f x)")
    assertEquals(
      result,
      Right(
        Type.FunctionType(
          Type.FunctionType(Type.IntType, Type.IntType),
          Type.FunctionType(Type.IntType, Type.IntType)
        )
      )
    )
  }

  test("type check print") {
    val result = parseAndCheck("print \"hello\"")
    assertEquals(result, Right(Type.UnitType))
  }

  test("type check println") {
    val result = parseAndCheck("println \"world\"")
    assertEquals(result, Right(Type.UnitType))
  }

  test("type check complex nested expression") {
    val result = parseAndCheck("""
      let add = fn x: Int => fn y: Int => x + y in
      let result = add 5 10 in
      if result == 15 then result else 0
    """)
    assertEquals(result, Right(Type.IntType))
  }

  // Negative tests - should catch type errors

  test("reject unbound variable") {
    val result = parseAndCheck("x")
    assert(result.isLeft, "Should reject unbound variable")
    assert(result.left.getOrElse("").contains("Unbound variable"))
  }

  test("reject unbound variable in lambda body") {
    val result = parseAndCheck("fn x: Int => y")
    assert(result.isLeft, "Should reject unbound variable in lambda body")
    assert(result.left.getOrElse("").contains("Unbound variable"))
  }

  test("reject function application on non-function") {
    val result = parseAndCheck("5 10")
    assert(result.isLeft, "Should reject application on non-function")
    assert(result.left.getOrElse("").contains("Expected function type"))
  }

  test("reject argument type mismatch in application") {
    val result = parseAndCheck("(fn x: Int => x + 1) \"hello\"")
    assert(result.isLeft, "Should reject argument type mismatch")
    assert(result.left.getOrElse("").contains("Type mismatch"))
  }

  test("reject addition with non-Int types") {
    val result = parseAndCheck("\"hello\" + \"world\"")
    assert(result.isLeft, "Should reject addition on strings")
    assert(result.left.getOrElse("").contains("Type mismatch"))
  }

  test("reject addition with mixed types") {
    val result = parseAndCheck("5 + \"hello\"")
    assert(result.isLeft, "Should reject addition with mixed types")
    assert(result.left.getOrElse("").contains("Type mismatch"))
  }

  test("reject multiplication with non-Int types") {
    val result = parseAndCheck("true * false")
    assert(result.isLeft, "Should reject multiplication on booleans")
    assert(result.left.getOrElse("").contains("Type mismatch"))
  }

  test("reject comparison of different types") {
    val result = parseAndCheck("5 == \"five\"")
    assert(result.isLeft, "Should reject comparison of different types")
    assert(result.left.getOrElse("").contains("Type mismatch"))
  }

  test("reject if with non-Bool condition") {
    val result = parseAndCheck("if 42 then 1 else 2")
    assert(result.isLeft, "Should reject if with non-Bool condition")
    assert(result.left.getOrElse("").contains("Type mismatch"))
  }

  test("reject if branches with different types") {
    val result = parseAndCheck("if true then 1 else \"two\"")
    assert(result.isLeft, "Should reject if branches with different types")
    assert(result.left.getOrElse("").contains("Type mismatch"))
  }

  test("reject logical and with non-Bool") {
    val result = parseAndCheck("5 && 10")
    assert(result.isLeft, "Should reject && on non-Bool")
    assert(result.left.getOrElse("").contains("Type mismatch"))
  }

  test("reject logical and with mixed types") {
    val result = parseAndCheck("true && 5")
    assert(result.isLeft, "Should reject && with mixed types")
    assert(result.left.getOrElse("").contains("Type mismatch"))
  }

  test("reject not with non-Bool") {
    val result = parseAndCheck("not 42")
    assert(result.isLeft, "Should reject not on Int")
    assert(result.left.getOrElse("").contains("Type mismatch"))
  }

  test("reject string concatenation with non-String") {
    val result = parseAndCheck("\"hello\" ++ 42")
    assert(result.isLeft, "Should reject ++ with non-String")
    assert(result.left.getOrElse("").contains("Type mismatch"))
  }

  test("reject string concatenation with mixed types") {
    val result = parseAndCheck("5 ++ \"world\"")
    assert(result.isLeft, "Should reject ++ with mixed types")
    assert(result.left.getOrElse("").contains("Type mismatch"))
  }

  test("reject print with non-String") {
    val result = parseAndCheck("print 42")
    assert(result.isLeft, "Should reject print with non-String")
    assert(result.left.getOrElse("").contains("Type mismatch"))
  }

  test("reject println with non-String") {
    val result = parseAndCheck("println true")
    assert(result.isLeft, "Should reject println with non-String")
    assert(result.left.getOrElse("").contains("Type mismatch"))
  }

  test("reject higher-order function with wrong argument type") {
    val result = parseAndCheck("""
      let apply = fn f: Int -> Int => fn x: Int => f x in
      apply "not a function" 5
    """)
    assert(result.isLeft, "Should reject wrong function type")
    assert(result.left.getOrElse("").contains("Type mismatch"))
  }

  test("reject nested application with type error") {
    val result = parseAndCheck("""
      let f = fn x: Int => x + 1 in
      let g = fn x: String => x ++ "!" in
      f (g 5)
    """)
    assert(result.isLeft, "Should reject type error in nested application")
    assert(result.left.getOrElse("").contains("Type mismatch"))
  }

  test("reject let binding with type error in body") {
    val result = parseAndCheck("""
      let x = 5 in x ++ " is a number"
    """)
    assert(result.isLeft, "Should reject type error in let body")
    assert(result.left.getOrElse("").contains("Type mismatch"))
  }

  test("reject complex expression with buried type error") {
    val result = parseAndCheck("""
      let add = fn x: Int => fn y: Int => x + y in
      let result = add 5 "ten" in
      result
    """)
    assert(result.isLeft, "Should reject buried type error")
    assert(result.left.getOrElse("").contains("Type mismatch"))
  }

  test("allow shadowing with same type") {
    val result = parseAndCheck("""
      let x = 5 in
      let x = 10 in
      x
    """)
    assertEquals(result, Right(Type.IntType))
  }

  test("allow shadowing with different type") {
    val result = parseAndCheck("""
      let x = 5 in
      let x = "hello" in
      x
    """)
    assertEquals(result, Right(Type.StringType))
  }

  test("reject using outer binding after shadowing with wrong type") {
    val result = parseAndCheck("""
      let x = 5 in
      let x = "hello" in
      x + 1
    """)
    assert(result.isLeft, "Should reject type error with shadowed variable")
    assert(result.left.getOrElse("").contains("Type mismatch"))
  }

  test("type check lambda returning Unit") {
    val result = parseAndCheck("fn x: String => println x")
    assertEquals(result, Right(Type.FunctionType(Type.StringType, Type.UnitType)))
  }

  test("type check if expression returning Unit") {
    val result = parseAndCheck("if true then () else ()")
    assertEquals(result, Right(Type.UnitType))
  }

  test("type check let binding with Unit value") {
    val result = parseAndCheck("let u = () in u")
    assertEquals(result, Right(Type.UnitType))
  }
