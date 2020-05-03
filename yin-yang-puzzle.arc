;; The yin-yang puzzle
;; cf. https://en.wikipedia.org/wiki/Call-with-current-continuation
(withs
    (yin  ((fn (cc) (prn)    cc) (ccc (fn (c) c)))
     yang ((fn (cc) (pr #\*) cc) (ccc (fn (c) c))))
  (yin yang))

;; => \n*\n**\n***\n****\n*****\n******\n...
