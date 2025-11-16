* everything lives in the ast package.
    - reorganize
        - ast
        - graph
        - typechecker

* create a parser for a simple language that fits the ast we have defined
    - using fast parse

* update the read me in light of `docs/` and `context/todo.md`
    - also format `docs/vision-and-architecture.md` to 120 cols max lines.

* [DONE] revisit `Term.Print`/`Term.Println` typing to ensure they use `String -> Unit` (introduce `Type.UnitType`/`Term.UnitLit`) or document why they currently return strings

* [DONE] replace the lambda calculus demo
    - implement the example using MicroML directly
        - write the equivalent source to the current programatically built ast.
    - call the parser
    - then the demo is the same.

* [DONE] create a second language
    - this task is to design, and spec, not yet build
    - new language:
        - imperative
        - small like MicroML
        - call it NanoProc
            - in package `languages.nanoproc`

* [DONE] implement nanoproc
    - NOTE: No code reuse from MicroML - independent implementation to discover commonalities
    - Package: `langdb.languages.nanoproc`
    - Implementation steps:
        1. AST definitions:
            - Type.scala (IntType, StringType, BoolType, UnitType)
            - Expr.scala (Var, literals, Add, Sub, Mult, Div, comparisons, And, Not, StringConcat, ProcCall, Print, Println)
            - Stmt.scala (VarDecl, Assign, ExprStmt, Return, If, While, Block)
            - Program.scala (Program, ProcDef)
        2. Parser:
            - NanoProcParser.scala (main interface)
            - ProgramParser.scala (FastParse impl: types -> exprs -> stmts -> procs -> program)
        3. Type checker:
            - TypeChecker.scala (check exprs, stmts, procs, full programs with return checking)
        4. Demo:
            - ParserDemo.scala (factorial, loops, conditionals, multiple procedures)
        5. Tests:
            - NanoProcParserSpec.scala (32 comprehensive tests, all passing)
            - TypeCheckerSpec.scala (33 comprehensive type checker tests, all passing)
