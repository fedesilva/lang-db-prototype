
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

* update the graph to be capable of storing arbitrary (the two languages) languages.
    - design task:
        - look at the current graph api
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

