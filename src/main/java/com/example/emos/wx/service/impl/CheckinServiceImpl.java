package com.example.emos.wx.service.impl;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateRange;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.example.emos.wx.config.SystemConstants;
import com.example.emos.wx.db.dao.*;
import com.example.emos.wx.db.pojo.TbCheckin;
import com.example.emos.wx.db.pojo.TbFaceModel;
import com.example.emos.wx.exception.EmosException;
import com.example.emos.wx.service.CheckinService;
import com.example.emos.wx.task.EmailTask;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

@Service
@Slf4j
@Scope("prototype") //为什么需要使用这个注解
public class CheckinServiceImpl implements CheckinService {
    @Autowired
    private TbHolidaysDao holidaysDao;

    @Autowired
    private TbWorkdayDao workdayDao;

    @Autowired
    private TbCheckinDao checkinDao;

    @Autowired
    private SystemConstants systemConstants;

    @Autowired
    private TbFaceModelDao faceModelDao;

    @Autowired
    private TbCityDao cityDao;

    @Autowired
    private TbUserDao userDao;

    @Autowired
    private EmailTask emailTask;

    @Value("${emos.face.createFaceModelUrl}")
    private String createFaceModelUrl;

    @Value("${emos.face.checkinUrl}")
    private String checkinUrl;

    @Value("${emos.email.hr}")
    private String hrEmail;


    /**
     * 判断当天能否进行签到
     *
     * @param userId 用户id
     * @param date   签到的时间
     * @return
     */
    @Override
    public String validCanCheckIn(int userId, String date) {
        //查询是否是特殊节假日
        boolean bool_1 = holidaysDao.searchTodayIsHolidays() != null ? true : false;
        boolean bool_2 = workdayDao.searchTodayIsWork() != null ? true : false;
        //如果上面两个结果 都为false,则今天是普通的工作日或者节假日
        //或者结果为一个是false,一个是true,不可能同时为false或者同时为true
        String type = "工作日";
        //判断当天是工作日还是节假日
        if (DateUtil.date().isWeekend()) {
            type = "节假日";
        }

        if (bool_2) {
            type = "工作日；";
        } else if (bool_1) {
            type = "节假日";
        }

        if (type.equals("节假日")) {
            return "节假日不需要考勤";
        } else {
            //是工作日，判断是否已经考勤
            //now是用来检查考勤时间的
            DateTime now = DateUtil.date();
            //考勤开始和结束时间
            String start = DateUtil.today() + " " + systemConstants.attendanceStartTime;
            String end = DateUtil.today() + " " + systemConstants.attendanceEndTime;
            DateTime attendanceStart = DateUtil.parse(start);
            DateTime attendanceEnd = DateUtil.parse(end);
            if (now.isBefore(attendanceStart)) {
                return "没有到上班考勤开始时间";
            } else if (now.isAfter(attendanceEnd)) {
                return "超过了上班考勤结束时间";
            } else {
                HashMap map = new HashMap();
                map.put("userId", userId);
                map.put("date", date);
                map.put("start", start);
                map.put("end", end);
                boolean bool = checkinDao.haveCheckin(map) != null ? true : false;
                return bool ? "今日已经考勤，不用重复考勤" : "可以考勤";
            }
        }
    }

