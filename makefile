JFLAGS= 
JC= javac
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = \
	               RandomGenerator.java \
       BetterPacketProcessor.java \
       PaddedPrimitiveNonVolatile.java \
	   SerialPacketProcessor.java \
	    Cache.java \
		ParallelHistogram.java \
	             SkipList.java \
				  Dispatcher.java \
	                    ParallelPacketProcessor.java \
	       StopWatch.java \
Fingerprint.java \
	                   QuadraticProbe.java \
	                T.java \
					 LamportQ.java \
	                      QuadraticTable.java \
	                Tester.java \
PacketGenerator.java \
	               Worker.java \
PacketProcessor.java \
	               SerialHistogram.java \
				   CacheProcessor.java \
				   PNGProcessor.java \
				   RProcessor.java  \
				   BetterDispatcher.java  \
				   BetterMainDispatcher.java  \
				   RWorkers.java \
				   PNGWorkers.java \
				   CacheWorkers.java \
				   MultiQ.java \
				   Tester2.java


default: classes

classes: $(CLASSES:.java=.class)

clean:
	$(RM) *.class
