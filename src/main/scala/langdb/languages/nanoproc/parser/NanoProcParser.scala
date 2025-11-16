package langdb.languages.nanoproc.parser

import langdb.languages.nanoproc.ast.Program

object NanoProcParser:

  def parse(input: String): Either[String, Program] =
    ProgramParser.parse(input)