    /**
     * 员工进行签到
     *
     * @param param 用户数据
     *              不需要返回值，因为如果报错了就直接抛出异常
     */
    @Override
    public void checkin(HashMap param) {
        //得到签到的时间
        Date d1 = DateUtil.date();
        //得到今天的日期，并且拼接常量得到考勤时间
        //上班时间
        Date d2 = DateUtil.parse(DateUtil.today() + " " + systemConstants.attendanceStartTime);
        //考勤结束时间
        Date d3 = DateUtil.parse(DateUtil.today() + " " + systemConstants.attendanceEndTime);
        int status = 1;
        //签到时间与考勤时间比较，如果在考勤时间内，status则为1，否则为2
        if (d1.compareTo(d2) <= 0) {
            //正常签到
            status = 1;
        } else if (d1.compareTo(d2) > 0 && d1.compareTo(d3) < 0) {
            //迟到
            status = 2;
        }

        //无论是否正常考勤都需要进行人脸识别
        int userId = (int) param.get("userId");
        String address = (String) param.get("address");
        String country = (String) param.get("country");
        String province = (String) param.get("province");
        String faceModel = faceModelDao.searchFaceModel(userId);
        if (faceModel == null) {
            throw new EmosException("不存在人脸模型");
        } else {
            //得到人脸照片的路径
            String path = (String) param.get("path");
            //存在人脸签到模型，进行验证模型
            HttpRequest request = HttpUtil.createPost(checkinUrl);
            request.form("photo", FileUtil.file(path), "targetModel", faceModel);
            //发送请求
            HttpResponse response = request.execute();
            if (response.getStatus() != 200) {
                log.error("人脸识别服务异常");
                throw new EmosException("人脸识别服务异常");
            }
            //人脸识别正常，还需要进行验证
            String body = response.body();
            if ("无法识别出人脸".equals(body) || "照片中存在多张人脸".equals(body)) {
                throw new EmosException(body);
            } else if ("False".equals(body)) {
                throw new EmosException("签到无效，非本人签到");
            } else if ("True".equals(body)) {
                //TODO 查询疫情风险等级
                //定义风险等级 rick = 1,2,3分别对应低 中 高风险
                int risk = 1;
                //因为查询可以为空，当不需要查询的时候，就可以传空值
                String city = (String) param.get("city");
                String district = (String) param.get("district");
                if (city != null && city.length() > 0 && district != null && district.length() > 0) {
                    //查询当时区域的风险等级
                    String code = cityDao.searchCode(city);
                    try {
                        //得到请求的url路径
                        String url = "http://m." + code + ".bendibao.com/news/yqdengji/?qu=" + district;
                        //解析路径
                        Document document = Jsoup.connect(url).get();
                        //得到标签内的数据
                        Elements elements = document.getElementsByClass(" list-detail");
                        //循环标签内的数据
                        if (elements.size() > 0) {
                            Element element = elements.get(0);
                            String result = element.select("p:last-child").text();
                            if (result.equals("中风险")) {
                                risk = 2;
                            } else if (result.equals("高风险")) {
                                risk = 3;
                                //TODO 发送警告邮件
                                //查询员工的部门以及员工姓名
                                HashMap<String, String> map = userDao.searchNameAndDept(userId);
                                String name = map.get("name");
                                String deptName = map.get("deptName");
                                deptName = deptName != null ? deptName : "";
                                //封装发送邮件内容
                                SimpleMailMessage message = new SimpleMailMessage();
                                message.setTo(hrEmail);
                                message.setSubject("员工" + name + "身处高风险疫情地区警告");
                                message.setText(deptName + "员工" + name + "，" + DateUtil.format(new Date(), "yyyy年MM月dd日") + "处于" + address + "，属于新冠疫情高风险地区，请及时与该员工联系，核实情况！");
                                emailTask.sendAsync(message);

                            }
                        }
                    } catch (IOException e) {
                        log.error("执行异常", e);
                        throw new EmosException("获取风险等级失败");
                    }
                }
                //保存签到信息
                TbCheckin entity = new TbCheckin();
                entity.setUserId(userId);
                entity.setAddress(address);
                entity.setCountry(country);
                entity.setProvince(province);
                entity.setCity(city);
                entity.setDistrict(district);
                entity.setStatus((byte) status);
                entity.setRisk(risk);
                entity.setDate(DateUtil.today());
                entity.setCreateTime(d1);
                checkinDao.insert(entity);
            }
        }
    }

    /**
     * 创建人脸签到模型
     *
     * @param userId 用户userId
     * @param path   模型保存路径
     */
    @Override
    public void createFaceModel(int userId, String path) {
        HttpRequest request = HttpUtil.createPost(createFaceModelUrl);
        HttpRequest photo = request.form("photo", FileUtil.file(path));
        //提交请求
        HttpResponse response = request.execute();
        String body = response.body();
        if ("无法识别出人脸".equals(body) || "照片中存在多张人脸".equals(body)) {
            throw new EmosException(body);
        } else {
            //可以正常存入模型
            TbFaceModel model = new TbFaceModel();
            model.setUserId(userId);
            model.setFaceModel(body);
            faceModelDao.insert(model);
        }
    }

