(prn
 ((fn (path c)
      (def add (s)
           (push s path))
      (protect
       (fn () (add (ccc (fn (c0)
                            (= c c0)
                            'talk1))))
       (fn () (add 'disconnect)))
      (if (< (len path) 3)
          (c 'talk2)
        (rev path)))
  nil nil))
;; => (talk1 disconnect talk2 disconnect)

(prn (ccc (fn (c)
              (protect
               (fn ()
                   (prn 1)
                   (c 100)
                   2)
               (fn () (prn 'bye))))))
;; =>
;; 1
;; bye
;; 100
