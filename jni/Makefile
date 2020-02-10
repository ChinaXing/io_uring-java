JAVA_HOME=/opt/taobao/java
CC = gcc
FLAGS = -std=gnu99 -D_REENTRANT -Wl,-export-dynamic
INCLUDE = -I . -I ${JAVA_HOME}/include -I ${JAVA_HOME}/include/linux/

all: iouringjni.so

ioUring.o: ioURing.c
	$(CC) $(FLAGS) $(INCLUDE) -c -fpic $< -o $@

iouringjni.so: ioUring.o
	$(info LIBURING_DYNAMIC_LINK is ${LIBURING_DYNAMIC_LINK})
ifeq ($(LIBURING_DYNAMIC_LINK),1)
	$(CC) -shared -luring -o $@ $<
else
	$(CC) -shared -l:liburing.a -o $@ $<
endif

dynamic	: export LIBURING_DYNAMIC_LINK=1
dynamic	: iouringjni.so

.PHONY : clean
clean:
	rm -f ioUring.o iouringjni.so
