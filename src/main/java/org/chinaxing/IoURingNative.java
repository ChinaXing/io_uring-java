package org.chinaxing;

/**
 * Interface of liburing
 *
 * simply expose liburing api to java
 *
 * this is a low level and raw interface
 */
public class IoURingNative {
	static {
		System.loadLibrary("Lib-io-uring-jni");
	}
	private long _this; // holder of pointer of io_uring instance

	private final int queueDepth;
	private final long flags;
	/**
	 * Construct a IoURing instance
	 *
	 * IoURing underline entity will be create, and queue initialized
	 * @param queueDepth
	 * @param flags
	 */
	public IoURingNative(int queueDepth, long flags) {
		this.queueDepth = queueDepth;
		this.flags = flags;
	}

    // int io_uring_queue_init(unsigned entries, struct io_uring *ring, unsigned flags)
	private native void init0(int queueDepth, long flags);
	public void init() {
		init0(queueDepth, flags);
	}
    //void io_uring_queue_exit(struct io_uring *ring)
	private native void exit0();
	public void exit() {
		exit0();
	}

    /** -------------- REGISTER -------------------- **/
    // extern int io_uring_register_buffers(struct io_uring *ring, const struct iovec *iovecs, unsigned nr_iovecs)
    // extern int io_uring_unregister_buffers(struct io_uring *ring)
    // extern int io_uring_register_files(struct io_uring *ring, const int *files, unsigned nr_files);
    // extern int io_uring_unregister_files(struct io_uring *ring);
    
    public native int registerBuffers(long[] buffers, long[] bufferLengths); // 0 success
    public native int unregisterBuffers(); // 0 success
    public native int registerFiles(int[] fds);
    public native int unregisterFiles();

    /**  ------------- PREPARE READ/WRITE ----------- **/
    // static inline void io_uring_prep_read(struct io_uring_sqe *sqe, int fd, void *buf, unsigned nbytes, off_t offset)
    // static inline void io_uring_prep_readv(struct io_uring_sqe *sqe, int fd, const struct iovec *iovecs, unsigned nr_vecs, off_t offset)
    // static inline void io_uring_prep_read_fixed(struct io_uring_sqe *sqe, int fd, void *buf, unsigned nbytes, off_t offset, int buf_index)
    
    // static inline void io_uring_prep_write(struct io_uring_sqe *sqe, int fd, void *buf, unsigned nbytes, off_t offset)
    // static inline void io_uring_prep_writev(struct io_uring_sqe *sqe, int fd, const struct iovec *iovecs, unsigned nr_vecs, off_t offset)
    // static inline void io_uring_prep_write_fixed(struct io_uring_sqe *sqe, int fd, const void *buf, unsigned nbytes, off_t offset, int buf_index)
    //
    // static inline void io_uring_prep_fsync(struct io_uring_sqe *sqe, int fd, unsigned fsync_flags)

	public native void prepareRead(long sqe, int fd, long buf, int bytes, long offset);
	public native void prepareReads(long sqe, int fd, long[] buf, int[] bytes, long offset);
	public native void prepareReadFixed(long sqe, int fd, long buf, int bytes, long offset, int bufIndex);
	
	public native void prepareWrite(long sqe, int fd, long buf, int bytes, long offset);
	public native void prepareWrites(long sqe, int fd, long[] buf, int[] bytes, long offset);
	public native void prepareWriteFixed(long sqe, int fd, long[] buf, int[] bytes, long offset);
	
	public native void prepareFsync(long sqe, int fd, long flags);
	
    /** ------------------------ REQUEST / RESPONSE ------------------ **/
    // extern struct io_uring_sqe *io_uring_get_sqe(struct io_uring *ring);
    // static inline void io_uring_sqe_set_flags(struct io_uring_sqe *sqe, unsigned flags)
    // static inline void io_uring_sqe_set_data(struct io_uring_sqe *sqe, void *data)
    //
    // static inline void *io_uring_cqe_get_data(struct io_uring_cqe *cqe)
    //
    // extern int io_uring_submit(struct io_uring *ring);
    // extern int io_uring_submit_and_wait(struct io_uring *ring, unsigned wait_nr);
    //
    // extern int io_uring_wait_cqe_timeout(struct io_uring *ring, struct io_uring_cqe **cqe_ptr, struct __kernel_timespec *ts);
    // static inline int io_uring_wait_cqe(struct io_uring *ring, struct io_uring_cqe **cqe_ptr)
    // static inline int io_uring_wait_cqe_nr(struct io_uring *ring, struct io_uring_cqe **cqe_ptr, unsigned wait_nr)
    //
    // unsigned io_uring_peek_batch_cqe(struct io_uring *ring, struct io_uring_cqe **cqes, unsigned count);
    // static inline int io_uring_peek_cqe(struct io_uring *ring, struct io_uring_cqe **cqe_ptr)
    //
    // static inline void io_uring_cqe_seen(struct io_uring *ring, struct io_uring_cqe *cqe)
    // static inline void io_uring_cq_advance(struct io_uring *ring, unsigned nr)
    
    public native long getSQEntry();
    public native void setSQEntryFlags(long sqe, long flags);
    public native void setSQEntryCookie(long sqe, long cookie);
    public native long getCQEntryCookie(long cqe);
    public native int submit();
    public native int submitAndWait(int waitNr);
    public native int waitCQEntryTimeout(long[] cqes, long millis);
    public native int waitCQEntry(long[] cqes);
    public native int waitCQEntries(long[] cqes, int waitNr);
    public native int peekCQEntries(long[] cqes, int count);
    public native int peekCQEntry(long[] cqes);
    public native void seenCQEntry(long cqe);
    public native void advanceCQ(int nr);
}
