package langdb.ast

// Types in our lambda calculus
enum Type:
  case IntType
  case StringType
  case BoolType
  case FunctionType(from: Type, to: Type)

  override def toString: String = this match
    case IntType => "Int"
    case StringType => "String"
    case BoolType => "Bool"
    case FunctionType(from, to) => s"($from -> $to)"
