package langdb.languages.nanoproc.ast

import langdb.common.SourceSpan

// Statements in NanoProc
enum Stmt derives CanEqual:
  // Variable declaration with initialization
  case VarDecl(name: String, typ: Type, init: Expr, span: SourceSpan)

  // Assignment to existing variable
  case Assign(name: String, value: Expr, span: SourceSpan)

  // Expression as statement (for side effects like procedure calls)
  case ExprStmt(expr: Expr, span: SourceSpan)

  // Return from procedure
  case Return(value: Expr, span: SourceSpan)

  // Conditional
  case If(cond: Expr, thenBlock: Block, elseBlock: Option[Block], span: SourceSpan)

  // While loop
  case While(cond: Expr, body: Block, span: SourceSpan)

  // Block of statements
  case Block(stmts: List[Stmt], span: SourceSpan)

  def sourceSpan: SourceSpan = this match
    case VarDecl(_, _, _, span) => span
    case Assign(_, _, span) => span
    case ExprStmt(_, span) => span
    case Return(_, span) => span
    case If(_, _, _, span) => span
    case While(_, _, span) => span
    case Block(_, span) => span
