package langdb.ast

import cats.effect.IO
import langdb.ast.*

object LambdaCalculusDemo:

  // Example terms to demonstrate the lambda calculus
  def examples(): List[(String, Term)] = List(
    // Simple identity function: λx:Int. x
    ("identity", Term.Lambda("x", Type.IntType, Term.Var("x"))),

    // Constant function: λx:Int. 42
    ("constant", Term.Lambda("x", Type.IntType, Term.IntLit(42))),

    // Addition function: λx:Int. λy:Int. x + y
    (
      "add",
      Term.Lambda(
        "x",
        Type.IntType,
        Term.Lambda("y", Type.IntType, Term.Add(Term.Var("x"), Term.Var("y")))
      )
    ),

    // Application: (λx:Int. x + 1) 5
    (
      "addOne",
      Term.App(
        Term.Lambda("x", Type.IntType, Term.Add(Term.Var("x"), Term.IntLit(1))),
        Term.IntLit(5)
      )
    ),

    // Let binding: let double = λx:Int. x + x in double 3
    (
      "letExample",
      Term.Let(
        "double",
        Term.Lambda("x", Type.IntType, Term.Add(Term.Var("x"), Term.Var("x"))),
        Term.App(Term.Var("double"), Term.IntLit(3))
      )
    ),

    // Conditional: λx:Int. if (x == 0) then 1 else x
    (
      "conditional",
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

    // Free variable example: λx:Int. x + y (y is free)
    ("freeVar", Term.Lambda("x", Type.IntType, Term.Add(Term.Var("x"), Term.Var("y"))))
  )

  def demo(): IO[Unit] =
    for {
      _ <- IO.println("=== Lambda Calculus with Simple Types Demo ===")
      _ <- IO.println()
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
            if (dependencies.freeVariables.nonEmpty)
              IO.println(s"  Free variables: ${dependencies.freeVariables.mkString(", ")}")
            else
              IO.println("  No free variables")

          // Type check
          _ <- TypeChecker
            .typeCheck(term)
            .flatMap(typ => IO.println(s"Type: $typ"))
            .handleErrorWith(err => IO.println(s"Type error: ${err.getMessage}"))

          _ <- IO.println()
        } yield ()
      }
      _ <- IO.println("Demo completed!")
    } yield ()
