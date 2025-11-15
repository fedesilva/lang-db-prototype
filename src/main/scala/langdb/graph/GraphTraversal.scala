package langdb.graph

import scala.collection.mutable

object GraphTraversal:

  // Depth-first traversal from a starting node
  def dfs(graph: ASTGraph, startId: NodeId): List[ASTNode] =
    val visited = mutable.Set[NodeId]()
    val result  = mutable.ListBuffer[ASTNode]()

    def visit(nodeId: NodeId): Unit =
      if !visited.contains(nodeId) then
        visited += nodeId
        graph.getNode(nodeId).foreach { node =>
          result += node
          graph.getChildren(nodeId).foreach(edge => visit(edge.to))
        }

    visit(startId)
    result.toList

  // Breadth-first traversal from a starting node
  def bfs(graph: ASTGraph, startId: NodeId): List[ASTNode] =
    val visited = mutable.Set[NodeId]()
    val queue   = mutable.Queue[NodeId]()
    val result  = mutable.ListBuffer[ASTNode]()

    queue.enqueue(startId)
    visited += startId

    while queue.nonEmpty do
      val nodeId = queue.dequeue()
      graph.getNode(nodeId).foreach { node =>
        result += node
        graph.getChildren(nodeId).foreach { edge =>
          if !visited.contains(edge.to) then
            visited += edge.to
            queue.enqueue(edge.to)
        }
      }

    result.toList

  // Find all nodes of a specific type
  def findNodesByType(graph: ASTGraph, nodeType: String): List[ASTNode] =
    graph.getAllNodes.filter(_.nodeType == nodeType)

  // Find all nodes that reference a variable by name
  def findVariableReferences(graph: ASTGraph, varName: String): List[ASTNode] =
    graph.getAllNodes.filter { node =>
      node.nodeType == "Var" && node.data.get("name").contains(varName)
    }

  // Find all lambda nodes that bind a specific parameter
  def findParameterBindings(graph: ASTGraph, paramName: String): List[ASTNode] =
    graph.getAllNodes.filter { node =>
      node.nodeType == "Lambda" && node.data.get("param").contains(paramName)
    }

  // Get all ancestors (parents, grandparents, etc.) of a node
  def getAncestors(graph: ASTGraph, nodeId: NodeId): List[ASTNode] =
    val visited = mutable.Set[NodeId]()
    val result  = mutable.ListBuffer[ASTNode]()

    def visitParents(id: NodeId): Unit =
      graph.getParents(id).foreach { edge =>
        if !visited.contains(edge.from) then
          visited += edge.from
          graph.getNode(edge.from).foreach { node =>
            result += node
            visitParents(edge.from)
          }
      }

    visitParents(nodeId)
    result.toList

  // Get all descendants (children, grandchildren, etc.) of a node
  def getDescendants(graph: ASTGraph, nodeId: NodeId): List[ASTNode] =
    val visited = mutable.Set[NodeId]()
    val result  = mutable.ListBuffer[ASTNode]()

    def visitChildren(id: NodeId): Unit =
      graph.getChildren(id).foreach { edge =>
        if !visited.contains(edge.to) then
          visited += edge.to
          graph.getNode(edge.to).foreach { node =>
            result += node
            visitChildren(edge.to)
          }
      }

    visitChildren(nodeId)
    result.toList

  // Find root nodes (nodes with no incoming edges)
  def findRoots(graph: ASTGraph): List[ASTNode] =
    graph.getAllNodes.filter(node => graph.getParents(node.id).isEmpty)

  // Find leaf nodes (nodes with no outgoing edges)
  def findLeaves(graph: ASTGraph): List[ASTNode] =
    graph.getAllNodes.filter(node => graph.getChildren(node.id).isEmpty)
