JAVA_HOME ?= /opt/taobao/java
CC ?= clang
FLAGS = -D_REENTRANT
INCLUDE = -I . -I ${JAVA_HOME}/include -I ${JAVA_HOME}/include/linux/

all: liburingjni.so

install:
	cp liburingjni.so /usr/lib/

ioURing.o: ioURing.c
	$(CC) $(FLAGS) $(INCLUDE) -c -fpic $< -o $@

liburingjni.so: ioURing.o
	$(CC) -shared -fpic -luring -o $@ $<

.PHONY : clean
clean:
	rm -f ioUring.o liburingjni.so
