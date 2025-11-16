# Vision and Architecture

## Vision

The graph **IS** the IR. Each rewrite creates an overlay of nodes connected to previous versions,
maintaining provenance through the transformation chain. End goal: interaction network
representation enabling interaction calculus computation model.

---

## Core Principles

### 1. Graph as First-Class IR

The graph is not just a representation of an AST—it IS the intermediate representation. All
transformations, analyses, and optimizations operate on and produce graph structures.

**Progression:**
```
Surface Language (MicroML, NanoProc)
    ↓ parse
Parse Tree (graph)
    ↓ rewrite
AST (graph)
    ↓ type check
Typed AST (graph)
    ↓ optimize
Optimized IR (graph)
    ↓ transform
Interaction Network (graph)
```

### 2. Append-Only Transformations with Provenance

Each compilation phase creates a new layer in the graph without modifying previous layers.
Phases are connected through provenance edges.

**Model:**
```
root₁ (parse) ──→ [parse tree subgraph]
                   ↓ (some nodes shared)
root₂ (ast) ────→ [ast subgraph, references parse tree nodes]
                   ↓ (mostly shared + new type nodes)
root₃ (typed) ──→ [typed ast + type annotation nodes]
                   ↓ (transformations)
root₄ (optimized) → [optimized subgraph]
```

**Key insight:** Phase identity is determined by reachability from roots, not by metadata.
Like persistent collections, shared structure implies sharing across phases.

**Example - AST vs Typed AST:**
```
AST root → Lambda(param="x") → body → ...

TypedAST root → Lambda(param="x") → body → ...
                    ↓ (new edge, only reachable from typed root)
                 TypeNode(Int → Int)
```

From `ast-root`: can traverse Lambda, body, etc. Type nodes are unreachable.
From `typed-ast-root`: can traverse same structure PLUS type information.

**Phases add edges and nodes; they don't modify existing structure.**

### 3. Immutable Storage Aligned with Immutable Semantics

Both Arrow IPC and Parquet are immutable storage formats, which perfectly aligns with the
append-only graph model.

---

## Development Strategy

### Phase 1: Learn by Doing (Current)

1. Implement two small languages: MicroML (functional) and NanoProc (imperative)
2. Write traditional case-class based analyses (dependency analysis, type checking, etc.)
3. Do this for BOTH languages
4. Learn patterns, understand commonalities and differences

**Rationale:** Avoid premature abstraction. Learn what needs to be generic by implementing
it twice in concrete form first.

### Phase 2: Graph-Based Analyses (Medium Term)

Once patterns are clear from Phase 1:
1. Migrate analyses to work directly on graph
2. Implement graph-based dependency analysis
3. Implement graph-based type checking
4. Design and implement rewrite/transformation API
5. Design and implement pattern matching/query API

### Phase 3: Interaction Networks (Long Term)

Transform graph IR into interaction network representation for interaction calculus
computation model.

---

## Storage Architecture

### Two-Tier Storage Strategy

**Arrow IPC for Active Work:**
- Fast read/write during development
- Working compilation session
- Can be memory-mapped: let OS manage paging
- Don't keep full graph in heap
- OS keeps hot parts in RAM, pages out cold parts
- Program stays lean, OS does memory management

**Parquet for Stable Code + Distribution:**
- Compiled modules that rarely change
- High compression saves disk space
- Columnar format enables partial reads
- Distribution format for libraries
- Query optimization via predicate pushdown

### Workflow for Large Programs

```
1. Parse → Arrow (hot, working set)
2. Type check → Arrow (hot, working set)
3. Unchanged modules → already in Parquet (cold storage)
4. Changed module → recompile via Arrow
5. Once stable → write to Parquet
6. Next compilation:
   - Load Parquet for stable modules (partial read, just metadata)
   - Load Arrow for active work
   - Merge graphs
```

**Key advantage:** 80% of large program unchanged between recompiles. Parquet's columnar
format allows reading only what's needed (types, names, etc.) without deserializing
entire subgraphs.

### Library Distribution

Libraries are distributed as Parquet files containing fully typed IR graphs.

