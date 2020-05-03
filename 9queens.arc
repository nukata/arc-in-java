;; N クイーン問題
;; 猪股・益崎「Schemeによる記号処理入門」森北出版 1994年
;; §8.2 の Scheme コードから

(def node-expand (n lst)
  (if (is n 0) nil
    (cons (cons n lst)
          (node-expand (- n 1) lst))))

(def safe? ((new . hlst))
  (if (no hlst) t
    (safe-aux? new (+ new 1) (- new 1) hlst)))

(def safe-aux? (new up down hlst)
  (if (no hlst) t
    (let pos (car hlst)
      (and (~is pos new)
           (~is pos up)
           (~is pos down)
           (safe-aux? new (+ up 1) (- down 1) (cdr hlst))))))

(def goal? (x n) (is (len x) n))

(def nqueens (n)
  (with (lst      (node-expand n nil)
         solution nil
         x        nil)
     (let search (afn ()
                      (if (no lst) solution
                        (do (= x (pop lst))
                            ;;(pr "lst= " lst)
                            ;;(prn " x= " x)
                            (if (safe? x)
                                (if (goal? x n)
                                    (push x solution)
                                  (= lst (+ (node-expand n x) lst))))
                            (self))))
          (search))))

(prn (len (nqueens 9)))                 ; => 352
