io_uring-java
==============

java binding for : **_liburing_** (https://github.com/axboe/liburing)

for more information see : **_io_uring_**

Usage
-----

1. AsyncIO interface
   this interface wrapper IoURing and provide an interface of Asynchronouse io.
      use CompletableFuture as a programming Object.
      2. IoURing interface
        this interface just provide very basical io_uring interface, it straightforwardly
          expose each primitive to java side.

TODO
----
1. expose more io_uring function.
2. refine exception handle.
3. refine AsyncIO interface.

License
-------

All software contained within this repo is MIT.
