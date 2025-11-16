package langdb.languages.nanoproc.ast

// Statements in NanoProc
enum Stmt derives CanEqual:
  // Variable declaration with initialization
  case VarDecl(name: String, typ: Type, init: Expr)

  // Assignment to existing variable
  case Assign(name: String, value: Expr)

  // Expression as statement (for side effects like procedure calls)
  case ExprStmt(expr: Expr)

  // Return from procedure
  case Return(value: Expr)

  // Conditional
  case If(cond: Expr, thenBlock: Block, elseBlock: Option[Block])

  // While loop
  case While(cond: Expr, body: Block)

  // Block of statements
  case Block(stmts: List[Stmt])
