package org.chinaxing;

import org.junit.Test;
import org.junit.Assert;

import java.io.File;
import java.io.FileDescriptor;
import java.io.RandomAccessFile;
import java.util.concurrent.CompletableFuture;
import java.io.IOException;

public class AsyncIOTest {
	@Test
	public void copy() throws Exception {
                String srcFile = "/tmp/a.txt";
                String dstFile = "/tmp/b.txt";
                prepdata(srcFile, 1024);
                prepdata(dstFile, 0);
		File src = new File(srcFile);
		File dst = new File(dstFile);

		RandomAccessFile src0 = new RandomAccessFile(src, "r");
		RandomAccessFile dst0 = new RandomAccessFile(dst, "rw");

		FileDescriptor srcFd = src0.getFD();
		FileDescriptor dstFd = dst0.getFD();

		AsyncIO aio = new AsyncIO("test-cp", 20, 0);
		byte[] buf = new byte[1024];
		int offset = 0;
		while (true) {
                    CompletableFuture<Long> f = aio.prepareReads(srcFd, offset, new byte[][]{buf}, new int[]{0}, new int[]{1024});
			aio.submit();
			Long res = f.get();
                        System.out.println("R: " + res);
			if(res == 0) {
				break;
			}
			f = aio.prepareWrites(dstFd, offset, new byte[][]{buf}, new int[]{0}, new int[]{(int)res.intValue()});
			aio.submit();
			f.get();
                        System.out.println("W: " + res);
			offset += res.intValue();
		}
		dst0.close();
		src0.close();
		aio.shutdown();
                Assert.assertTrue(fileContentEquals(srcFile, dstFile));
	}
    @Test
    public void readFully() throws Exception {
        int n = 2048;
        prepdata("/tmp/c.txt", n); // 1000 * 11
        File src = new File("/tmp/c.txt");
        RandomAccessFile src0 = new RandomAccessFile(src, "r");
        AsyncIO aio = new AsyncIO("test-cp", 20, 0);
        int sz = n * 11;
        byte[] buf = new byte[sz];
        long s = readFully(aio, buf, 0, sz, src0, 0).get();
        Assert.assertEquals(s, sz);
        src0.close();
    }

    public CompletableFuture<Long> readFully(AsyncIO aio, byte[] out, int pos, int len, RandomAccessFile file, long offset) throws IOException {
        CompletableFuture<Long> f = new CompletableFuture<>();
        read(aio, out, pos, len, file, offset).whenComplete((r ,e) -> {
                if (e != null) {
                    System.out.println("Read ERROR : " + len + " " + offset  + e.getMessage());
                    f.completeExceptionally(e);
                } else {
                    System.out.println("Read OK : " + len + " " + offset + " " + r);
                    if(r.intValue() == len || r.intValue() == 0) {
                        f.complete(r);
                    } else {
                        try {
                            readFully(aio, out, pos + r.intValue(), len - r.intValue(), file, offset + r.intValue())
                                .whenComplete((r2, e2) -> {
                                        if(e2 != null) {
                                            f.completeExceptionally(e2);
                                        } else {
                                            f.complete(r + r2);
                                        }
                                    });
                        }catch (IOException e1) {
                            System.out.println("ReadFully ex :" + e1.getMessage());
                            f.completeExceptionally(e1);
                        }
                    }
                }
            });
        return f;
    }
    public CompletableFuture<Long> read(AsyncIO aio, byte[] out, int pos, int len, RandomAccessFile file, long offset) throws IOException {
        CompletableFuture<Long> c = aio.prepareRead(file.getFD(), offset, out, pos, len);
        aio.submit();
        return c;
    }

    private static void prepdata(String file, int round) throws Exception {
        String cmd = "echo -n > " + file + ";" +
            "for((i=0; i<" + round + "; i++)) ; do " +
               "echo helloworld >> " + file +
            "; done";
        System.out.println("prepare data CMD : " + cmd);
        Process p = Runtime.getRuntime().exec(new String[]{"bash", "-c", cmd});
        p.waitFor();
    }

    private static boolean fileContentEquals(String file, String file2) throws Exception {
        return 0 == Runtime.getRuntime().exec(new String[]{"diff", file, file2}).waitFor();
    }
}
