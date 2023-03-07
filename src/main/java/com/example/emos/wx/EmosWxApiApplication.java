package com.example.emos.wx;

import cn.hutool.core.util.StrUtil;
import com.example.emos.wx.config.SystemConstants;
import com.example.emos.wx.db.dao.SysConfigDao;
import com.example.emos.wx.db.pojo.SysConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

import javax.annotation.PostConstruct;
import java.io.File;
import java.lang.reflect.Field;
import java.util.List;

@SpringBootApplication
@ServletComponentScan
@Slf4j
@EnableAsync
public class EmosWxApiApplication {
    @Autowired
    private SysConfigDao sysConfigDao;

    @Autowired
    private SystemConstants constants;

    @Value("${emos.image-folder}")
    private String imageFolder;


    public static void main(String[] args) {
        SpringApplication.run(EmosWxApiApplication.class, args);
    }

    /**
     * 初始化系统常量数据
     */
    @PostConstruct
    public void init(){
        List<SysConfig> list = sysConfigDao.selectAllParam();
        //遍历list
        list.forEach(one->{
            //得到key
            String key = one.getParamKey();
            //将key转为驼峰命名
            key = StrUtil.toCamelCase(key);
            //得到value
            String value = one.getParamValue();
            //通过反射来得到spring框架中的sysConfig对象
            try {
                //field得到的是对应key的字段
                Field field = constants.getClass().getDeclaredField(key);
                //将这个字段塞入
                field.set(constants, value);
            } catch (Exception e) {
                log.error("执行异常");
            }
            new File(imageFolder).mkdir();
        });
    }
}
