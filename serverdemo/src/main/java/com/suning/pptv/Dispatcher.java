package com.suning.pptv;

import java.io.IOException;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by kewangk on 2017/10/28.
 */
public class Dispatcher implements Runnable {
    private volatile Selector selector;
    private boolean started;
    private ReadWorker readWorker;
    private WriteWorker writeWorker;

    //打算把单个读写线程换成线程池，后来我发现问题远比我想象的复杂，就没有实现，这个我后面会继续下去，直到达到我想要的功能。
    private ExecutorService readerPool;
    private ExecutorService writerPool;

    public Dispatcher(){
        try {
            selector=Selector.open();
            readWorker=new ReadWorker(selector);
            writeWorker=new WriteWorker();
            started=true;
        } catch (IOException e) {
            e.printStackTrace();
            started=false;
        }
    }

    public Dispatcher(Selector selector){
        this.selector=selector;
        readWorker=new ReadWorker(selector);
        writeWorker=new WriteWorker();
        started=true;
    }

    public Dispatcher(Selector selector, ReadWorker reader, WriteWorker writer){
        this.selector=selector;
        this.readWorker=reader;
        this.writeWorker=writer;
        started=true;
    }

    public Dispatcher(ExecutorService readWorker, ExecutorService writeWorker){
        try {
            this.selector=Selector.open();
            started=true;
        } catch (IOException e) {
            e.printStackTrace();
            started=false;
        }
        this.readerPool=readWorker;
        this.writerPool=writeWorker;
    }

    public void run() {
        while (started){
            try {
                //selector.select(1000*3);
                //selector.selectNow();
                //这里即使阻塞住也没关系，当他没事的时候阻塞住正是我想要的效果，因为当有事来了会有人叫醒他
                selector.select();

                //JDK文档里有强调在遍历已选择键集的过程中可能抛出ConcurrentModificationException异常，所以需要进行适当的同步操作
                //不过我又分析了一下,因为我的selector只有dispatcher一个线程去维护，不存在多线程并发修改的问题，所以这里不用对selectedKey集合的遍历操作进行同步
                /*Lock lock=new ReentrantLock();
                try {
                    lock.tryLock(10, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }finally {
                    lock.unlock();
                }*/
                //Set<SelectionKey> keys=selector.keys();
                Set<SelectionKey> selectedKeys=selector.selectedKeys();
                //System.out.println("key size->"+keys.size()+"  selected keys->"+selectedKeys.size());

                Iterator<SelectionKey> iterator=selectedKeys.iterator();
                while (iterator.hasNext()){
                    SelectionKey key=iterator.next();
                    //remove操作把这个事件移除出去，下一次轮询的时候才会感知，不影响本次操作。
                    iterator.remove();

                    // 这是一个反面教材，key的有效性判断不应该这么早，因为操作都被分散到不同的线程中去了
                    // 你不能保证他在这个线程中执行的时候是有效的，在另一个线程中还是有效的
                    // 不过我判断的是无效条件，所以放这也无所谓。这里只是给大家一个建议，有效性判断不应该太早
                    // 应该在各自线程内部进行判断，而且应该适当同步
                    /*if(!key.isValid()){
                        continue;
                    }*/

                    try{
                        //这个函数才是dispatcher的真正作用，将选择出来的键对应的操作事件进行分发
                        dispatch(key);
                    }catch (Exception e){
                        e.printStackTrace();
                        if(key.isValid()){
                            key.channel().close();
                        }
                        key.cancel();
                    }
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("dispatcher stop");
        if(selector!=null){
            try {
                selector.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        selector=null;
    }

    public void dispatch(SelectionKey key) throws IOException{
        if(key.isValid()){
            //这里accept放到一个单独的acceptor线程中执行
            /*if(key.isAcceptable()){
                ServerSocketChannel ssc= (ServerSocketChannel) key.channel();
                SocketChannel sc=ssc.accept();
                sc.configureBlocking(false);
                sc.register(selector, SelectionKey.OP_READ);
            }*/
            if(key.isReadable()){
                //readWorker.handleInput(key);
                System.out.println("add read task");
                readWorker.addTask(key);
            }
            if(key.isWritable()){
                //writeWorker.handleOutput(key);
                writeWorker.addTask(key);
            }
        }
    }

    public boolean isRunning(){
        return this.started;
    }

    public void stop(){
        this.started=false;
    }

    public void register(SocketChannel channel, int selectionKey){
        if(this.started){
            try {
                // 原来这个wakeup是我用的姿势不对，因为register和select方法会锁很多对象，其中有一些重叠的对象，会造成死锁
                // 但是wakeup是无条件打断阻塞的，所以wakeup应该在register之前调用，而不是在register之后调用
                selector.wakeup();
                channel.register(selector, selectionKey);
                System.out.println("registered");
            } catch (ClosedChannelException e) {
                e.printStackTrace();
            }
        }
    }

    public void wakeup(){
        selector.wakeup();
    }

}
