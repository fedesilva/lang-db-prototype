package langdb.languages.nanoproc.parser

import fastparse.*
import langdb.languages.nanoproc.ast.{Expr, Program, ProcDef, Stmt, Type}

import MultiLineWhitespace.*

/** Parser for NanoProc, a small imperative language.
  *
  * Syntax:
  *   - Variable declarations: var x: Int = 42;
  *   - Assignment: x = x + 1;
  *   - Procedures: proc name(x: Int, y: Int): Int { ... }
  *   - Control flow: if (cond) { ... } else { ... }, while (cond) { ... }
  *   - Literals: 42, "hello", true, false, ()
  *   - Operators: +, *, ==, &&, not, ++
  */
private[nanoproc] object ProgramParser:

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
    P(CharIn("0-9").rep(1).!.map(s => Expr.IntLit(s.toInt)))

  def stringLit[$: P]: P[Expr] =
    P("\"" ~ CharsWhile(_ != '"').! ~ "\"").map(Expr.StringLit.apply)

  def boolLit[$: P]: P[Expr] =
    P("true".!.map(_ => Expr.BoolLit(true)) | "false".!.map(_ => Expr.BoolLit(false)))

  def unitLit[$: P]: P[Expr] = P("()".!).map(_ => Expr.UnitLit)

  // Atomic expressions
  def atom[$: P]: P[Expr] =
    P(
      intLit |
        stringLit |
        boolLit |
        unitLit |
        procCall |
        identifier.map(Expr.Var.apply) |
        ("(" ~ expr ~ ")")
    )

  // Procedure call: name(arg1, arg2, ...)
  def procCall[$: P]: P[Expr] =
    P(identifier ~ "(" ~ expr.rep(0, ",") ~ ")").map { case (name, args) =>
      Expr.ProcCall(name, args.toList)
    }

  // Unary operators: not, print, println
  def unary[$: P]: P[Expr] =
    P(
      ("not" ~~ !(CharIn("a-zA-Z0-9_")) ~ unary).map(Expr.Not.apply) |
        ("println" ~~ !(CharIn("a-zA-Z0-9_")) ~ "(" ~ expr ~ ")").map(Expr.Println.apply) |
        ("print" ~~ !(CharIn("a-zA-Z0-9_")) ~ "(" ~ expr ~ ")").map(Expr.Print.apply) |
        atom
    )

  // Multiplicative operators: *, /
  def multiplicative[$: P]: P[Expr] =
    P(unary ~ (("*" ~ unary).map(("*", _)) | ("/" ~ unary).map(("/", _))).rep).map {
      case (first, rest) =>
        rest.foldLeft(first) { case (left, (op, right)) =>
          if op == "*" then Expr.Mult(left, right)
          else Expr.Div(left, right)
        }
    }

  // Additive operators: +, -, ++
  def additive[$: P]: P[Expr] =
    P(
      multiplicative ~ (("++" ~ multiplicative).map(("++", _)) | ("+" ~ multiplicative).map(
        ("+", _)
      ) | ("-" ~ multiplicative).map(("-", _))).rep
    ).map { case (first, rest) =>
      rest.foldLeft(first) { case (left, (op, right)) =>
        if op == "+" then Expr.Add(left, right)
        else if op == "-" then Expr.Sub(left, right)
        else Expr.StringConcat(left, right)
      }
    }

  // Comparison operators: ==, >, <, >=, <=
  def comparison[$: P]: P[Expr] =
    P(
      additive ~ (("<=" ~ additive).map(("<=", _)) |
        (">=" ~ additive).map((">=", _)) |
        ("==" ~ additive).map(("==", _)) |
        ("<" ~ additive).map(("<", _)) |
        (">" ~ additive).map((">", _))).?
    ).map {
      case (left, Some(("==", right))) => Expr.Eq(left, right)
      case (left, Some((">", right))) => Expr.Gt(left, right)
      case (left, Some(("<", right))) => Expr.Lt(left, right)
      case (left, Some((">=", right))) => Expr.Gte(left, right)
      case (left, Some(("<=", right))) => Expr.Lte(left, right)
      case (term, None) => term
      case (_, Some((op, _))) => throw new RuntimeException(s"Unknown operator: $op")
    }

  // Logical operators: &&
  def logical[$: P]: P[Expr] =
    P(comparison ~ ("&&" ~ comparison).rep).map { case (first, rest) =>
      rest.foldLeft(first)((left, right) => Expr.And(left, right))
    }

  // Top-level expression parser
  def expr[$: P]: P[Expr] = P(logical)

  // Statement parsers

  // Variable declaration: var x: Int = 42;
  def varDecl[$: P]: P[Stmt] =
    P("var" ~ identifier ~ ":" ~ typeExpr ~ "=" ~ expr ~ ";").map { case (name, typ, init) =>
      Stmt.VarDecl(name, typ, init)
    }

  // Assignment: x = expr;
  def assign[$: P]: P[Stmt] =
    P(identifier ~ "=" ~ expr ~ ";").map { case (name, value) =>
      Stmt.Assign(name, value)
    }

  // Expression statement: println("hello");
  def exprStmt[$: P]: P[Stmt] =
    P(expr ~ ";").map(Stmt.ExprStmt.apply)

  // Return statement: return expr;
  def returnStmt[$: P]: P[Stmt] =
    P("return" ~ expr ~ ";").map(Stmt.Return.apply)

  // Block: { stmt1 stmt2 ... }
  def block[$: P]: P[Stmt.Block] =
    P("{" ~ stmt.rep ~ "}").map(stmts => Stmt.Block(stmts.toList))

  // If statement: if (cond) { ... } else { ... }
  def ifStmt[$: P]: P[Stmt] =
    P("if" ~ "(" ~ expr ~ ")" ~ block ~ ("else" ~ block).?).map {
      case (cond, thenBlock, elseBlock) =>
        Stmt.If(cond, thenBlock, elseBlock)
    }

  // While loop: while (cond) { ... }
  def whileStmt[$: P]: P[Stmt] =
    P("while" ~ "(" ~ expr ~ ")" ~ block).map { case (cond, body) =>
      Stmt.While(cond, body)
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
    P("proc" ~ identifier ~ "(" ~ param.rep(0, ",") ~ ")" ~ ":" ~ typeExpr ~ block).map {
      case (name, params, returnType, body) =>
        ProcDef(name, params.toList, returnType, body)
    }

  // Complete program: one or more procedure definitions
  def program[$: P]: P[Program] =
    P(Start ~ procDef.rep(1) ~ End).map(procs => Program(procs.toList))

  // Parse a string into a Program
  def parse(input: String): Either[String, Program] =
    fastparse.parse(input, program(_)) match
      case Parsed.Success(prog, _) => Right(prog)
      case f: Parsed.Failure => Left(f.trace().longMsg)
