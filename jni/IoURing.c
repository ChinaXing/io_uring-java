#include <stdlib.h>
#include "liburing.h"
#include "ioURing.h"

#define GET_RING(env, self) (struct io_uring*)(*#env)->GetLongField(#env, #self, ring_fid);
#define GET_SQE(env, self, flags, reqId)                                \
    struct io_uring_sqe * sqe = io_uring_get_sqe(GET_RING(#env, #self)); \
    if(!sqe) return -1; \
    if (!#flags) io_uring_sqe_set_flags(sqe, (unsigned)#flags); \
    io_uring_sqe_set_data(sqe, (void*)reqId);

#define MSEC_TO_TS(ts, msec) \
    do {                                          \
        #ts->tv_sec = #msec / 1000;               \
        #ts->tv_nsec = (#msec % 1000) * 1000000;  \
    } while(0);

static struct iovec * build_iovecs(JNIEnv *env, jlongArray buffers, jlongArray lens, jsize cnt) {
    struct iovec* vecs = malloc(cnt * sizeof(struct iovec));
    if (vecs) {
        jlong* buffer_ptr= (*env)->GetLongArrayElements(env, buffers, NULL);
        jlong* len_ptr = (*env)->GetLongArrayElements(env, lens, NULL);
        for(int i =0 ; i < cnt; i++) {
            vecs[i].iov_base = buffer_ptr[i];
            vecs[i].iov_len = len_ptr[i];
        }
        (*env)->ReleaseLongArrayElements(env, buffers, buffer_ptr, JNI_ABORT);
        (*env)->ReleaseLongArrayElements(env, lens, len_ptr, JNI_ABORT);
    }
    return vecs;
}

jfieldID ring_fid;

JNIEXPORT void JNICALL Java_org_chinaxing_IOURingNative_initIDs(JNIEnv * env, jclass clz)
{
    ring_fid = (*env)->GetFieldID(env, clz, "_ring", "J");
}

JNIEXPORT jint JNICALL Java_org_chinaxing_IoURingNative_init0(JNIEnv * env, jobject self, jint entries, jlong flags)
{
    struct io_uring * ring = malloc(sizeof(io_uring));
    (*env)->SetLongField(env, self, ring_fid, (jlong)ring);
    return (jint)io_uring_queue_init(entries, ring, flags);
}

JNIEXPORT void JNICALL Java_org_chinaxing_IoURingNative_exit0(JNIEnv * env, jobject self)
{
    struct io_uring * ring = GET_RING(env, self);
    io_uring_queue_exit(ring);
    free(ring);
}



JNIEXPORT jint JNICALL Java_org_chinaxing_IoURingNative_registerBuffers(JNIEnv * env, jobject self, jlongArray buffers, jlongArray lens)
{
    jsize  cnt = (*env)->GetArrayLength(env, buffers);
    struct iovec * vecs = build_iovecs(env, buffers, lens, cnt);
    if(!vecs) {
        return -1;
    }
    return io_uring_register_buffers(GET_RING(env, self), iovecs);
}

JNIEXPORT jint JNICALL Java_org_chinaxing_IoURingNative_unregisterBuffers(JNIEnv * env, jobject self)
{
    return io_uring_unregister_buffers(GET_RING(env, self));
}

JNIEXPORT jint JNICALL Java_org_chinaxing_IoURingNative_registerFiles(JNIEnv * env, jobject self, jintArray fds)
{
    jsize cnt = (*env)->GetArrayLength(env, fds);
    int fds_[cnt];
    (*env)->GetLongArrayRegion(env, fds, 0, cnt, fd_);
    return io_uring_register_files(GET_RING(env, self), fd_, cnt)
}

JNIEXPORT jint JNICALL Java_org_chinaxing_IoURingNative_unregisterFiles(JNIEnv * env, jobject self)
{
    return io_uring_unregister_files(GET_RING(env, self));
}

JNIEXPORT void JNICALL Java_org_chinaxing_IoURingNative_prepareRead(JNIEnv * env, jobject self, jlong reqIds, jlong flags, jint fd, jlong bufptr, jint len, jlong offset)
{
    GET_SQE(env, self, flags, reqId);
    io_uring_prep_read(sqe, (int)fd, (const void*) bufptr, (unsigned)len, (off_t)offset);
}

JNIEXPORT void JNICALL Java_org_chinaxing_IoURingNative_prepareReads(JNIEnv * env, jobject self, jlong reqIds, jlong flags, jint fd, jlongArray bufptrs, jintArray lens, jlong offset)
{
    GET_SQE(env, self, flags, reqId);
    jsize nr_vecs =  (*env)->GetArrayLength(env, bufptr);
    vecs = build_iovecs(env, bufptr, len, nr_vecs);
    io_uring_prep_readv(sqe, (int)fd, vecs, (unsigned)nr_vecs, (off_t)offset);
}

JNIEXPORT void JNICALL Java_org_chinaxing_IoURingNative_prepareReadFixed(JNIEnv * env, jobject self, jlong reqIds, jlong flags, jint fd, jlong bufptr, jint len, jlong offset, jint bufIndex)
{
    GET_SQE(env, self, flags, reqId);
    io_uring_prep_write_fixed(sqe, (int)fd, (const void*)bufptr, (unsigned)len, (off_t)offset, (int) bufIndex);
}

JNIEXPORT void JNICALL Java_org_chinaxing_IoURingNative_prepareWrite(JNIEnv * env, jobject self, jlong reqId, jlong flags, jint fd, jlong bufptr, jint len, jlong offset)
{
    GET_SQE(env, self, flags, reqId);
    io_uring_prep_write(sqe, (int)fd, (const void*) bufptr, (unsigned)len, (off_t)offset);
}

JNIEXPORT void JNICALL Java_org_chinaxing_IoURingNative_prepareWrites(JNIEnv * env, jobject self, jlong reqId, jlong flags, jint fd, jlongArray bufptr, jintArray len, jlong offset)
{
    GET_SQE(env, self, flags, reqId);
    jsize nr_vecs =  (*env)->GetArrayLength(env, bufptr);
    vecs = build_iovecs(env, bufptr, len, nr_vecs);
    io_uring_prep_writev(sqe, (int)fd, vecs, (unsigned)nr_vecs, (off_t)offset);
}

JNIEXPORT void JNICALL Java_org_chinaxing_IoURingNative_prepareWriteFixed(JNIEnv * env, jobject self, jlong reqId, jlong flags, jint fd, jlong bufptr, jint len, jlong offset, jint bufIndex)
{
    GET_SQE(env, self, flags, reqId);
    io_uring_prep_write_fixed(sqe, (int)fd, (const void*)bufptr, (unsigned)len, (off_t)offset, (int) bufIndex);
}

JNIEXPORT jint JNICALL Java_org_chinaxing_IoURingNative_prepareFsync(JNIEnv * env, jobject self, jlong reqId, jlong flags, jint fd, jlong fsyncFlags)
{
    GET_SQE(env, self, flags, reqId);
    io_uring_prep_fsync((struct io_uring_sqe *)sqe, (int)fd, (unsigned)fsyncFlags);
    return 0;
}

JNIEXPORT jint JNICALL Java_org_chinaxing_IoURingNative_submit(JNIEnv * env, jobject self)
{
    return io_uring_submit(GET_RING(env, self));
}

JNIEXPORT jint JNICALL Java_org_chinaxing_IoURingNative_submitAndWait(JNIEnv * env, jobject self, jint n)
{
    return io_uring_submit_and_wait(GET_RING(env, self), n);
}



/*
 * Class:     org_chinaxing_IoURingNative
 * Method:    waitCQEntryTimeout
 * Signature: ([J[JJ)I
 */
JNIEXPORT jint JNICALL Java_org_chinaxing_IoURingNative_waitCQEntryTimeout(JNIEnv * env, jobject self, jlongArray reqIds, jlongArray retCodes, long ms)
{
    __kernel_timespec ts;
    MSEC_TO_TS(ts, ms);
    struct io_uring_cqe * cqe_ptr;
    ret = io_uring_wait_cqe_timeout(GET_RING(env, self), &cqe_ptr, ts);
    if (!ret) {
        long reqIds_[1] = { (long)cqe_ptr->user_data };
        long res_[1] = { (long)cqe_ptr->res };
        (*env)->SetLongArrayRegion(env, reqIds, 0, 1, reqIds_);
        (*env)->SetLongArrayRegion(env, retCodes, 0, 1, res_);
    }
    return ret;
}

/*
 * class:     org_chinaxing_iouringnative
 * method:    waitcqentries
 * signature: ([j[ji)i
 */
jniexport jint jnicall java_org_chinaxing_iouringnative_waitcqentries(jnienv * env, jobject self, jlongarray reqids, jlongarray retcodes, jint n)
{
    struct io_uring_cqe * cqe_ptr[n];
    int cnt = io_uring_wait_cqes(get_ring(env, self), cqe_ptr, n);
    if (cnt) {
        long reqids_[cnt];
        long res_[cnt];
        for(int i=0; i<cnt; i++) {
            reqids_[i] = cqe_ptr[i]->user_data;
            res_[i] = cqe_ptr[i]->res;
        }
        (*env)->SetLongArrayRegion(env, reqIds, 0, cnt, reqIds_);
        (*env)->SetLongArrayRegion(env, retCodes, 0, cnt, res_);
    }
    return cnt;
}

/*
 * Class:     org_chinaxing_IoURingNative
 * Method:    peekCQEntries
 * Signature: ([J[JI)I
 */
JNIEXPORT jint JNICALL Java_org_chinaxing_IoURingNative_peekCQEntries(JNIEnv * env, jobject self, jlongArray reqIds, jlongArray retCodes, jint n)
{
    io_uring_cqe* cqe_ptr[n];
    int cnt = io_uring_peek_batch_cqe(GET_RING(env, self), cqe_ptr, n);
    if (cnt) {
        long reqIds_[cnt];
        long res_[cnt];
        for(int i=0; i<cnt; i++) {
            reqIds_[i] = cqe_ptr[i]->user_data;
            res_[i] = cqe_ptr[i]->res;
        }
        (*env)->SetLongArrayRegion(env, reqIds, 0, cnt, reqIds_);
        (*env)->SetLongArrayRegion(env, retCodes, 0, cnt, res_);
    }
    return cnt;
}

/*
 * Class:     org_chinaxing_IoURingNative
 * Method:    advanceCQ
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_org_chinaxing_IoURingNative_advanceCQ(JNIEnv * env, jobject self, jint n)
{
    struct io_uring * ring = GET_RING(env, self);
    io_uring_cq_advance(ring, n);
}
