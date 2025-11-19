package langdb.languages.microml.parser

import langdb.languages.microml.ast.{Term, Type}
import munit.FunSuite

class MicroMLParserSpec extends FunSuite:

  test("parse integer literal") {
    val result = MicroMLParser.parse("42")
    assert(result.isRight)
    assert(result.toOption.get.isInstanceOf[Term.IntLit])
  }

  test("parse string literal") {
    val result = MicroMLParser.parse("\"hello\"")
    assert(result.isRight)
    assert(result.toOption.get.isInstanceOf[Term.StringLit])
  }

  test("parse boolean literals") {
    assert(MicroMLParser.parse("true").isRight)
    assert(MicroMLParser.parse("false").isRight)
  }

  test("parse variable") {
    val result = MicroMLParser.parse("x")
    assert(result.isRight)
    assert(result.toOption.get.isInstanceOf[Term.Var])
  }

  test("parse simple lambda") {
    val result = MicroMLParser.parse("fn x: Int => x")
    assert(result.isRight)
    assert(result.toOption.get.isInstanceOf[Term.Lambda])
  }

  test("parse lambda with body expression") {
    val result = MicroMLParser.parse("fn x: Int => x + 1")
    assert(result.isRight)
    assert(result.toOption.get.isInstanceOf[Term.Lambda])
  }

  test("parse nested lambdas") {
    val result = MicroMLParser.parse("fn x: Int => fn y: Int => x + y")
    assert(result.isRight)
    val outer = result.toOption.get.asInstanceOf[Term.Lambda]
    assert(outer.body.isInstanceOf[Term.Lambda])
  }

  test("parse function application") {
    val result = MicroMLParser.parse("f x")
    assert(result.isRight)
    assert(result.toOption.get.isInstanceOf[Term.App])
  }

  test("parse multiple applications (left-associative)") {
    val result = MicroMLParser.parse("f x y")
    assert(result.isRight)
    val outer = result.toOption.get.asInstanceOf[Term.App]
    assert(outer.func.isInstanceOf[Term.App])
  }

  test("parse let binding") {
    val result = MicroMLParser.parse("let x = 5 in x + 1")
    assert(result.isRight)
    assert(result.toOption.get.isInstanceOf[Term.Let])
  }

  test("parse let with lambda value") {
    val result = MicroMLParser.parse("let double = fn x: Int => x + x in double 3")
    assert(result.isRight)
    val let = result.toOption.get.asInstanceOf[Term.Let]
    assert(let.value.isInstanceOf[Term.Lambda])
    assert(let.body.isInstanceOf[Term.App])
  }

  test("parse if expression") {
    val result = MicroMLParser.parse("if true then 1 else 2")
    assert(result.isRight)
    assert(result.toOption.get.isInstanceOf[Term.If])
  }

  test("parse if with condition") {
    val result = MicroMLParser.parse("if x == 0 then 1 else x")
    assert(result.isRight)
    val ifExpr = result.toOption.get.asInstanceOf[Term.If]
    assert(ifExpr.cond.isInstanceOf[Term.Eq])
  }

  test("parse addition") {
    val result = MicroMLParser.parse("1 + 2")
    assert(result.isRight)
    assert(result.toOption.get.isInstanceOf[Term.Add])
  }

  test("parse multiplication") {
    val result = MicroMLParser.parse("2 * 3")
    assert(result.isRight)
    assert(result.toOption.get.isInstanceOf[Term.Mult])
  }

  test("parse multiplication has higher precedence than addition") {
    val result = MicroMLParser.parse("1 + 2 * 3")
    assert(result.isRight)
    val add = result.toOption.get.asInstanceOf[Term.Add]
    assert(add.right.isInstanceOf[Term.Mult])
  }

  test("parse parentheses for grouping") {
    val result = MicroMLParser.parse("(1 + 2) * 3")
    assert(result.isRight)
    val mult = result.toOption.get.asInstanceOf[Term.Mult]
    assert(mult.left.isInstanceOf[Term.Add])
  }

  test("parse equality") {
    val result = MicroMLParser.parse("x == 5")
    assert(result.isRight)
    assert(result.toOption.get.isInstanceOf[Term.Eq])
  }

  test("parse logical and") {
    val result = MicroMLParser.parse("true && false")
    assert(result.isRight)
    assert(result.toOption.get.isInstanceOf[Term.And])
  }

  test("parse not") {
    val result = MicroMLParser.parse("not true")
    assert(result.isRight)
    assert(result.toOption.get.isInstanceOf[Term.Not])
  }

  test("parse string concatenation") {
    val result = MicroMLParser.parse("\"hello\" ++ \"world\"")
    assert(result.isRight)
    assert(result.toOption.get.isInstanceOf[Term.StringConcat])
  }

  test("parse print") {
    val result = MicroMLParser.parse("print \"hello\"")
    assert(result.isRight)
    assert(result.toOption.get.isInstanceOf[Term.Print])
  }

  test("parse println") {
    val result = MicroMLParser.parse("println \"world\"")
    assert(result.isRight)
    assert(result.toOption.get.isInstanceOf[Term.Println])
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
    assert(result.isRight)
    val lambda = result.toOption.get.asInstanceOf[Term.Lambda]
    assertEquals(lambda.paramType, Type.FunctionType(Type.IntType, Type.IntType))
  }

  test("parse higher-order function types") {
    val result = MicroMLParser.parse("fn f: Int -> Int -> Int => f 1 2")
    assert(result.isRight)
    val lambda = result.toOption.get.asInstanceOf[Term.Lambda]
    assertEquals(
      lambda.paramType,
      Type.FunctionType(Type.IntType, Type.FunctionType(Type.IntType, Type.IntType))
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
    assert(result.isRight)
    assert(result.toOption.get.isInstanceOf[Term.Let])
  }

  test("parse nested if expressions") {
    val input  = "if x == 0 then 0 else if x == 1 then 1 else 2"
    val result = MicroMLParser.parse(input)
    assert(result.isRight)
    val ifExpr = result.toOption.get.asInstanceOf[Term.If]
    assert(ifExpr.elseBranch.isInstanceOf[Term.If])
  }

  test("parse operators with correct associativity") {
    val result = MicroMLParser.parse("1 + 2 + 3")
    assert(result.isRight)
    val outer = result.toOption.get.asInstanceOf[Term.Add]
    assert(outer.left.isInstanceOf[Term.Add])
  }

  test("parse unit literal") {
    val result = MicroMLParser.parse("()")
    assert(result.isRight)
    assert(result.toOption.get.isInstanceOf[Term.UnitLit])
  }

  test("typecheck unit literal") {
    import cats.effect.unsafe.implicits.global
    import langdb.languages.microml.typechecker.TypeChecker

    val Right(term) = MicroMLParser.parse("()")
    val result      = TypeChecker.typeCheck(term).unsafeRunSync()

    assertEquals(result, Type.UnitType)
  }

  test("parse and typecheck println expression") {
    import cats.effect.unsafe.implicits.global
    import langdb.languages.microml.typechecker.TypeChecker

    val code   = "println \"hello\""
    val parsed = MicroMLParser.parse(code)

    assert(parsed.isRight, "Parsing failed")

    val typeChecked = TypeChecker.typeCheck(parsed.toOption.get).unsafeRunSync()

    assertEquals(typeChecked, Type.UnitType)
  }

  test("parse and typecheck a function returning unit") {
    import cats.effect.unsafe.implicits.global
    import langdb.languages.microml.typechecker.TypeChecker

    val code   = "fn x: String => println x"
    val parsed = MicroMLParser.parse(code)

    assert(parsed.isRight, "Parsing failed")

    val typeChecked = TypeChecker.typeCheck(parsed.toOption.get).unsafeRunSync()

    assertEquals(typeChecked, Type.FunctionType(Type.StringType, Type.UnitType))
  }

  test("parse and typecheck a let binding with a unit value") {
    import cats.effect.unsafe.implicits.global
    import langdb.languages.microml.typechecker.TypeChecker

    val code   = "let u = () in u"
    val parsed = MicroMLParser.parse(code)

    assert(parsed.isRight, "Parsing failed")

    val typeChecked = TypeChecker.typeCheck(parsed.toOption.get).unsafeRunSync()

    assertEquals(typeChecked, Type.UnitType)
  }

  test("parse and typecheck if expression returning unit") {
    import cats.effect.unsafe.implicits.global
    import langdb.languages.microml.typechecker.TypeChecker

    val code   = "if true then () else ()"
    val parsed = MicroMLParser.parse(code)

    assert(parsed.isRight, "Parsing failed")

    val typeChecked = TypeChecker.typeCheck(parsed.toOption.get).unsafeRunSync()

    assertEquals(typeChecked, Type.UnitType)
  }
