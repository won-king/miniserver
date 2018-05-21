package com.wonking.server;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.concurrent.*;

/**
 * Created by kewangk on 2017/10/20.
 */
public class TestServerSocket {
    private static final int PORT=8080;

    private static ServerSocket serverSocket;
    private static InetAddress address;
    private static ExecutorService executor;

    public static void main(String[] args){
        try {
            //ServerSokcet有多个构造函数，new ServerSocket(PORT,50,inetAddress),第二个是等待的客户端最大连接数,是一个队列,默认是50
            //第三个是IP地址，如果传null就默认是localhost
            //不知道你们看出来没有，ServerSocket提供了这个可以绑定IP地址的构造方法，其实为黑客留下了后门
            //也就是说如果我把这个IP地址设置为其他机器的IP，那么我就可以侦听到发往那个机器的请求。并且配合不同的端口号，我能侦听所有到这个机器的连接
            //我在学校的时间写过一个类似wireshark的抓包程序，将网卡设置为混杂模式，就能侦听到局域网内所有的通信数据报文。
            //不过这个报文更加原始，要从MAC层对它进行层层剥离，通信协议是一层套一层的
            serverSocket=new ServerSocket(PORT);
            executor= Executors.newCachedThreadPool();
            int count=0;
            while(true){
                ++count;
                Socket socket=serverSocket.accept();
                System.out.println("-----visitor accept-----");
                System.out.println("visitor->"+count);
                Future future=executor.submit(new ClientHandler(socket));
                if(!future.isDone()){
                    try {
                        future.get(10, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        System.out.println("interrupted");
                    } catch (ExecutionException e) {
                        System.out.println("execute error");
                        e.printStackTrace();
                    } catch (TimeoutException e) {
                        System.out.println("time out");
                    } finally {
                        future.cancel(true);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class ClientHandler implements Callable{
    private Socket socket;

    public ClientHandler(Socket socket){
        this.socket=socket;
    }

    public Object call() throws Exception {
        //BufferedReader bis=new BufferedReader(new InputStreamReader(socket.getInputStream()));
        BufferedInputStream bis=new BufferedInputStream(socket.getInputStream());
        BufferedWriter bos=new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

        StringBuilder sb=new StringBuilder();
        byte[] bytes=new byte[1024];
        int i=readOnce(bis, bytes);

        /*ByteBuffer bb=ByteBuffer.wrap(bytes);
        Charset charset=Charset.forName("UTF-8");
        CharBuffer cb=charset.decode(bb);*/

        System.out.println(i);
        if(i==0 || i==-1){
            System.out.println("-----visitor gone-----");
            socket.close();
            return null;
        }

        /*ByteBuffer cb=readByByte(bis);
        System.out.print(new String(cb.array()));*/

        //System.out.println("client finished transport");
        //bis.close();
        bos.write("HTTP/1.1 200 OK\r\n");
        bos.write("Content-Type: text/html;charset=UTF-8\r\n");
        bos.write("Content-Length: "+"hello visitor!".length()+"\r\n");
        bos.write("\r\n");
        bos.write("hello visitor!");
        bos.flush();
        bos.close();
        socket.close();
        System.out.println("-----visitor gone-----");
        return null;
    }

    //一次性全读出来，返回读取的字节数
    private static int readOnce(InputStream is, byte[] bytes) throws Exception{
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

    //逐字节读取，读取出来直接转化为字符，可能会乱码
    private static CharBuffer readByChar(InputStream is) throws Exception{
        CharBuffer chars=CharBuffer.allocate(1024*2);
        int total=0;
        int i=-1;
        int flag=0;
        while ((i=is.read())!=-1){
            total++;
            if(i=='\r'){
                System.out.print(total);
            }else if(i=='\n'){
                System.out.print((char)i);
                if(total-flag==2){
                    break;
                }else {
                    flag=total;
                }
            }else{
                System.out.print((char)i);
            }
            chars.append((char)i);
        }
        return chars.subSequence(0,total);
    }

    //逐字节读取，读出来的是未编码的字节流
    //不要用这个方法，ByteBuffer没有截取子串的方法
    private static ByteBuffer readByByte(InputStream is) throws Exception{
        ByteBuffer bytes=ByteBuffer.allocate(1024*2);
        bytes.clear();
        int total=0;
        int i=-1;
        int flag=0;
        while ((i=is.read())!=-1){
            total++;
            if(i=='\r'){
                System.out.print(total);
            }else if(i=='\n'){
                System.out.print((char)i);
                if(total-flag==2){
                    int k=-1;
                    total++;
                    while ((k=is.read())!=-1){
                        if(k=='\n'){
                            break;
                        }else {
                            System.out.print((char)k);
                        }
                        bytes.put((byte)k);
                    }
                    break;
                }else {
                    flag=total;
                }

            }else{
                System.out.print((char)i);
            }
            bytes.put((byte)i);
        }
        ByteBuffer result=ByteBuffer.allocate(total);
        for(int j=0;j<total;++j){
            result.put(bytes.get(j));
        }
        return result;
    }
}
