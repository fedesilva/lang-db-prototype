package langdb.languages.nanoproc.parser

import fastparse.*
import langdb.common.SourceSpan
import langdb.languages.nanoproc.ast.{Expr, ProcDef, Program, Stmt, Type}

import MultiLineWhitespace.*

/** Parser for NanoProc, a small imperative language. */
private[nanoproc] class ProgramParserInstance(input: String, source: String):

  // Precompute positions once to avoid O(n²) performance
  private val positions: Map[Int, SourceSpan.Position] = SourceSpan.computePositions(input)

  private def makeSpan(start: Int, end: Int): SourceSpan =
    SourceSpan.fromPositions(source, positions, start, end)

  // Helpers to capture source spans
  def withSpanExpr[$: P, T](p: => P[T])(f: (T, SourceSpan) => Expr): P[Expr] =
    P(Index ~ p ~ Index).map { case (start, result, end) =>
      f(result, makeSpan(start, end))
    }

  def withSpanStmt[$: P, T](p: => P[T])(f: (T, SourceSpan) => Stmt): P[Stmt] =
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

  def typeExpr[$: P]: P[Type] = P(baseType)

  // Identifier parser
  def identifier[$: P]: P[String] =
    P(CharIn("a-zA-Z_") ~~ CharsWhileIn("a-zA-Z0-9_").?).!.filter(!keywords.contains(_))

  val keywords: Set[String] =
    Set(
      "var",
      "proc",
      "if",
      "else",
      "while",
      "return",
      "true",
      "false",
      "not",
      "print",
      "println"
    )

  // Literal parsers
  def intLit[$: P]: P[Expr] =
    import NoWhitespace.*
    withSpanExpr(CharIn("0-9").rep(1).!)((s, span) => Expr.IntLit(s.toInt, span))

  def stringLit[$: P]: P[Expr] =
    withSpanExpr("\"" ~ CharsWhile(_ != '"').! ~ "\"")((s, span) => Expr.StringLit(s, span))

  def boolLit[$: P]: P[Expr] =
    withSpanExpr(P("true").!.map(_ => true) | P("false").!.map(_ => false))((b, span) =>
      Expr.BoolLit(b, span)
    )

  def unitLit[$: P]: P[Expr] =
    withSpanExpr("()".!)((_, span) => Expr.UnitLit(span))

  // Variables
  def varExpr[$: P]: P[Expr] =
    withSpanExpr(identifier)((name, span) => Expr.Var(name, span))

  // Atomic expressions
  def atom[$: P]: P[Expr] =
    P(
      intLit |
        stringLit |
        boolLit |
        unitLit |
        procCall |
        varExpr |
        ("(" ~ expr ~ ")")
    )

  // Procedure call: name(arg1, arg2, ...)
  def procCall[$: P]: P[Expr] =
    withSpanExpr(identifier ~ "(" ~ expr.rep(0, ",") ~ ")") { case ((name, args), span) =>
      Expr.ProcCall(name, args.toList, span)
    }

  // Unary operators: not, print, println
  def unary[$: P]: P[Expr] =
    P(
      withSpanExpr("not" ~~ !(CharIn("a-zA-Z0-9_")) ~ unary)((operand, span) =>
        Expr.Not(operand, span)
      ) |
        withSpanExpr("println" ~~ !(CharIn("a-zA-Z0-9_")) ~ "(" ~ expr ~ ")")((operand, span) =>
          Expr.Println(operand, span)
        ) |
        withSpanExpr("print" ~~ !(CharIn("a-zA-Z0-9_")) ~ "(" ~ expr ~ ")")((operand, span) =>
          Expr.Print(operand, span)
        ) |
        atom
    )

  // Multiplicative operators: *, /
  def multiplicative[$: P]: P[Expr] =
    P(Index ~ unary ~ (("*" ~ unary).map(("*", _)) | ("/" ~ unary).map(("/", _))).rep ~ Index).map {
      case (start, first, rest, end) =>
        val span = makeSpan(start, end)
        rest.foldLeft(first) { case (left, (op, right)) =>
          if op == "*" then Expr.Mult(left, right, span)
          else Expr.Div(left, right, span)
        }
    }

  // Additive operators: +, -, ++
  def additive[$: P]: P[Expr] =
    P(
      Index ~ multiplicative ~ (("++" ~ multiplicative).map(("++", _)) | ("+" ~ multiplicative).map(
        ("+", _)
      ) | ("-" ~ multiplicative).map(("-", _))).rep ~ Index
    ).map { case (start, first, rest, end) =>
      val span = makeSpan(start, end)
      rest.foldLeft(first) { case (left, (op, right)) =>
        if op == "+" then Expr.Add(left, right, span)
        else if op == "-" then Expr.Sub(left, right, span)
        else Expr.StringConcat(left, right, span)
      }
    }

  // Comparison operators: ==, >, <, >=, <=
  def comparison[$: P]: P[Expr] =
    P(
      Index ~ additive ~ (("<=" ~ additive).map(("<=", _)) |
        (">=" ~ additive).map((">=", _)) |
        ("==" ~ additive).map(("==", _)) |
        ("<" ~ additive).map(("<", _)) |
        (">" ~ additive).map((">", _))).? ~ Index
    ).map { case (start, left, maybeRight, end) =>
      maybeRight match
        case Some(("==", right)) => Expr.Eq(left, right, makeSpan(start, end))
        case Some((">", right)) => Expr.Gt(left, right, makeSpan(start, end))
        case Some(("<", right)) => Expr.Lt(left, right, makeSpan(start, end))
        case Some((">=", right)) => Expr.Gte(left, right, makeSpan(start, end))
        case Some(("<=", right)) => Expr.Lte(left, right, makeSpan(start, end))
        case Some((op, _)) => throw new RuntimeException(s"Unknown operator: $op")
        case None => left
    }

  // Logical operators: &&
  def logical[$: P]: P[Expr] =
    P(Index ~ comparison ~ ("&&" ~ comparison).rep ~ Index).map { case (start, first, rest, end) =>
      val span = makeSpan(start, end)
      rest.foldLeft(first)((left, right) => Expr.And(left, right, span))
    }

  // Top-level expression parser
  def expr[$: P]: P[Expr] = P(logical)

  // Statement parsers

  // Variable declaration: var x: Int = 42;
  def varDecl[$: P]: P[Stmt] =
    withSpanStmt("var" ~ identifier ~ ":" ~ typeExpr ~ "=" ~ expr ~ ";") {
      case ((name, typ, init), span) =>
        Stmt.VarDecl(name, typ, init, span)
    }

  // Assignment: x = expr;
  def assign[$: P]: P[Stmt] =
    withSpanStmt(identifier ~ "=" ~ expr ~ ";") { case ((name, value), span) =>
      Stmt.Assign(name, value, span)
    }

  // Expression statement: println("hello");
  def exprStmt[$: P]: P[Stmt] =
    withSpanStmt(expr ~ ";")((expr, span) => Stmt.ExprStmt(expr, span))

  // Return statement: return expr;
  def returnStmt[$: P]: P[Stmt] =
    withSpanStmt("return" ~ expr ~ ";")((value, span) => Stmt.Return(value, span))

  // Block: { stmt1 stmt2 ... }
  def block[$: P]: P[Stmt.Block] =
    P(Index ~ "{" ~ stmt.rep ~ "}" ~ Index).map { case (start, stmts, end) =>
      Stmt.Block(stmts.toList, makeSpan(start, end))
    }

  // If statement: if (cond) { ... } else { ... }
  def ifStmt[$: P]: P[Stmt] =
    withSpanStmt("if" ~ "(" ~ expr ~ ")" ~ block ~ ("else" ~ block).?) {
      case ((cond, thenBlock, elseBlock), span) =>
        Stmt.If(cond, thenBlock, elseBlock, span)
    }

  // While loop: while (cond) { ... }
  def whileStmt[$: P]: P[Stmt] =
    withSpanStmt("while" ~ "(" ~ expr ~ ")" ~ block) { case ((cond, body), span) =>
      Stmt.While(cond, body, span)
    }

  // Statement (order matters for disambiguation)
  def stmt[$: P]: P[Stmt] =
    P(
      varDecl |
        returnStmt |
        ifStmt |
        whileStmt |
        assign |
        exprStmt
    )

  // Procedure parameter: name: Type
  def param[$: P]: P[(String, Type)] =
    P(identifier ~ ":" ~ typeExpr)

  // Procedure definition: proc name(x: Int, y: Int): RetType { ... }
  def procDef[$: P]: P[ProcDef] =
    P(Index ~ "proc" ~ identifier ~ "(" ~ param.rep(0, ",") ~ ")" ~ ":" ~ typeExpr ~ block ~ Index)
      .map { case (start, name, params, returnType, body, end) =>
        ProcDef(name, params.toList, returnType, body, makeSpan(start, end))
      }

  // Complete program: one or more procedure definitions
  def program[$: P]: P[Program] =
    P(Start ~ Index ~ procDef.rep(1) ~ Index ~ End).map { case (start, procs, end) =>
      Program(procs.toList, makeSpan(start, end))
    }

private[nanoproc] object ProgramParser:
  // Parse a string into a Program
  def parse(input: String, source: String = "<input>"): Either[String, Program] =
    val instance = ProgramParserInstance(input, source)
    fastparse.parse(input, instance.program(_)) match
      case Parsed.Success(prog, _) => Right(prog)
      case f: Parsed.Failure => Left(f.trace().longMsg)
