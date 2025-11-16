# Vision and Architecture

## Vision

The graph IS the IR. Each rewrite creates an overlay of nodes connected to previous versions, maintaining
provenance through the transformation chain. End goal: a unified Interaction Network Intermediate
Representation (INIR) that enables a powerful, provable, and parallel computation model.

## Core Principles

### 1. Graph as First-Class IR

The graph is not just a representation—it IS the intermediate representation. All transformations, analyses,
and optimizations operate on and produce graph structures.

### 2. Append-Only Transformations with Provenance

Each compilation phase creates a new layer in the graph without modifying previous layers. Phases are
connected through provenance edges. This is the key to non-destructive transformation and perfect error
reporting.

**Model:**

```
root₁ (parsetree) ──→ [ParseTree subgraph]
                     ↓ (provenance edges)
root₂ (inir) ──────→ [INIR subgraph, derived from parsetree]
                     ↓ (validation/rewrite)
root₃ (typed) ─────→ [INIR subgraph + type annotations]
                     ↓ (analysis/rewrite)
root₄ (lifetime) ──→ [INIR subgraph + memory/lifetime info]
                     ↓ (optimization)
root₅ (optimized) ─→ [Optimized INIR subgraph]
...
```

**Structural sharing:** Each new root reuses nodes from previous phases—like a persistent data structure.
For example, the typecheck phase (root₃) doesn't duplicate the INIR nodes from root₂. Instead, it adds
new type nodes and new edges connecting these type nodes to the existing INIR nodes. Each root creates a
new "version" of the graph that shares structure with previous versions, only adding what's new for that
phase.

**Key insight:** Phase identity is determined by reachability from roots. An error found in the typed graph
can be traced back to the user's source code by following provenance edges back to the parsetree graph.
A downstream analysis (like lifetime) can query the typed graph's annotations simply by traversing from
its own root.

### 3. Immutable Storage Aligned with Immutable Semantics

Both Arrow IPC and Parquet are immutable storage formats, which perfectly aligns with the append-only
graph model.

## The INIR: Core Computation Model

The Interaction Network Intermediate Representation (INIR) is the single, unified target for all language
frontends. It is not just an IR; it is a static analysis and optimization framework based on the
interaction calculus.

The INIR represents the program as a graph of agents (nodes) and wires (edges). "Optimization" is the
process of applying local rewrite rules to this graph at compile-time.

By lowering to the INIR, the compiler can prove properties about the code
that enable massive optimizations:

- **GC-Free Code:** Linearity in the graph proves a value is not shared, allowing for stack-based
  allocation or in-place mutation. Consumption of agents in rewrite rules translates directly to `free()`
  or stack-popping.

- **Opportunistic Mutation:** The compiler can prove when a value is linear (not-shared) and safely emit
  a native mutating instruction (e.g., `INC %rax`) for a functionally "immutable" operation, achieving
  C-like performance.

- **Automatic Fusion:** A chain like `map.filter.map` is represented as a series of connected agents.
  Compile-time rewrite rules will "fuse" these agents, eliminating all intermediate data structuresread
  before code generation.

- **Inherent Parallelism:** The graph topology makes data-flow explicit, enabling the compiler to
  identify parallel execution paths and emit SIMD instructions.

- **Parallel Compiler:** Local rewrite rules are independent—the compiler itself can apply them in
  parallel across multiple cores during optimization.

## Development Strategy & Pipeline

The old strategy of building separate, traditional compilers is replaced by a cleaner, more direct
pipeline focused on the INIR.

### The Pipeline:

```
Parser → ParseTree (Graph) → Transformation → INIR (Graph) → Profit (Codegen)
```

- **Parser:** Consumes source text and emits a graph conforming to the ParseTree-Schema. This graph is
  the concrete syntax tree.

- **ParseTree Graph:** A language-specific graph that represents the source code 1:1. Its only purpose is
  to provide the ground truth for source provenance.

- **Transformation:** A language-specific function that rewrites the ParseTree graph into the INIR graph,
  adding provenance edges from the new INIR nodes back to the ParseTree nodes. This pass does all
  language-specific desugaring (e.g., lowering while loops or var assignments).

- **INIR Graph:** The universal, language-agnostic representation. All optimization, type checking, and
  analysis is performed at this level.

- **Profit (Codegen):** A backend that traverses the final, optimized INIR graph and emits native code.

## Schemas: A "Type System" for the Graph

The graph itself is generic (Node, Edge). Schemas provide the "type system" for the graph, defining the
rules for a valid graph at a specific phase.

This formalizes the pipeline and makes it robust:

