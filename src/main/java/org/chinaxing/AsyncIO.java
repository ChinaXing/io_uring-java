package org.chinaxing;

import java.io.File;
import java.io.FileDescriptor;
import java.nio.channels.FileChannel;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

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
	}
	
	public CompletableFuture<Long> prepareRead(FileDescriptor fd, long offset, byte[] buf, int bufPos, int len) {
		long reqId = ring.prepareRead(fd, offset, buf, bufPos, len);
		CompletableFuture<Long> future = new CompletableFuture<>();
		ioRequestFutures.put(reqId, future);
		return future;
	}
	
	public void submit() {
		ring.submit();
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
				long[] reqIds = new long[1], retCodes = new long[1];
				ring.waitCQEntry(reqIds, retCodes);
				CompletableFuture<Long> future = ioRequestFutures.get(reqIds[0]);
				if(future != null) {
					future.complete(retCodes[0]);
				}
				ring.seenCQEntry(1);
			}
		}
	}
	
}
