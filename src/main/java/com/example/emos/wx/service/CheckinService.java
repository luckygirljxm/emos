package com.example.emos.wx.service;

import java.util.ArrayList;
import java.util.HashMap;

public interface CheckinService {

    public String validCanCheckIn(int userId, String date);

    //这个写法是错误的，将form类作为参数。它其实是一个VO对象，要加上各种参数，而这里的参数是DAO对象，没有注解
    //将有注解的参数传给service层是恰当的，它应该是在controller层的
    //public void checkin(int userId, CheckinForm form,String path);
    public void checkin(HashMap param);

    public void createFaceModel(int userId, String path);

    public HashMap searchTodayCheckin(int userId);

    public long searchCheckinDays(int userId);

    public ArrayList<HashMap> searchWeekCheckin(HashMap param);
}