    /**
     * 查询用户签到详情
     *
     * @param userId
     * @return
     */
    @Override
    public HashMap searchTodayCheckin(int userId) {
        HashMap hashMap = checkinDao.searchTodayCheckin(userId);
        return hashMap;
    }

    /**
     * 查询用户总签到天数
     *
     * @param userId
     * @return
     */
    @Override
    public long searchCheckinDays(int userId) {
        long days = checkinDao.searchCheckinDays(userId);
        return days;
    }



    /**
     * 查询用户某段时间（每周）的签到详情-->主要用于统计
     * 这个方法中需要考虑，如果用户不是在周一入职的，而在入职之前的考勤是不需要计算的，并且默认的
     * 考勤是周一到周五。如果有特殊的节日也是需要特殊记录是否是缺勤还是正常考勤
     *
     * @param param
     * @return
     */
    @Override
    public ArrayList<HashMap> searchWeekCheckin(HashMap param) {
        //查询 连续时间内的特殊节假日
        ArrayList<String> holidayList = holidaysDao.searchHolidaysInRange(param);
        //查询连续时间内的特殊工作日
        ArrayList<String> workdayList = workdayDao.searchWorkdayInRange(param);
        //查询出本周的考勤记录
        ArrayList<HashMap> checkinList = checkinDao.searchWeekCheckin(param);
        //得到检查考勤开始的时间
        DateTime startDate = DateUtil.parseDate(param.get("startDate").toString());
        //得到检查考勤的结束时间
        DateTime endDate = DateUtil.parseDate(param.get("endDate").toString());
        //得到七天考勤的连续对象
        DateRange range = DateUtil.range(startDate, endDate, DateField.DAY_OF_MONTH);

        ArrayList list = new ArrayList();
        //循环遍历考勤对象
        range.forEach(one -> {
            //得到当前对象的String类型的时间，便于后续的操作
            //得到日期格式
            String date = one.toString("yyyy-MM-dd");
            //判断当天是工作日还是节假日
            String type = "工作日";
            if (one.isWeekend()) {
                type = "节假日";
            }
            if (holidayList != null && holidayList.contains(date)) {
                type = "节假日";
            } else if (workdayList != null && workdayList.contains(workdayList)) {
                type = "工作日";
            }

            //插叙考勤状态
            //未来的状态都空字符串，因为未来的时间没到，不能旷工
            //签到状态默认为空值
            String status = "";
            //如果查询的当天是工作日，并且是过去的时间
            if (type.equals("工作日") && DateUtil.compare(one, DateUtil.date()) <= 0) {
                //默认status的值
                status = "缺勤";
                //查询当天的签到结果 只需要遍历已经签到的的天数，如果有的话就是签到过的，没有的的话就是缺勤
                for (HashMap<String, String> map : checkinList) {
                    boolean flag = false;
                    if (map.containsValue(date)) {
                        //用于判断现在的时间是否已经超过考勤时间，如果没有超过，那么今天还是可以签到的，不算缺勤
                        //查到数据
                        status = map.get("status");
                        flag = true;
                        break;

                    }
                    //判断查询考勤时是否已经过了考勤时间
                    //得到考勤结束时间
                    DateTime endTime = DateUtil.parse(DateUtil.today() + " " + systemConstants.attendanceEndTime);
                    String today = DateUtil.today();
                    //没有到当天的考勤结束时间
                    if (date.equals(today) && DateUtil.date().isBefore(endTime) && flag == false) {
                        status = "";
                    }
                }
            }

            HashMap map = new HashMap();
            map.put("date", date);
            map.put("status", status);
            map.put("type", type);
            map.put("day", one.dayOfWeekEnum().toChinese("周"));
            list.add(map);

        });

        return list;
    }


}
