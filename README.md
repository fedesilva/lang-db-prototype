# lang-db-prototype

*One IR to rule them all*

A prototype exploring graph-based intermediate representations for multiple programming languages.

## Overview

This project investigates whether a generic graph database can serve as shared infrastructure for
representing different programming languages. The core idea: use immutable graphs as the intermediate
representation, with each compilation phase creating new graph layers connected via provenance edges.

The plan is to implement two languages—one functional, one imperative—to explore what infrastructure can
be shared:

*   **MicroML:** A small ML-like language ( Simple Typed Lambda Calculus ). Currently implemented.
*   **NanoProc:** A small imperative language. In the works.

What exists today:

*   **Generic Graph API:** Node/edge representation with bidirectional traversal
*   **Apache Arrow Serialization:** Columnar storage for the graph structures
*   **Basic Analyses:** Type checking, dependency analysis

**Current state:** MicroML parses to native Scala AST structures, which are then converted to the generic
graph representation for serialization. Analyses currently operate on the traditional heap-based AST.

**The approach:** By implementing both languages with traditional ParseTree/AST/IR structures first, we can
observe what differs and what can be shared. Graph-direct operations will be "grown" from these
observations, then we'll migrate the traditional structures into the graph. See
`docs/vision-and-architecture.md` for the vision.

## Documentation

- `docs/vision-and-architecture.md` - Long-term vision and architectural approach
- `docs/graph-api.md` - Current graph API implementation details

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

## MicroML Language

MicroML is a small ML-like language used as the first test case:

**Syntax:**
```ocaml
// Lambda functions
fn x: Int => x + 1

// Let bindings
let double = fn x: Int => x + x in double 5

// Literals and operators
42, "hello", true
x + y, x == y, x && y

// Conditionals
if x == 0 then 1 else x
```

**Type System:**
- Base types: `Int`, `String`, `Bool`
- Function types: `A -> B`
- Hindley-Milner type inference

The implementation includes a FastParse-based parser, type checker, and basic dependency analysis.
