;; Mario Finelli (mfinelli)
;; CMSC 421 - Section 0201
;; Project 3 (due 8 May 2014)

;; Sample configuration in blocks world:
;;
;;  :a
;;  :b :d
;;  :c :e
;; :table
;;
;; (def initial-pos {:a :b, :b :c, :c :table, :d :e, :e :table})
;; (def initial-state (init initial-pos))

(defn init
  "Returns an appropriate initial state as specified by pos.
   By default, the arm is empty."
  [pos]
  (let [holding nil
        non-clear (set (vals pos))
        clear (set
                (remove #(contains? non-clear %)
                        (keys pos)))]

    {:pos pos, :holding holding, :clear clear}))

(defn pickup
  "Returns state resulting from picking up block, nil if pickup is illegal.
   Preconditions: block is clear, arm is empty.
   Postconditions: arm is holding block, block is not clear,
                   block-below (if any) is now clear. pos of block is nil."
  [state block]
  (cond (:holding state) nil
        (not ((:clear state) block)) nil
        :else
          (let [holding block
                pos (assoc (:pos state) block nil)
                block-below ((:pos state) block)
                clear (if (= block-below :table)
                          (-> (:clear state) (disj block))
                          (-> (:clear state) (disj block)
                              (conj block-below)))]

            {:pos pos, :holding holding, :clear clear})))

(defn puton
  "Returns state resulting from putting currently held block on target.
   Returns nil if puton is an illegal action.
   Preconditions: arm is holding block, target is :table or clear.
   Postconditions: arm is empty, target is not clear, block is clear,
                   block is on top of target."
  [state target]
  (cond (not (:holding state)) nil
        (and (not ((:clear state) target)) (not= target :table)) nil
        :else
          (let [block (:holding state)
                holding nil
                pos (assoc (:pos state) block target)
                clear (-> (:clear state) (disj target) (conj block))]

            {:pos pos, :holding holding, :clear clear})))

(defn reached-goal?
  "Returns true iff state satsifies goal."
  [state goal]
  (every? true? (for [k (keys goal)]
                  (= ((:pos state) k) (goal k)))))

