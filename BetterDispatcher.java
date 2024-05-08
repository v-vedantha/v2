public class BetterDispatcher {
    // We want to check the cache manually, and then enq into the relevent workers
    // They will then output the results onto a stream, which we join at the very end, and then re-enq if needed
    
}
