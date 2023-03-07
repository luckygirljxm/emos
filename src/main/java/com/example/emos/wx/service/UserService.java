package com.example.emos.wx.service;


import com.example.emos.wx.db.pojo.TbUser;

import java.util.Set;

public interface UserService {
    //注册新用户
    public int registerUser(String registerCode,String code,String nickname,String photo);
    //查询用户的权限列表
    public Set<String> searchUserPermissions(int userId);

    //用户登录
    public Integer login(String code);

    //查询用户信息
    public TbUser searchById(int userId);

    //查询用户入职日期
    public String searchUserHiredate(int userId);
}
