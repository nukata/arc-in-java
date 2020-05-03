all:
	rm -f arc/*.class
	javac arc/Main.java
	jar cfm arc.jar arc/Manifest LICENSE arc/arc.arc arc/*.class

clean:
	rm -f arc/*.class *.class
	rm -rf doc

distclean: clean
	rm -f *~ arc/*~ arc.jar

doc:
	javadoc -package -d doc arc
