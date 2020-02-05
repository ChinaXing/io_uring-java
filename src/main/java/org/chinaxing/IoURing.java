package org.chinaxing;

/**
 * Interface of IoURing
 *
 * simply expose liburing api to java
 *
 * this is a low level and raw interface
 */
public class IoURing {
	static {
		System.loadLibrary("Lib-io-uring-jni");
	}
	private long _IoURingPointer;
	// -------------- INITIALIZE ------------------
	// int io_uring_queue_init(unsigned entries, struct io_uring *ring, unsigned flags)
	// zero on success
	//void io_uring_queue_exit(struct io_uring *ring)
	//struct io_uring_sqe *io_uring_get_sqe(struct io_uring *ring)
	
	private final int queueDepth;
	private final long flags;
	/**
	 * Construct a IoURing instance
	 *
	 * IoURing underline entity will be create, and queue initialized
	 * @param queueDepth
	 * @param flags
	 */
	public IoURing(int queueDepth, long flags) {
		this.queueDepth = queueDepth;
		this.flags = flags;
	}
	private native void init0(int queueDepth, long flags);
	public void init() {
		init0(queueDepth, flags);
	}
	private native void exit0();
	public void exit() {
		exit0();
	}
	
	// -------------- FUNCTIONALITY ---------------
	public native long getSqe();
	//int io_uring_register_buffers(struct io_uring *ring, const struct iovec *iovecs,
	//                              unsigned nr_iovecs)
	public native int registerBuffers(long[] buffers, long[] bufLengths);
	// int io_uring_unregister_buffers(struct io_uring *ring)
	public native int unRegisterBuffers();
	//static inline void io_uring_prep_read(struct io_uring_sqe *sqe, int fd,
	//                                      void *buf, unsigned nbytes, off_t offset)
	public native void prepRead(long sqe, int fd, long buf, long size, long offset);
	//static inline void io_uring_prep_write(struct io_uring_sqe *sqe, int fd,
	//                                       void *buf, unsigned nbytes, off_t offset)
	public native void prepWrite(long sqe, int fd, long buf, long size, long offset);
	// static inline void io_uring_prep_read_fixed(struct io_uring_sqe *sqe, int fd,
	//                                            void *buf, unsigned nbytes,
	//                                            off_t offset, int buf_index)
	public native void prepReadFixed(long sqe, int fd, long buf, long size, long offset, int bufIndex);
	
	// TODO: more `liburing` api expose
}
