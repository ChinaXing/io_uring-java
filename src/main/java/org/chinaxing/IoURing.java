package org.chinaxing;

/**
 * IoURing high level interface
 *
 */
public class IoURing {
    private final IoURingNative _native;
    public static class Flags {
        //#define IORING_SETUP_IOPOLL     (1U << 0)       /* io_context is polled */
        //#define IORING_SETUP_SQPOLL     (1U << 1)       /* SQ poll thread */
        //#define IORING_SETUP_SQ_AFF     (1U << 2)       /* sq_thread_cpu is valid */
        //#define IORING_SETUP_CQSIZE     (1U << 3)       /* app defines CQ size */
        //#define IORING_SETUP_CLAMP      (1U << 4)       /* clamp SQ/CQ ring sizes */
        //#define IORING_SETUP_ATTACH_WQ  (1U << 5)       /* attach to existing wq */
        public static final int IOPOLL = (1 << 0);
        public static final int SQPOLL = (1 << 1);
        public static final int SQ_AFF = (1 << 2);
        public static final int CQSIZE = (1 << 3);
        public static final int CLAMP  = (1 << 4);
        public static final int ATTACH_WQ = (1 << 5);
    }
    
    public IoURing(int queueDepth, int flags) {
        this._native = new IoURingNative(queueDepth, flags);
        this._native.init();
    }
    
    public void shutdown() {
        this._native.exit();
    }
    
    /**
     * high level wrappers
     *
     * READ/WRITE
     *
     * REQUEST/COMPLETION
     */
     
     
}
