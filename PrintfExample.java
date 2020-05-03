import java.io.PrintWriter;
import java.util.ArrayList;
import arc.*;

/** An example user-defined function of Arc written in Java.
 * 
 * <pre> 
 * {@code
 * $ javac -cp arc.jar PrintfExample.java
 * $ java -jar arc.jar
 * arc> (java-def "PrintfExample")
 * #<printf:-2>
 * arc> (help printf)
 * (printf FORMAT ...) writes args in FORMAT.
 * #<printf:-2>
 * arc> (printf "<<%08X>>\n" 123456789)
 * <<075BCD15>>
 * nil
 * arc> 
 * }
 * </pre>
 */
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
