package langdb.languages.nanoproc.ast

import langdb.common.SourceSpan

// Procedure definition
case class ProcDef(
  name:       String,
  params:     List[(String, Type)],
  returnType: Type,
  body:       Stmt.Block,
  span:       SourceSpan
) derives CanEqual

// Complete NanoProc program
case class Program(procs: List[ProcDef], span: SourceSpan) derives CanEqual
