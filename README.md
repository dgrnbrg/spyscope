# Spyscope

A Clojure(Script) library designed to make it easy to debug single- and multi-threaded applications.

## Installation


#### Leiningen

Add `[spyscope "0.1.6"]` to your project.clj's `:dependencies`.

If you want spyscope to be automatically loaded and available in every project,
add the following to the `:user` profile in `~/.lein/profiles.clj`:

    :dependencies [[spyscope "0.1.6"]]
    :injections [(require 'spyscope.core)]

#### Boot

After requiring the namespace, you must also run `(boot.core/load-data-readers!)` 
to get the reader tags working. Using a `~/.boot/profile.boot` file:

```
(set-env! :dependencies #(conj % '[spyscope "0.1.6"]))

(require 'spyscope.core)
(boot.core/load-data-readers!)
```

## Usage

Spyscope includes 3 reader tools for debugging your Clojure code, which are exposed as reader tags:
`#spy/p`, `#spy/d`, and `#spy/t`, which stand for *print*, *details*, and *trace*, respectively.
Reader tags were chosen because they allow one to use Spyscope by only writing 6 characters, and
since they exist only to the left of the form one wants to debug, they require the fewest possible
keystrokes, optimizing for developer happiness. :)

### `#spy/p`

First, let's look at `#spy/p`, which just pretty-prints the form of interest:

```clojure
spyscope.repl=> (take 20 (repeat #spy/p (+ 1 2 3)))
6
(6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6)
```

`#spy/p` is an extremely simple tool that merely saves a few keystores when
one needs to dump out a value in the middle of a calculation.

### `#spy/d`

Next, let's look at `#spy/d`. This is where the real power lies:

```clojure
spyscope.repl=> (take 20 (repeat #spy/d (+ 1 2 3)))
spyscope.repl$eval3869.invoke(NO_SOURCE_FILE:1) (+ 1 2 3) => 6
(6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6)
```

In the simplest usage, the form is printed along with the stack trace
it occurred on, which makes it easier to grep through logs that have
many tracing statements enabled.

Often, you may find that additional context would be beneficial.
One way to add context is to include a marker in all of the output.
This lets you add a semantic name to any spy:

```clojure
spyscope.repl=> #spy/d ^{:marker "triple-add"} (+ 1 2 3)
spyscope.repl$eval3935.invoke(NO_SOURCE_FILE:1) triple-add (+ 1 2 3) => 6
6
```

In addition, you can request additional stack frames with the
metadata key `:fs`, which gives you a richer context without you
doing anything:

aside: (`:fs` comes from first and last letters of "frames")

```clojure
spyscope.repl=> (take 20 (repeat #spy/d ^{:fs 3} (+ 1 2 3)))
----------------------------------------
clojure.lang.Compiler.eval(Compiler.java:6477)
clojure.lang.Compiler.eval(Compiler.java:6511)
spyscope.repl$eval675.invoke(REPL:13) (+ 1 2 3) => 6
(6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6)
```

As you can see, when multiple stack frames are printed, a row of dashes
is printed before the trace to keep the start of the stack frame group
clearly denoted.

As you debug further, you may realize that the context of the creation of
certain values is important; however, if you print out 10 or 20 lines of
stack trace, you'll end up with an unreadable mess. The metadata key `:nses`
allows you to apply a regex to the stacktrace frames to filter out noise:

```clojure
spyscope.repl=> (take 20 (repeat #spy/d ^{:fs 3 :nses #"core|spyscope"} (+ 1 2 3)))
----------------------------------------
clojure.core$apply.invoke(core.clj:601)
clojure.core$eval.invoke(core.clj:2797)
spyscope.repl$eval678.invoke(REPL:14) (+ 1 2 3) => 6
(6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6)
```

If you leave your application unattended for a period of time, you may
wish to have timestamps included in all the output lines. Spyscope can use
a default time format, or a user-provided one:

```clojure
;; Default formatter is yyyy-mm-ddThh:mm:ss
spyscope.repl=> #spy/d ^{:time true} (+ 1 2 3)
spyscope.repl$eval4028.invoke(NO_SOURCE_FILE:1) 2013-04-11T03:20:46 (+ 1 2 3) => 6
6
```

```clojure
;; Custom formatters use clj-time
spyscope.repl=> #spy/d ^{:time "hh:mm:ss"} (+ 1 2 3)
spyscope.repl$eval4061.invoke(NO_SOURCE_FILE:1) 03:21:40 (+ 1 2 3) => 6
6
```

The last feature of `#spy/d` is that it can suppress printing the code
that generated the value, which can be used to de-clutter the output
if you have particularly large forms. This is controlled by setting
the metadata key `:form` to `false`:

```clojure
spyscope.repl=> {:a #spy/d ^{:form false} (+ 1 2 3)
                 :b #spy/d ^{:form false} (- 16 10)}
spyscope.repl$eval685.invoke(REPL:16) => 6
spyscope.repl$eval685.invoke(REPL:16) => 6
{:a 6, :b 6}
```

Under the hood, `#spy/d` actually does all of its printing on another thread
--the tracing store thread! This provides 2 benefits: if you are printing
from multiple threads, your output will not be interleaved amongst threads. The
other benefit is that every trace statement is logged, so that you can use
the `#spy/t` api to refine your search after you start tracing with `#spy/d`.

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

## Example annotated `#spy/t` session

```clojure
;;Let's run some code on futures, but see the chronological result
user=> (future (Thread/sleep 1000) #spy/t ^{:form false} (+ 1 2))
       (future #spy/t (+ 3 4))
#<Future@1013d7df: :pending>
;;We'll need to use the repl functions
user=> (use 'spyscope.repl)
nil
;;trace-query shows all the traces by default, separated by dashed lines
user=> (trace-query)
user$eval35677$fn__35689.invoke(NO_SOURCE_FILE:1) (+ 3 4) => 7
----------------------------------------
user$eval35677$fn__35678.invoke(NO_SOURCE_FILE:1) => 3
nil
;;We'll define and invoke a function with a #spy/t
user=> (defn my-best-fn [] #spy/t ^{:form false} (* 5 6))
       (my-best-fn)
30 ;Here's the return value--note that the trace isn't printed
;;Let's see all traces so far 
user=> (trace-query)
user$eval35677$fn__35689.invoke(NO_SOURCE_FILE:1) (+ 3 4) => 7
----------------------------------------
user$eval35677$fn__35678.invoke(NO_SOURCE_FILE:1) => 3
----------------------------------------
user$eval35822$my_best_fn__35823.invoke(NO_SOURCE_FILE:1) => 30 ;Here's our new trace
nil
;;We can use a filter regex to only see matching stack frames 
;;Usually, you can filter by the name of the innermost function
;;You can increase the :fs metadata parameter to have more context to filter by
user=> (trace-query #"best")
user$eval35822$my_best_fn__35823.invoke(NO_SOURCE_FILE:1) => 30
nil
;;Move onto a new generation
user=> (trace-next)
nil
;;No traces in this generation
user=> (trace-query)
nil
;;Increase the number of generations in the query to review older traces
user=> (trace-query 2)
user$eval35677$fn__35689.invoke(NO_SOURCE_FILE:1) (+ 3 4) => 7
----------------------------------------
user$eval35677$fn__35678.invoke(NO_SOURCE_FILE:1) => 3
----------------------------------------
user$eval35822$my_best_fn__35823.invoke(NO_SOURCE_FILE:1) => 30
nil
;;Add a new trace to the current generation
user=> (my-best-fn)
30
;;We can see that there's only one trace in this generation--the one we just made
user=> (trace-query)
user$eval35822$my_best_fn__35823.invoke(NO_SOURCE_FILE:1) => 30
nil
;;We can combine the generation and regex filter to search and filter many generations
;;Here we see the invocations of my-best-fn from the current and previous generation
user=> (trace-query #"best" 2)
user$eval35822$my_best_fn__35823.invoke(NO_SOURCE_FILE:1) => 30
----------------------------------------
user$eval35822$my_best_fn__35823.invoke(NO_SOURCE_FILE:1) => 30
nil
user=> 
```

## Contributors

David Greenberg (@dgrnbrg) and Herwig Hochleitner (@bendlas)

## License

Copyright © 2012 David Greenberg

Distributed under the Eclipse Public License, the same as Clojure.
