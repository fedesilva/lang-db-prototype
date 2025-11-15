package langdb

import cats.effect.{IO, IOApp}
import langdb.ast.LambdaCalculusDemo

object Main extends IOApp.Simple {
  def run: IO[Unit] =
    for {
      _ <- IO.println("Hello from lang-db-prototype!")
      _ <- IO.println("")
      _ <- LambdaCalculusDemo.demo()
    } yield ()
}
