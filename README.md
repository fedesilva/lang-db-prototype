# lang-db-prototype

*One IR to rule them all*

A prototype exploring graph-based intermediate representations for multiple programming languages.

## Overview

This project investigates whether an interaction network-based intermediate representation can
serve as shared infrastructure for multiple programming languages—both functional and imperative.

The implementation uses immutable graphs as the IR, with each compilation phase creating new graph
layers connected via provenance edges. This allows non-destructive transformations while maintaining
perfect traceability from optimized code back to source.

The plan is to implement two languages—one functional, one imperative—to explore what infrastructure
can be shared:

*   **MicroML:** A small ML-like language (Simple Typed Lambda Calculus). **Fully implemented.**
*   **NanoProc:** A small imperative language. **Fully implemented.**

What exists today:

*   **Generic Graph API:** Node/edge representation with bidirectional traversal
*   **Apache Arrow Serialization:** Columnar storage for the graph structures
*   **Traditional Analyses:** Type checking and dependency analysis using classic in-heap AST techniques

**Current state:** Both languages parse to native Scala AST structures, which are then converted to the generic
graph representation for serialization. Type checking and analyses currently operate on traditional heap-based
ASTs using standard compiler techniques.

**The approach:** By implementing both languages with traditional ParseTree/AST/IR structures first, we can
observe what differs and what can be shared. Graph-direct operations will be "grown" from these
observations, then we'll migrate the traditional structures into the graph. See
[vision-and-architecture.md](docs/vision-and-architecture.md) for the vision.

## Documentation

- [vision-and-architecture.md](docs/vision-and-architecture.md) - Long-term vision and architectural approach
- [graph-api.md](docs/graph-api.md) - Current graph API implementation details

## Dependencies

- Scala 3.6.4
- Cats Effect for functional programming
- FastParse for parsing
- MUnit for testing
- Apache Arrow 21.0.0 for columnar storage
- Apache Parquet 1.13.1 (experimental)

## Building and Testing

```bash
# Run the demo
sbt run

# Run tests
sbt test

# Format code
sbt scalafmtAll

# Run scalafix
sbt scalafixAll
```

## Languages

### MicroML (Functional)

A small ML-like language implementing simply typed lambda calculus:

**Syntax:**
```ocaml
// Lambda functions
fn x: Int => x + 1

// Let bindings
let double = fn x: Int => x + x in double 5

// Literals and operators
42, "hello", true, ()
x + y, x * y, x == y, x && y

// Conditionals
if x == 0 then 1 else x
```

**Type System:**
- Base types: `Int`, `String`, `Bool`, `Unit`
- Function types: `A -> B`
- Higher-order functions
- Simply typed lambda calculus with explicit type annotations (no inference)

**Implementation:**
- FastParse-based parser
- Type checker with traditional AST traversal
- Dependency analysis
- Comprehensive test suite (82 tests)

### NanoProc (Imperative)

A small imperative language for exploring procedural programming:

**Syntax:**
```c
// Variable declarations and assignment
var x: Int = 42;
x = x + 1;

// Procedures
proc factorial(n: Int): Int {
  var result: Int = 1;
  var i: Int = n;
  while (i > 0) {
    result = result * i;
    i = i - 1;
  }
  return result;
}

// Control flow
if (x > 0) {
  println("positive");
} else {
  println("non-positive");
}
```

**Type System:**
- Base types: `Int`, `String`, `Bool`, `Unit`
- Procedures (not first-class)
- Mutable variables
- Explicit type annotations

**Implementation:**
- FastParse-based parser
- Type checker with procedure signature validation and return checking
- Comprehensive test suite (65 tests)

For full specification, see [nanoproc-spec.md](docs/nanoproc-spec.md).
