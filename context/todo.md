
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
    - call the parser
    - then the demo is the same.

* create a second language
    - first, move the current ast, parser to `languages.microml`
    - this task is to design, and spec, not yet build 
    - new language:
        - imperative
        - small like MicroML
        - call it NanoProc
            - in package `languages.nanoproc`
    - design questions:
        - can we use the same internal representation?
        - others here.


* extend the languages to use  the graph db
    - first, walk the ast, write into db
        - validate
    - second, try to just parse directly into the db



