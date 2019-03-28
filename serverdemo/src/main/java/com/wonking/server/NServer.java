package com.wonking.server;

import io.netty.bootstrap.Bootstrap;

import java.io.IOException;
import java.nio.channels.Selector;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by kewangk on 2017/10/28.
 */
public class NServer {

    public static void main(String[] args){
        Selector selector=null;
        Lock selectLock=new ReentrantLock();
        Condition condition=selectLock.newCondition();
        try {
            selector=Selector.open();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        }
        ReadWorker readWorker=new ReadWorker(selector);
        WriteWorker writeWorker=new WriteWorker();
        Dispatcher dispatcher=new Dispatcher(selector,readWorker,writeWorker, selectLock, condition);
        Acceptor acceptor=new Acceptor(selector, selectLock, condition);
        new Thread(acceptor,"acceptor").start();
        new Thread(dispatcher,"dispatcher").start();
        new Thread(readWorker,"readWorker").start();
        new Thread(writeWorker,"writeWorker").start();

    }

}
