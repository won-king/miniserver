package com.suning.pptv;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * Created by kewangk on 2017/10/24.
 */
public class Acceptor implements Runnable{
    private static final int PORT=8080;

    private ServerSocketChannel server;
    private SocketAddress address;
    private volatile boolean running;
    private static Acceptor instance;

    private Acceptor(){
        running=true;
        instance=this;
    }

    private SocketChannel accept() throws IOException{
        while(running){
            return server.accept();
        }
        return null;
    }

    public static void start() throws IOException{
        if(instance==null){
            synchronized (Acceptor.class){
                if(instance==null){
                    instance=new Acceptor();
                }
            }
        }
        instance.server=ServerSocketChannel.open();
        instance.server.socket().bind(new InetSocketAddress(PORT));
        instance.server.socket().setReuseAddress(true);
        instance.server.configureBlocking(false);
    }

    public static void stop(){
        if(instance!=null){
            instance.running=false;
        }
    }

    public void run() {
        try {
            SocketChannel socket=accept();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
