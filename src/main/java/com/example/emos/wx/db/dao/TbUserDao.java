package com.example.emos.wx.db.dao;

import com.example.emos.wx.db.pojo.TbUser;
import org.apache.ibatis.annotations.Mapper;

import java.util.HashMap;
import java.util.Set;

@Mapper
public interface TbUserDao {
    //判断是否为管理员
    public boolean haveRootUser();

    //插入用户信息
    public int insert(HashMap param);

    //根据openId获得用户id
    public Integer searchIdByOpenId(String openId);

    //根据userId获得用户权限列表
    public Set<String> searchUserPermissions(int userId);

    //查询用户信息
    public TbUser searchById(int userId);

    //查询员工姓名和部门名称  用于发送邮件给hr 让hr得知高风险地区情况情况
    public HashMap searchNameAndDept(int userId);

    //查询用户的入职日期
    //为什么这条查询语句的SQL不需进行类相转换，因为我们规定返回的数据类型是String  所以会自动帮我们转换
    public String searchUserHiredate(int UserId);

}
