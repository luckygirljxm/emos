package com.example.emos.wx.db.dao;

import com.example.emos.wx.db.pojo.TbCheckin;
import org.apache.ibatis.annotations.Mapper;

import java.util.ArrayList;
import java.util.HashMap;

@Mapper
public interface TbCheckinDao {
    //查询用户当天是否在规定时间签到
    // 考勤 上班或者下班 是有考勤开始和结束时间的。 所以采用hashmap来封装需要的数据
    public Integer haveCheckin(HashMap param);
    //插入用户签到数据
    public void insert(TbCheckin entity);

    //查询用户的签到信息
    public HashMap searchTodayCheckin(int userId);

    //查询用户的签到总数
    public long searchCheckinDays(int userId);

    //查询用户某段时间内的考勤情况  为什么需要使用ArrayList集合？？？
    public ArrayList<HashMap> searchWeekCheckin(HashMap map);

}
