package com.example.emos.wx.controller;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import com.example.emos.wx.common.util.R;
import com.example.emos.wx.config.SystemConstants;
import com.example.emos.wx.config.shiro.JwtUtil;
import com.example.emos.wx.controller.from.CheckinForm;
import com.example.emos.wx.exception.EmosException;
import com.example.emos.wx.service.CheckinService;
import com.example.emos.wx.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.rmi.CORBA.Util;
import javax.validation.Valid;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

@Controller
@Api("签到模块Web接口")
@Slf4j
@RequestMapping("/checkin")
public class CheckinController {
    @Autowired
    private CheckinService checkinService;

    @Value("${emos.image-folder}")
    private String imageFolder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserService userService;

    //用于查询考勤时间常量
    @Autowired
    private SystemConstants constants;

    @GetMapping("/validCanCheckIn")
    @ApiOperation("查看用户今天是否可以签到")
    public R validCanCheckIn(@RequestHeader("token") String token) { //从请求头中获得token，因为需要userId
        int userId = jwtUtil.getUserId(token);
        // DateTime date = DateUtil.date();
        String today = DateUtil.today();
        String result = checkinService.validCanCheckIn(userId, today);
        return R.ok(result);
    }


    //保存用户签到数据，首先是需要前端传来的数据，以及
    @PostMapping("/checkin")
    @ApiOperation("签到")
    public R checkin(@RequestHeader("token") String token, @Valid CheckinForm form, @RequestParam("photo") MultipartFile file) {
        if (file == null) {
            //路径不存在
            return R.error("没有上传文件");
        }
        //得到用户的userId
        int userId = jwtUtil.getUserId(token);
        //获得文件名
        String fileName = file.getOriginalFilename().toLowerCase();
        //得到图片路径
        String path = imageFolder + "/" + fileName;
        if (!fileName.endsWith(".jpg")) {
            FileUtil.del("path");
            return R.error("必须上传JPG格式的文件");
        } else {
            //图片格式上传正确
            try {
                //这是什么意思！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！
                file.transferTo(Paths.get(path));
                //进行签到内容保存
                HashMap<String, Object> param = new HashMap<>();
                //保存前端传来的数据 保存到数据库
                param.put("userId", userId);
                param.put("path", path);
                param.put("city", form.getCity());
                param.put("district", form.getDistrict());
                param.put("address", form.getAddress());
                param.put("country", form.getCountry());
                param.put("province", form.getProvince());
                //插入到数据库
                checkinService.checkin(param);
                return R.ok("签到成功");
            } catch (IOException e) {
                log.error(e.getMessage());
                throw new EmosException("保存图片错误");
            }finally {
                //删除图片路径
                FileUtil.del(path);
            }
        }

    }


    @PostMapping("/createFaceModel")
    @ApiOperation("创建人脸模型数据")
    public R createFaceModel(@RequestParam("photo") MultipartFile file,@RequestHeader("token") String token){
        if (file == null){
            return R.error("没有上传文件");
        }
        String fileName = file.getOriginalFilename().toLowerCase();
        String path = imageFolder + "/" + fileName;
        int userId = jwtUtil.getUserId(token);
        if (!fileName.endsWith(".jpg")){
            return R.error("文件必须是JPG格式");
        }
        else{
            try {
                file.transferTo(Paths.get(path));
                checkinService.createFaceModel(userId,path);
                return R.ok("人脸建模成功");
            } catch (IOException e) {
                log.error(e.getMessage());
                throw new EmosException("图片保存错误");
            }finally {
                FileUtil.del(path);
            }
        }

    }


    /**
     * 考勤成功的页面  考勤的页面
     * @param token 从请求头中获得的用户凭证
     * @return
     */
    @GetMapping("/searchTodayCheckin")
    @ApiOperation("查询用户当日签到数据")
    public R searchTodayCheckin(@RequestHeader("token") String token){
        //获得userId
        int userId = jwtUtil.getUserId(token);
        //查询用户的签到详情  需要传到前端在前端展示
        HashMap map = checkinService.searchTodayCheckin(userId);
        map.put("attendanceTime", constants.attendanceTime);
        map.put("closingTime", constants.closingTime);
        //用户的总签到次数
        long days = checkinService.searchCheckinDays(userId);
        map.put("checkinDays",days);
        //判断员工入职时间
        DateTime hireDate = DateUtil.parseDate(userService.searchUserHiredate(userId));
        //这周开始的时间，为了判断员工是否在每周开始之后就入职
        DateTime startDate = DateUtil.beginOfWeek(DateUtil.date());
        if (startDate.isBefore(hireDate)){
            //员工在周一之后入职
            startDate = hireDate;
        }
        HashMap<String, Object> param = new HashMap<>();
        //查询每周的结束时间
        DateTime endDate = DateUtil.endOfWeek(DateUtil.date());
        param.put("startDate",startDate.toString());
        param.put("endDate",endDate.toString());
        //为什么需要把userId放进去
        param.put("userId",userId);
        //得到每周的考勤记录
        ArrayList<HashMap> list = checkinService.searchWeekCheckin(param);
        map.put("weekCheckin", list);
        return R.ok().put("result", map);
    }

}
