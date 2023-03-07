package com.example.emos.wx.db.dao;

import com.example.emos.wx.db.pojo.TbFaceModel;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TbFaceModelDao {
    //查询人脸模型数据
    public String searchFaceModel(int userId);
    //插入数据
    public int insert(TbFaceModel tbFaceModel);

    //删除数据
    public int deleteFaceModel(int userId);


}
