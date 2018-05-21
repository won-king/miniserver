package com.wonking.server;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Created by kewangk on 2017/10/30.
 */
public class Request {
    private String method;
    private String requestUrl;
    private String httpVer;
    private Map<String,String> headers;
    private String body;

    public Request(){
        headers=new HashMap<>();
    }

    public String getMethod() {
        return method;
    }

    public String getRequestUrl() {
        return requestUrl;
    }

    public String getHttpVer() {
        return httpVer;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getBody() {
        return body;
    }

    public void parseRequest(String content){
        Scanner scanner=new Scanner(content);
        int flag=0;
        while (scanner.hasNextLine()){
            switch (flag){
                case 0:
                    parseRequestLine(scanner.nextLine());
                    flag=1;
                    break;
                case 1:
                    flag=parseHeader(scanner.nextLine())?2:flag;
                    break;
                default:
                    body=scanner.nextLine();
                    break;
            }
        }
    }

    private void parseRequestLine(String line){
        String[] strings=line.split(" ");
        method=strings[0];
        requestUrl=strings[1];
        httpVer=strings[2];
    }

    private boolean parseHeader(String line){
        if("".equals(line)){
            return true;
        }
        String[] strings=line.split(":");
        headers.put(strings[0], strings[1].trim());
        return false;
    }

    public String toString(){
        return method+"/"+requestUrl+"/"+httpVer+"/"+headers.toString()+"/"+body;
    }

}
