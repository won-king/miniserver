package com.suning.pptv;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by kewangk on 2017/10/24.
 */
public class ClientThread {

    public static void main(String[] args){
        ExecutorService executor= Executors.newCachedThreadPool();
        List<Task> taskList=new ArrayList<Task>(10);
        for(int i=0;i<1;++i){
            Task task=new Task();
            taskList.add(task);
            executor.execute(task);
        }
        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for(int j=0;j<1;++j){
            taskList.get(j).cancle();
        }
        executor.shutdown();
    }

}

class Task implements Runnable{

    private Socket socket;
    private volatile boolean running;

    public Task(){
        running=true;
    }

    private static int readOnce(InputStream is, byte[] bytes) throws IOException{
        int i;
        try{
            i=is.read(bytes);
        }catch(IOException e){
            e.printStackTrace();
            i=-1;
        }
        return i;
        /*System.out.println("total length->"+i);
        for(int j=0;j<i;++j){
            sb.append((char)bytes[j]);
        }
        System.out.println(sb.toString());*/
    }

    public void cancle(){
        this.running=false;
    }

    public void run() {
        synchronized (Task.class){
            while (running){
                if(socket==null || socket.isClosed()){
                    try {
                        System.out.println("connect");
                        socket=new Socket("127.0.0.1",8080);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    System.out.println("close");
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    TimeUnit.MICROSECONDS.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("finish");

            /*while (running){
                OutputStream os=null;
                InputStream is=null;
                try {
                    os=socket.getOutputStream();
                    os.write("GET /hello?name=wangke HTTP/1.1\r\n".getBytes());
                    os.write("Host: localhost:8080\r\n".getBytes());
                    os.write("Connection: keep-alive\r\n".getBytes());
                    os.write("Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng;q=0.8\r\n".getBytes());
                    os.write("Accept-Encoding: gzip, deflate, br\r\n".getBytes());
                    os.write("Accept-Language: zh-CN,zh;q=0.8\r\n".getBytes());
                    os.write("\r\n".getBytes());
                    is=socket.getInputStream();
                    byte[] bytes=new byte[1024];
                    int i=readOnce(is, bytes);
                    System.out.print(i);
                } catch (IOException e) {
                    e.printStackTrace();
                    //System.out.println("write exception");
                } finally {
                    if(os!=null){
                        try {
                            os.close();
                        } catch (IOException e) {
                            System.out.println("close os exception");
                            //e.printStackTrace();
                        }
                    }
                    if(is!=null){
                        try {
                            is.close();
                        } catch (IOException e) {
                            System.out.println("close socket exception");
                            //e.printStackTrace();
                        }
                    }
                    if(socket!=null){
                        try {
                            socket.close();
                        } catch (IOException e) {
                            System.out.println("close socket exception");
                            //e.printStackTrace();
                        }
                    }
                }
            }*/

        }
    }
}
