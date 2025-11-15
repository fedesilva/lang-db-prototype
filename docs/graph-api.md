**Graph Core**
- `NodeId`, `ASTNode`, and `ASTEdge` capture IDs, node metadata, and labeled edges (`src/main/scala/langdb/graph/Graph.scala:4`).
- `ASTGraph` stores immutable maps of nodes plus incoming/outgoing adjacency to keep owner→owned and owned→owner links in sync
  (`Graph.scala:22`).
- `addNode` inserts a node and primes empty edge lists so later traversals never hit missing keys (`Graph.scala:27`).
- `addEdge` pushes the edge into both adjacency maps, preserving multi-edges with simple prepends (`Graph.scala:34`).
- Query helpers `getNode`, `getChildren`, `getParents`, `getAllNodes`, `getAllEdges` expose the structure without mutation
  (`Graph.scala:42`).
- `ASTGraph.empty` seeds an empty instance for builders to accumulate into (`Graph.scala:52`).

**Traversal Helpers**
- `GraphTraversal.dfs`/`bfs` offer depth-first and breadth-first walks returning visited `ASTNode`s from a start ID
  (`src/main/scala/langdb/graph/GraphTraversal.scala:7`).
- Pattern searches include `findNodesByType`, `findVariableReferences`, and `findParameterBindings`, currently biased toward the
  MicroML schema (e.g., hardcoded `"Var"/"Lambda"` checks) (`GraphTraversal.scala:45`).
- Relationship queries `getAncestors`, `getDescendants`, `findRoots`, `findLeaves` leverage the stored incoming/outgoing edges to
  climb or descend the graph (`GraphTraversal.scala:61`, `GraphTraversal.scala:97`).

**Arrow Serialization**
- `GraphArrowSerializer.saveGraph` writes the in-memory graph into two Arrow IPC files (nodes/edges) using Arrow vectors and
  schemas tailored to IDs, types, labels, and serialized key/value blobs (`src/main/scala/langdb/graph/GraphArrowSerializer.scala:36`).
- `saveNodes` flattens each node into UTF-8 key/value strings joined by commas—simple but lossy if keys or values contain commas
  (`GraphArrowSerializer.scala:43`).
- `saveEdges` persists `from`, `to`, `edge_type`, and optional `label` for each edge; no deduplication today
  (`GraphArrowSerializer.scala:94`).
- `loadGraph` reads both files back into memory, reconstructing `ASTNode`s and `ASTEdge`s before replaying `addNode`/`addEdge`
  (`GraphArrowSerializer.scala:141`). Error handling is basic—IO failures bubble through the `Resource` wrappers.

**MicroML AST Ingestion**
- `ASTToGraph.convertToGraph` (currently MicroML-specific) walks a `Term`, emits canonical node types (e.g., `"Lambda"`, `"Let"`,
  `"Add"`) and labeled edges (`"body"`, `"arg"`, `"left"`, etc.), and resets IDs per conversion to keep deterministic numbering
  (`src/main/scala/langdb/languages/microml/graph/ASTToGraph.scala:17`).
- Binary/unary helpers ensure arithmetic, boolean, and IO nodes look uniform in the graph (`ASTToGraph.scala:118`).
- The private `GraphBuilder` batches nodes/edges in mutable collections before folding them into an immutable `ASTGraph`
  (`ASTToGraph.scala:141`).

Overall the API gives us immutable graph storage, basic traversal/query tooling, Arrow persistence, and one MicroML ingestion
path. It lacks mutation primitives beyond add-only, richer schemas for node data, or graph-level metadata, which we’ll want to
consider while defining the next steps for the “graph API” task.
