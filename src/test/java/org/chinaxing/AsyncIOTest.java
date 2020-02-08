package org.chinaxing;

import org.junit.Test;

import java.io.File;
import java.io.FileDescriptor;
import java.io.RandomAccessFile;

public class AsyncIOTest {
	@Test
	public void copy() throws Exception {
		File src = File.createTempFile("io-uring-cp", "src");
		File dst = File.createTempFile("io-uring-cp", "dst");
		
		RandomAccessFile src0 = new RandomAccessFile(src, "r");
		RandomAccessFile dst0 = new RandomAccessFile(dst, "w");
		
		FileDescriptor srcFd = src0.getFD();
		FileDescriptor dstFd = dst0.getFD();
		
		AsyncIO aio = new AsyncIO("test-cp", 20, 0);
		byte[] buf = new byte[1024];
		while (true) {
			Long res = aio.prepareRead(srcFd, 0, buf, 0, 1024).get();
			if(res == 0) {
				break;
			}
			aio.prepareWrite(dstFd, 0, buf, 0, res.intValue()).get();
		}
		dst0.close();
		src0.close();
	}
}