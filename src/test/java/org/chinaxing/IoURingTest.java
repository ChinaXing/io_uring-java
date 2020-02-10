package org.chinaxing;

import org.chinaxing.IoURing.IOResult;
import org.junit.Test;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.RandomAccessFile;

public class IoURingTest {
	@Test
	public void copy() throws IOException {
		IoURing ring = new IoURing(20, 0);
		System.out.println(System.getProperty("java.library.path"));
//		File src = File.createTempFile("io-uring-cp", "src");
		File src = new File("/root/a.txt");
		File dst = new File("/root/b.txt");
//		File dst = File.createTempFile("io-uring-cp", "dst");

		RandomAccessFile src0 = new RandomAccessFile(src, "r");
		RandomAccessFile dst0 = new RandomAccessFile(dst, "rw");

		FileDescriptor srcFd = src0.getFD();
		FileDescriptor dstFd = dst0.getFD();
		byte[] buf = new byte[1024];
		long offset = 0;
		while (true) {
                    //ring.prepareReads(srcFd, offset, new byte[][]{buf}, new int[]{0}, new int[]{1024});
                    ring.prepareRead(srcFd, offset, buf, 0, 1024);
                    ring.submit();
                    IOResult r = ring.waitCQEntry();
                    System.out.println("R: " + r.reqId + " -> " + r.res);
                    ring.seenCQEntry(1);
                    if(r.res == 0) {
                        break;
                    }
                    //ring.prepareWrites(dstFd, offset, new byte[][]{buf}, new int[]{0}, new int[]{(int)r.res});
                    ring.prepareWrite(dstFd, offset, buf, 0, (int)r.res);
                    ring.submit();
                    r = ring.waitCQEntry();
                    System.out.println("W: " + r.reqId + " -> " + r.res);
                    ring.seenCQEntry(1);
                    offset += r.res;
		}
		src0.close();
		dst0.close();
		ring.shutdown();
	}
}
