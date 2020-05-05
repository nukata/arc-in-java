# Arc in Java

This is an interpreter of a nearly complete subset of Arc, a dialect of Lisp
designed by Paul Graham et al.
It optimizes _tail calls_ and handles _first-class continuations_ properly.
It is written in about 4600 lines of Java 11 (OpenJDK 11.0.7)
with the same prelude [`arc.arc`](arc/arc.arc) as the original in
[arc3.2.tar](http://www.arclanguage.org/arc3.2.tar)
at <http://www.arclanguage.org/install>.

You can run _all_ examples in the original Arc tutorial,
<http://www.arclanguage.org/tut.txt>
(or its copy, <https://arclanguage.github.io/tut-stable.html>)
except for `defop`'s "web app" example in the latter section.

Note that its string representation of a function differs from that in the
tutorial.  For example, you will see

```
arc> (def average (x y)
       (/ (+ x y) 2))
#<fn:2:((/ (+ #0:0:x #0:1:y) 2))>       
arc> (average 2 4)
3
```

instead of

```
arc> (def average (x y)
       (/ (+ x y) 2))
#<procedure: average>
arc> (average 2 4)
3
```

where `fn:2` means a function of arity 2 and `#0:0:x` means a resolved
local variable in the frame of level 0, located at offset 0 in the frame and
named `x`.

I wrote the interpreter first in Java 5, named _Semi-Arc_
for lack of first-class continuations then, and presented it under
the MIT License at <http://www.oki-osk.jp/esc/llsp/> (broken link)
from 2010 (H22) until 2017 (H29).
Now in 2020 (R02), I revised it to implement first-class continuations
this time in the same way as
[little-scheme-in-java](https://github.com/nukata/little-scheme-in-java).
The codes are still commented in Japanese for now.


## How to run it

```
$ make
rm -f arc/*.class
javac arc/Main.java
jar cfm arc.jar arc/Manifest LICENSE arc/arc.arc arc/*.class
$ java -jar arc.jar
arc> (+ 5 6 7)
18
arc> (cons 'a (cons 'b 'c))
(a b . c)
arc>
```

Press EOF (e.g. Control-D) to exit the session.

```
arc> Goodbye
$
```

`arc.jar` is self-contained and includes `arc.arc` and `LICENSE`.
You can copy it anywhere solely.

You can run it with Arc scripts.

```
$ cat yin-yang-puzzle.arc
;; The yin-yang puzzle
;; cf. https://en.wikipedia.org/wiki/Call-with-current-continuation
(withs
    (yin  ((fn (cc) (prn)    cc) (ccc (fn (c) c)))
     yang ((fn (cc) (pr #\*) cc) (ccc (fn (c) c))))
  (yin yang))

;; => \n*\n**\n***\n****\n*****\n******\n...
$ java -jar arc.jar yin-yang-puzzle.arc

*
**
***
****
*****
******
*******
********
*********
^C$
```

Press INTR (e.g. Control-C) to terminate the yin-yang-puzzle.

Put a "`-`" after the script in the command line to begin a session 
after running the script.

```
$ java -jar arc.jar 9queens.arc -
352
arc> (nqueens 6)
((5 3 1 6 4 2) (4 1 5 2 6 3) (3 6 2 5 1 4) (2 4 6 1 3 5))
arc>
```


## How to extend it

The code below is an example to define an Arc function in Java.

```Java
import java.io.PrintWriter;
import java.util.ArrayList;
import arc.*;

public class PrintfExample extends Intrinsic {
    public PrintfExample () {
        super
            (1,          // It has one fixed argument, i.e., "format".
             null,       // It has no default values.
             true,       // It has rest arguments.
             "printf",   // It is named "printf".
             "(printf FORMAT ...) writes args in FORMAT.", // Help document
             null,         // Function body (Object[])->{...}.
             (a, eval)-> { // Alternative body (Object[], Eval)->{...}.
                PrintWriter wf = eval.getInterp().getWriter();
                var format = new String ((char[]) a[0]);
                var list = new ArrayList<Object> ();
                if (a[1] != null) {
                    for (var e: (Cell) a[1]) {
                        if (e instanceof char[])
                            e = new String ((char[]) e);
                        list.add(e);
                    }
                }
                var args = list.toArray();
                wf.format(format, args);
                return null;
            });
    }
}
```

You can compile the code and use it as follows:

```
$ javac -cp arc.jar PrintfExample.java
$ java -jar arc.jar
arc> (java-def "PrintfExample")
#<printf:-2>
arc> (help printf)
(printf FORMAT ...) writes args in FORMAT.
#<printf:-2>
arc> (printf "<<%08X>>\n" 123456789)
<<075BCD15>>
nil
arc>
```
