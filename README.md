io_uring-java
==============

java binding for : **_liburing_** (https://github.com/axboe/liburing)

for more information see : **_io_uring_**

Usage
-----

1. **AsyncIO interface**\
   this interface wrapper IoURing and provide an interface of Asynchronouse io.\
   use CompletableFuture as a async programming Object.
2. **IoURing interface**\
   this interface just provide very basical io_uring interface, it straightforwardly
   expose each primitive to java side.

TODO
----
1. expose more io_uring function.
   1. timeout
   2. network
   3. file open/fallocate/splice etc.
   4. sqe link : SQE_OP_LINK | SQE_OP_HARD_LINK
   5. iopoll : IORING_SET_IOPOLL
   6. sqpoll : SQE_OP_FIXED_FILE - when enable IORING_SET_SQPOLL auto set this flag to sqe
   7. init queue with params
   8. expose io_uring_params to java side
2. refine exception handle.
3. refine AsyncIO interface.
4. document.
5. more comphensive test.

License
-------

All software contained within this repo is MIT.
