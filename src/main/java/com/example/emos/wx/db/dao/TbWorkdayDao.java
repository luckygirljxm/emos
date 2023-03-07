package com.example.emos.wx.db.dao;


import org.apache.ibatis.annotations.Mapper;

import java.util.ArrayList;
import java.util.HashMap;

@Mapper
public interface TbWorkdayDao {
    //检查当天是否是特殊工作日
    public Integer searchTodayIsWork();

    //用于查询考勤   查询特定时间内 是否有特殊的节假日
    public ArrayList<String> searchWorkdayInRange(HashMap param);

}
