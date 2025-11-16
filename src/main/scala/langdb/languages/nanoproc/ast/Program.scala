package langdb.languages.nanoproc.ast

// Procedure definition
case class ProcDef(
  name:       String,
  params:     List[(String, Type)],
  returnType: Type,
  body:       Stmt.Block
) derives CanEqual

// Complete NanoProc program
case class Program(procs: List[ProcDef]) derives CanEqual
