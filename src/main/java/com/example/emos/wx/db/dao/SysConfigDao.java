package com.example.emos.wx.db.dao;

import com.example.emos.wx.db.pojo.SysConfig;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SysConfigDao {
    //查询系统常量数据
    public List<SysConfig> selectAllParam();
}
