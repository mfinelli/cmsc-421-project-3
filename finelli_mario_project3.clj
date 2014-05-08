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

(defn table
  "Returns a vector of blocks that are on the table from a given postion map."
  [pos]
  (vec
    (remove nil?
      (map
        (fn [block]
          (if (= (block pos) :table)
            block))
        (keys pos)))))

(defn put-on-table
  "Returns a vector of blocks that should be picked up and put on the table
   based on a given state"
  [state]
  (vec
    (remove
      (set (table (:pos state)))
      (:clear state))))

(defn plan-blocks-on-table
  "Given a state returns a plan for moving all blocks onto the table and a new
   state from the resulting moves."
  [state]
  (if (= (count (put-on-table state)) 0)
    {:plan [], :state state}
    (let [plan (reduce into
                 (for [block (put-on-table state)]
                   [`(pickup ~block) `(puton :table)]))
          newstate (apply-plan state plan)]
      (let [nxt (plan-blocks-on-table newstate)]
        (if (> (count (:plan nxt)) 0)
          {:plan (reduce conj plan (:plan nxt)), :state (:state nxt)}
          {:plan plan, :state newstate})))))

(defn build
  "Given blocks out of place and in place and end goal and a current state
   return the steps needed to build the solution."
  [in-place out-of-place goal state]
  (if (= (count out-of-place) 0)
    {:plan [], :state state}
    (let [move (remove nil?
                 (for [block out-of-place]
                   (if (in? in-place (block goal))
                     block)))
          plan (reduce into
                  (for [block move]
                    [`(pickup ~block) `(puton ~(block goal))]))
          new-state (apply-plan state plan)
          new-in-place (vec (reduce conj move in-place))
          new-out-of-place (vec (remove (set move) out-of-place))]
      (let [nxt (build new-in-place new-out-of-place goal new-state)]
        (if (> (count (:plan nxt)) 0)
          {:plan (reduce conj plan (:plan nxt)), :state (:state nxt)}
          {:plan plan, :state new-state})))))

(defn find-plan
  "Finds a plan from start-pos to goal.
   We'll accomplish this by first putting all of the blocks on the table and
   then reconstructing the goal state as we want it."
  [start-pos goal]
    (let [start-state (init start-pos)]
      (if (= (reached-goal? (init start-pos) goal) true)
        [] ;; if we've got the goal state return an empty plan
        (let [setup (plan-blocks-on-table start-state)
              out-of-place (vec (keys (apply dissoc goal (table goal))))
              in-place (table goal)
              build (build in-place out-of-place goal (:state setup))
              plan (reduce conj (:plan setup) (:plan build))]
          plan))))

;;;; TESTS ;;;;

;;; Write your own tests. Tests are good!
;;; (I've included tests directly in this file in the hopes that more people
;;; will use them for this project.)

(def tall {:a :b, :b :c, :c :d, :d :e,
           :e :f, :f :g, :g :h, :h :i, :i :table})

(def tri {:a :b, :b :c, :c :table, :d :e, :e :f, :f :table})

(def goal-small {:c :a, :a :b, :b :table})

(def goal-large {:c :a, :a :b, :b :e, :e :table, :d :f, :f :table})

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
             (test-find-plan tri goal-large))))

;; user=> (time (run-tests))
;; Running tests...
;; true true true true
;; "Elapsed time: 59.365189 msecs"
;; nil
;;

