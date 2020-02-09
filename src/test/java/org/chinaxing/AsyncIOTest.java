package org.chinaxing;

import org.junit.Test;

import java.io.File;
import java.io.FileDescriptor;
import java.io.RandomAccessFile;
import java.util.concurrent.CompletableFuture;

public class AsyncIOTest {
	@Test
	public void copy() throws Exception {
		System.out.println(System.getProperty("java.library.path"));
		File src = File.createTempFile("io-uring-cp", "src");
		File dst = File.createTempFile("io-uring-cp", "dst");
		
		RandomAccessFile src0 = new RandomAccessFile(src, "r");
		RandomAccessFile dst0 = new RandomAccessFile(dst, "w");
		
		FileDescriptor srcFd = src0.getFD();
		FileDescriptor dstFd = dst0.getFD();
		
		AsyncIO aio = new AsyncIO("test-cp", 20, 0);
		byte[] buf = new byte[1024];
		while (true) {
			CompletableFuture<Long> f = aio.prepareRead(srcFd, 0, buf, 0, 1024);
			aio.submit();
			Long res = f.get();
			if(res == 0) {
				break;
			}
			f = aio.prepareWrite(dstFd, 0, buf, 0, res.intValue());
			aio.submit();
			f.get();
		}
		dst0.close();
		src0.close();
	}
}