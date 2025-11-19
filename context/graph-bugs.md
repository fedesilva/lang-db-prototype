The graph implementation is not good.

- Graph persistence corrupts node payloads: `GraphArrowSerializer` flattens the data map into comma-joined key and value
strings before writing (`src/main/scala/langdb/graph/GraphArrowSerializer.scala:64-75`) and reconstructs it by splitting on
commas when reading (`src/main/scala/langdb/graph/GraphArrowSerializer.scala:170-176`). Any key or value containing a comma
(string literals, type strings like `IntType,IntType`) or an empty string silently disappears or becomes mismatched because
the split counts no longer line up. The serializer needs a structured encoding (e.g., two ListVectors of equal length) or at
least escaping + explicit arity so arbitrary strings round-trip safely.

- Graph conversion drops provenance entirely: when lowering MicroML ASTs into the graph you discard every `SourceSpan`
(`src/main/scala/langdb/languages/microml/graph/ASTToGraph.scala:13-111`). The resulting graph only stores node kind and
a few attributes, so you cannot trace later graph analyses back to source even though the high-level goal demands it. Each
`ASTNode` should include the span (and ideally source file) in its `data` map or through dedicated attributes so provenance
survives serialization.
