# NanoProc Language Specification

## Core Design

**No higher-order functions:**
- Procedures are top-level definitions only
- No procedure types or passing procedures as values
- Simpler than MicroML in this regard

**Built-ins (same as MicroML):**
- `print(expr)` - returns Unit
- `println(expr)` - returns Unit

## Complete Syntax

### Variable Declarations

```nanoproc
var x: Int = 42;
var name: String = "Alice";
var flag: Bool = true;
```

### Assignment

```nanoproc
x = x + 1;
name = "Bob";
```

### Procedure Definitions (top-level only)

```nanoproc
proc greet(name: String): Unit {
  println("Hello, " ++ name);
}

proc add(x: Int, y: Int): Int {
  return x + y;
}
```

### Control Flow

```nanoproc
if (x > 0) {
  println("positive");
} else {
  println("non-positive");
}

while (x > 0) {
  x = x - 1;
  println(x);
}
```

### Expressions (within statements)

- Literals: `42`, `"hello"`, `true`, `false`, `()`
- Variables: `x`
- Binary: `+`, `*`, `==`, `&&`, `++`
- Unary: `not`
- Calls: `add(x, y)`, `print(x)`, `println(x)`

## Complete Example

```nanoproc
proc factorial(n: Int): Int {
  var result: Int = 1;
  var i: Int = n;
  while (i > 0) {
    result = result * i;
    i = i - 1;
  }
  return result;
}

proc main(): Unit {
  var x: Int = 5;
  var fact: Int = factorial(x);
  println(fact);
}
```

## AST Structure

### Program

```scala
case class Program(procs: List[ProcDef])
```

### Statements

```scala
enum Stmt:
  case VarDecl(name: String, typ: Type, init: Expr)
  case Assign(name: String, value: Expr)
  case ExprStmt(expr: Expr)  // for print/println calls
  case Return(value: Expr)
  case If(cond: Expr, thenBlock: Block, elseBlock: Option[Block])
  case While(cond: Expr, body: Block)
  case Block(stmts: List[Stmt])
```

### Expressions

```scala
enum Expr:
  case Var(name: String)
  case IntLit(value: Int)
  case StringLit(value: String)
  case BoolLit(value: Boolean)
  case UnitLit
  case Add(left: Expr, right: Expr)
  case Mult(left: Expr, right: Expr)
  case Eq(left: Expr, right: Expr)
  case And(left: Expr, right: Expr)
  case Not(operand: Expr)
  case StringConcat(left: Expr, right: Expr)
  case ProcCall(name: String, args: List[Expr])
  case Print(operand: Expr)
  case Println(operand: Expr)
```

### Procedure Definitions

```scala
case class ProcDef(
  name: String,
  params: List[(String, Type)],
  returnType: Type,
  body: Stmt.Block
)
```

### Types (reuse from MicroML)

```scala
enum Type:
  case IntType
  case StringType
  case BoolType
  case UnitType
```

## Summary

Clean, simple imperative language that contrasts with MicroML's functional style.
