package langdb.parser

import fastparse.*
import langdb.ast.{Term, Type}

import MultiLineWhitespace.*

object TermParser:

  // Type parsers
  def baseType[$: P]: P[Type] =
    P(
      "Int".!.map(_ => Type.IntType) |
        "String".!.map(_ => Type.StringType) |
        "Bool".!.map(_ => Type.BoolType) |
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
    P(CharIn("0-9").rep(1).!.map(s => Term.IntLit(s.toInt)))

  def stringLit[$: P]: P[Term] =
    P("\"" ~ CharsWhile(_ != '"').! ~ "\"").map(Term.StringLit.apply)

  def boolLit[$: P]: P[Term] =
    P("true".!.map(_ => Term.BoolLit(true)) | "false".!.map(_ => Term.BoolLit(false)))

  // Atomic expressions (highest precedence)
  def atom[$: P]: P[Term] =
    P(
      lambda |
        letExpr |
        ifExpr |
        intLit |
        stringLit |
        boolLit |
        identifier.map(Term.Var.apply) |
        ("(" ~ expr ~ ")")
    )

  // Lambda expressions: fn x: Int => body
  def lambda[$: P]: P[Term] =
    P("fn" ~ identifier ~ ":" ~ typeExpr ~ "=>" ~ expr).map { case (param, paramType, body) =>
      Term.Lambda(param, paramType, body)
    }

  // Let bindings: let x = value in body
  def letExpr[$: P]: P[Term] =
    P("let" ~ identifier ~ "=" ~ expr ~ "in" ~ expr).map { case (name, value, body) =>
      Term.Let(name, value, body)
    }

  // If expressions: if cond then thenBranch else elseBranch
  def ifExpr[$: P]: P[Term] =
    P("if" ~ expr ~ "then" ~ expr ~ "else" ~ expr).map { case (cond, thenBranch, elseBranch) =>
      Term.If(cond, thenBranch, elseBranch)
    }

  // Application: f x y (left-associative)
  def application[$: P]: P[Term] =
    P(atom.rep(1)).map { terms =>
      terms.tail.foldLeft(terms.head)((func, arg) => Term.App(func, arg))
    }

  // Unary operators: not, print, println (order matters - longest first!)
  def unary[$: P]: P[Term] =
    P(
      ("not" ~~ !(CharIn("a-zA-Z0-9_")) ~ unary).map(Term.Not.apply) |
        ("println" ~~ !(CharIn("a-zA-Z0-9_")) ~ unary).map(Term.Println.apply) |
        ("print" ~~ !(CharIn("a-zA-Z0-9_")) ~ unary).map(Term.Print.apply) |
        application
    )

  // Multiplicative operators: *, (left-associative)
  def multiplicative[$: P]: P[Term] =
    P(unary ~ ("*" ~ unary).rep).map { case (first, rest) =>
      rest.foldLeft(first)((left, right) => Term.Mult(left, right))
    }

  // Additive operators: +, ++ (left-associative)
  def additive[$: P]: P[Term] =
    P(
      multiplicative ~ (("++" ~ multiplicative).map(("++", _)) | ("+" ~ multiplicative).map(
        ("+", _)
      )).rep
    ).map { case (first, rest) =>
      rest.foldLeft(first) { case (left, (op, right)) =>
        if op == "+" then Term.Add(left, right)
        else Term.StringConcat(left, right)
      }
    }

  // Comparison operators: ==, (non-associative)
  def comparison[$: P]: P[Term] =
    P(additive ~ ("==" ~ additive).?).map {
      case (left, Some(right)) => Term.Eq(left, right)
      case (term, None) => term
    }

  // Logical operators: &&, (left-associative)
  def logical[$: P]: P[Term] =
    P(comparison ~ ("&&" ~ comparison).rep).map { case (first, rest) =>
      rest.foldLeft(first)((left, right) => Term.And(left, right))
    }

  // Top-level expression parser
  def expr[$: P]: P[Term] = P(logical)

  // Parse a complete program (expression with optional surrounding whitespace)
  def program[$: P]: P[Term] = P(Start ~ expr ~ End)

  // Convenience method to parse a string
  def parse(input: String): Either[String, Term] =
    fastparse.parse(input, program(_)) match
      case Parsed.Success(term, _) => Right(term)
      case f: Parsed.Failure => Left(f.trace().longMsg)