(defn apply-plan
  "Returns the result of applying, in sequence, every action in actions to
   state. e.g. (apply-actions state ['(pickup :a) '(puton :table)]) returns
   the equivalent of (puton (pickup state :a) :table)."
  [state actions]
  (eval (concat `(-> ~state) actions)))

;;;;;;;;

(defn in?
  "Returns true if the sequence s contains the element e.
   https://stackoverflow.com/a/3249777"
  [s e]
  (some #(= e %) s))

(defn remove-all
  "Removes every element in a from b.
   https://stackoverflow.com/a/1511428"
  [a b]
  (remove (set a) b))

(defn on-table
  "Returns a vector of blocks that are on the table from a given postion map."
  [pos]
  (vec
    (remove nil?
      (map
        (fn [block]
          (if (= (block pos) :table)
            block))
        (keys pos)))))
;; this works by taking all of the keys in the position map -- that is every
;; block (separated from what it is sitting on be that the table or another
;; block. -> (map fn (keys pos))
;; then we use the anonymous function that takes each block and checks if it's
;; on the table and if it it returns it. -> (block pos) returns the block or
;; table that block is sitting on.
;; if it isn't on the table then it'll return null and we want to remove those
;; then return a vector. -> (vec (remove nil? *results from anon. function*))

(defn put-on-table
  "Returns a vector of blocks that should be picked up and put on the table
   based on a given state"
  [state]
  (vec
    (remove
      (set (on-table (:pos state)))
      (:clear state))))
;; here we want to remove all of the blocks that are already on the table from
;; the set of clear blocks -- those that are free to be moved.
;; this gives us a set of blocks that aren't already on the table and don't
;; have any blocks on top of them which means we can safely put them on the
;; table.

(defn unstack
  "Given a state returns a plan for moving all blocks onto the table and a new
   state from the resulting moves."
  [state]
  (if (= (count (put-on-table state)) 0)
    {:plan [], :state state}
    (let [plan (reduce into
                 (for [block (put-on-table state)]
                   [`(pickup ~block) `(puton :table)]))
          new-state (apply-plan state plan)]
      (let [nxt (unstack new-state)]
        (if (> (count (:plan nxt)) 0)
          {:plan (reduce conj plan (:plan nxt)), :state (:state nxt)}
          {:plan plan, :state new-state})))))
;; since this is a recursive function first thing we want to do is check if
;; we have no more blocks to put on the table since this will be what we check
;; for when deciding whether to recurse or not. if we don't have any more
;; blocks to move then return an empty plan.
;; now our plan consists of taking each block not already on the table and
;; picking it up and putting it on the table. the for loop takes each block
;; free to move to the table and saves the actions pickup and puton. the
;; results from that loop are then reduced into a single vector.
;; then we take that partial plan and apply it to the state. Then we do a
;; "look-ahead" (recurse) on the new state that we got after applying the plan.
;; if there are no more blocks to move (as explained above) then we return our
;; plan and the resulting state. otherwise we need add the results of the
;; look-ahead to out plan and update the state and return that.

(defn stack
  "Given blocks out of place and in place and end goal and a current state
   return the steps needed to build the solution."
  [in-place out-of-place goal state]
  (if (= (count out-of-place) 0)
    {:plan [], :state state}
    (let [move (remove nil?
                 (for [block out-of-place]
                   (if (in? in-place (block goal))
                     block)))
          plan (if (zero? (count move))
                 []
                 (vec (reduce into
                  (for [block move]
                    [`(pickup ~block) `(puton ~(block goal))]))))
          new-state (apply-plan state plan)
          new-in-place (if (zero? (count move))
                         (vec 
                           (remove-all out-of-place 
                                       (distinct 
                                         (reduce conj 
                                                 (vals goal) 
                                                 (keys goal)))))
                         (vec (reduce conj move in-place)))
          new-out-of-place (vec (remove (set move) out-of-place))]
      (let [nxt (stack new-in-place new-out-of-place goal new-state)]
        (if (> (count (:plan nxt)) 0)
          {:plan (reduce conj plan (:plan nxt)), :state (:state nxt)}
          {:plan plan, :state new-state})))))
;; this is very similiar to the function that unstacks the blocks but here the
;; criteria for recursing is if we have any blocks that are out of place or
;; not. (we recurse only if we do have blocks out of place)
;; to get the blocks that we want to move we loop through the blocks that are
;; out of place and if the block below them are in place then we can safely
;; move it into position.
;; the plan is created similarly to above only instead of moving the block
;; onto the table we move it on top of the block that it belongs on to satisfy
;; the goal.
;; then we get the new state on which we'll recurse and add the blocks we moved
;; to the in-place array and remove them from the out of place array.
;; if we had no moves then it means that we have blocks that satisfy their
;; goal even when not sitting on the table. to fix an infinite loop we get all
;; of the blocks (distinct (reduce conj (vals goal) (keys goal))) and remove
;; the ones that are out of place and what is left we know is in place. mark
;; these blocks as in place and recurse.
;; then do the "look-ahead" and if we get the empty plan then return our plan
;; and updated state, otherwise combine the plan from recursing with our
;; current plan and return that.

(defn find-plan
  "Finds a plan from start-pos to goal.
   We'll accomplish this by first putting all of the blocks on the table and
   then reconstructing the goal state as we want it."
  [start-pos goal]
    (let [start-state (init start-pos)]
      (if (= (reached-goal? (init start-pos) goal) true)
        [] ;; if we've got the goal state return an empty plan
        (let [setup (unstack start-state)
              out-of-place (vec (keys (apply dissoc goal (on-table goal))))
              in-place (on-table goal)
              build (stack in-place out-of-place goal (:state setup))
              plan (reduce conj (:plan setup) (:plan build))]
          plan))))
;; if we already have the goal state just return an empty plan. otherwise we
;; save the plan to unstack the blocks in the variable setup and then calculate
;; the out of place and in-place blocks and then calculate the steps required
;; to build the solution starting from all blocks on the table.
;; then combine the destruct and construct plans and return the entire plan.

;;;; TESTS ;;;;

;;; Write your own tests. Tests are good!
;;; (I've included tests directly in this file in the hopes that more people
;;; will use them for this project.)

(def tall {:a :b, :b :c, :c :d, :d :e,
           :e :f, :f :g, :g :h, :h :i, :i :table})

(def tri {:a :b, :b :c, :c :table, :d :e, :e :f, :f :table})

(def huge {:a :b, :b :table, :c :a, :d :table, :e :table, :f :g, :g :h,
           :h :i, :i :j, :j :table, :k :m, :m :l, :l :table, :n :o, :o :p,
           :p :q, :q :r, :r :table, :s :table, :t :table, :u :table, :v :u,
           :w :v, :x :w, :y :x, :z :table})

(def all-on-table {:a :table, :b :table, :c :table})

(def goal-small {:c :a, :a :b, :b :table})

(def goal-large {:c :a, :a :b, :b :e, :e :table, :d :f, :f :table})

(def goal-not-on-table {:c :b, :b :a, :e :f})

(def goal-half-not-on-table {:c :b, :b :a, :a :table, :d :e, :e :f, :g :h,
                             :i :table})

(def goal-stairs {:a :table, :b :c, :c :table, :d :e, :e :f, :f :table, :g :h,
                  :h :i, :i :j, :j :table, :k :l, :l :m, :m :n, :n :o,
                  :o :table, :p :q, :q :r, :r :s, :s :t, :t :u, :u :table})

(defn test-find-plan
  "True if the plan from find-plan succesfully reaches the goal."
  [start-pos goal]
  (reached-goal? (apply-plan (init start-pos) (find-plan start-pos goal))
                 goal))

(defn run-tests []
  (do
    (println "Running tests...")
    (println (test-find-plan tall goal-small)
             (test-find-plan tall goal-large)
             (test-find-plan tri goal-small)
             (test-find-plan tri goal-large)
             (test-find-plan all-on-table goal-small)
             (test-find-plan tall goal-not-on-table)
             (test-find-plan huge goal-stairs)
             (test-find-plan tall goal-half-not-on-table))))

;; user=> (time (run-tests))
;; Running tests...
;; true true true true
;; "Elapsed time: 59.365189 msecs"
;; nil
;;
