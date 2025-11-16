
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

* update the graph to be capable of storing arbitrary (the two languages) languages.

* extend the languages to use  the graph db
    - first, walk the ast, write into db
        - validate
    - second, try to just parse directly into the db

