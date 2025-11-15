package langdb.ast

import scala.collection.mutable

object ASTToGraph:
  
  private var nextId: Long = 1L
  
  private def freshId(): NodeId =
    val id = NodeId(nextId)
    nextId += 1
    id
  
  def convertToGraph(term: Term): ASTGraph =
    nextId = 1L // reset for consistent IDs
    val builder = GraphBuilder()
    convertTerm(term, builder)
    builder.build()
  
  private def convertTerm(term: Term, builder: GraphBuilder): NodeId =
    term match
      case Term.Var(name) =>
        val id = freshId()
        builder.addNode(ASTNode(id, "Var", Map("name" -> name)))
        id
      
      case Term.Lambda(param, paramType, body) =>
        val id = freshId()
        val bodyId = convertTerm(body, builder)
        
        builder.addNode(ASTNode(id, "Lambda", Map(
          "param" -> param,
          "paramType" -> paramType.toString
        )))
        builder.addEdge(ASTEdge(id, bodyId, "body"))
        id
      
      case Term.App(func, arg) =>
        val id = freshId()
        val funcId = convertTerm(func, builder)
        val argId = convertTerm(arg, builder)
        
        builder.addNode(ASTNode(id, "App", Map.empty))
        builder.addEdge(ASTEdge(id, funcId, "func"))
        builder.addEdge(ASTEdge(id, argId, "arg"))
        id
      
      case Term.Let(name, value, body) =>
        val id = freshId()
        val valueId = convertTerm(value, builder)
        val bodyId = convertTerm(body, builder)
        
        builder.addNode(ASTNode(id, "Let", Map("name" -> name)))
        builder.addEdge(ASTEdge(id, valueId, "value"))
        builder.addEdge(ASTEdge(id, bodyId, "body"))
        id
      
      case Term.IntLit(value) =>
        val id = freshId()
        builder.addNode(ASTNode(id, "IntLit", Map("value" -> value.toString)))
        id
      
      case Term.StringLit(value) =>
        val id = freshId()
        builder.addNode(ASTNode(id, "StringLit", Map("value" -> value)))
        id
      
      case Term.BoolLit(value) =>
        val id = freshId()
        builder.addNode(ASTNode(id, "BoolLit", Map("value" -> value.toString)))
        id
      
      case Term.Add(left, right) =>
        convertBinaryOp("Add", left, right, builder)
      
      case Term.Mult(left, right) =>
        convertBinaryOp("Mult", left, right, builder)
      
      case Term.And(left, right) =>
        convertBinaryOp("And", left, right, builder)
      
      case Term.StringConcat(left, right) =>
        convertBinaryOp("StringConcat", left, right, builder)
      
      case Term.Not(operand) =>
        convertUnaryOp("Not", operand, builder)
      
      case Term.Print(operand) =>
        convertUnaryOp("Print", operand, builder)
      
      case Term.Println(operand) =>
        convertUnaryOp("Println", operand, builder)
  
  private def convertBinaryOp(opName: String, left: Term, right: Term, builder: GraphBuilder): NodeId =
    val id = freshId()
    val leftId = convertTerm(left, builder)
    val rightId = convertTerm(right, builder)
    
    builder.addNode(ASTNode(id, opName, Map.empty))
    builder.addEdge(ASTEdge(id, leftId, "left"))
    builder.addEdge(ASTEdge(id, rightId, "right"))
    id
  
  private def convertUnaryOp(opName: String, operand: Term, builder: GraphBuilder): NodeId =
    val id = freshId()
    val operandId = convertTerm(operand, builder)
    
    builder.addNode(ASTNode(id, opName, Map.empty))
    builder.addEdge(ASTEdge(id, operandId, "operand"))
    id

private class GraphBuilder:
  private val nodes = mutable.Map[NodeId, ASTNode]()
  private val edges = mutable.ListBuffer[ASTEdge]()
  
  def addNode(node: ASTNode): Unit =
    nodes += node.id -> node
  
  def addEdge(edge: ASTEdge): Unit =
    edges += edge
  
  def build(): ASTGraph =
    val graph = ASTGraph.empty
    val withNodes = nodes.values.foldLeft(graph)(_.addNode(_))
    edges.foldLeft(withNodes)(_.addEdge(_))
