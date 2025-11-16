package langdb.languages.microml.parser

import fastparse.*
import langdb.common.SourceSpan
import langdb.languages.microml.ast.{Term, Type}

import MultiLineWhitespace.*

/** Parser for MicroML, a minimalistic ML-like language.
  *
  * Syntax:
  *   - Lambdas: fn x: Int => body
  *   - Let bindings: let x = value in body
  *   - Application: f x y (space-separated, left-associative)
  *   - Literals: 42, "hello", true, false
  *   - Operators: +, *, ==, &&, not, ++
  *   - Conditionals: if cond then e1 else e2
  *   - Types: Int, String, Bool, A -> B
  */
private[microml] class TermParserInstance(input: String, source: String):

  private def makeSpan(start: Int, end: Int): SourceSpan =
    SourceSpan.fromIndices(source, input, start, end)

  // Helper to capture source spans
  def withSpan[$: P, T](p: => P[T])(f: (T, SourceSpan) => Term): P[Term] =
    P(Index ~ p ~ Index).map { case (start, result, end) =>
      f(result, makeSpan(start, end))
    }

  // Type parsers
  def baseType[$: P]: P[Type] =
    P(
      "Int".!.map(_ => Type.IntType) |
        "String".!.map(_ => Type.StringType) |
        "Bool".!.map(_ => Type.BoolType) |
        "Unit".!.map(_ => Type.UnitType) |
        ("(" ~ typeExpr ~ ")")
    )

  def typeExpr[$: P]: P[Type] =
    P(baseType ~ ("->" ~ typeExpr).?).map {
      case (from, Some(to)) => Type.FunctionType(from, to)
      case (typ, None) => typ
    }

  // Identifier parser (variables and parameter names)
  def identifier[$: P]: P[String] =
    P(CharIn("a-zA-Z_") ~~ CharsWhileIn("a-zA-Z0-9_").?).!.filter(!keywords.contains(_))

  val keywords: Set[String] =
    Set("fn", "let", "in", "if", "then", "else", "true", "false", "not", "print", "println")

  // Literal parsers
  def intLit[$: P]: P[Term] =
    import NoWhitespace.*
    withSpan(CharIn("0-9").rep(1).!)((s, span) => Term.IntLit(s.toInt, span))

  def stringLit[$: P]: P[Term] =
    withSpan("\"" ~ CharsWhile(_ != '"').! ~ "\"")((s, span) => Term.StringLit(s, span))

  def boolLit[$: P]: P[Term] =
    withSpan("true".! | "false".!)((s, span) => Term.BoolLit(s == "true", span))

  def unitLit[$: P]: P[Term] =
    withSpan("()".!)((_, span) => Term.UnitLit(span))

  // Variables
  def varExpr[$: P]: P[Term] =
    withSpan(identifier)((name, span) => Term.Var(name, span))

  // Atomic expressions (highest precedence)
  def atom[$: P]: P[Term] =
    P(
      lambda |
        letExpr |
        ifExpr |
        intLit |
        stringLit |
        boolLit |
        unitLit |
        varExpr |
        ("(" ~ expr ~ ")")
    )

  // Lambda expressions: fn x: Int => body
  def lambda[$: P]: P[Term] =
    withSpan("fn" ~ identifier ~ ":" ~ typeExpr ~ "=>" ~ expr) {
      case ((param, paramType, body), span) =>
        Term.Lambda(param, paramType, body, span)
    }

  // Let bindings: let x = value in body
  def letExpr[$: P]: P[Term] =
    withSpan("let" ~ identifier ~ "=" ~ expr ~ "in" ~ expr) { case ((name, value, body), span) =>
      Term.Let(name, value, body, span)
    }

  // If expressions: if cond then thenBranch else elseBranch
  def ifExpr[$: P]: P[Term] =
    withSpan("if" ~ expr ~ "then" ~ expr ~ "else" ~ expr) {
      case ((cond, thenBranch, elseBranch), span) =>
        Term.If(cond, thenBranch, elseBranch, span)
    }

  // Application: f x y (left-associative)
  def application[$: P]: P[Term] =
    P(Index ~ atom.rep(1) ~ Index).map { case (start, terms, end) =>
      val span = makeSpan(start, end)
      terms.tail.foldLeft(terms.head)((func, arg) => Term.App(func, arg, span))
    }

  // Unary operators: not, print, println (order matters - longest first!)
  def unary[$: P]: P[Term] =
    P(
      withSpan("not" ~~ !(CharIn("a-zA-Z0-9_")) ~ unary)((operand, span) =>
        Term.Not(operand, span)
      ) |
        withSpan("println" ~~ !(CharIn("a-zA-Z0-9_")) ~ unary)((operand, span) =>
          Term.Println(operand, span)
        ) |
        withSpan("print" ~~ !(CharIn("a-zA-Z0-9_")) ~ unary)((operand, span) =>
          Term.Print(operand, span)
        ) |
        application
    )

  // Multiplicative operators: *, (left-associative)
  def multiplicative[$: P]: P[Term] =
    P(Index ~ unary ~ ("*" ~ unary).rep ~ Index).map { case (start, first, rest, end) =>
      val span = makeSpan(start, end)
      rest.foldLeft(first)((left, right) => Term.Mult(left, right, span))
    }

  // Additive operators: +, ++ (left-associative)
  def additive[$: P]: P[Term] =
    P(
      Index ~ multiplicative ~ (("++" ~ multiplicative).map(("++", _)) | ("+" ~ multiplicative).map(
        ("+", _)
      )).rep ~ Index
    ).map { case (start, first, rest, end) =>
      val span = makeSpan(start, end)
      rest.foldLeft(first) { case (left, (op, right)) =>
        if op == "+" then Term.Add(left, right, span)
        else Term.StringConcat(left, right, span)
      }
    }

  // Comparison operators: ==, (non-associative)
  def comparison[$: P]: P[Term] =
    P(Index ~ additive ~ ("==" ~ additive).? ~ Index).map { case (start, left, maybeRight, end) =>
      maybeRight match
        case Some(right) =>
          val span = makeSpan(start, end)
          Term.Eq(left, right, span)
        case None => left
    }

  // Logical operators: &&, (left-associative)
  def logical[$: P]: P[Term] =
    P(Index ~ comparison ~ ("&&" ~ comparison).rep ~ Index).map { case (start, first, rest, end) =>
      val span = makeSpan(start, end)
      rest.foldLeft(first)((left, right) => Term.And(left, right, span))
    }

  // Top-level expression parser
  def expr[$: P]: P[Term] = P(logical)

  // Parse a complete program (expression with optional surrounding whitespace)
  def program[$: P]: P[Term] = P(Start ~ expr ~ End)

private[microml] object TermParser:
  // Convenience method to parse a string
  def parse(input: String, source: String = "<input>"): Either[String, Term] =
    val instance = TermParserInstance(input, source)
    fastparse.parse(input, instance.program(_)) match
      case Parsed.Success(term, _) => Right(term)
      case f: Parsed.Failure => Left(f.trace().longMsg)
