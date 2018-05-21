package com.wonking.server;

/**
 * Created by kewangk on 2017/10/31.
 */
public class MethodRepository {
    public String sayHello(String name){
        return "Hello! "+name;
    }

    public int calculate(int a, int b, String op){
        switch (op){
            case "plus":
                return a+b;
            case "sub":
                return a-b;
            case "multiply":
                return a*b;
            case "divide":
                return b!=0?a/b:0;
            default:
                return -1;
        }
    }
}
