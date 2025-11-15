package langdb.ast

// Types in our lambda calculus
enum Type derives CanEqual:
  case IntType
  case StringType
  case BoolType
  case FunctionType(from: Type, to: Type)
