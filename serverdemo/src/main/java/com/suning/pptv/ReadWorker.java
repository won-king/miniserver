package com.suning.pptv;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 * Created by kewangk on 2017/10/28.
 */
public class ReadWorker implements Runnable {

    private static final int DEFAULT_SIZE=1024;

    private ByteBuffer byteBuffer;

    private BlockingQueue<SelectionKey> taskQueue;

    private volatile boolean started;

    //这里一开始考虑的是ReadWorder线程持有Dispatcher线程，他完成read操作后通过dispatcher回调通知来注册接下来的OP_WRITE
    //但是这样一来他们相互持有，这就造成了先有鸡还是先有蛋的死循环问题
    //索性就让他们共同持有selector，直接对selector操作
    private volatile Selector selector;

    public ReadWorker(int size){
        if(size<1){
            byteBuffer=ByteBuffer.allocate(DEFAULT_SIZE);
        }else {
            byteBuffer=ByteBuffer.allocate(size);
        }
        taskQueue=new LinkedBlockingDeque<SelectionKey>();
        started=true;
    }

    public ReadWorker(Selector selector){
        this.selector=selector;
        byteBuffer=ByteBuffer.allocate(DEFAULT_SIZE);
        taskQueue=new LinkedBlockingDeque<SelectionKey>();
        started=true;
    }

    public void addTask(SelectionKey key){
        try {
            taskQueue.put(key);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void handleInput(SelectionKey key) throws IOException{
        if(key.isValid()){
            SocketChannel channel= (SocketChannel) key.channel();
            byteBuffer.clear();
            int readSize=channel.read(byteBuffer);
            Request request=null;
            while(readSize>1){
                //每read完一次，就要重置指针，准备好下一次write
                byteBuffer.flip();
                byte[] bytes=new byte[byteBuffer.remaining()];
                byteBuffer.get(bytes);
                String s=new String(bytes,"UTF-8");
                System.out.print(s);
                request=new Request();
                request.parseRequest(s);
                System.out.println(request);
                //同样，每次write完之后，指针归零，准备好下一次read
                byteBuffer.clear();
                readSize=channel.read(byteBuffer);
            }
            if(readSize==0){
                System.out.println("read zero byte");
                //因为selector是阻塞式的，所以register完后必须调用wakeup唤醒selector立即开始选择
                //selector.wakeup();
                channel.register(selector, SelectionKey.OP_WRITE);
                System.out.println("registered write");
                key.attach(request);
            }
            if(readSize==-1){
                channel.close();
                key.cancel();
            }
        }
    }

    public void stop(){
        this.started=false;
    }

    public void run() {
        while (started){
            SelectionKey key=null;
            try {
                key=taskQueue.take();
                handleInput(key);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
                if(key!=null){
                    key.cancel();
                    try {
                        key.channel().close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }
    }
}
