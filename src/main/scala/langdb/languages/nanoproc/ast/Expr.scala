package langdb.languages.nanoproc.ast

// Expressions in NanoProc
enum Expr derives CanEqual:
  // Variables
  case Var(name: String)

  // Literals
  case IntLit(value: Int)
  case StringLit(value: String)
  case BoolLit(value: Boolean)
  case UnitLit

  // Binary operators
  case Add(left: Expr, right: Expr)
  case Sub(left: Expr, right: Expr)
  case Mult(left: Expr, right: Expr)
  case Div(left: Expr, right: Expr)
  case Eq(left: Expr, right: Expr)
  case Gt(left: Expr, right: Expr)
  case Lt(left: Expr, right: Expr)
  case Gte(left: Expr, right: Expr)
  case Lte(left: Expr, right: Expr)
  case And(left: Expr, right: Expr)
  case StringConcat(left: Expr, right: Expr)

  // Unary operators
  case Not(operand: Expr)

  // Procedure call (as expression)
  case ProcCall(name: String, args: List[Expr])

  // Built-in I/O
  case Print(operand: Expr)
  case Println(operand: Expr)
