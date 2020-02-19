package org.chinaxing;

import org.chinaxing.IoURing.IOResult;
import org.chinaxing.exception.IoURingException;

import java.io.FileDescriptor;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * capabilities:
 *
 * 1) submit io request and get a Future
 *    each Future associate a request-Id internally
 * 2) when sync with Future , trigger fetch io result
 * 3) future can be completed by internal Io-Completion-Reap EventLoop
 *
 * parameters like buffer requirement same as {@link IoURing}
 */
public class AsyncIO {
	// use Integer as Future result data Type, because most of io-result return int
	private final Map<Long, CompletableFuture<Long>> ioRequestFutures = new ConcurrentHashMap<>();
	private final IoURing ring;
	private final String name;

	public AsyncIO(String name, int queueDepth, int flags) {
		this.name = name;
		ring = new IoURing(queueDepth, flags);
		new IoCompletionLoopThread().start();
	}

	public CompletableFuture<Long> prepareRead(FileDescriptor fd, long offset, byte[] buf, int bufPos, int len) {
		long reqId = ring.prepareRead(fd, offset, buf, bufPos, len);
		CompletableFuture<Long> future = new CompletableFuture<>();
		ioRequestFutures.put(reqId, future);
		return future;
	}

	public CompletableFuture<Long> prepareReads(FileDescriptor fd, long offset, byte[][] buf, int[] bufPos, int[] len) {
		long reqId = ring.prepareReads(fd, offset, buf, bufPos, len);
		CompletableFuture<Long> future = new CompletableFuture<>();
		ioRequestFutures.put(reqId, future);
		return future;
	}

	public CompletableFuture<Long> prepareWrite(FileDescriptor fd, long offset, byte[] buf, int bufPos, int len) {
		long reqId = ring.prepareWrite(fd, offset, buf, bufPos, len);
		CompletableFuture<Long> future = new CompletableFuture<>();
		ioRequestFutures.put(reqId, future);
		return future;
	}

	public CompletableFuture<Long> prepareWrites(FileDescriptor fd, long offset, byte[][] buf, int[] bufPos, int[] len) {
		long reqId = ring.prepareWrites(fd, offset, buf, bufPos, len);
		CompletableFuture<Long> future = new CompletableFuture<>();
		ioRequestFutures.put(reqId, future);
		return future;
	}

    public void seen(int n) {
        ring.seenCQEntry(n);

    }
    public int submit() {
        return ring.submit();
    }

    public void shutdown() {
        ring.shutdown();
    }

    /**
     * This eventLoop Poll completion queue
     * and complete the waiting futures for each request
     */
    class IoCompletionLoopThread extends Thread {
        public IoCompletionLoopThread() {
            super();
			setName("Io-URing-Completion-Loop-`" + name + "`");
		}

		@Override
		public void run() {
			pollCQ();
		}

		private void pollCQ() {
			while (true) {
				try {
					IOResult result = ring.waitCQEntry();
					CompletableFuture<Long> future = ioRequestFutures.get(result.reqId);
					if (future != null) {
                                            future.complete(result.res);
					}
					ring.seenCQEntry(1);
				}catch (IoURingException e) {
					e.printStackTrace();
				}
			}
		}
	}

}
