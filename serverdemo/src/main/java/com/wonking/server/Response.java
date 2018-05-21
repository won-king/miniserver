package com.wonking.server;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by kewangk on 2017/10/30.
 */
public class Response {
    private MethodRepository methods;
    private Map<String,String> params;
    private Request request;

    public Response(Request request){
        methods=new MethodRepository();
        params=new HashMap<>();
        this.request=request;
    }

    public String dealWith(){
        parseParam();
        if(params.containsKey("name")){
            return methods.sayHello(params.get("name"));
        }
        if(params.containsKey("a") && params.containsKey("b") && params.containsKey("op")){
            int res=methods.calculate(Integer.parseInt(params.get("a")), Integer.parseInt(params.get("b")), params.get("op"));
            return String.valueOf(res);
        }
        return null;
    }

    public byte[] doResponse(){
        String data=dealWith();
        if(data==null){
            return write404();
        }
        return writeOK(data);
    }

    private byte[] writeOK(String data){
        ByteBuffer buffer=ByteBuffer.allocate(1024);
        buffer.put("HTTP/1.1 200 OK\r\n".getBytes());
        buffer.put("Content-Type: text/html;charset=UTF-8\r\n".getBytes());
        buffer.put(("Content-Length: "+data.length()+"\r\n").getBytes());
        buffer.put("\r\n".getBytes());
        buffer.put(data.getBytes());
        buffer.flip();
        byte[] bytes=new byte[buffer.remaining()];
        buffer.get(bytes);
        return bytes;
    }

    private byte[] write404(){
        ByteBuffer buffer=ByteBuffer.allocate(1024);
        buffer.put("HTTP/1.1 404 NotFound\r\n".getBytes());
        buffer.put("Content-Type: text/html;charset=UTF-8\r\n".getBytes());
        buffer.put("\r\n".getBytes());
        buffer.flip();
        byte[] bytes=new byte[buffer.remaining()];
        buffer.get(bytes);
        return bytes;
    }

    private void parseParam(){
        String[] paramString=request.getRequestUrl().split("\\?");
        if(paramString.length<2){
            return;
        }
        String[] param=paramString[1].split("&");
        for(String s:param){
            String[] kv=s.split("=");
            if(kv.length!=2){
                continue;
            }
            params.put(kv[0],kv[1]);
        }
    }

    public String toString(){
        return params.toString();
    }
}
