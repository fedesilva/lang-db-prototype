package langdb

import cats.effect.{IO, IOApp}
import langdb.languages.microml.{LambdaCalculusDemo, ParserDemo}

object Main extends IOApp.Simple {
  def run: IO[Unit] =
    for {
      _ <- IO.println("Hello from lang-db-prototype!")
      _ <- IO.println("")
      _ <- IO.println("")
      _ <- ParserDemo.demo()
      _ <- IO.println("")
      _ <- IO.println("")
      _ <- langdb.languages.nanoproc.ParserDemo.demo()
      _ <- IO.println("")
      _ <- IO.println("")
      _ <- LambdaCalculusDemo.demo()
    } yield ()
}
