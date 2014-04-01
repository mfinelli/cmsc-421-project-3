Project 3
=========

Blocks world is a world with a table, blocks that can be stacked on top of each
other, an arm that can pick-up or put-down blocks (one at a time) and *nothing* 
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

(Note that :table, and nil are NOT valid blocks.)

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
:pos map, and yes, the release tests will check this)

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

    user=> (apply-plan start plan)
    {:pos {:a :table, :c :b, :b :table}, :holding nil, :clear #{:a :c}}
    
    user=> (goal-reached? (apply-plan start plan) goal)
    true

    user=> (goal-reached? (apply-plan start bad-plan) goal)
    false

Note that the normal invocation of an operator is:
    
    (pickup state block)
    ;; or
    (puton state target)

The function (apply-plan) is using the (->) "threading" macro. See:
http://clojuredocs.org/clojure_core/clojure.core/-%3E

Assignment
----------


Notes and hints
---------------

...

