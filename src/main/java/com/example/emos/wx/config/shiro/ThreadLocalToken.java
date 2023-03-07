package com.example.emos.wx.config.shiro;

import org.springframework.stereotype.Component;

@Component
public class ThreadLocalToken {
    //设置一个线程用来保存token
    private ThreadLocal local = new ThreadLocal();

    //存入token
    public void setToken(String token) {
        local.set(token);
    }

    //得到token
    public String getToken() {
        return (String) local.get();
    }



    //清除缓存
    public void clear() {
        local.remove();
    }
}
