package com.wonking.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * Created by kewangk on 2017/10/24.
 */
public class Acceptor implements Runnable{
    private static final int PORT=8080;

    private ServerSocketChannel server;
    private volatile boolean running;
    private static Acceptor instance=null;
    private volatile Dispatcher dispatcher;
    private volatile Lock selectLock;
    private Condition condition;

    //时刻牢记Java的内存模型，这个类是个任务类，并且被多个线程共享
    private volatile Selector selector;

    public Acceptor(Dispatcher dispatcher){
        this.dispatcher=dispatcher;
        if(dispatcher.isRunning()){
            running=true;
        }
        if(instance==null){
            try {
                this.server=ServerSocketChannel.open();
                this.server.socket().bind(new InetSocketAddress(PORT));
                this.server.socket().setReuseAddress(true);
                this.server.configureBlocking(false);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        instance=this;
    }

    public Acceptor(Selector selector, Lock lock, Condition condition){
        this.selector=selector;
        this.selectLock=lock;
        this.condition=condition;
        if(instance==null){
            try {
                this.server=ServerSocketChannel.open();
                this.server.socket().bind(new InetSocketAddress(PORT));
                this.server.socket().setReuseAddress(true);
                this.server.configureBlocking(false);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        running=true;
        instance=this;
    }

    public void stop(){
        if(instance!=null){
            instance.running=false;
        }
    }

    public void run() {
        while (running){
            selectLock.lock();
            try {
                SocketChannel socket=server.accept();
                if(socket!=null){
                    System.out.println("-----client access-----");
                    socket.configureBlocking(false);
                    //因为dispatcher是一个线程，所以当这个线程在select的时候阻塞住了之后
                    //除非直接调用selector.wakeup()
                    //dispatcher.register(socket, SelectionKey.OP_READ);
                    //dispatcher.wakeup();

                    //selector.wakeup();
                    socket.register(selector, SelectionKey.OP_READ);
                    System.out.println("-----accept success-----");
                    condition.signal();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                selectLock.unlock();
            }

        }
        try {
            server.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("server stopped");
    }
}
