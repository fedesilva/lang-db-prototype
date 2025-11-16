package langdb.languages.nanoproc.parser

import langdb.languages.nanoproc.ast.Program

object NanoProcParser:

  def parse(input: String, source: String = "<input>"): Either[String, Program] =
    ProgramParser.parse(input, source)
