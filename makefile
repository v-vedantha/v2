JFLAGS= 
JC= javac
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = \
	PacketGenerator.java \
	RandomGenerator.java \
	Fingerprint.java


default: classes

classes: $(CLASSES:.java=.class)

clean:
	$(RM) *.class
