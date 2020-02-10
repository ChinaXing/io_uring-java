#include <stdlib.h>
#include <errno.h>
#include "liburing.h"
#include "ioURing.h"

#define GET_RING(env, self) (struct io_uring*)(*env)->GetLongField(env, self, ring_fid)
#define GET_SQE(env, self, flags)                                      \
    struct io_uring_sqe * sqe = io_uring_get_sqe(GET_RING(env, self)); \
    if(!sqe) return -1;                                                \
    if (!flags) io_uring_sqe_set_flags(sqe, (unsigned)flags)

#define MSEC_TO_TS(ts, msec)                      \
    do {                                          \
        ts.tv_sec = msec / 1000;                  \
        ts.tv_nsec = (msec % 1000) * 1000000;     \
    } while(0)

#define SUPPORT_OP_CODE(op_code) (io_uring_probe_p && io_uring_opcode_supported((struct io_uring_probe *)io_uring_probe_p, op_code))

#define BUILD_IO_VEC_1(buf, len)                        \
    struct iovec* vecs = malloc(sizeof(struct iovec));  \
    if (!vecs)  return -errno;                          \
    vecs->iov_base = (void*)buf;                        \
    vecs->iov_len = (unsigned)len


static struct iovec * build_iovecs(JNIEnv *env, jlongArray buffers, jlongArray lens, jsize cnt) {
    struct iovec* vecs = malloc(cnt * sizeof(struct iovec));
    if (vecs) {
        jlong* buffer_ptr= (*env)->GetLongArrayElements(env, buffers, NULL);
        jlong* len_ptr = (*env)->GetLongArrayElements(env, lens, NULL);
        for(int i =0 ; i < cnt; i++) {
            vecs[i].iov_base = (void*)buffer_ptr[i];
            vecs[i].iov_len = (unsigned)len_ptr[i];
        }
        (*env)->ReleaseLongArrayElements(env, buffers, buffer_ptr, JNI_ABORT);
        (*env)->ReleaseLongArrayElements(env, lens, len_ptr, JNI_ABORT);
    }
    return vecs;
}

jfieldID ring_fid;
jlong io_uring_probe_p;

JNIEXPORT void JNICALL Java_org_chinaxing_IoURingNative_initIDs(JNIEnv * env, jclass clz)
{
    ring_fid = (*env)->GetFieldID(env, clz, "_ring", "J");
    io_uring_probe_p = (jlong)io_uring_get_probe();
}

JNIEXPORT jint JNICALL Java_org_chinaxing_IoURingNative_init0(JNIEnv * env, jobject self, jint entries, jlong flags)
{
    struct io_uring * ring = malloc(sizeof(struct io_uring));
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
    return io_uring_register_buffers(GET_RING(env, self), vecs, (unsigned)cnt);
}

JNIEXPORT jint JNICALL Java_org_chinaxing_IoURingNative_unregisterBuffers(JNIEnv * env, jobject self)
{
    return io_uring_unregister_buffers(GET_RING(env, self));
}

JNIEXPORT jint JNICALL Java_org_chinaxing_IoURingNative_registerFiles(JNIEnv * env, jobject self, jintArray fds)
{
    jsize cnt = (*env)->GetArrayLength(env, fds);
    jint fds_[cnt];
    (*env)->GetIntArrayRegion(env, fds, 0, cnt, fds_);
    return io_uring_register_files(GET_RING(env, self), (int*)fds_, cnt);
}

JNIEXPORT jint JNICALL Java_org_chinaxing_IoURingNative_unregisterFiles(JNIEnv * env, jobject self)
{
    return io_uring_unregister_files(GET_RING(env, self));
}

JNIEXPORT jint JNICALL Java_org_chinaxing_IoURingNative_prepareRead(JNIEnv * env, jobject self, jlong reqId, jlong flags, jint fd, jlong bufptr, jint len, jlong offset)
{
    GET_SQE(env, self, flags);
    if (SUPPORT_OP_CODE(IORING_OP_READ)) {
        io_uring_prep_read(sqe, (int)fd, (void*) bufptr, (unsigned)len, (off_t)offset);
    } else {
        // fallback to readv
        BUILD_IO_VEC_1(bufptr, len);
        io_uring_prep_readv(sqe, (int)fd, vecs, 1, (off_t)offset);
    }
    io_uring_sqe_set_data(sqe, (void*)reqId);
    return 0;
}

JNIEXPORT jint JNICALL Java_org_chinaxing_IoURingNative_prepareReads(JNIEnv * env, jobject self, jlong reqId, jlong flags, jint fd, jlongArray bufptrs, jintArray lens, jlong offset)
{
    GET_SQE(env, self, flags);
    jsize nr_vecs =  (*env)->GetArrayLength(env, bufptrs);
    struct iovec * vecs = build_iovecs(env, bufptrs, lens, nr_vecs);
    io_uring_prep_readv(sqe, (int)fd, vecs, (unsigned)nr_vecs, (off_t)offset);
    io_uring_sqe_set_data(sqe, (void*)reqId);
    return 0;
}

