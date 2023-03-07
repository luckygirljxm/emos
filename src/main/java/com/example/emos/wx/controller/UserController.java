package com.example.emos.wx.controller;


import com.example.emos.wx.common.util.R;
import com.example.emos.wx.config.shiro.JwtUtil;
import com.example.emos.wx.controller.from.LoginForm;
import com.example.emos.wx.controller.from.RegisterForm;
import com.example.emos.wx.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/user")
@Api("用户模块Web接口")
public class UserController {
    @Autowired
    private UserService userService;

    @Resource
    private RedisTemplate redisTemplate;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${emos.jwt.cache-expire}")
    private int cacheExpire;

    @PostMapping("/register")
    @ApiOperation("注册用户")
    public R register(@Valid @RequestBody RegisterForm form) {
        //注册用户
        int id = userService.registerUser(form.getRegisterCode(), form.getCode(),
                form.getNickname(), form.getPhoto());
        //通过id来创建token
        String token = jwtUtil.createToken(id);
        //将token存入redis中
        saveCacheToken(token,id);
        //获得用户权限
        Set<String> permsSet = userService.searchUserPermissions(id);
        //返回对象
        return R.ok("用户注册成功").put("token", token).put("permission", permsSet);

    }

    /**
     * 用户登录
     * 注意点：判定用户登陆成功之后，向客户端返回权限列表和Token令牌
     * @param form 前端传来的表单
     * @return
     */
    //??????????????????为什么使用POST请求
    @PostMapping("/login")
    @ApiOperation("登录系统")
    public R login(@Valid @RequestBody LoginForm form){
        String code = form.getCode();
        //根据临时授权字符串得到id
        int id = userService.login(code);
        //生成token
        String token = jwtUtil.createToken(id);
        //得到权限列表
        Set<String> permsSet = userService.searchUserPermissions(id);
        //保存token到redis
        saveCacheToken(token,id);
        return R.ok("登录成功").put("token", token).put("permission", permsSet);

    }

    private void saveCacheToken(String token, int userId) {
        redisTemplate.opsForValue().set(token, userId + "", cacheExpire, TimeUnit.DAYS);
    }
}
