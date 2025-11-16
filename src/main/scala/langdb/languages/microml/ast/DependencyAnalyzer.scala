package langdb.languages.microml.ast

/** Tracks how variable names are bound and referenced inside a term. */
final case class NameInfo(
  name:            String,
  bindingLocation: Option[Term],
  references:      List[Term]
)

final case class DependencyGraph(
  bindings:      Map[String, NameInfo],
  freeVariables: Set[String]
):
  def addBinding(name: String, bindingTerm: Term): DependencyGraph =
    val updated = bindings.get(name) match
      case Some(existing) => existing.copy(bindingLocation = Some(bindingTerm))
      case None => NameInfo(name, Some(bindingTerm), Nil)
    copy(bindings = bindings.updated(name, updated), freeVariables = freeVariables - name)

  def addReference(name: String, referenceTerm: Term): DependencyGraph =
    val updated = bindings.get(name) match
      case Some(existing) => existing.copy(references = referenceTerm :: existing.references)
      case None => NameInfo(name, None, List(referenceTerm))
    val withBinding = bindings.updated(name, updated)
    val newFree =
      if updated.bindingLocation.isEmpty then freeVariables + name
      else freeVariables
    copy(bindings = withBinding, freeVariables = newFree)

object DependencyAnalyzer:

  def analyze(term: Term): DependencyGraph =
    analyzeWithContext(term, DependencyGraph(Map.empty, Set.empty))

  private def analyzeWithContext(
    term:  Term,
    graph: DependencyGraph
  ): DependencyGraph =
    term match
      case v @ Term.Var(name, _) =>
        graph.addReference(name, v)

      case l @ Term.Lambda(param, _, body, _) =>
        val graphWithBinding = graph.addBinding(param, l)
        analyzeWithContext(body, graphWithBinding)

      case app @ Term.App(func, arg, _) =>
        val afterFunc = analyzeWithContext(func, graph)
        analyzeWithContext(arg, afterFunc)

      case let @ Term.Let(name, value, body, _) =>
        val afterValue  = analyzeWithContext(value, graph)
        val withBinding = afterValue.addBinding(name, let)
        analyzeWithContext(body, withBinding)

      case Term.Add(left, right, _) =>
        analyzeBinary(left, right, graph)

      case Term.Mult(left, right, _) =>
        analyzeBinary(left, right, graph)

      case Term.Eq(left, right, _) =>
        analyzeBinary(left, right, graph)

      case Term.And(left, right, _) =>
        analyzeBinary(left, right, graph)

      case Term.StringConcat(left, right, _) =>
        analyzeBinary(left, right, graph)

      case Term.If(cond, thenBranch, elseBranch, _) =>
        val afterCond = analyzeWithContext(cond, graph)
        val afterThen = analyzeWithContext(thenBranch, afterCond)
        analyzeWithContext(elseBranch, afterThen)

      case Term.Not(operand, _) =>
        analyzeWithContext(operand, graph)

      case Term.Print(operand, _) =>
        analyzeWithContext(operand, graph)

      case Term.Println(operand, _) =>
        analyzeWithContext(operand, graph)

      case Term.IntLit(_, _) | Term.StringLit(_, _) | Term.BoolLit(_, _) | Term.UnitLit(_) =>
        graph

  private def analyzeBinary(left: Term, right: Term, graph: DependencyGraph): DependencyGraph =
    val afterLeft = analyzeWithContext(left, graph)
    analyzeWithContext(right, afterLeft)
