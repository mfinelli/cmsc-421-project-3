Project 3
=========

Blocks world is a world with a table, blocks that can be stacked on top of each
other, an arm that can pickup or put-down blocks (one at a time) and *nothing* 
else.

(Lest you think this problem is completely contrived, note that container 
stacking is a very real problem:
[intermodal container](http://en.wikipedia.org/wiki/Intermodal_container).
Granted, our treatment is quite simple compared to the real-life problem.)

State representation
--------------------

The state representation for this world will be a map, with the following keys:
    
    {:pos {the position of all blocks},
     :holding current-block-being-held, ;; nil, if the arm is empty
     :clear #{set of blocks with no blocks stacked above them}}

The value of :pos is also a map from blocks to the block they are stacked on.

    ;; :a is on :b, :b is on :c, :c is on the :table, etc...
    {:a :b, :b :c, :c :table, :d :e, :e :table}

(Note that :table, false and nil are NOT valid blocks.)

Goals and operators
-------------------

A *goal* is a *partial* position map. e.g. 

    {:c :b, :b :a, :a :table}

A state satisfies a goal if, for every key in the goal,

    (= (goal key) ((:pos state) key))

(i.e every block in the state is in the correct position as specified by the 
goal)

Normally, the first step in an AI planning problem is to write planning
*operators*, functions that act on states, representing possible actions.

This has already been done for you. The only operators are are:

    (pickup state block) ;; pickup a clear block
    (puton state target) ;; place the currently held block on target

These operators have preconditions and postconditions specified in blocks.clj
Attempting to apply an operator on a state that does not satisfy the
preconditions is an illegal action.

These planning operators are the only legal way to update the state of the
world. (i.e. you are not allowed to apply your own operators that modify the 
:pos map)

Plans
-----

A plan is a sequence of (legal) operators that, when applied in sequence, will
result in a state that satisfies the goal. e.g.

    ;; initial state
    (def start (init {:a :b, :b :c, :c :table})
    (def goal {:c :b, :b :table})
    (def plan ['(pickup :a) '(puton :table) '(pickup :b) '(puton :table) 
               '(pickup :c) '(puton :b)]))
    (def bad-plan ['(pickup :a) '(puton :table)]) 

    user=> start
    {:pos {:a :b, :c :table, :b :c}, :holding nil, :clear #{:a}}

    user=> (apply-plan start plan)
    {:pos {:a :table, :c :b, :b :table}, :holding nil, :clear #{:a :c}}
    
    user=> (reached-goal? (apply-plan start plan) goal)
    true

    user=> (reached-goal? (apply-plan start bad-plan) goal)
    false

Note that the normal invocation of an operator is:
    
    (pickup state block)
    ;; or
    (puton state target)

The function (apply-plan) is using the (->) "threading" macro. See:
http://clojuredocs.org/clojure_core/clojure.core/-%3E

Assignment
----------

Write the function (find-plan start-pos goal) that returns a plan (i.e. vector of
operators) such that:

    (let [start (init start-pos)
          some-plan (find-plan start goal)]
      (reached-goal? (apply-plan start some-plan) goal)

is true. i.e. find-plan should return a plan that reaches the goal. Note that
find-plan is being passed an initial position map, not an entire state. (i.e.
just the :pos part of a state)

We do *not* require that the plan returned be *optimal* (i.e. as short as
possible), but it should be within an order of magnitude or so.
As a rule of thumb, don't worry too much about optimizing plan length; your
primary concern should be in making sure that the plan is correct.

*If your (find-plan) takes longer than 30 seconds to run, the release tests
will time-out.* (For reference, I'm running these on an Intel i7 @ 2.30 GHz.)
Therefore, when testing, make sure your (find-plan) terminates *well* within
that limit. No points for find-plan's that time-out or blow out the JVM's 
memory (per release test, i.e. if your find-plan times-out on only one test, 
then you only lose the points for that test).

A note on memory: an data structure bound to a var via (def) will *not* be 
garbage collected, unless that var is rebound using another def. Don't
use (def) for local vars; that's what (let) is for (garbage can be collected
immediately after the let goes out of scope).

Submission
----------

Name your file lastname_firstname_project3.clj and submit it (and only it)
to the submit server. (And don't wrap it in a folder; makes my job easier.)

Notes and hints
---------------

Breadth-first search is generally prohibitively expensive for these types of
problems. You are likely going to need some kind of search heuristic. Since
optimality is not required, this heuristic doesn't need to be admissible.

Naive depth-first search can result in an infinite loop (e.g. pickup :a, puton
:table, pickup :a, puton :table, etc...). Design your find-plan function
accordingly.

You will probably want helper function(s) that generate the next set of legal
moves. The logic for determining if a state satisfies the necessary
preconditions is embedded into the definitions of pickup and puton - it should
be straightforward to adapt this to your needs.

Clojure's data structures are all persistent! When you apply an operator, it
returns the state resulting from performing that action, but if you've kept
a reference to the original state, you get to keep it for free. Use this to
your advantage.

You may use functions in core.logic in your find-plan code. But exercise
caution, poorly written relations can take a very long time to execute.

**Test your program.** We will provide some initial states and goals (that we'd
expect your find-plan to be able to solve). You should write more of your own.

**In particular, make sure your code will run as submitted.** (i.e. before you
submit, close the REPL, restart it and reload your code). Clojure requires
functions be defined before they can be referenced which led to some problems
in previous projects. If you submit code that does not run, you will receive
no points on this project.
