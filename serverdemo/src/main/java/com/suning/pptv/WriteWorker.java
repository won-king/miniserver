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
public class WriteWorker implements Runnable {

    private volatile boolean started;

    private BlockingQueue<SelectionKey> taskQueue;

    public WriteWorker(){
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

    public void handleOutput(SelectionKey key) throws IOException{
        if(key.isValid()){
            SocketChannel channel= (SocketChannel) key.channel();
            Request request= (Request) key.attachment();
            Response response=new Response(request);
            /*ByteBuffer buffer=ByteBuffer.allocate(1024);
            buffer.clear();
            buffer.put("HTTP/1.1 200 OK\r\n".getBytes());
            buffer.put("Content-Type: text/html;charset=UTF-8\r\n".getBytes());
            buffer.put(("Content-Length: "+"Hello visitor!".length()+"\r\n").getBytes());
            buffer.put("\r\n".getBytes());
            buffer.put("Hello visitor!".getBytes());
            buffer.flip();
            byte[] bytes=new byte[buffer.remaining()];
            buffer.get(bytes);*/
            channel.write(ByteBuffer.wrap(response.doResponse()));
            channel.close();
            key.cancel();
            System.out.println("-----client gone-----");
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
                handleOutput(key);
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
