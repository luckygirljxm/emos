package com.example.emos.wx.config.xss;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HtmlUtil;
import cn.hutool.json.JSONUtil;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.*;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

//HttpServletRequestWrapper使用了装饰器功能
public class XssHttpServletRequestWrapper extends HttpServletRequestWrapper {
    //构造器接收一下请求对象
    public XssHttpServletRequestWrapper(HttpServletRequest request) {
        super(request);
    }


    //覆盖方法
    //但凡能从请求中获得数据的方法都要覆盖
    @Override
    public String getParameter(String name) {
        //得到数据
        String value = super.getParameter(name);
        //判断数据是否有效  不为空
        if (!StrUtil.hasEmpty(value)) {
            //转义数据
            value = HtmlUtil.filter(value);
        }
        return value;
    }

    @Override
    public String[] getParameterValues(String name) {
        //得到数据
        String[] values = super.getParameterValues(name);
        //判断数据是否有效
        if (values != null) {
            //遍历数组，将每个数据进行转义
            for (int i = 0; i < values.length; i++) {
                String value = values[i];
                if (!StrUtil.hasEmpty(value)) {
                    value = HtmlUtil.filter(value);
                }
                values[i] = value;
            }
        }
        return values;
    }


    //把请求中的数据返回成一个Map对象，也要进行转义
    @Override
    public Map<String, String[]> getParameterMap() {
        //得到转义前的数据
        Map<String, String[]> parameters = super.getParameterMap();
        Map<String, String[]> map = new LinkedHashMap<>();
        if (parameters != null) {
            //遍历key来得到value
            for (String key : parameters.keySet()) {
                //得到value数组
                String[] values = parameters.get(key);
                for (int i = 0; i < values.length; i++) {
                    String value = values[i];
                    if (!StrUtil.hasEmpty(value)) {
                        value = HtmlUtil.filter(value);
                    }
                    values[i] = value;
                }
                map.put(key, values);
            }
        }
        return map;
    }

    //覆盖转义
    @Override
    public String getHeader(String name) {
        String value = super.getHeader(name);
        if (!StrUtil.hasEmpty(value)) {
            value = HtmlUtil.filter(value);
        }
        return value;

    }

    //IO流  覆盖转义

    /**
     * 最重要的方法   SpringMVC框架通过这个方法从请求里提取客户端请求的数据，然后把这些数据封装到from对象里面
     * @return
     * @throws IOException
     */
    @Override
    public ServletInputStream getInputStream() throws IOException {
        InputStream in = super.getInputStream();
        //创建字符流，因为要从in流里读取字符数据
        InputStreamReader reader = new InputStreamReader(in, Charset.forName("UTF-8"));
        StringBuffer body = new StringBuffer();
        BufferedReader buffer = new BufferedReader(reader);
        String line = buffer.readLine();
        while (line != null) {
            body.append(line);
            line = buffer.readLine();
        }
        buffer.close();
        reader.close();
        in.close();
        //数据类型的转换
        Map<String, Object> map = JSONUtil.parseObj(body.toString());
        Map<String, Object> resultMap = new HashMap(map.size());
        for (String key : map.keySet()) {
            Object val = map.get(key);
            if (map.get(key) instanceof String) {
                resultMap.put(key, HtmlUtil.filter(val.toString()));

            } else {
                resultMap.put(key, val);

            }

        }
        String str = JSONUtil.toJsonStr(resultMap);
        final ByteArrayInputStream bain = new ByteArrayInputStream(str.getBytes());
        return new ServletInputStream() {
            @Override
            public int read() throws IOException {
                return bain.read();

            }

            @Override
            public boolean isFinished() {
                return false;

            }

            @Override
            public boolean isReady() {
                return false;

            }

            @Override
            public void setReadListener(ReadListener listener) {

            }

        };

    }

}
