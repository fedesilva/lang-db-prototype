**Vision:**
The graph IS the IR. Each rewrite creates an overlay of nodes connected to previous versions,
maintaining provenance through the transformation chain. End goal: interaction network
representation enabling interaction calculus computation model.

If you *need* you can peruse the documents in `docs/`.

---

**Language Implementations (Current State):**

Both languages are fully implemented using traditional compiler techniques:

**MicroML (Functional):**
- Complete AST: Term, Type enums
- FastParse-based parser
- Type checker with traditional AST traversal
- Dependency analyzer
- 82 tests (parser + type checker)
- Higher-order functions, let bindings, lambdas

**NanoProc (Imperative):**
- Complete AST: Expr, Stmt, Program, ProcDef
- FastParse-based parser
- Type checker with procedure validation and return checking
- 65 tests (parser + type checker)
- Mutable variables, procedures, while loops, if/else

**Both use traditional in-heap AST techniques.** Type checking, analysis, and all operations
work on native Scala data structures. Graph conversion exists but is only for serialization -
no analyses operate on the graph yet.

---

**What we have (Graph Infrastructure):**
- Generic node/edge representation: `ASTNode(id, nodeType, data)` + `ASTEdge(from, to, edgeType)`
- Bidirectional graph: efficient parent/child traversal
- Basic traversals: DFS, BFS, ancestors, descendants, type-based search
- Arrow serialization: columnar storage works
- Language-agnostic: String-based types, key-value data

**Short term (two languages): COMPLETE (with traditional techniques)**
Both MicroML and NanoProc are fully implemented using classic compiler approaches.
The graph API can represent both languages structurally. The stringly-typed approach is flexible enough
for serialization, but no analyses operate on the graph representation yet.

**Medium term (graph-based analyses): SIGNIFICANT GAPS**

1. **Semantic layer missing** - we have structure but no semantics:
   - No scope/binding representation (DependencyAnalyzer still walks AST, not graph)
   - No way to encode "this Var references that Lambda's param"
   - Type information from checker isn't attached to graph nodes

2. **No rewrite/transformation API:**
   - Can build graphs but can't transform them
   - No subgraph replacement operations
   - No rewrite rules framework
   - Can't implement "walk graph, apply optimizations"

3. **No pattern matching/querying:**
   - `findNodesByType` is primitive
   - Can't query "find all lambdas where body has free variable X"
   - Can't match graph patterns for optimization opportunities

4. **Edge ordering not explicit:**
   - Critical for imperative NanoProc (statement order matters!)
   - Function args need ordering
   - Currently List[ASTEdge] but no guaranteed/explicit order

**Long term (multiple IRs, transformations, interaction networks): MAJOR GAPS**

5. **No transformation provenance:**
   - Can't track "this HIR node derived from that AST node"
   - Can't maintain source linkage through rewrites
   - No version/layer concept for AST→HIR→MIR style pipelines

6. **Missing metadata infrastructure:**
   - No source locations (line/column/file)
   - No place to attach analysis results (liveness, escape, etc.)
   - No type annotations from type checker
   - No comments/documentation preservation

7. **Static, not incremental:**
   - Build once, can't efficiently update
   - No structural sharing between graph versions
   - Problematic for IDE scenarios

**Status**
We have **two complete language implementations** using traditional techniques. The graph infrastructure
provides a **structural foundation** but lacks the **semantic and transformation layers** needed for
our stated goals. We can represent ASTs from multiple languages in the graph, but analyses still
operate on the traditional heap-based ASTs. We can't yet DO anything sophisticated with the graph
representation (query, rewrite, analyze, transform, track provenance).

**Next Phase:**
Migrate analyses to operate on graph structures rather than traditional ASTs. This will help us
discover what APIs and representations are needed for the graph-based approach.

**What needs design attention:**
1. Scope and binding representation (urgent for dependency analysis on graph)
2. Edge ordering semantics (urgent for NanoProc)
3. Rewrite/transformation API (medium term)
4. Query/pattern matching API (medium term)
5. Provenance tracking (long term)
6. Metadata attachment strategy (ongoing)
