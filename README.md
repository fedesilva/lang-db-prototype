# lang-db-prototype

A Scala 3 project for language database prototyping.

## Project Overview

The `lang-db-prototype` aims to represent and manage language data in a structured, queryable format. It combines a simple typed lambda calculus implementation with a graph-based AST representation, which is then serialized into Apache Arrow's high-performance columnar format.

Key components include:

*   **Language Frontend:** Defines a simple typed lambda calculus with `Term` and `Type` enums, capable of type-checking and dependency analysis.
*   **Graph Representation:** Converts the AST into a generic graph structure (`ASTGraph`) for flexible traversal and analysis.
*   **Apache Arrow Integration:** Serializes the `ASTGraph` into Apache Arrow files, leveraging its columnar storage for efficient I/O and potential future analytical queries.

## Dependencies

This project uses the same dependencies as the MML project, plus Apache Arrow:
- Scala 3.6.4
- Cats Effect for functional programming
- Refined for type-safe refinements
- Monocle for optics
- FastParse for parsing
- Scopt for command-line parsing
- MUnit for testing
- **Apache Arrow 21.0.0** for columnar data processing
- **Apache Parquet 1.13.1** (optional, for future integration)

## Running

```bash
sbt run
```

## Testing

```bash
sbt test
```

## Code Formatting

```bash
sbt scalafmt
```

## Scalafix

```bash
sbt scalafix
```

## Apache Arrow Features

The project includes a demonstration of Apache Arrow's columnar data capabilities:

- **In-memory columnar storage** for efficient language data processing
- **Zero-copy operations** for high performance
- **Schema-aware data structures** with type safety
- **Arrow IPC format** for fast serialization/deserialization

The demo (`langdb.arrow.ArrowExample`) shows:
- Creating Arrow schemas for language data
- Writing language records to Arrow format
- Reading and processing Arrow data
- Memory-efficient operations using Arrow vectors

Run the demo with `sbt run` to see Arrow in action!