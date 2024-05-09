public class Tester2 {
    
    public static void main(String[] args) {

    int config = Integer.parseInt(args[0]);    
    int which = Integer.parseInt(args[1]);
    int numMilliseconds = Integer.parseInt(args[2]);
    int numWorkers = Integer.parseInt(args[3]);
    int numAddressesLog;
    int numTrainsLog;
    double meanTrainSize;
    double meanTrainsPerComm;
    int meanWindow;
    int meanCommsPerAddress;
    int meanWork;
    double configFraction;
    double pngFraction;
    double acceptingFraction;
    switch(config) {
        case 1:
        numAddressesLog = 11;
        numTrainsLog = 12;
        meanTrainSize = 5;
        meanTrainsPerComm = 1;
        meanWindow = 3;
        meanCommsPerAddress = 3;
        meanWork = 3822;
        configFraction = 0.24;
        pngFraction = 0.04;
        acceptingFraction = 0.96;
        break;
        case 2:
        numAddressesLog = 12;
        numTrainsLog = 10;
        meanTrainSize = 1;
        meanTrainsPerComm = 3;
        meanWindow = 3;
        meanCommsPerAddress = 1;
        meanWork = 2644;
        configFraction = 0.11;
        pngFraction = 0.09;
        acceptingFraction = 0.92;
        break;
        case 3:
        numAddressesLog = 12;
        numTrainsLog = 10;
        meanTrainSize = 4;
        meanTrainsPerComm = 3;
        meanWindow = 6;
        meanCommsPerAddress = 2;
        meanWork = 1304;
        configFraction = 0.10;
        pngFraction = 0.03;
        acceptingFraction = 0.90;
        break;
        case 4:
        numAddressesLog = 14;
        numTrainsLog = 10;
        meanTrainSize = 5;
        meanTrainsPerComm = 5;
        meanWindow = 6;
        meanCommsPerAddress = 2;
        meanWork = 315;
        configFraction = 0.08;
        pngFraction = 0.05;
        acceptingFraction = 0.90;
        break;
        case 5:
        numAddressesLog = 15;
        numTrainsLog = 14;
        meanTrainSize = 9;
        meanTrainsPerComm = 16;
        meanWindow = 7;
        meanCommsPerAddress = 10;
        meanWork = 4007;
        configFraction = 0.02;
        pngFraction = 0.10;
        acceptingFraction = 0.84;
        break;
        case 6:
        numAddressesLog = 15;
        numTrainsLog = 15;
        meanTrainSize = 9;
        meanTrainsPerComm = 10;
        meanWindow = 9;
        meanCommsPerAddress = 9;
        meanWork = 7125;
        configFraction = 0.01;
        pngFraction = 0.20;
        acceptingFraction = 0.77;
        break;
        case 7:
        numAddressesLog = 15;
        numTrainsLog = 15;
        meanTrainSize = 10;
        meanTrainsPerComm = 13;
        meanWindow = 8;
        meanCommsPerAddress = 10;
        meanWork = 5328;
        configFraction = 0.04;
        pngFraction = 0.18;
        acceptingFraction = 0.80;
        break;
        case 8:
        numAddressesLog = 16;
        numTrainsLog = 14;
        meanTrainSize = 15;
        meanTrainsPerComm = 12;
        meanWindow = 9;
        meanCommsPerAddress = 5;
        meanWork = 8840;
        configFraction = 0.04;
        pngFraction = 0.19;
        acceptingFraction = 0.76;
        break;
        default:
        throw new IllegalArgumentException("Invalid config number");
    }
    
        // Make some lamportQ's for my cache, R, PNG, betterDispatcher
        MultiQ[] dispatcherQs = new MultiQ[numWorkers];
        for (int i = 0; i < numWorkers; i++) {
          dispatcherQs[i] = new MultiQ();
        }
        MultiQ[] CacheQs = new MultiQ[numWorkers];
        for (int i = 0; i < numWorkers; i++) {
          CacheQs[i] = new MultiQ();
        }
        MultiQ[] PNGQs = new MultiQ[numWorkers];
        for (int i = 0; i < numWorkers; i++) {
          PNGQs[i] = new MultiQ();
        }
        MultiQ[] RQs = new MultiQ[numWorkers];
        for (int i = 0; i < numWorkers; i++) {
          RQs[i] = new MultiQ();
        }

        StopWatch timer = new StopWatch();
        //
        // allocate and initialize Lamport queues and hash tables (if tableType != -1)
        //
        PacketGenerator source = new PacketGenerator(numAddressesLog, numTrainsLog, meanTrainSize, meanTrainsPerComm,
                meanWindow, meanCommsPerAddress, meanWork, configFraction, pngFraction, acceptingFraction);
        // Dependong on tableType do something different
        AllData p = new AllData();
        Runnable[] runnables = new Runnable[numWorkers*4];
        PaddedPrimitiveNonVolatile<Boolean> done = new PaddedPrimitiveNonVolatile<Boolean>(false);
        for (int i = 0; i < dispatcherQs.length; ++i){
            runnables[i] = new BetterDispatcher(dispatcherQs[i], p, CacheQs, done);
        }
        for (int i = 0; i < CacheQs.length; i++) {
          runnables[i+dispatcherQs.length] = new CacheWorkers(CacheQs[i], done, p, PNGQs, RQs);
        }
        for (int i = 0; i < PNGQs.length; i++) {
          runnables[i+dispatcherQs.length+CacheQs.length] = new PNGWorkers(PNGQs[i], done, p, dispatcherQs[i]);
        }
        for (int i = 0; i < RQs.length; i++) {
          runnables[i+dispatcherQs.length+CacheQs.length + PNGQs.length] = new RWorkers(RQs[i], done, p, dispatcherQs[i]);
        }
        BetterMainDispatcher d= new BetterMainDispatcher(dispatcherQs, source, done);

        Thread[] threads = new Thread[numWorkers*4];
        for (int i = 0; i < runnables.length; i++) {
          Thread t = new Thread(runnables[i]);
          threads[i] = t;
          t.start();
        }
    
        // allocate and inialize Dispatcher and Worker threads
        //
        // call .start() on your Workers
        //
        timer.startTimer();
        //
        // call .start() on your Dispatcher
        //
        Thread dispatcherTrhead = new Thread(d);
        dispatcherTrhead.start();
        try {
          Thread.sleep(numMilliseconds);
        } catch (InterruptedException ignore) {;}
        done.value = true;
        try {
          for (int i = 0; i < numWorkers; i++) {
            threads[i].join();
          }
        } catch (InterruptedException ignore) {; }
        try {
          dispatcherTrhead.join();
        } catch (InterruptedException ignore) {;}
        // try {
        // //   Thread.sleep(numMilliseconds);
        // } catch (InterruptedException ignore) {;}
        timer.stopTimer();
        // report the total number of packets processed and total time
        int sum = 0;
        int idx = 0;
        for (Runnable r : runnables) {
            idx++;
           int oldsum = sum;
          if (r instanceof BetterDispatcher) {
            sum += ((BetterDispatcher) r).packetsProcessed; 
            System.out.println("enq into cache " + ((BetterDispatcher) r).cachesenq);
          } else if (r instanceof CacheWorkers) {
            sum += ((CacheWorkers) r).packetsProcessed;
          } else if (r instanceof PNGWorkers) {
            sum += ((PNGWorkers) r).packetsProcessed;
          } else if (r instanceof RWorkers) {
            sum += ((RWorkers) r).packetsProcessed;
          }
          System.out.println("Worker  " + idx + " processed " + (sum - oldsum) + " packets");
        }
        System.out.println("Total time was " + timer.getElapsedTime() + " ms" + "and processed " + sum + " packets");
      }
}