**Example workflow:**
```
ASCII-art library:
  1. Compiled to fully typed IR graph
  2. Saved as ascii-art-v1.0.parquet
  3. Other projects:
     - Read parquet (just public API types/signatures initially)
     - Lazy-load implementations as needed
     - Merge into working graph
```

**Benefits:**
- Don't load entire library graph upfront
- Query: "what types does this module export?" → read just type columns
- Only when calling a function, load its implementation subgraph
- Incremental, on-demand loading
- Compression matters for distribution (smaller downloads)

### Storage Analogy

Think of it like traditional compilation:
- **Arrow IPC** = `.o` object files (working, mutable workflow, fast)
- **Parquet** = `.a` static libraries (immutable, compressed, distributable)
- **Memory mapping** = virtual memory for IR
- **Columnar reads** = lazy loading with fine granularity

---

## Graph Structure Details

### Nodes and Edges

```scala
case class NodeId(value: Long) extends AnyVal

case class ASTNode(
  id:       NodeId,
  nodeType: String,              // "Var", "Lambda", "App", "TypeNode", etc.
  data:     Map[String, String]  // node-specific data
)

case class ASTEdge(
  from:     NodeId,
  to:       NodeId,
  edgeType: String,              // "body", "func", "arg", "type", "derived-from", etc.
  label:    Option[String] = None
)
```

### Edge Categories (by convention, not structure)

**Structural edges** (within a phase):
- `"body"`, `"func"`, `"arg"`, `"left"`, `"right"`, etc.
- Define AST structure within a phase

**Provenance edges** (across phases):
- `"previous-root"`: links roots across phases
- `"derived-from"`: new node derived from previous phase node
- `"corresponds-to"`: node in new phase corresponds to node in previous phase

**Semantic edges** (added by analyses):
- `"type"`: node's type annotation
- `"binding"`: variable reference to its binding site
- `"dependency"`: dependency relationship

### Phase Management

**Roots define phases:**
- Each phase has a root node (entry point)
- Tracked externally: `Map[String, NodeId]` mapping phase names to root IDs
- Phase identity determined by reachability from root
- No phase metadata needed on nodes

**Query pattern:**
```scala
// Get all nodes in a phase
def getPhaseGraph(root: NodeId): Set[NodeId] =
  // Traverse from root following structural edges
  // Stop at provenance edges (don't follow backwards)

// Get type information (only in typed phase)
def getTypedView(typedRoot: NodeId): TypedGraph =
  // Traverse from typed root, including type edges
  // Type nodes unreachable from earlier phase roots
```

---

## Future Directions

### Query and Transformation APIs

**Pattern Matching:**
- Match subgraph patterns for optimization opportunities
- Find all instances of specific patterns
- Example: "find all lambdas where body has free variable X"

**Graph Rewriting:**
- Define rewrite rules as pattern → replacement
- Apply rules to transform graphs
- Create new phase with transformed subgraphs

### Metadata and Analysis Results

**Extensible metadata:**
- Source locations (line, column, file)
- Type information from type checker
- Analysis results (liveness, escape analysis, etc.)
- Error and warning information
- Comments and documentation

**Attachment strategy:** Either extend `data: Map[String, String]` with conventions,
or add semantic edges to metadata nodes, or both.

### Incremental Compilation

- Structural sharing between graph versions
- Efficient updates to subgraphs
- Only recompute affected regions
- Cache analysis results attached to stable nodes

### Interaction Networks

Transform high-level graph IR into interaction network representation:
- Nodes become interaction agents
- Edges become connections
- Computation becomes local graph rewriting
- Inherently parallel execution model

---

## Summary

This architecture provides:

1. **Unified representation:** All phases use the same graph structure
2. **Provenance tracking:** Every transformation maintains links to source
3. **Efficient storage:** Hot (Arrow) / Cold (Parquet) tiers match access patterns
4. **Library ecosystem:** Distribute compiled libraries as queryable graph databases
5. **Incremental compilation:** Share structure, only recompute what changed
6. **Scalability:** OS-managed memory mapping + columnar partial reads
7. **Future-proof:** Path to interaction networks and advanced computation models

The graph is not just a data structure—it's a **columnar IR database** with hot/cold
storage tiers, immutable semantics, and provenance tracking built in from the start.
