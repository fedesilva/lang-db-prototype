package langdb

import cats.effect.{IO, IOApp}
import langdb.ast.LambdaCalculusDemo
import langdb.parser.ParserDemo

object Main extends IOApp.Simple {
  def run: IO[Unit] =
    for {
      _ <- IO.println("Hello from lang-db-prototype!")
      _ <- IO.println("")
      _ <- IO.println("")
      _ <- ParserDemo.demo()
      _ <- IO.println("")
      _ <- IO.println("")
      _ <- LambdaCalculusDemo.demo()
    } yield ()
}
