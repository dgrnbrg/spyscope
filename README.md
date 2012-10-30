# Spyscope

A Clojure library designed to make it easy to debug single- and multi-threaded applications.

## Usage

Add `[spyscope "0.1.0"]` to your project.clj's `:dependencies`.

Spyscope includes 3 reader tools for debugging your Clojure code, which are exposed as reader tags:
`#spy/p`, `#spy/d`, and `#spy/t`, which stand for *print*, *details*, and *trace*, respectively.
Reader tags were chosen because they allow one to use Spyscope by only writing 6 characters, and
since they exist only to the left of the form one wants to debug, they require the fewest possible
keystrokes, optimizing for developer happiness. :)

### `#spy/p`

First, let's look at `#spy/p`, which just pretty-prints the form of interest:

    spyscope.repl=> (take 20 (repeat #spy/p (+ 1 2 3)))
    6
    (6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6)

`#spy/p` is an extremely simple tool that merely saves a few keystores when
one needs to dump out a value in the middle of a calculation.

### `#spy/d`

Next, let's look at `#spy/d`. This is where the real power lies:

    spyscope.repl=> (take 20 (repeat #spy/d (+ 1 2 3)))
    spyscope.repl$eval672.invoke(REPL:12) => 6
    (6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6)

In the simplest usage, the form is printed along with the stack trace
it occurred on, which makes it easier to grep through logs that have
many tracing statements enabled.

Often, you may find that additional context would be beneficial, so
you can request additional stack frames with the metadata key `:fs`
(first and last letters of "frames"):

    spyscope.repl=> (take 20 (repeat #spy/d ^{:fs 3} (+ 1 2 3)))
    ----------------------------------------
    clojure.lang.Compiler.eval(Compiler.java:6477)
    clojure.lang.Compiler.eval(Compiler.java:6511)
    spyscope.repl$eval675.invoke(REPL:13) => 6
    (6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6)

As you can see, when multiple stack frames are printed, a row of dashes
is printed before the trace to keep the start of the stack frame group
clearly denoted.

As you debug further, you may realize that the context of the creation of
certain values is important; however, if you print out 10 or 20 lines of
stack trace, you'll end up with an unreadable mess. The metadata key `:nses`
allows you to apply a regex to the stacktrace frames to filter out noise:

    spyscope.repl=> (take 20 (repeat #spy/d ^{:fs 3 :nses #"core|spyscope"} (+ 1 2 3)))
    ----------------------------------------
    clojure.core$apply.invoke(core.clj:601)
    clojure.core$eval.invoke(core.clj:2797)
    spyscope.repl$eval678.invoke(REPL:14) => 6
    (6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6)

The last feature of `#spy/d` is that it can print the code that generated
the value, which can help you disambiguate multiple nearby related values.
This is controlled by setting the metadata key `:form` to `true`:

    spyscope.repl=> {:a #spy/d ^{:form true} (+ 1 2 3)
                     :b #spy/d ^{:form true} (- 16 10)}
    spyscope.repl$eval685.invoke(REPL:16) (+ 1 2 3) => 6
    spyscope.repl$eval685.invoke(REPL:16) (- 16 10) => 6
    {:a 6, :b 6}

### `#spy/t`

Finally, let's look at `#spy/t`. Tracing is very similar to detailed
printing, but it enables us to get meaningful results when using `#spy/d`
on a program that has multiple interacting threads without affecting
most interactive development workflows!

`#spy/t` accepts all of the metadata arguments that `#spy/d` does (i.e.
`:fs`, `:nses`, and `:form`).

Instead of immediately printing out results, it stores them in an
agent asynchronously. Each time a trace is logged, it is placed into
the current generation. One can use a function to increment the generation
counter, and previous generations are stored, so that one can compare
several recent generations to understand what effects changes may have had.

There are several functions you can use to interact with the trace store:

* `trace-query` is the workhorse function. With no arguments, it prints every
trace from the current generation. With a numeric argument `generations`,
it prints every trace from the past `generations` generations. With a
regex argument `re`, it prints every trace from the current generation whose
root stack frame matches the regex. Also accepts 2 arguments to specify the
filtering regex and how many generations to include.

* `trace-next` moves onto the next generation. One usually calls this between
trials or experiments.

* `trace-clear` deletes all trace data collected so far. Since all trace
data is saved, that can become quite a lot of data, so this can be used
to clean up very long running sessions.

## License

Copyright © 2012 David Greenberg

Distributed under the Eclipse Public License, the same as Clojure.
