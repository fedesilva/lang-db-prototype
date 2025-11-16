package langdb.languages.microml

import cats.effect.IO
import cats.syntax.all.*
import langdb.languages.microml.ast.{DependencyAnalyzer, Term, Type}
import langdb.languages.microml.parser.MicroMLParser
import langdb.languages.microml.typechecker.TypeChecker

object LambdaCalculusDemo:

  private case class ExampleSpec(name: String, source: String, expectedTerm: Term)

  private val exampleSpecs: List[ExampleSpec] = List(
    ExampleSpec(
      "identity",
      "fn x: Int => x",
      Term.Lambda("x", Type.IntType, Term.Var("x"))
    ),
    ExampleSpec(
      "constant",
      "fn x: Int => 42",
      Term.Lambda("x", Type.IntType, Term.IntLit(42))
    ),
    ExampleSpec(
      "add",
      "fn x: Int => fn y: Int => x + y",
      Term.Lambda(
        "x",
        Type.IntType,
        Term.Lambda("y", Type.IntType, Term.Add(Term.Var("x"), Term.Var("y")))
      )
    ),
    ExampleSpec(
      "addOne",
      "(fn x: Int => x + 1) 5",
      Term.App(
        Term.Lambda("x", Type.IntType, Term.Add(Term.Var("x"), Term.IntLit(1))),
        Term.IntLit(5)
      )
    ),
    ExampleSpec(
      "letExample",
      "let double = fn x: Int => x + x in double 3",
      Term.Let(
        "double",
        Term.Lambda("x", Type.IntType, Term.Add(Term.Var("x"), Term.Var("x"))),
        Term.App(Term.Var("double"), Term.IntLit(3))
      )
    ),
    ExampleSpec(
      "conditional",
      "fn x: Int => if x == 0 then 1 else x",
      Term.Lambda(
        "x",
        Type.IntType,
        Term.If(
          Term.Eq(Term.Var("x"), Term.IntLit(0)),
          Term.IntLit(1),
          Term.Var("x")
        )
      )
    ),
    ExampleSpec(
      "freeVar",
      "fn x: Int => x + y",
      Term.Lambda("x", Type.IntType, Term.Add(Term.Var("x"), Term.Var("y")))
    )
  )

  // Example terms to demonstrate the lambda calculus, sourced via the MicroML parser
  def examples(): List[(String, Term)] =
    exampleSpecs.map { spec =>
      val parsedTerm = MicroMLParser.parse(spec.source) match
        case Right(term) => term
        case Left(error) =>
          throw new IllegalStateException(
            s"Failed to parse example '${spec.name}':\n${spec.source}\n$error"
          )
      require(
        parsedTerm == spec.expectedTerm,
        s"Parser produced unexpected AST for '${spec.name}'. Expected: ${spec.expectedTerm}, got: $parsedTerm"
      )
      (spec.name, parsedTerm)
    }

  def demo(): IO[Unit] =
    for {
      _ <- IO.println("=== Lambda Calculus with Simple Types Demo ===")
      _ <- examples().traverse { case (name, term) =>
        for {
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
        } yield ()
      }
      _ <- IO.println("Demo completed!")
    } yield ()
