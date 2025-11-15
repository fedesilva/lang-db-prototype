package langdb.graph

// Node ID for referencing nodes in the graph
case class NodeId(value: Long) extends AnyVal

// Graph node representing any AST term
case class ASTNode(
  id:       NodeId,
  nodeType: String, // "Var", "Lambda", "App", etc.
  data:     Map[String, String] // node-specific data (name, value, etc.)
)

// Edge representing relationships between nodes
case class ASTEdge(
  from:     NodeId,
  to:       NodeId,
  edgeType: String, // "body", "func", "arg", "param_binding", etc.
  label:    Option[String] = None // optional label for edges like parameter names
)

// Bidirectional graph structure
case class ASTGraph(
  nodes:         Map[NodeId, ASTNode],
  outgoingEdges: Map[NodeId, List[ASTEdge]], // owner -> owned
  incomingEdges: Map[NodeId, List[ASTEdge]] // owned -> owner
):
  def addNode(node: ASTNode): ASTGraph =
    copy(
      nodes         = nodes + (node.id -> node),
      outgoingEdges = outgoingEdges + (node.id -> List.empty),
      incomingEdges = incomingEdges + (node.id -> List.empty)
    )

  def addEdge(edge: ASTEdge): ASTGraph =
    copy(
      outgoingEdges =
        outgoingEdges.updatedWith(edge.from)(edges => Some(edge :: edges.getOrElse(List.empty))),
      incomingEdges =
        incomingEdges.updatedWith(edge.to)(edges => Some(edge :: edges.getOrElse(List.empty)))
    )

  def getNode(id: NodeId): Option[ASTNode] = nodes.get(id)

  def getChildren(id: NodeId): List[ASTEdge] = outgoingEdges.getOrElse(id, List.empty)

  def getParents(id: NodeId): List[ASTEdge] = incomingEdges.getOrElse(id, List.empty)

  def getAllNodes: List[ASTNode] = nodes.values.toList

  def getAllEdges: List[ASTEdge] = outgoingEdges.values.flatten.toList

object ASTGraph:
  def empty: ASTGraph = ASTGraph(Map.empty, Map.empty, Map.empty)
