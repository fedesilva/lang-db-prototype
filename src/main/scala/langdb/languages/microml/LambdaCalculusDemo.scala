package langdb.languages.microml

import cats.effect.IO
import cats.syntax.all.*
import langdb.languages.microml.ast.DependencyAnalyzer
import langdb.languages.microml.parser.MicroMLParser
import langdb.languages.microml.typechecker.TypeChecker

object LambdaCalculusDemo:

  private case class ExampleSpec(name: String, source: String)

  private val exampleSpecs: List[ExampleSpec] = List(
    ExampleSpec("identity", "fn x: Int => x"),
    ExampleSpec("constant", "fn x: Int => 42"),
    ExampleSpec("add", "fn x: Int => fn y: Int => x + y"),
    ExampleSpec("addOne", "(fn x: Int => x + 1) 5"),
    ExampleSpec("letExample", "let double = fn x: Int => x + x in double 3"),
    ExampleSpec("conditional", "fn x: Int => if x == 0 then 1 else x"),
    ExampleSpec("freeVar", "fn x: Int => x + y")
  )

  // Example terms to demonstrate the lambda calculus, sourced via the MicroML parser
  def examples(): List[(String, langdb.languages.microml.ast.Term)] =
    exampleSpecs.map { spec =>
      val parsedTerm = MicroMLParser.parse(spec.source) match
        case Right(term) => term
        case Left(error) =>
          throw new IllegalStateException(
            s"Failed to parse example '${spec.name}':\n${spec.source}\n$error"
          )
      (spec.name, parsedTerm)
    }

  def demo(): IO[Unit] =
    for
      _ <- IO.println("=== Lambda Calculus with Simple Types Demo ===")
      _ <- examples().traverse { case (name, term) =>
        for
          _ <- IO.println(s"Example: $name")
          _ <- IO.println(s"Term: $term")

          // Analyze dependencies
          dependencies = DependencyAnalyzer.analyze(term)
          _ <- IO.println("Dependencies:")
          _ <- dependencies.bindings.toList.traverse { case (varName, info) =>
            IO.println(
              s"  $varName: bound=${info.bindingLocation.isDefined}, refs=${info.references.size}"
            )
          }
          _ <-
            if dependencies.freeVariables.nonEmpty then
              IO.println(s"  Free variables: ${dependencies.freeVariables.mkString(", ")}")
            else IO.println("  No free variables")

          // Type check
          _ <- TypeChecker
            .typeCheck(term)
            .flatMap(typ => IO.println(s"Type: $typ"))
            .handleErrorWith(err => IO.println(s"Type error: ${err.getMessage}"))

          _ <- IO.println("")
        yield ()
      }
      _ <- IO.println("Demo completed!")
    yield ()
