JAVA_HOME=/opt/taobao/java
CC = gcc
FLAGS = -std=gnu99 -D_REENTRANT -Wl,-export-dynamic
INCLUDE = -I . -I ${JAVA_HOME}/include -I ${JAVA_HOME}/include/linux/ 

all: iouringjni.so

ioUring.o: ioURing.c 
	$(CC) $(FLAGS) $(INCLUDE) -c -fpic $< -o $@

iouringjni.so: ioUring.o
ifeq ($(LIBURING_STATIC_LINK), 0)
	$(CC) -shared -luring -o $@ $<
else
	$(CC) -shared -l:liburing.a -o $@ $<
endif


.PHONY : clean
clean:
	rm -f ioUring.o iouringjni.so