JNIEXPORT jint JNICALL Java_org_chinaxing_IoURingNative_prepareReadFixed(JNIEnv * env, jobject self, jlong reqId, jlong flags, jint fd, jlong bufptr, jint len, jlong offset, jint bufIndex)
{
    GET_SQE(env, self, flags);
    io_uring_prep_write_fixed(sqe, (int)fd, (void*)bufptr, (unsigned)len, (off_t)offset, (int) bufIndex);
    io_uring_sqe_set_data(sqe, (void*)reqId);
    return 0;
}

JNIEXPORT jint JNICALL Java_org_chinaxing_IoURingNative_prepareWrite(JNIEnv * env, jobject self, jlong reqId, jlong flags, jint fd, jlong bufptr, jint len, jlong offset)
{
    GET_SQE(env, self, flags);
    if (SUPPORT_OP_CODE(IORING_OP_WRITE)) {
        io_uring_prep_write(sqe, (int)fd, (void*) bufptr, (unsigned)len, (off_t)offset);
    } else {
        // fallback to readv
        BUILD_IO_VEC_1(bufptr, len);
        io_uring_prep_writev(sqe, (int)fd, vecs, 1, (off_t)offset);
    }
    io_uring_sqe_set_data(sqe, (void*)reqId);
    return 0;
}

JNIEXPORT jint JNICALL Java_org_chinaxing_IoURingNative_prepareWrites(JNIEnv * env, jobject self, jlong reqId, jlong flags, jint fd, jlongArray bufptr, jintArray len, jlong offset)
{
    GET_SQE(env, self, flags);
    jsize nr_vecs =  (*env)->GetArrayLength(env, bufptr);
    struct iovec * vecs = build_iovecs(env, bufptr, len, nr_vecs);
    io_uring_prep_writev(sqe, (int)fd, vecs, (unsigned)nr_vecs, (off_t)offset);
    io_uring_sqe_set_data(sqe, (void*)reqId);
    return 0;
}

JNIEXPORT jint JNICALL Java_org_chinaxing_IoURingNative_prepareWriteFixed(JNIEnv * env, jobject self, jlong reqId, jlong flags, jint fd, jlong bufptr, jint len, jlong offset, jint bufIndex)
{
    GET_SQE(env, self, flags);
    io_uring_prep_write_fixed(sqe, (int)fd, (void*)bufptr, (unsigned)len, (off_t)offset, (int) bufIndex);
    io_uring_sqe_set_data(sqe, (void*)reqId);
    return 0;
}

JNIEXPORT jint JNICALL Java_org_chinaxing_IoURingNative_prepareFsync(JNIEnv * env, jobject self, jlong reqId, jlong flags, jint fd, jlong fsyncFlags)
{
    GET_SQE(env, self, flags);
    io_uring_prep_fsync((struct io_uring_sqe *)sqe, (int)fd, (unsigned)fsyncFlags);
    io_uring_sqe_set_data(sqe, (void*)reqId);
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


JNIEXPORT jint JNICALL Java_org_chinaxing_IoURingNative_waitCQEntryTimeout(JNIEnv * env, jobject self, jlongArray reqIds, jlongArray retCodes, long ms)
{
    struct __kernel_timespec ts;
    MSEC_TO_TS(ts, ms);
    struct io_uring_cqe * cqe_ptr;
    int ret = io_uring_wait_cqe_timeout(GET_RING(env, self), &cqe_ptr, &ts);
    if (!ret) {
        long reqIds_[1] = { (long)cqe_ptr->user_data };
        long res_[1] = { (long)cqe_ptr->res };
        (*env)->SetLongArrayRegion(env, reqIds, 0, 1, reqIds_);
        (*env)->SetLongArrayRegion(env, retCodes, 0, 1, res_);
    }
    return ret;
}

JNIEXPORT jint JNICALL Java_org_chinaxing_IoURingNative_waitCQEntries(JNIEnv * env, jobject self, jlongArray reqIds, jlongArray retCodes, jint n)
{
    struct io_uring_cqe * cqe_ptr;
    int ret = io_uring_wait_cqe_nr(GET_RING(env, self), &cqe_ptr, n);
    if (!ret) {
        long reqIds_[1] = { (long)cqe_ptr->user_data };
        long res_[1] = { (long)cqe_ptr->res };
        (*env)->SetLongArrayRegion(env, reqIds, 0, 1, reqIds_);
        (*env)->SetLongArrayRegion(env, retCodes, 0, 1, res_);
    }
    return ret;
}

JNIEXPORT jint JNICALL Java_org_chinaxing_IoURingNative_peekCQEntries(JNIEnv * env, jobject self, jlongArray reqIds, jlongArray retCodes, jint n)
{
    struct io_uring_cqe* cqe_ptr[n];
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

JNIEXPORT void JNICALL Java_org_chinaxing_IoURingNative_advanceCQ(JNIEnv * env, jobject self, jint n)
{
    struct io_uring * ring = GET_RING(env, self);
    io_uring_cq_advance(ring, n);
}
