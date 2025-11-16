
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

* replace the lambda calculus demo 
    - implement the example using MicroML directly
        - write the equivalent source to the current programatically built ast.
    - call the parser
    - then the demo is the same.

* create a second language
    - this task is to design, and spec, not yet build 
    - new language:
        - imperative
        - small like MicroML
        - call it NanoProc
            - in package `languages.nanoproc`

* implement nanoproc

* update the graph to be capable of storing arbitrary (the two languages) languages.

* extend the languages to use  the graph db
    - first, walk the ast, write into db
        - validate
    - second, try to just parse directly into the db



