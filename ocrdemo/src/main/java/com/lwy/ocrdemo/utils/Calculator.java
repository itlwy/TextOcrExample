package com.lwy.ocrdemo.utils;

/**
 * Created by lwy on 2018/7/8.
 * 递推快速计算方差和均值：
 * https://blog.csdn.net/u014485485/article/details/77679669
 */

public class Calculator {
    private double m;
    private double s;
    private int N;


    public void addDataValue(double x)
    {
        N++;
        s=s+1.0*(N-1)/N*(x-m)*(x-m);
        m=m+(x-m)/N;
    }
    public double mean()
    {
        return  m;
    }
    public double var()
    {
        return s/(N-1);
    }
    public double stddev()
    {
        return Math.sqrt(this.var());
    }
}
