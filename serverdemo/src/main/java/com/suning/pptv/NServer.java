package com.suning.pptv;

import java.io.IOException;
import java.nio.channels.Selector;

/**
 * Created by kewangk on 2017/10/28.
 */
public class NServer {

    //这里加volatile也没用，自行百度volatile用法详解
    //public volatile Selector selector=null;

    public static void main(String[] args){
        Selector selector=null;
        try {
            selector=Selector.open();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        }
        ReadWorker readWorker=new ReadWorker(selector);
        WriteWorker writeWorker=new WriteWorker();
        Dispatcher dispatcher=new Dispatcher(selector,readWorker,writeWorker);
        Acceptor acceptor=new Acceptor(selector);
        new Thread(acceptor,"acceptor").start();
        new Thread(dispatcher,"dispatcher").start();
        new Thread(readWorker,"readWorker").start();
        new Thread(writeWorker,"writeWorker").start();

    }

}
