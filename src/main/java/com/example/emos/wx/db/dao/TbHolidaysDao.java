package com.example.emos.wx.db.dao;

import com.example.emos.wx.db.pojo.TbHolidays;
import org.apache.ibatis.annotations.Mapper;

import java.util.ArrayList;
import java.util.HashMap;

@Mapper
public interface TbHolidaysDao {
    //查询当天是否是特殊休息日
    public Integer searchTodayIsHolidays();

    //用于查询考勤   查询特定时间内 是否有特殊的节假日
    public ArrayList<String> searchHolidaysInRange(HashMap param);

}
