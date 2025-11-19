
# Goals

## Main Goal 

* define two languages, one functional and one imperative.
    - MicroML
    - NanoProc
* try to share infrastucture and find out what we can and can't share.
* evolve the graph engine
    - represent both languages
    - rewrite/write specific functionality (like dependency checking, escapa analysis)
        using the graph itself.
    - design a mechanism to 

* write small compilers (no codegen, just generating the ast and pretty printing it is ok)
    - first source to in memory ast, dump to console
    - pretty print the ast.




## Tasks


* parsers and asts: track and store SourceSpans(line, col, charindex)
    - for provenance, later
    - for completeness
    - test
    - Implementation: SourceSpan type, both parsers capture positions, all ASTs updated
    - Codebase made purely functional (removed var, ThreadLocal, mutable collections)

    [ISSUES]

    - Nested expression spans collapse: chained applications/operators always reuse the full expression span for every newly
    synthesized node (`src/main/scala/langdb/languages/microml/parser/TermParser.scala:106-164` and
    `src/main/scala/langdb/languages/nanoproc/parser/ProgramParser.scala:110-160`). As soon as you parse something like `f x y`
    or `1 + 2 + 3`, the intermediate `App`/`Add` nodes inherit the span of the entire chain rather than the slice they actually
    cover, which breaks the provenance guarantees you just added and makes precise diagnostics impossible. Each fold step needs
    to derive a span from the operands (start = left.startIndex, end = right.endIndex) instead of reusing the outer `Index`
    pair.

    - SourceSpan generation is quadratic: every call to `SourceSpan.fromIndices` scans the entire input and rebuilds an index→
    position map (`src/main/scala/langdb/common/SourceSpan.scala:36-55`). Because the parsers invoke it for nearly every node,
    parsing costs blow up to O(n²) for long files. Consider precomputing a line/column lookup table once per parser instance or
    threading line/column counters directly from FastParse to keep span creation O(1).

* update the graph to be capable of storing arbitrary (the two languages) languages.
    - design task:
        - look at the current graph api
            - the current graph api is insuficient and the implementation is buggy as heck
                - see `context/graph-bugs.md`
        - look at both parsers/asts
        - do we have the tools in the api to serialize the asts into the graph engine?
    - we should
        - serialize both ast with a generic api
        - preserve all information
        - be able to deserialize back to ast.
        - when we have interpreters the deserialized ast should run
        - final serialization should happen to a persisten file
            - for now just the arrow ipc file.
        - we should be able to get an old file and deserialize it and run it with the interpreter.
        - interpreter is out of scope, just plan for it.
        

* implement interpreters
    - we want to validate that serialization/deserialization gets us back the proper ast
        - and that we can run the deserialized programs


* extend the languages to use  the graph db
    - first, walk the ast, write into db
        - validate
    - second, try to just parse directly into the db

