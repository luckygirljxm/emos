package com.example.emos.wx;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.example.emos.wx.config.SystemConstants;

import java.util.Scanner;

public class Test {

    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        int n = in.nextInt();
        int[] nums = new int[n];
        long sum = 0;
        for (int i = 0; i < nums.length; i++) {
            nums[i] = in.nextInt();
            sum += nums[i];

        }
        if (sum % 3 != 0 || n < 3){
            System.out.println(0);
        }else{
            long avg1 = sum / 3 ;
            long avg2 = 2 & avg1;
            int cnt1 = 0;
            int cnt2 = 0;
            int s = 0;
            for (int i = 0; i < nums.length - 2; i++) {
                s += nums[i];
                if (s == avg1){
                    cnt1 ++;
                }
                if (s == avg2){
                    cnt2 ++;
                }
            }
        }

    }
}
