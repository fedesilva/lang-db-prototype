package langdb.languages.microml.graph

import langdb.graph.{ASTEdge, ASTGraph, ASTNode, NodeId}
import langdb.languages.microml.ast.Term

object ASTToGraph:

  def convertToGraph(term: Term): ASTGraph =
    val builder           = GraphBuilder.empty
    val (_, finalBuilder) = convertTerm(term, builder)
    finalBuilder.build()

  private def convertTerm(term: Term, builder: GraphBuilder): (NodeId, GraphBuilder) =
    term match
      case Term.Var(name, _) =>
        val (id, b1) = builder.freshId()
        val b2       = b1.addNode(ASTNode(id, "Var", Map("name" -> name)))
        (id, b2)

      case Term.Lambda(param, paramType, body, _) =>
        val (id, b1)     = builder.freshId()
        val (bodyId, b2) = convertTerm(body, b1)
        val b3 = b2
          .addNode(
            ASTNode(
              id,
              "Lambda",
              Map(
                "param" -> param,
                "paramType" -> paramType.toString
              )
            )
          )
          .addEdge(ASTEdge(id, bodyId, "body"))
        (id, b3)

      case Term.App(func, arg, _) =>
        val (id, b1)     = builder.freshId()
        val (funcId, b2) = convertTerm(func, b1)
        val (argId, b3)  = convertTerm(arg, b2)
        val b4 = b3
          .addNode(ASTNode(id, "App", Map.empty))
          .addEdge(ASTEdge(id, funcId, "func"))
          .addEdge(ASTEdge(id, argId, "arg"))
        (id, b4)

      case Term.Let(name, value, body, _) =>
        val (id, b1)      = builder.freshId()
        val (valueId, b2) = convertTerm(value, b1)
        val (bodyId, b3)  = convertTerm(body, b2)
        val b4 = b3
          .addNode(ASTNode(id, "Let", Map("name" -> name)))
          .addEdge(ASTEdge(id, valueId, "value"))
          .addEdge(ASTEdge(id, bodyId, "body"))
        (id, b4)

      case Term.IntLit(value, _) =>
        val (id, b1) = builder.freshId()
        val b2       = b1.addNode(ASTNode(id, "IntLit", Map("value" -> value.toString)))
        (id, b2)

      case Term.StringLit(value, _) =>
        val (id, b1) = builder.freshId()
        val b2       = b1.addNode(ASTNode(id, "StringLit", Map("value" -> value)))
        (id, b2)

      case Term.BoolLit(value, _) =>
        val (id, b1) = builder.freshId()
        val b2       = b1.addNode(ASTNode(id, "BoolLit", Map("value" -> value.toString)))
        (id, b2)

      case Term.Add(left, right, _) =>
        convertBinaryOp("Add", left, right, builder)

      case Term.Mult(left, right, _) =>
        convertBinaryOp("Mult", left, right, builder)

      case Term.Eq(left, right, _) =>
        convertBinaryOp("Eq", left, right, builder)

      case Term.And(left, right, _) =>
        convertBinaryOp("And", left, right, builder)

      case Term.StringConcat(left, right, _) =>
        convertBinaryOp("StringConcat", left, right, builder)

      case Term.If(cond, thenBranch, elseBranch, _) =>
        val (id, b1)     = builder.freshId()
        val (condId, b2) = convertTerm(cond, b1)
        val (thenId, b3) = convertTerm(thenBranch, b2)
        val (elseId, b4) = convertTerm(elseBranch, b3)
        val b5 = b4
          .addNode(ASTNode(id, "If", Map.empty))
          .addEdge(ASTEdge(id, condId, "cond"))
          .addEdge(ASTEdge(id, thenId, "then"))
          .addEdge(ASTEdge(id, elseId, "else"))
        (id, b5)

      case Term.Not(operand, _) =>
        convertUnaryOp("Not", operand, builder)

      case Term.Print(operand, _) =>
        convertUnaryOp("Print", operand, builder)

      case Term.Println(operand, _) =>
        convertUnaryOp("Println", operand, builder)

      case Term.UnitLit(_) =>
        val (id, b1) = builder.freshId()
        val b2       = b1.addNode(ASTNode(id, "UnitLit", Map.empty))
        (id, b2)

  private def convertBinaryOp(
    opName:  String,
    left:    Term,
    right:   Term,
    builder: GraphBuilder
  ): (NodeId, GraphBuilder) =
    val (id, b1)      = builder.freshId()
    val (leftId, b2)  = convertTerm(left, b1)
    val (rightId, b3) = convertTerm(right, b2)
    val b4 = b3
      .addNode(ASTNode(id, opName, Map.empty))
      .addEdge(ASTEdge(id, leftId, "left"))
      .addEdge(ASTEdge(id, rightId, "right"))
    (id, b4)

  private def convertUnaryOp(
    opName:  String,
    operand: Term,
    builder: GraphBuilder
  ): (NodeId, GraphBuilder) =
    val (id, b1)        = builder.freshId()
    val (operandId, b2) = convertTerm(operand, b1)
    val b3 = b2.addNode(ASTNode(id, opName, Map.empty)).addEdge(ASTEdge(id, operandId, "operand"))
    (id, b3)

private case class GraphBuilder(
  nodes:  List[ASTNode],
  edges:  List[ASTEdge],
  nextId: Long
):
  def freshId(): (NodeId, GraphBuilder) =
    (NodeId(nextId), copy(nextId = nextId + 1))

  def addNode(node: ASTNode): GraphBuilder =
    copy(nodes = node :: nodes)

  def addEdge(edge: ASTEdge): GraphBuilder =
    copy(edges = edge :: edges)

  def build(): ASTGraph =
    val graph     = ASTGraph.empty
    val withNodes = nodes.foldLeft(graph)(_.addNode(_))
    edges.foldLeft(withNodes)(_.addEdge(_))

private object GraphBuilder:
  def empty: GraphBuilder = GraphBuilder(Nil, Nil, 1L)
