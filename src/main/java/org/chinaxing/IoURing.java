package org.chinaxing;

import sun.misc.Unsafe;
import sun.nio.ch.DirectBuffer;

import java.io.FileDescriptor;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;

/**
 * IoURing high level interface
 *
 * capabilities:
 *
 * 0) setup and customize IOURing instance
 *    a) io-poll for (nvme + xfs)
 *    b) sq-poll     (reduce syscall)
 *
 * 1) submit read/write
 *    a) against heap byte[]
 *    b) against DirectByteBuffer
 * 2) wait request completion
 * 3) batch submit read/write requests
 *    a) multi-buffer per file
 *    b) multi-io
 * 4) wait batch completion
 *
 * 5) register DirectByteBuffer to use Fixed read/write ( for performance )
 * 6) register Files to use Fixed file ( for performance )
 *
 * 7) buffer conversion:
 *    1) read/write base on Heap bytes array
 *    2) read/write base on DirectByteBuffer
 *
 * <h2>Read/Write flow</h2>
 * 1) get Submission Queue entry will return the <b>request-Id</b>
 *    internally, we associate the SQE with a request-Id (cookie)
 * 2) prepare read/write/readv/writev against request-Id
 * 3) goto 1) until all read/write prepared
 * 4) submit prepared ios in the Submission Queue
 * 5) query or wait completion of submitted requests in the Queue.
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
	
	private final AtomicLong requestID = new AtomicLong(0);
    
    public IoURing(int queueDepth, int flags) {
        this._native = new IoURingNative(queueDepth, flags);
        this._native.init();
    }
    
    public void shutdown() {
        this._native.exit();
    }
    

    private static Unsafe unsafe;
	static {
		try {
			Field f = Unsafe.class.getDeclaredField("theUnsafe");
			f.setAccessible(true);
			unsafe = (Unsafe) f.get(null);
		}catch (NoSuchFieldException | IllegalAccessException t) {
			throw new RuntimeException(t);
		}
	}
	
	private static final long _FD_OFFSET;
	static {
		try {
			_FD_OFFSET = unsafe.objectFieldOffset(FileDescriptor.class.getField("fd"));
		}catch (NoSuchFieldException e) {
			throw new RuntimeException(e);
		}
	}
	
    
    private int getFd(FileDescriptor fd) {
    	return unsafe.getInt(fd, _FD_OFFSET);
    }
    
    /**
     * high level wrappers
     *
     * READ/WRITE
     *
     * REQUEST/COMPLETION
     */
     
     public long prepareRead(FileDescriptor fd, long offset, byte[] buf, int bufPos, int len) {
     	int fd0 = getFd(fd);
		 long reqId = requestID.incrementAndGet();
		 DirectBuffer dBuf = (DirectBuffer) ByteBuffer.allocateDirect(len);
		 int ret = _native.prepareRead(reqId, 0, fd0, dBuf.address(), len, offset);
		 if(ret != 0) {
		 	return ret;
		 }
     	return reqId;
     }
	
	/**
	 * submit prepared requests
	 *
	 * @throws CompletionQueueOverflowException when cq is full
	 */
	public void submit() {
     	int ret = _native.submit();
     	// TODO: handle submit failure, eg: completion Queue is overflow etc.
     }
     
     public void waitCQEntry(long[] reqIds, long[] retCodes) {
		 int ret = _native.waitCQEntries(reqIds, retCodes, 1);
		 // TODO: if ret != 0, throw exception
     }
     
     public void seenCQEntry(int n) {
     	_native.advanceCQ(n);
     }
     
     public void peekCQEntries(long[] reqIds, long[] retCodes, int nr) {
		 int ret = _native.peekCQEntries(reqIds, retCodes, nr);
		 // TODO: if ret != 0, throw exception
     }
}
