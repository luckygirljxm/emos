package com.example.emos.wx.service.impl;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.example.emos.wx.db.dao.TbUserDao;
import com.example.emos.wx.db.pojo.TbUser;
import com.example.emos.wx.exception.EmosException;
import com.example.emos.wx.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Set;

@Slf4j
@Service
@Scope("prototype")
public class UserServiceImpl implements UserService {
    @Autowired
    private TbUserDao userDao;

    @Value("${wx.app-id}")
    private String appId;

    @Value("${wx.app-secret}")
    private String appSecret;

    //获得openId
    private String getOpenId(String code) {
        String url = "https://api.weixin.qq.com/sns/jscode2session";
        HashMap<String, Object> map = new HashMap<>();
        map.put("appid", appId);
        map.put("secret", appSecret);
        map.put("js_code", code);
        map.put("grant_type", "authorization_code");
        //HTTPS调用
        String response = HttpUtil.post(url, map);
        //转回json
        JSONObject json = JSONUtil.parseObj(response);
        String openId = json.getStr("openid");
        //判断openId是否有效
        if (openId == null || openId.length() == 0) {
            throw new RuntimeException("临时登陆凭证错误");
        }
        return openId;
    }

    /**
     * 注册新用户
     *
     * @param registerCode 注册邀请码
     * @param code         临时授权字符串
     * @param nickname     昵称
     * @param photo        头像
     * @return
     */
    @Override
    public int registerUser(String registerCode, String code, String nickname, String photo) {
        //验证邀请码
        if (registerCode.equals("000000")) {
            //注册管理员，判断是否已经有了管理员
            boolean bool = userDao.haveRootUser();
            if (!bool) {
                //可以注册管理员
                String openId = getOpenId(code);
                HashMap<String, Object> param = new HashMap<>();
                param.put("openId", openId);
                param.put("nickname", nickname);
                param.put("photo", photo);
                param.put("role", "[0]");
                param.put("status", 1);
                param.put("createTime", new Date());
                param.put("root", true);
                //存入数据库
                userDao.insert(param);
                //查询插入
                int id = userDao.searchIdByOpenId(openId);
                return id;

            } else {
                //已经有管理员了，无法注册成管理员
                throw new EmosException("无法注册管理员");
            }
        }
        //TODO 其他员工注册
        else {

            return 0;
        }

    }

    @Override
    public Set<String> searchUserPermissions(int userId) {
        Set<String> permissions = userDao.searchUserPermissions(userId);
        return permissions;

    }

    /**
     *
     * @param code 临时授权字符串，用于得到用户的openId
     * @return
     */
    @Override
    public Integer login(String code) {
        //得到openId
        String openId = getOpenId(code);
        //根据openId查询id
        Integer id = userDao.searchIdByOpenId(openId);
        if (id == null){
            //用户不存在
            throw new EmosException("用户不存在");
        }
        //TODO 从消息队列中接收消息，转移到消息表
        return id;
    }

    @Override
    public TbUser searchById(int userId) {
        TbUser tbUser = userDao.searchById(userId);
        return tbUser;
    }

    @Override
    public String searchUserHiredate(int userId) {
        return userDao.searchUserHiredate(userId);
    }
}
