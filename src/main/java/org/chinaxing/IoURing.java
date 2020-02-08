package org.chinaxing;

import org.chinaxing.exception.PrepareRWException;
import org.chinaxing.exception.SubmitException;
import org.chinaxing.exception.WaitCQEException;
import sun.misc.Unsafe;
import sun.nio.ch.DirectBuffer;

import java.io.FileDescriptor;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
    
    public static class IOResult {
    	public final long reqId;
    	public final long res;
	
		public IOResult(long reqId, long res) {
			this.reqId = reqId;
			this.res = res;
		}
	}
    
    public static class ReadIOCtx {
    	public final long reqId;
    	// user supplied read dest byte[]
    	public final byte[] outputBytes;
    	public final int outputOffset;
		// underline directBuffer io_uring used
    	public final ByteBuffer directBuffer;
	
		public ReadIOCtx(long reqId, byte[] buf, int offset, ByteBuffer dBuf) {
			this.reqId = reqId;
			this.outputBytes = buf;
			this.outputOffset = offset;
			this.directBuffer = dBuf;
		}
	}
 
	private final AtomicLong requestID = new AtomicLong(0);
	private final Map<Long, ReadIOCtx> readIOCtxMap = new ConcurrentHashMap<>();
    
    public IoURing(int queueDepth, int flags) {
        this._native = new IoURingNative(queueDepth, flags);
        this._native.init();
    }
    
    public void shutdown() {
        this._native.exit();
        readIOCtxMap.clear();
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

	public long prepareRead(FileDescriptor fd, long offset, byte[] bytes, int bufPos, int len) {
		int fd0 = getFd(fd);
		long reqId = requestID.incrementAndGet();
		ByteBuffer buf = ByteBuffer.allocateDirect(len);
		DirectBuffer dBuf = (DirectBuffer) buf;
		int ret = _native.prepareRead(reqId, 0, fd0, dBuf.address(), len, offset);
		if (ret != 0) {
			dBuf.cleaner().clean();
			throw new PrepareRWException("ret: " + ret);
		}
		readIOCtxMap.put(reqId, new ReadIOCtx(reqId, bytes, bufPos, buf));
		return reqId;
	}
	
	public long prepareWrite(FileDescriptor fd, long offset, byte[] bytes, int bufPos, int len) {
		int fd0 = getFd(fd);
		long reqId = requestID.incrementAndGet();
		ByteBuffer buf = ByteBuffer.allocateDirect(len);
		buf.put(bytes, bufPos, len);
		int ret = _native.prepareWrite(reqId, 0, fd0, ((DirectBuffer) buf).address(), len, offset);
		if(ret != 0) {
			((DirectBuffer)buf).cleaner().clean();
			throw new PrepareRWException("ret: " + ret);
		}
		return reqId;
	}
	
	
	public void submit() {
     	int ret = _native.submit();
     	if(ret != 0) {
     		throw new SubmitException("ret: " + ret);
		}
     }
     
     public IOResult waitCQEntry() {
		 IOResult result = waitCQEntries(1);
		 postProcessRead(result);
		 return result;
     }
	
	public IOResult waitCQEntries(int nr) {
		long[] reqIds = new long[1], retCodes = new long[1];
		int ret = _native.waitCQEntries(reqIds, retCodes, nr);
		if(ret != 0) {
			throw new WaitCQEException("ret: " + ret);
		}
		IOResult result = new IOResult(reqIds[0], retCodes[0]);
		postProcessRead(result);
		return result;
	}
     
     public void seenCQEntry(int n) {
     	_native.advanceCQ(n);
     }
	
	public IOResult[] peekCQEntries(int nr) {
		long[] reqIds = new long[nr], retCodes = new long[nr];
		int cnt = _native.peekCQEntries(reqIds, retCodes, nr);
		IOResult[] result = new IOResult[cnt];
		for (int i = 0; i < cnt; i++) {
			result[i] = new IOResult(reqIds[i], retCodes[i]);
			postProcessRead(result[i]);
		}
		return result;
	}
     
     public IOResult waitCQEntryTimeout(long ms) {
     	long[] reqIds = new long[1], retCodes = new long[1];
     	int ret = _native.waitCQEntryTimeout(reqIds, retCodes, ms);
		 if(ret != 0) {
			 throw new WaitCQEException("ret: " + ret);
		 }
		 IOResult result = new IOResult(reqIds[0], retCodes[0]);
		 postProcessRead(result);
		 return result;
     }
     
     private void postProcessRead(IOResult ioResult) {
		 ReadIOCtx ctx = readIOCtxMap.remove(ioResult.reqId);
		 if(ctx != null) {
		 	if(ioResult.res > 0) {
				ctx.directBuffer.get(ctx.outputBytes, ctx.outputOffset, (int)ioResult.res);
			}
			 ((DirectBuffer)ctx.directBuffer).cleaner().clean();
		 }
     }
}
