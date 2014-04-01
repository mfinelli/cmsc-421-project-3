;; Your Name
;; Your User ID (what you use to log into submit.cs.umd.edu)

;; Sample configuration:
;; 
;;  :a 
;;  :b :d
;;  :c :e
;; :table
;; 
(def sample {:a :b, :b :c, :c :table, :d :e, :e :table})

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
   Returns nil if putdown is an illegal action.
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


