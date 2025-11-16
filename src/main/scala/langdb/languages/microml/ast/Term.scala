package langdb.languages.microml.ast

import langdb.common.SourceSpan

// Terms in our lambda calculus
enum Term derives CanEqual:
  // Variables (references)
  case Var(name: String, span: SourceSpan)

  // Lambda abstraction (lam)
  case Lambda(param: String, paramType: Type, body: Term, span: SourceSpan)

  // Function application (app)
  case App(func: Term, arg: Term, span: SourceSpan)

  // Let binding (bnd)
  case Let(name: String, value: Term, body: Term, span: SourceSpan)

  // Literals
  case IntLit(value: Int, span: SourceSpan)
  case StringLit(value: String, span: SourceSpan)
  case BoolLit(value: Boolean, span: SourceSpan)
  case UnitLit(span: SourceSpan)

  // Binary operators
  case Add(left: Term, right: Term, span: SourceSpan)
  case Mult(left: Term, right: Term, span: SourceSpan)
  case Eq(left: Term, right: Term, span: SourceSpan)
  case And(left: Term, right: Term, span: SourceSpan)
  case StringConcat(left: Term, right: Term, span: SourceSpan)

  // Unary operators
  case Not(operand: Term, span: SourceSpan)

  // Control flow
  case If(cond: Term, thenBranch: Term, elseBranch: Term, span: SourceSpan)

  // Built-in I/O
  case Print(operand: Term, span: SourceSpan)
  case Println(operand: Term, span: SourceSpan)

  def sourceSpan: SourceSpan = this match
    case Var(_, span) => span
    case Lambda(_, _, _, span) => span
    case App(_, _, span) => span
    case Let(_, _, _, span) => span
    case IntLit(_, span) => span
    case StringLit(_, span) => span
    case BoolLit(_, span) => span
    case UnitLit(span) => span
    case Add(_, _, span) => span
    case Mult(_, _, span) => span
    case Eq(_, _, span) => span
    case And(_, _, span) => span
    case StringConcat(_, _, span) => span
    case Not(_, span) => span
    case If(_, _, _, span) => span
    case Print(_, span) => span
    case Println(_, span) => span
