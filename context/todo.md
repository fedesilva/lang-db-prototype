
# Goals

## Main Goal for now

* define two languages, one functional and one imperative.
    - MicroML
    - NanoProc
* try to share infrastucture and find out what we can and can't share.

* write small compilers (no codegen, just generating the ast and pretty printing it is ok)
    - first source to in memory ast, dump to console
    - pretty print the ast.

* extend the languages to use  the graph db
    - first, walk the ast, write into db
        - validate
    - second, try to just parse directly into the db



## Tasks

* Current 
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



* selected
    * define and refine the graph api
        - document what we have right now