### ParseTree-Schema (Language-Specific):
- Defines the valid `nodeTypes` (e.g., "WhileLoop", "VarDecl") and `edgeTypes` (e.g., "condition",
  "body") for a language's parse tree.
- Validates the output of the parser.

### INIR-Schema (Universal):
- Defines the universal agents and connections of the interaction calculus (e.g., Lambda, Apply, Add).
- The INIR-Type-Checker is just a validator for this schema. If a graph is well-formed and passes this
  schema's validation, it is type-correct.

Adding a new language becomes a clear, modular process:
1. Define the language's ParseTree-Schema.
2. Write a Parser that emits a graph conforming to that schema.
3. Write the Transformation function that lowers a valid ParseTree-Schema graph to a valid INIR-Schema
   graph.

## Storage Architecture

### Two-Tier Storage Strategy

- **Arrow IPC (Hot):** For the active, working compilation graph. Memory-mapped to let the OS manage
  paging, keeping the compiler's heap small.

- **Parquet (Cold):** For stable, compiled libraries. High compression and columnar format allow for
  partial, lazy reads.

### Library Distribution & WPO-by-Default

Stable compilation artifacts are shared as libraries—Parquet files containing their fully typed INIR
graph.

**Why Parquet:**
- High compression reduces distribution size
- Columnar format enables partial reads: query names, types, and signatures without loading full
  implementations
- Standard format with broad tooling support

**Linking is Graph Subsumption:**
- When you import a library, its graph is merged (subsumed) into your working graph
- This provides Whole-Program Optimization (WPO) by default: optimization passes run on the entire
  merged graph, enabling inlining, specialization, and fusion across library boundaries

**Late-Stage Native Optimization:**
- Because the full INIR is available, native optimizations can be applied based on the target
  architecture
- The final binary can be optimized beyond what the library author could anticipate or provide

**Pay-Per-Use Codegen:**
- Codegen walks from the entry point (for executables) or public API (for libraries)
- Use a single function from a huge library? Only that function and its transitive dependencies are
  compiled
- Unused code is never visited, never emitted—true zero-cost abstraction

## Graph Structure Draft

### Nodes and Edges

```scala
case class NodeId(value: Long) extends AnyVal

case class Node(
  id:         NodeId,
  nodeType:   String,                 // Meaning defined by the schema (e.g., "WhileLoop", "Lambda")
  data:       Map[String, String]     // Payloads (e.g., variable names, literal values)
)

case class Edge(
  from:       NodeId,
  to:         NodeId,
  edgeType:   String,                 // Meaning defined by the schema (e.g., "body", "derived-from")
  label:      Option[String] = None
)
```

### Edge Categories (by convention)

- **Structural:** ("body", "condition", "arg") Defines the structure within a phase.
- **Provenance:** ("derived-from", "corresponds-to") Links nodes across phases (e.g., INIR node to
  ParseTree node).

### Phase Management

- **Roots define phases:** A `Map[String, NodeId]` tracks the entry points (e.g., "parsetree" → root₁,
  "inir" → root₂).
- **Phase identity is reachability:** A node's phase is determined by the root it is reachable from.

## Summary of Benefits

This architecture provides a unified system where advanced optimizations are not special passes, but
natural properties of the representation:

- **GC-Free Code Generation:** Provable linearity analysis on the INIR enables stack allocation and
  `free()`-like-precision.

- **Efficient Cell Reuse:** The compiler generates explicit allocation and deallocation, enabling
  freelist-based memory management. Freed cells are immediately available for reuse without returning
  memory to the OS, providing predictable performance with no garbage collection pauses.

- **Unboxed Primitives:** Affine types enable primitive values (integers, floats) to be represented
  as raw machine types without boxing or heap allocation, matching C-like efficiency while
  maintaining functional semantics.

- **Opportunistic Mutation:** Compile-time proof that a value is unshared, enabling safe use of
  high-performance mutating instructions.

- **Automatic Deforestation:** `map.filter.map` and other chains are fused at compile-time via graph
  rewrites, eliminating intermediate allocations.

- **Whole-Program Optimization (WPO) by Default:** "Linking is graph subsumption," so optimizations run
  on the fully-merged project and library graphs.

- **Effortless Dead Code Elimination (DCE):** Codegen is just a graph reachability traversal from the main
  root. If a node is unreachable, it's not "eliminated"—it is simply never visited and never emitted.

- **Parallel Compiler:** Local rewrite rules can be applied in parallel by the compiler itself on modern
  multi-core machines.

- **Unified & Minimal IR:** All languages target the single INIR, requiring only one type-checker and one
  optimizer.

- **Perfect Error Provenance:** Type errors in the INIR trace directly back to the ParseTree graph, which
  maps 1:1 with the user's source code.
