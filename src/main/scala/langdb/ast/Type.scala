package langdb.ast

// Types in our lambda calculus
enum Type:
  case IntType
  case StringType
  case BoolType
  case FunctionType(from: Type, to: Type)
