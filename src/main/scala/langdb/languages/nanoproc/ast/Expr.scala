package langdb.languages.nanoproc.ast

import langdb.common.SourceSpan

// Expressions in NanoProc
enum Expr derives CanEqual:
  // Variables
  case Var(name: String, span: SourceSpan)

  // Literals
  case IntLit(value: Int, span: SourceSpan)
  case StringLit(value: String, span: SourceSpan)
  case BoolLit(value: Boolean, span: SourceSpan)
  case UnitLit(span: SourceSpan)

  // Binary operators
  case Add(left: Expr, right: Expr, span: SourceSpan)
  case Sub(left: Expr, right: Expr, span: SourceSpan)
  case Mult(left: Expr, right: Expr, span: SourceSpan)
  case Div(left: Expr, right: Expr, span: SourceSpan)
  case Eq(left: Expr, right: Expr, span: SourceSpan)
  case Gt(left: Expr, right: Expr, span: SourceSpan)
  case Lt(left: Expr, right: Expr, span: SourceSpan)
  case Gte(left: Expr, right: Expr, span: SourceSpan)
  case Lte(left: Expr, right: Expr, span: SourceSpan)
  case And(left: Expr, right: Expr, span: SourceSpan)
  case StringConcat(left: Expr, right: Expr, span: SourceSpan)

  // Unary operators
  case Not(operand: Expr, span: SourceSpan)

  // Procedure call (as expression)
  case ProcCall(name: String, args: List[Expr], span: SourceSpan)

  // Built-in I/O
  case Print(operand: Expr, span: SourceSpan)
  case Println(operand: Expr, span: SourceSpan)

  def sourceSpan: SourceSpan = this match
    case Var(_, span) => span
    case IntLit(_, span) => span
    case StringLit(_, span) => span
    case BoolLit(_, span) => span
    case UnitLit(span) => span
    case Add(_, _, span) => span
    case Sub(_, _, span) => span
    case Mult(_, _, span) => span
    case Div(_, _, span) => span
    case Eq(_, _, span) => span
    case Gt(_, _, span) => span
    case Lt(_, _, span) => span
    case Gte(_, _, span) => span
    case Lte(_, _, span) => span
    case And(_, _, span) => span
    case StringConcat(_, _, span) => span
    case Not(_, span) => span
    case ProcCall(_, _, span) => span
    case Print(_, span) => span
    case Println(_, span) => span
