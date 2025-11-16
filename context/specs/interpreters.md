# AST Walking Interpreters

Estimation for implementing simple AST-walking interpreters for both languages.

## MicroML Interpreter

**Complexity: Medium**

Need to implement:
- Value representation (IntVal, StringVal, BoolVal, UnitVal, Closure)
- Environment (immutable map for variable bindings)
- Evaluation for each Term case

**Main challenges:**
- **Closures:** Lambda evaluation must capture environment at definition time
- **Higher-order functions:** Functions as first-class values
- Environment threading through let/lambda

**Straightforward parts:**
- Literals, binary ops, conditionals (just recurse and apply)
- Let bindings (extend environment)
- No mutable state simplifies things

**Estimated effort:** 2-3 hours implementation + 1-2 hours tests

## NanoProc Interpreter

**Complexity: Medium**

Need to implement:
- Value representation (IntVal, StringVal, BoolVal, UnitVal)
- Mutable environment (variables can be reassigned)
- Procedure call stack
- Control flow handling (returns, breaks in loops)

**Main challenges:**
- **Mutable state:** Variable assignments modify environment
- **Control flow:** Return statements must propagate up through nested blocks/loops
- **Call stack:** Procedure calls need separate environment frames

**Straightforward parts:**
- No closures (procedures aren't first-class)
- Sequential execution is natural
- Expressions simpler than MicroML

**Estimated effort:** 2-3 hours implementation + 1-2 hours tests

## Total Estimation

**Both interpreters: 6-9 hours total**

**Pros of doing this:**
- Validates AST designs
- Provides executable semantics
- Useful for debugging/demos
- Tests can compare type checker vs interpreter results
- Simple to implement (no codegen, no optimization)

**Cons:**
- Doesn't advance the graph-based goals
- Traditional AST walking (not graph-based)
- Would need to be reimplemented if we switch to graph IR

**Recommendation:**
If you want executable languages to play with: **worth it** (relatively cheap)
If focused on graph migration: **defer** (doesn't advance that goal)

## Implementation Notes

### MicroML Value Type

```scala
enum Value:
  case IntVal(value: Int)
  case StringVal(value: String)
  case BoolVal(value: Boolean)
  case UnitVal
  case Closure(param: String, body: Term, env: Environment)
```

### NanoProc Value Type

```scala
enum Value:
  case IntVal(value: Int)
  case StringVal(value: String)
  case BoolVal(value: Boolean)
  case UnitVal
```

### Key Differences

- MicroML needs closures; NanoProc doesn't
- NanoProc needs mutable environment; MicroML uses immutable
- NanoProc needs control flow signaling (for return); MicroML doesn't
- MicroML is simpler for expressions but complex for closures
- NanoProc is simpler overall but needs imperative plumbing
