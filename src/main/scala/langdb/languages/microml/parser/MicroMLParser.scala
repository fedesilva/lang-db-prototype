package langdb.languages.microml.parser

import langdb.languages.microml.ast.Term

object MicroMLParser:

  def parse(input: String, source: String = "<input>"): Either[String, Term] =
    TermParser.parse(input, source)
