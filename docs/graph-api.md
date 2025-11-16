# Graph API Reference

**TL;DR:** Immutable graph storage with Arrow persistence. Add-only, no mutations. MicroML converter exists. Basic traversal/query tools.

---

## Core Types

**Location:** `src/main/scala/langdb/graph/Graph.scala`

### NodeId, ASTNode, ASTEdge (lines 4+)
- `NodeId`: Unique node identifier
- `ASTNode`: ID + node type + key-value data map
- `ASTEdge`: Source + target + edge type + optional label

### ASTGraph (line 22)
- **Storage:** Immutable maps of nodes + bidirectional adjacency
- **Bidirectional:** Both parent→child and child→parent edges maintained
- **Invariant:** All nodes have edge lists (never missing keys)

---

## Operations

### Adding Elements
- `addNode(node)` - Insert node, initialize empty edge lists (line 27)
- `addEdge(edge)` - Add to both adjacency maps, supports multi-edges (line 34)

### Queries
- `getNode(id)` - Fetch node by ID
- `getChildren(id)` - Outgoing edges
- `getParents(id)` - Incoming edges
- `getAllNodes()` - All nodes
- `getAllEdges()` - All edges

All queries return immutable data (line 42).

### Factory
- `ASTGraph.empty` - Create empty graph (line 52)

---

## Traversal

**Location:** `src/main/scala/langdb/graph/GraphTraversal.scala`

### Basic Traversal (line 7)
- `dfs(startId)` - Depth-first walk, returns visited nodes
- `bfs(startId)` - Breadth-first walk, returns visited nodes

### Pattern Search (line 45)
**Current:** MicroML-specific (hardcoded node types)
- `findNodesByType(type)` - All nodes matching type
- `findVariableReferences(name)` - Find Var nodes
- `findParameterBindings(param)` - Find Lambda params

### Relationship Queries
- `getAncestors(id)` - Walk up (line 61)
- `getDescendants(id)` - Walk down
- `findRoots()` - Nodes with no parents (line 97)
- `findLeaves()` - Nodes with no children

---

## Serialization

**Location:** `src/main/scala/langdb/graph/GraphArrowSerializer.scala`

### Save (line 36)
```scala
saveGraph(graph, nodesPath, edgesPath): IO[Unit]
```

**Format:** Arrow IPC (two files)
- **Nodes file:** ID, type, serialized key-value data
- **Edges file:** from, to, edge_type, optional label

**Data encoding:** Key-value pairs as comma-separated UTF-8 strings
- **Warning:** Lossy if keys/values contain commas
- **No deduplication:** Multi-edges stored as-is

### Load (line 141)
```scala
loadGraph(nodesPath, edgesPath): IO[ASTGraph]
```

**Process:**
1. Read Arrow files
2. Reconstruct ASTNode + ASTEdge objects
3. Replay addNode/addEdge operations

**Error handling:** Basic - IO failures bubble through Resource wrappers

---

## Language Converters

### MicroML → Graph

**Location:** `src/main/scala/langdb/languages/microml/graph/ASTToGraph.scala`

**Entry point:** `convertToGraph(term: Term): ASTGraph` (line 17)

**Process:**
1. Walk MicroML Term AST
2. Emit canonical node types: `Lambda`, `Let`, `Add`, etc.
3. Emit labeled edges: `body`, `arg`, `left`, `right`, etc.
4. Reset IDs per conversion (deterministic numbering)

**Helpers:**
- Binary/unary ops normalized (line 118)
- Private GraphBuilder batches mutations (line 141)

**Output:** Immutable ASTGraph

---

## Limitations

### Current State
✓ Immutable storage
✓ Basic traversal
✓ Arrow persistence
✓ MicroML ingestion

✗ No mutation primitives (only add-only)
✗ No rich schemas for node data
✗ No graph-level metadata
✗ Pattern searches MicroML-specific
✗ Lossy serialization (comma-separated values)

### Next Steps
See `context/todo.md` for planned improvements.
