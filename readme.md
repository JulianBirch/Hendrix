# hendrix - clojure asset pipeline

Hendrix is intended to be a web asset pipeline for Clojure, solving
the same problem as the rails asset pipeline, but not necessarily in
the same way.  It's motivated by my experience of ASP.NET MVC and its
deficiencies.

## What it can do

At the moment, the honest answer is very little.  There's
<ul>
<li>a pretty basic make (called execute) missing task scheduling</li>
<li>a generalized watch infrastructure</li>
<li>a neat way of handling temporary directories</li>
<li>some fairly cool test code that enables you to fake out the file system</li>
</ul>

So, a lot of meta.  In fact, the only actual use of it right now is to
compile bootstrap with your own variable file.

## How to use it

At the moment, its best used through a lein run command.  Later
versions will hopefully support more magic.  Here's some actual
working code:

<pre>
(ns semele.commands
  (:use [hendrix core command watch]
        [clojure pprint]))

(def bootstrap
  (new-merge-rule
   "assets/variables.less"
   "checkouts/bootstrap/lib/*.less"))

(def cb (new-rule (bootstrap "bootstrap.less")
                   "resources/public/site.css"
                   lessc
                   bootstrap))

(defn compile-bootstrap [] (execute bootstrap cb))

(defn watch [] (start compile-bootstrap))
</pre>

"bootstrap" defines rule for a temporary directory with the contents
of the two expressions following.  Earlier items take precedence over
later ones.  "cb" is the actual rule to compile bootstrap.  The order
of parameters is

<ul>
<li>The main file.  Observe the way the temporary directory is referenced.</li>
<li>The output file</li>
<li>The command to use, defined in hendrix.command</li>
<li>Implicit inputs to check.  Here, the entire contents of the temporary directory are referenced.</li>
</ul>

## What's next?

Things I'd like it to be able to do, roughly in order:
<ul>
<li>cljs-watch functionality</li>
<li>Minify</li>
<li>Switching between uncompressed and minified versions</li>
<li>Allowing live-reloading of CSS</li>
</ul>

Adding test watching functionality would be cool, but would require
some thought.  I don't think I need a general aggregation strategy
like rails, given that less and cljs already have their own, but I
could be proved wrong.
