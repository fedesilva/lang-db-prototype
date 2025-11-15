package langdb.languages.microml.ast

// Terms in our lambda calculus
enum Term derives CanEqual:
  // Variables (references)
  case Var(name: String)

  // Lambda abstraction (lam)
  case Lambda(param: String, paramType: Type, body: Term)

  // Function application (app)
  case App(func: Term, arg: Term)

  // Let binding (bnd)
  case Let(name: String, value: Term, body: Term)

  // Literals
  case IntLit(value: Int)
  case StringLit(value: String)
  case BoolLit(value: Boolean)

  // Built-in functions (assume they exist)
  case Add(left: Term, right: Term)
  case Mult(left: Term, right: Term)
  case Eq(left: Term, right: Term)
  case If(cond: Term, thenBranch: Term, elseBranch: Term)
  case Not(operand: Term)
  case And(left: Term, right: Term)
  case StringConcat(left: Term, right: Term)
  case Print(operand: Term)
  case Println(operand: Term)
